package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 订单提交
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        //处理业务异常（地址簿为空，购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //地址簿为空，抛异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //User_id可以通过拦截器得到
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0) {
            //购物车为空，抛异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //对两张表操作order和order_detail
        Orders orders = new Orders();
        //addressBookId,payMethod,amount,remark,estimatedDeliveryTime,delivery_status,packAmount,tablewareNumber,tablewareStatus
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        //Status订单提交阶段默认设为待付款Orders.PENDING_PAYMENT = 1
        orders.setStatus(Orders.PENDING_PAYMENT);
        //设置userid
        orders.setUserId(userId);
        //Order_time自动创建为now
        orders.setOrderTime(LocalDateTime.now());
        //Pay_status订单提交阶段默认设为未支付Orders.UN_PAID = 0
        orders.setPayStatus(Orders.UN_PAID);
        //Number订单号设置为当前系统时间的时间戳
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        //phone, address，consignee可以根据OrdersSubmitDTO.addressBookId从address表中查到
        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        //其余参数默认为null: Checkout_Time,Cancel_reason,Rejection_reason,Cancel_time,Delivery_time
        //插入order表中
        orderMapper.insert(orders);

        //order_detail
        List<OrderDetail> orderDetailList = new ArrayList<OrderDetail>();
        //根据userId从购物车表中查到name，image，dish_id，setmeal_id，dish_flavor，number，amount
        for (ShoppingCart shoppingCart1 : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart1, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        //批量插入order_detail
        orderDetailMapper.insertBatch(orderDetailList);

        //下单成功之后清空购物车
        shoppingCartMapper.clean(userId);

        //封装返回参数
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setOrderNumber(orders.getNumber());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        orderSubmitVO.setOrderTime(orders.getOrderTime());

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //查询订单信息
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        Orders ordersCancel = new Orders();
        ordersCancel.setStatus(Orders.CANCELLED);
        BeanUtils.copyProperties(ordersCancelDTO, ordersCancel);
        //设置订单取消时间
        ordersCancel.setCancelTime(LocalDateTime.now());
        orderMapper.update(ordersCancel);
        //若用户已支付，则退款
        if (orders.getPayStatus().equals(Orders.PAID)) {
            //退款
        }
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        orderStatisticsVO.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        orderStatisticsVO.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        return orderStatisticsVO;
    }

    /**
     * 完成订单
     * @param id
     */
    public void complete(Long id) {
        //查询订单信息,判断是否为派送中
        Orders orders = orderMapper.getById(id);
        Integer status = orders.getStatus();
        //只有状态为派送中，才可以执行订单完成操作
        if(status.equals(Orders.DELIVERY_IN_PROGRESS)) {
            Orders ordersUpdate = new Orders();
            ordersUpdate.setStatus(Orders.COMPLETED);
            ordersUpdate.setId(id);
            //设置送达时间
            ordersUpdate.setDeliveryTime(LocalDateTime.now());
            orderMapper.update(ordersUpdate);
        }else {
            //否则抛异常
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        //查询订单信息
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        Integer status = orders.getStatus();
        //只有状态为待接单，才可以执行拒单操作
        if(status.equals(Orders.TO_BE_CONFIRMED)) {
            Orders ordersReject = new Orders();
            ordersReject.setStatus(Orders.CANCELLED);
            BeanUtils.copyProperties(ordersRejectionDTO, ordersReject);
            //设置订单取消时间
            ordersReject.setCancelTime(LocalDateTime.now());
            orderMapper.update(ordersReject);
            //若用户已支付，则退款
            if (orders.getPayStatus() == Orders.PAID) {
                //退款
            }
        }else {
            //否则抛异常
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersConfirm = new Orders();
        BeanUtils.copyProperties(ordersConfirmDTO, ordersConfirm);
        ordersConfirm.setStatus(Orders.CONFIRMED);
        orderMapper.update(ordersConfirm);
    }

    /**
     * 查询订单信息
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 派送订单
     * @param id
     */
    public void delivery(Long id) {
        //查询订单信息
        Orders orders = orderMapper.getById(id);
        Integer status = orders.getStatus();
        //只有状态为待派送，才可以执行派送订单操作
        if(status.equals(Orders.CONFIRMED)) {
            Orders ordersDelivery = new Orders();
            ordersDelivery.setStatus(Orders.DELIVERY_IN_PROGRESS);
            ordersDelivery.setId(id);
            orderMapper.update(ordersDelivery);
        }else {
            //否则抛异常
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    /**
     * 订单条件搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page);
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 根据orders获取orderVOList
     * @param page
     * @return
     */
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<Orders> ordersList = page.getResult();
        List<OrderVO> orderVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getById(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


}

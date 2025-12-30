package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
}

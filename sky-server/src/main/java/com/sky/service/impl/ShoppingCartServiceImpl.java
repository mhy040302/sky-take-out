package com.sky.service.impl;

import com.sky.annotation.AutoFill;
import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.enumeration.OperationType;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.properties.JwtProperties;
import com.sky.service.ShoppingCartService;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 购物车新增
     * @param shoppingCartDTO
     */
    @Transactional
    public void add(ShoppingCartDTO shoppingCartDTO) {
        // user_id,通过拦截器得到
        Long userId = BaseContext.getCurrentId();

        //（请求参数） dish_id, setmeal_id, dish_flavor
        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        //判断添加到购物车当中的商品是否已经存在
        List<ShoppingCart> shoppingCartExist = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartExist.size() == 0) {
            //若不存在则insert操作number设为1
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            //若新增的是菜品
            if(dishId != null) {
                //根据请求参数从dish表中查找得到冗余字段
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            }
            //若新增的是套餐
            if(setmealId != null) {
                //根据请求参数从setmeal表中查找得到冗余字段
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCartMapper.insert(shoppingCart);
        }
        if (shoppingCartExist.size() > 0) {
            //若存在则update操作number+1
            ShoppingCart shoppingCart1 = shoppingCartExist.get(0);
            shoppingCart1.setNumber(shoppingCart1.getNumber() + 1);
            shoppingCartMapper.updateNumber(shoppingCart1);
        }
    }

    /**
     * 展示购物车
     * @return
     */
    public List<ShoppingCart> list(Long userId) {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查找
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 清空购物车
     * @param userId
     */
    public void clean(Long userId) {

        shoppingCartMapper.clean(userId);

    }

    /**
     * 减去购物车某一商品
     * @param shoppingCartDTO
     * @return
     */
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(userId);

        //查找商品
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        ShoppingCart shoppingCart1 = shoppingCartList.get(0);
        Integer number = shoppingCart1.getNumber();

        //number，若大于1，则number-1；若等于1，则删除
        if(number > 1){
            shoppingCart1.setNumber(number - 1);
            shoppingCartMapper.updateNumber(shoppingCart1);
        }
        if(number == 1){
            shoppingCartMapper.sub(shoppingCart1);
        }

    }

}

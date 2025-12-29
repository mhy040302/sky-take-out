package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ShoppingCartService {

    /**
     * 购物车新增
     * @param shoppingCartDTO
     */
    void add(ShoppingCartDTO shoppingCartDTO);

    /**
     * 展示购物车
     * @return
     */
    List<ShoppingCart> list(Long userId);

    /**
     * 清空购物车
     * @param userId
     */
    void clean(Long userId);

    /**
     * 减去购物车某一商品
     * @param shoppingCartDTO
     * @return
     */
    void sub(ShoppingCartDTO shoppingCartDTO);
}

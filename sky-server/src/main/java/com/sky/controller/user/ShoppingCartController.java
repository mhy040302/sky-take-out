package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "C端-购物车管理")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 购物车新增
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("购物车新增")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("购物车新增：{}", shoppingCartDTO);

        shoppingCartService.add(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 展示购物车
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("展示购物车")
    public Result<List<ShoppingCart>> list(){
        log.info("展示购物车...");
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartService.list(userId);
        return Result.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result clean(){
        log.info("清空购物车...");
        Long userId = BaseContext.getCurrentId();
        shoppingCartService.clean(userId);
        return Result.success();
    }

    /**
     * 减去购物车某一商品
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("减去购物车某一商品")
    public Result sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("减去购物车某一商品{}", shoppingCartDTO);
        shoppingCartService.sub(shoppingCartDTO);
        return Result.success();
    }
}

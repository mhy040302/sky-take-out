package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
@Api(tags = "C端-套餐浏览接口")

public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 条件查询
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    @Cacheable(cacheNames = "setmealCache", key = "#categoryId")
    public Result<List<Setmeal>> list(Long categoryId) {

        List<Setmeal> list = setmealService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 根据套餐id查询套餐中包含的菜品
     * @param setmealId
     * @return
     */
    @GetMapping("/list/{setmealId}")
    @ApiOperation("根据套餐id查询套餐中包含的菜品")
    public Result<List<DishItemVO>> getDishItemBySetmealId(@PathVariable Long setmealId) {
        log.info("查询套餐中包含的菜品：{}", setmealId);
        List<DishItemVO> dishItemVOList = setmealService.getDishItemBySetmealId(setmealId);
        return Result.success(dishItemVOList);
    }
}

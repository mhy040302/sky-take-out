package com.sky.service;

import com.sky.entity.Setmeal;
import com.sky.vo.DishItemVO;

import java.util.List;


public interface SetmealService {

    /**
     * 根据分类id查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据套餐id查询套餐中包含的菜品
     * @param setmealId
     * @return
     */
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);
}

package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品
     * @param dishDTO
     */
    void addDishWithFlavor(DishDTO dishDTO);

    /**
     * 菜品数据分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品及口味
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品信息
     * @param dishDTO
     */
    void update(DishDTO dishDTO);

    /**
     * 根据分类查询菜品及口味
     * @param categoryId
     * @return
     */
    List<DishVO> getByCategoryIdWithFlavor(Long categoryId);
}

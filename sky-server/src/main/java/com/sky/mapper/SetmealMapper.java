package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    /**
     * 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    @Select("select * from setmeal where category_id = #{categoryId}")
    List<Setmeal> list(Long categoryId);


    /**
     * 查询套餐中包含的菜品
     * @param setmealId
     * @return
     */
    @Select("select sd.copies, sd.name, d.image, d.description " +
            "from setmeal_dish sd left outer join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);

    /**
     * 修改setmeal
     * @param setmeal
     */
    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    @Update("update setmeal set status = #{status} where id = #{id};")
    void startOrStop(Integer status, Long id);

    /**
     * 批量删除setmeal
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 新增setmeal
     * @param setmeal
     */
    @AutoFill(OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 根据id查询setmeal
     * @param id
     * @return
     */
    @Select("select * from setmeal where id = #{id}")
    Setmeal getById(Long id);
}

package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;


    /**
     * 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    public List<Setmeal> list(Long categoryId) {
        List<Setmeal> list = setmealMapper.list(categoryId);
        return list;
    }

    /**
     * 根据套餐id查询套餐中包含的菜品
     * @param setmealId
     * @return
     */
    public List<DishItemVO> getDishItemBySetmealId(Long setmealId) {
        List<DishItemVO> dishItemVOList = setmealMapper.getDishItemBySetmealId(setmealId);
        return dishItemVOList;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    public void updateSetmeal(SetmealDTO setmealDTO) {
        //涉及两张表setmeal和setmeal_dish
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //清空setmeal_dish中该套餐的信息
        List<Long> ids = new ArrayList<>();
        ids.add(setmeal.getId());
        setmealDishMapper.deleteBatch(ids);

        //setmeal_dish批量新增
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {

        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());;
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);

        return new PageResult(page.getPages(), page.getResult());
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     * @return
     */
    public void startOrStop(Integer status, Long id) {

        setmealMapper.startOrStop(status, id);

    }

    /**
     * 套餐批量删除
     * @param ids
     * @return
     */
    public void deleteSetmeal(List<Long> ids) {
        //对两张表批量删除
        setmealMapper.deleteBatch(ids);
        setmealDishMapper.deleteBatch(ids);
    }

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    public void addSetmeal(SetmealDTO setmealDTO) {
        //对两张表新增
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealDTO.getId());
            });
        }
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    public SetmealVO getById(Long id) {

        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setmealMapper.getById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);

        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }


}

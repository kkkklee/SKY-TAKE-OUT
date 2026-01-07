package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;


public interface DishService {
    /**
     *菜品分页查询
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);
    /**
     * 删除菜品
     * @param ids
     * @return
     */
    void deleteDishById(List<Long> ids);
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    void save(DishDTO dishDTO);
    /**
     * 根据id查询菜品信息
     * param id
     * return
     */
    DishVO getDishById(Long id);
    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据分类id查询菜品数据
     * @param categoryId
     * @return
     */
    List<Dish> listByCategoryId(Long categoryId,String name);
    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

}

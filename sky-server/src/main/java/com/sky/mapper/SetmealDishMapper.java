package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    //检查菜品是否与套餐捆绑，有一个绑定就不删除
    @Select("select count(id) from setmeal_dish where dish_id in (#{ids})")
    Integer countSetmealAndDish(List<Long> ids);

    //批量插入套餐菜品关系
    void insertBatch(List<SetmealDish> setmealDishes);

    //批量删除套餐菜品表中的信息
    void deleteBySetmealIds(@Param("setmealIds") List<Long> setmealIds);

    //根据套餐id查询套餐菜品关系
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);





}

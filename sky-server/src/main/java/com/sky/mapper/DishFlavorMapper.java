package com.sky.mapper;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量删除菜品对应的口味数据
     * @param dishIds
     * @return
     */
    /*@Delete("delete from dish_flavor where dish_id = #{id}")*/
    void deleteDishFlavorByDishIds(List<Long> dishIds);

    /**
     * 批量插入菜品对应的口味数据
     * @param dishFlavors
     * @return
     */
    void insertBatch(@Param("dishFlavors")List<DishFlavor> dishFlavors);

    /**
     * 根据菜品id查询对应的口味数据
     * @param id
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> getByDishId(Long id);



}

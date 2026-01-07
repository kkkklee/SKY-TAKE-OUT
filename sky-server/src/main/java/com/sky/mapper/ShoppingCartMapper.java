package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    //查询购物车
    List<ShoppingCart> list(ShoppingCart shoppingCart);
    //更改购物车菜品的数量
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ShoppingCart cart);
    //
    void insert(ShoppingCart shoppingCart);
    //清空购物车
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void clean(Long userId);

    //通过ID删除商品
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(ShoppingCart shoppingCart);
    //批量加入购物车
    void insertBatch(List<ShoppingCart> shoppingCartList);
}

package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     *菜品分页查询
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询：{}", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @Transactional//涉及多表删除
    @Override
    public void deleteDishById(List<Long> ids) {


        //检查菜品是否在售,有一个在售就不删除
        Integer count = dishMapper.countDishStatus(ids, StatusConstant.ENABLE);
        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        //检查菜品是否关联了套餐，有一个关联就不能删除
        Integer countSetmealAndDish = setmealDishMapper.countSetmealAndDish(ids);
        if (countSetmealAndDish > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        log.info("删除菜品：{}", ids);
            dishMapper.deleteDishByIds(ids);
            dishFlavorMapper.deleteDishFlavorByDishIds(ids);
        }
    /**
     * 新增菜品
     * @param dishDTO
     */
    @Override
    public void save(DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size()>0){
            flavors.forEach(dishFlavor ->{
                dishFlavor.setDishId(dishId);
            } );
            dishFlavorMapper.insertBatch(flavors);
        }

    }
    /**
     * 根据id查询菜品信息
     * param id
     * return
     */
    @Override
    public DishVO getDishById(Long id) {
        //查询菜品的基本信息
        log.info("根据id查询菜品信息：{}", id);
        Dish dish = dishMapper.getById(id);
        DishVO dishvo = new DishVO();
        BeanUtils.copyProperties(dish,dishvo);
        //查询菜品的分类名称
        log.info("根据菜品id查询分类");
        Long categoryId = dish.getCategoryId();//先从菜品表拿到分类id
        Category category = categoryMapper.getById(categoryId);
        dishvo.setCategoryName(category.getName());
        //查询菜品的口味信息
        log.info("根据菜品id查询口味信息");
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        dishvo.setFlavors(dishFlavors);
        return dishvo;
    }
    /**
     * 修改菜品及口味信息
     * @param dishDTO
     * @return
     */
    @Transactional
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //先修改菜品的基本信息
        dishMapper.update(dish);
        //修改菜品的口味信息,单独用改不好操作，先删除后插入
        List<Long> ids = Arrays.asList(dishDTO.getId());//只有一个id
        dishFlavorMapper.deleteDishFlavorByDishIds(ids);

        //这里插入的时候参照新增菜品的时候，注意要添加dishId！！！
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size()>0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        log.info("起售停售：{}", id);
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    //这里的条件查询用dish传递
    @Override
    public List<Dish> listByCategoryId(Long categoryId,String name) {
        log.info("根据分类id查询菜品，{}",categoryId);
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                 .name(name)
                .status(StatusConstant.ENABLE)
                .build();
        List<Dish> list = dishMapper.list(dish);
       return list;
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}



package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
/*import jdk.javadoc.internal.doclets.toolkit.taglets.UserTaglet;*/
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<Setmeal> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }
    /**
     * 新增套餐
     * @Param setmealDTO
     * @return
     */
    @Transactional
    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        //插入套餐的基本信息
        log.info("插入套餐基本信息:{}", setmealDTO);
        setmealMapper.insert(setmeal);

        //添加套餐菜品
        log.info("添加套餐相关菜品");
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmeal.getId());
        });
        setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
    }
    /**
     * 起售停售
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //起售前得先检查套餐菜品里是否有菜品停售
        if(status == 1){
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
            for (SetmealDish setmealDish : setmealDishes) {
                Dish dish = dishMapper.getById(setmealDish.getDishId());
                if(dish.getStatus() == 0){
                    throw new RuntimeException("起售失败，套餐内包含停售菜品");
                }
            }
        }
        //修改套餐状态
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }
    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @Transactional
    @Override
    public void delete(List<Long> ids) {
        //删除套餐的基本信息
        setmealMapper.deleteByIds(ids);
        //删除套餐菜品表中的绑定信息
        setmealDishMapper.deleteBySetmealIds(ids);

    }
    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @Override
    public SetmealVO getDishById(Long id) {
        //查询套餐基本信息
       Setmeal setmeal = setmealMapper.getBySetmealId(id);
       SetmealVO setmealVO = new SetmealVO();
       BeanUtils.copyProperties(setmeal,setmealVO);

       //用套餐表的分类id去获取分类名称
       Long categoryId = setmeal.getCategoryId();
       Category category = categoryMapper.getById(categoryId);
       setmealVO.setCategoryName(category.getName());
       //查询套餐菜品表的信息
       List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
       setmealVO.setSetmealDishes(setmealDishes);
       return setmealVO;
    }
    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @Transactional
    @Override
    public void update(SetmealDTO setmealDTO) {
        //更新菜品的基本信息
        log.info("更新菜品基本信息:{}", setmealDTO);
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        //更新套餐菜品信息,先删除后插入
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });
        log.info("更新套餐菜品信息，根据套餐ids先删除套餐相关菜品信息:");
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmealDTO.getId()));
        log.info("插入新的套餐菜品信息:{}",setmealDishes);
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}

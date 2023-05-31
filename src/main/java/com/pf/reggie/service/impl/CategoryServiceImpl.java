package com.pf.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pf.reggie.common.CustomException;
import com.pf.reggie.entity.Category;
import com.pf.reggie.entity.Dish;
import com.pf.reggie.entity.Setmeal;
import com.pf.reggie.mapper.CategoryMapper;
import com.pf.reggie.service.CategoryService;
import com.pf.reggie.service.DishService;
import com.pf.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前需要进行判断
     * @param id
     */
    @Override
    public void remove(Long id) {
        //查询当前分类是否关联了菜品，如果已经关联，抛出有一个业务异常
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Dish::getCategoryId,id);
        int count1 = dishService.count(queryWrapper1);
        if (count1 > 0){
            //已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }
        //查询当前分类是否关联了套餐，如果已经关联，抛出有一个业务异常
        LambdaQueryWrapper<Setmeal> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(Setmeal::getCategoryId,id);
        int count2 = setmealService.count(queryWrapper2);
        if (count2 > 0){
            //已经关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }
        //正常删除分类
        super.removeById(id);
    }
}

package com.pf.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pf.reggie.common.R;
import com.pf.reggie.dto.DishDto;
import com.pf.reggie.entity.Category;
import com.pf.reggie.entity.Dish;
import com.pf.reggie.entity.DishFlavor;
import com.pf.reggie.service.CategoryService;
import com.pf.reggie.service.DishFlavorService;
import com.pf.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        dishService.saveWithFlavor(dishDto);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(name != null,Dish::getName,name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();

        List<DishDto> list = new ArrayList<>();

        for (Dish record :records) {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(record,dishDto);
            Long categoryId = record.getCategoryId();
            //根据Id查询分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            list.add(dishDto);
        }

        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        dishService.updateWithFlavor(dishDto);

        //清理所有菜品的缓存数据
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);

        //清理某个分类下面的菜品缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        List<DishDto> dtoList = null;

        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();

        //先从redis中获取缓存数据
        dtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，直接返回，无需查询数据库
        if (dtoList != null){
            return R.success(dtoList);
        }

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null,Dish::getCategoryId,dish.getCategoryId());
        //查询状态为1(起售)的dish
        queryWrapper.eq(Dish::getStatus,1);
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);
        dtoList = new ArrayList<>();

        for (Dish dishExample : list) {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dishExample,dishDto);

            Long categoryId = dishExample.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            //当前菜品Id
            Long dishId = dishExample.getId();
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId,dishId);
            List<DishFlavor> dishFlavors = dishFlavorService.list(lambdaQueryWrapper);

            dishDto.setFlavors(dishFlavors);

            dtoList.add(dishDto);
        }

        //如果不存在，需要查询数据库，将查询到的菜品数据缓存到redis
        redisTemplate.opsForValue().set(key,dtoList,60, TimeUnit.MINUTES);

        return R.success(dtoList);
    }

    /**
     * 菜品的停售与启售功能
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> changeStatus(@PathVariable int status,Long[] ids){
        //1.将ids数组转为list
        List<Long> idsList = Arrays.asList(ids);
        //2.设置更新条件
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId,idsList);
        updateWrapper.set(Dish::getStatus,status);
        dishService.update(updateWrapper);

        return R.success("状态更新完毕");
    }
}

package com.pf.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pf.reggie.entity.Category;

public interface CategoryService extends IService<Category> {

    public void remove(Long id);
}

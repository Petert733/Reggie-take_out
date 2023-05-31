package com.pf.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pf.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {
    //用户下单
    public void submit(Orders order);
}

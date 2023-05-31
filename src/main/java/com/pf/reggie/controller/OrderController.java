package com.pf.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pf.reggie.common.BaseContext;
import com.pf.reggie.common.R;
import com.pf.reggie.dto.OrdersDto;
import com.pf.reggie.entity.OrderDetail;
import com.pf.reggie.entity.Orders;
import com.pf.reggie.entity.User;
import com.pf.reggie.service.OrderDetailService;
import com.pf.reggie.service.OrderService;
import com.pf.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserService userService;

    /**
     * 用户下单
     * @param order
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders order){
        orderService.submit(order);
        return R.success("下单成功");
    }

    /**
     * 分页条件查询订单数据
     * @param page
     * @param pageSize
     * @param number
     * @param beginTime
     * @param endTime
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number,String beginTime,String endTime){
        //1.将String类型的beginTime和endTime转为LocalDateTime类型
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localBeginTime = null;
        LocalDateTime localEndTime = null;
        if (beginTime != null){
            localBeginTime = LocalDateTime.parse(beginTime, df);
        }
        if (endTime != null){
            localEndTime = LocalDateTime.parse(endTime, df);
        }

        //2.构造分页
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);

        //3.构造查询条件
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(number != null,Orders::getNumber,number);
        queryWrapper.ge(localBeginTime != null,Orders::getOrderTime,localBeginTime);
        queryWrapper.le(localEndTime != null,Orders::getCheckoutTime,localEndTime);

        //4.查询Orders的分页数据
        orderService.page(pageInfo,queryWrapper);

        //5.对象拷贝
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> dtoList = new ArrayList<>();

        for (Orders record : records) {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(record,ordersDto);
            Long userId = record.getUserId();
            User user = userService.getById(userId);
            String userName = user.getName();
            ordersDto.setUserName(userName);
            LambdaQueryWrapper<OrderDetail> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(OrderDetail::getOrderId,record.getId());
            List<OrderDetail> list = orderDetailService.list(lambdaQueryWrapper);
            ordersDto.setOrderDetails(list);
            dtoList.add(ordersDto);
        }
        ordersDtoPage.setRecords(dtoList);

        return R.success(ordersDtoPage);
    }

    /**
     * 修改订单状态
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> changeStatus(@RequestBody Orders orders){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getId,orders.getId());
        orderService.update(orders,queryWrapper);

        return R.success("修改订单状态成功");
    }

    /**
     * 分页查询订单数据
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page,int pageSize){
        //1.构造分页
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        Page<OrdersDto> ordersDtoPage = new Page<>(page,pageSize);

        //2.获得现在登录的userId
        Long userId = BaseContext.getCurrentId();

        //3.构造查询条件
        LambdaQueryWrapper<Orders> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Orders::getUserId,userId);
        queryWrapper1.orderByDesc(Orders::getOrderTime);

        //4.获得该userId的最新一条订单
        orderService.page(pageInfo,queryWrapper1);

        //5.拷贝数据
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");
        List<Orders> orders = pageInfo.getRecords();
        List<OrdersDto> ordersDtoList = new ArrayList<>();
        for (Orders order : orders) {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(order,ordersDto);
            LambdaQueryWrapper<OrderDetail> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(OrderDetail::getOrderId,order.getId());
            List<OrderDetail> orderDetails = orderDetailService.list(queryWrapper2);
            ordersDto.setOrderDetails(orderDetails);
            ordersDtoList.add(ordersDto);
        }
        ordersDtoPage.setRecords(ordersDtoList);

        return R.success(ordersDtoPage);
    }


}

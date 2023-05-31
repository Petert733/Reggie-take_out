package com.pf.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.pf.reggie.common.BaseContext;
import com.pf.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否已经完成登录
 */
@WebFilter(filterName = "loginCheckFilter",urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {

    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //获取本次请求的uri
        String requestURI = request.getRequestURI();

        log.info("拦截到请求：{}",requestURI);

        //设置不拦截的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/user/sendMsg",
                "/user/login"
        };

        //判断本次请求是否需要处理
        boolean check = check(requestURI, urls);

        //如果不需要处理，直接放行
        if (check){
            log.info("本次请求{}不需要处理",requestURI);
            chain.doFilter(request,response);
            return;
        }

        //判断登录状态，如果已登录，则直接放行
        if (request.getSession().getAttribute("employee") != null){
            log.info("用户已登录，用户ID为：{}",request.getSession().getAttribute("employee"));

            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            chain.doFilter(request,response);
            return;
        }

        //4-2、判断登录状态，如果已登录，则直接放行
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登录，用户id为：{}",request.getSession().getAttribute("user"));

            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            chain.doFilter(request,response);
            return;
        }

        //如果未登录，则返回未登录信息
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     * @param requestURI
     * @param urls
     * @return
     */
    public boolean check(String requestURI,String[] urls){
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match == true){
                return true;
            }
        }
        return false;
    }
}

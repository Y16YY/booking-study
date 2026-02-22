package com.bookingstudyserve.interceptor; // 你的包名

import com.bookingstudyserve.common.UserContext;
import com.bookingstudyserve.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的 token
        String token = request.getHeader("Authorization");
        System.out.println("拦截器捕捉到请求，路径: " + request.getRequestURI() + "，Token: " + token);

        // 2. 校验 token 是否为空或无效 (这里简单写，具体看你之前的逻辑)
        if (token == null || "".equals(token)) {
            response.setStatus(401);
            return false;
        }

        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            // 3. 解析 Token (根据你自己的 JwtUtil 实现来写)
            Claims claims = JwtUtil.parseToken(token);
            String userId = claims.get("userId", String.class); // 假设 token 主体存的是 userId

            // === 核心步骤：把 ID 存入 ThreadLocal ===
            UserContext.setUserId(userId);

            return true; // 放行
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // === 核心步骤：请求结束时，必须清除数据 ===
        // 否则在线程池环境下可能会导致数据混乱（读取到上一个用户的 ID）
        UserContext.remove();
    }
}
package com.bookingstudyserve.controller.admin;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.service.ISysUserService;
import com.bookingstudyserve.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AuthController {

    @Autowired
    private ISysUserService sysUserService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody SysUser loginDto) {
        System.out.println("登录请求：" + loginDto);
        // 1. 调用 Service 校验账号密码
        SysUser user = sysUserService.login(loginDto.getStudentId(), loginDto.getPassword());

        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 2. 权限校验：确保是管理员身份 (role = 3)
        if (user.getRole() != 3) {
            return Result.error("权限不足，该账号非管理员");
        }

        // 3. 准备载荷 (Claims)，将 userId 存入 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId().toString()); // 将 ID 放入 JWT 载荷

        // 4. 生成 JWT Token
        String token = JwtUtil.createToken(claims);

        // 5. 封装返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getRealName());
        data.put("role", user.getRole());

        return Result.success(data);
    }
}
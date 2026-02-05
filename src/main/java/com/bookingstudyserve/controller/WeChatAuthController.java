package com.bookingstudyserve.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.service.ISysUserService;
import com.bookingstudyserve.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class WeChatAuthController {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Autowired
    private ISysUserService sysUserService;

    @PostMapping("/login")

    public Result<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");
        if (code == null) return Result.error("Code不能为空");

        // 1. 拼接请求微信 API 的 URL
        String url = "https://api.weixin.qq.com/sns/jscode2session?" +
                "appid=" + appid +
                "&secret=" + secret +
                "&js_code=" + code +
                "&grant_type=authorization_code";

        // 2. 发起 HTTP 请求 (这里用 RestTemplate 举例)
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);

        // 3. 解析微信返回的数据
        JSONObject json = JSON.parseObject(response); // 假设用了 FastJson
        String openid = json.getString("openid");

        if (openid == null) return Result.error("登录失败，微信未返回OpenID");
        log.info("登录的openid{}" , openid);
        // A. 去数据库查这个 openid 是否存在
        SysUser user = sysUserService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenid, openid));
        // B. 如果不存在，注册为新用户
        if (user == null) {
            user = new SysUser();
            // 生成一个随机且唯一的 ID (去掉横线，保证32位以内)
            String newUserId = UUID.randomUUID().toString().replace("-", "");
            user.setUserId(newUserId);
            user.setOpenid(openid);
            user.setRole(1);
            user.setStatus(1);
            user.setRealName("微信用户");
            user.setCreatedAt(LocalDateTime.now());
            boolean save = sysUserService.save(user);
            if(!save){
                return Result.error("登录失败，请稍后重试");
            }

        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("您的账号已被禁用，请联系管理员");
        }

        // --- C. 构造返回数据 ---

        // 1. 准备要放入 Token 的载荷 (Claims)
        Map<String, Object> claims = new HashMap<>();
        // 注意：这里的 Key ("userId") 必须和你在 LoginInterceptor 里读取的 Key 完全一致
        claims.put("userId", user.getUserId());

        // 2. 调用 JwtUtil 生成真实的加密 Token
        String token = JwtUtil.createToken(claims);

        // 3. 组装返回给前端的数据包
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);      // 现在返回的是一长串加密字符串了
        data.put("userInfo", user);    // 依然返回用户信息供前端展示

        log.info("用户登录成功，ID: {}, Token已生成", user.getUserId());

        return Result.success(data);
    }
}

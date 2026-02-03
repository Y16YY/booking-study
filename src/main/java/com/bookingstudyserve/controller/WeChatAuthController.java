package com.bookingstudyserve.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.service.ISysUserService;
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

        // 准备返回给前端的数据包
        Map<String, Object> data = new HashMap<>();

        // token: 这里暂时直接用 userId 作为简单的身份标识
        // (正式上线建议使用 JWT 工具生成加密 Token)
        data.put("token", user.getUserId());

        // userInfo: 把用户信息返回去，前端要在“个人中心”显示名字、学号等
        data.put("userInfo", user);

        return Result.success(data);
    }
}

package com.bookingstudyserve.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    // 1. 签名密钥 (随便写一串复杂的字符，不要泄露)
    private static final String SIGN_KEY = "MicroTeachingCenterKey_2026";

    // 2. 有效期 (比如 12 小时)
    private static final Long EXPIRE_TIME = 43200000L;

    /**
     * 生成 Token (给登录接口用)
     * @param claims 存入的信息，比如 userId
     * @return 字符串 Token
     */
    public static String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims) // 放入要加密的数据
                .signWith(SignatureAlgorithm.HS256, SIGN_KEY) // 签名算法
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME)) // 过期时间
                .compact();
    }

    /**
     * 解析 Token (给拦截器用)
     * @param token 前端传来的字符串
     * @return 解析出的数据
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(SIGN_KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
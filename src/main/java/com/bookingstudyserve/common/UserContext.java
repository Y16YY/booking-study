package com.bookingstudyserve.common;
public class UserContext {
    // 创建一个 ThreadLocal 对象，用来存储 String 类型的 userId
    private static final ThreadLocal<String> tl = new ThreadLocal<>();

    /**
     * 保存当前登录用户的 ID
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        tl.set(userId);
    }

    /**
     * 获取当前登录用户的 ID
     * @return 用户ID
     */
    public static String getUserId() {
        return tl.get();
    }

    /**
     * 清除当前线程的数据 (防止内存泄漏，非常重要！)
     */
    public static void remove() {
        tl.remove();
    }
}

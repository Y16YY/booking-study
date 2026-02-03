package com.bookingstudyserve.domain.vo;

import com.bookingstudyserve.domain.po.SysUser;
import lombok.Data;

@Data
public class UserProfileVO {
    // 1. 原来的用户信息
    private SysUser user;

    // 2. 新增的统计信息
    private Stats stats;

    @Data
    public static class Stats {
        private Long bookingCount;   // 累计预约
        private Long violationCount; // 违规记录
        private Integer score;       // 信用分
    }
}

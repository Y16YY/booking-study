package com.bookingstudyserve.domain.vo;

import lombok.Data;
import java.util.List;

/**
 * 首页控制台数据大盘 VO
 */
@Data
public class DashboardStatsVO {

    // ================= 1. 顶部基础数据卡片 =================

    /**
     * 今日预约总数
     */
    private Long todayBookingCount;

    /**
     * 待审批预约数 (status=0)
     */
    private Long pendingAuditCount;

    /**
     * 正常状态用户数 (status=1)
     */
    private Long activeUserCount;

    /**
     * 维修中教室数 (status=2)
     */
    private Long repairingRoomCount;

    // ================= 2. ECharts 图表数据 =================

    /**
     * 近7天预约趋势 (对应折线图)
     */
    private List<ChartData> dailyTrend;

    /**
     * 热门教室 Top5 (对应柱状图)
     */
    private List<ChartData> topRooms;

    /**
     * 【修改点】各班级预约占比 (对应饼图)
     */
    private List<ChartData> classRatio;
}
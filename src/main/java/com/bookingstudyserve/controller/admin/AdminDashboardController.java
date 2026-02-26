package com.bookingstudyserve.controller.admin;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysRoom;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.domain.vo.DashboardStatsVO;
import com.bookingstudyserve.service.IBizBookingService;
import com.bookingstudyserve.service.ISysRoomService;
import com.bookingstudyserve.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    @Autowired
    private IBizBookingService bookingService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoomService roomService;

    @GetMapping("/stats")
    public Result<DashboardStatsVO> getDashboardStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        // ================= 1. 基础数字卡片 =================
        // 今日预约
        vo.setTodayBookingCount(bookingService.lambdaQuery()
                .eq(BizBooking::getBookingDate, LocalDate.now())
                .count());

        // 待审批 (0-待审批)
        vo.setPendingAuditCount(bookingService.lambdaQuery()
                .eq(BizBooking::getStatus, 0)
                .count());

        // 活跃用户 (1-正常)
        vo.setActiveUserCount(userService.lambdaQuery()
                .eq(SysUser::getStatus, 1)
                .count());

        // 维修中教室 (2-维修中)
        vo.setRepairingRoomCount(roomService.lambdaQuery()
                .eq(SysRoom::getStatus, 2)
                .count());

        // ================= 2. 图表分析数据 =================
        vo.setDailyTrend(bookingService.getDailyTrend());
        vo.setTopRooms(bookingService.getTopRooms());
        vo.setClassRatio(bookingService.getClassRatio());

        return Result.success(vo);
    }
}
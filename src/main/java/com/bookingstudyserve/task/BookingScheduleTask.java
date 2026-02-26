package com.bookingstudyserve.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.service.IBizBookingService;
import com.bookingstudyserve.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BookingScheduleTask {

    @Autowired
    private IBizBookingService bookingService;

    @Autowired
    private ISysUserService userService;

    // 1. 初始化你的课表时间 Map
    private static final Map<Integer, LocalTime> TIME_MAP = new HashMap<>();

    // 2. 设置允许迟到的最大分钟数（缓冲时间）
    private static final int GRACE_PERIOD_MINUTES = 30;

    static {
        TIME_MAP.put(1, LocalTime.of(8, 0));
        TIME_MAP.put(2, LocalTime.of(8, 50));
        TIME_MAP.put(3, LocalTime.of(9, 40));
        TIME_MAP.put(4, LocalTime.of(10, 30));
        TIME_MAP.put(5, LocalTime.of(11, 20));
        TIME_MAP.put(6, LocalTime.of(14, 0));
        TIME_MAP.put(7, LocalTime.of(14, 50));
        TIME_MAP.put(8, LocalTime.of(15, 40));
        TIME_MAP.put(9, LocalTime.of(16, 30));
    }

    /**
     * 定时任务：每 10 分钟执行一次
     * cron = "0 0/10 * * * ?"
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void checkOverdueBookings() {
        log.info("【系统任务】开始巡检逾期未签到的微格教室预约...");

        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        // 查询所有今天且状态为 1 (已通过，等待签到) 的预约记录
        LambdaQueryWrapper<BizBooking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizBooking::getStatus, 1);

        List<BizBooking> pendingBookings = bookingService.list(wrapper);

        int overdueCount = 0;

        for (BizBooking booking : pendingBookings) {
            // 获取该预约第一节课的开始时间
            LocalTime classStartTime = TIME_MAP.get(booking.getStartSlot());

            if (classStartTime != null) {
                // 计算最晚签到时间 = 课程开始时间 + 允许迟到的分钟数
                // 例如：第1节课 08:00 开始，最晚 08:30 签到
                LocalTime latestSignTime = classStartTime.plusMinutes(GRACE_PERIOD_MINUTES);

                // 如果当前时间已经超过了最晚签到时间，则判定为逾期
                if (nowTime.isAfter(latestSignTime)) {
                    booking.setStatus(5); // 改为 5-已逾期/违约

                    String oldDesc = booking.getDescription() == null ? "" : booking.getDescription();
                    booking.setDescription(oldDesc + " [系统自动判定：逾期未签到违约]");
                    // 2. 统计该用户历史违约总次数 (状态为 5 的记录)
                    long violationCount = bookingService.lambdaQuery()
                            .eq(BizBooking::getUserId, booking.getUserId())
                            .eq(BizBooking::getStatus, 5)
                            .count();

                    // 3. 满 3 次直接拉黑
                    if (violationCount >= 3) {
                        SysUser user = new SysUser();
                        user.setUserId(booking.getUserId());
                        user.setStatus(0); // 0-禁用账号
                        // 暂停 1 周预约权限
                        user.setBlacklistEndTime(LocalDateTime.now().plusWeeks(1));

                        userService.updateById(user);
                        log.warn("【黑名单触发】用户 {} 累计违约 3 次，已封禁 1 周！", booking.getUserId());
                    }

                    bookingService.updateById(booking);
                    overdueCount++;
                }
            }
        }

        if (overdueCount > 0) {
            log.warn("【系统任务】巡检结束，共将 {} 条记录标记为逾期违约！", overdueCount);
        } else {
            log.info("【系统任务】巡检结束，当前无逾期记录。");
        }
    }

    /**
     * 任务2：每小时执行一次，自动解封到期的账号
     * cron = "0 0 * * * ?" 表示每小时的第 0 分钟执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void unlockBlacklistUsers() {
        LocalDateTime now = LocalDateTime.now();

        // 寻找满足条件的用户：
        // status = 0 且 blacklistEndTime <= 当前时间
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getStatus, 0)
                .isNotNull(SysUser::getBlacklistEndTime)
                .le(SysUser::getBlacklistEndTime, now)
                .set(SysUser::getStatus, 1) // 恢复正常状态
                .set(SysUser::getBlacklistEndTime, null); // 清空解封时间

        boolean updated = userService.update(updateWrapper);
        if (updated) {
            log.info("【系统任务】已自动解封黑名单到期的用户账号。");
        }
    }
}
package com.bookingstudyserve.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.mapper.BizBookingMapper;
import com.bookingstudyserve.service.IBizBookingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookingstudyserve.service.ISysRoomService;
import com.bookingstudyserve.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * <p>
 * 预约记录主表 服务实现类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Service
public class BizBookingServiceImpl extends ServiceImpl<BizBookingMapper, BizBooking> implements IBizBookingService {

    @Autowired
    private ISysRoomService roomService;

    @Autowired
    @Lazy
    private ISysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class) // 务必加事务
    public Result<String> submitBooking(BookingSubmitDTO dto, String userId) {

        // 1. 基础参数校验 (Slots 非空等)
        if (dto.getSlots() == null || dto.getSlots().isEmpty()) {
            return Result.error("请至少选择一个节次");
        }
        // 对 slots 排序
        List<Integer> slots = dto.getSlots();
        Collections.sort(slots);

        // 2. 获取用户身份 (需要查询 SysUser 表)
        SysUser user = sysUserService.getById(userId);
        boolean isTeacher = user.getRole() == 2; // 假设 2 是教师

        // ==========================================
        // 分支逻辑 A：学生 (有周限制，只能单次)
        // ==========================================
        if (!isTeacher) {
            // --- 校验连续性 (学生专属规则) ---
            if (!checkConsecutive(slots)) {
                return Result.error("学生预约的节次必须是连续的");
            }

            // --- 校验周配额 (学生专属规则) ---
            checkWeeklyLimit(userId);

            // --- 执行单次保存 ---
            LocalDate date = LocalDate.parse(dto.getDate());
            try {
                doSaveSingleBooking(dto, userId, date, slots);
            } catch (RuntimeException e) {
                return Result.error(e.getMessage());
            }
            return Result.success("预约提交成功");
        }

        // ==========================================
        // 分支逻辑 B：教师 (无限制，支持批量)
        // ==========================================
        else {
            // 1. 计算需要预约的所有日期列表
            List<LocalDate> dateList = calculateBatchDates(dto);

            // 2. 循环保存
            try {
                for (LocalDate date : dateList) {
                    doSaveSingleBooking(dto, userId, date, slots);
                }
            } catch (RuntimeException e) {
                // 只要有一天失败（比如容量不足），事务会自动回滚，之前的也不会保存
                // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); // 手动回滚(可选)
                return Result.error("批量预约失败：" + e.getMessage());
            }

            return Result.success("成功预约 " + dateList.size() + " 次课程");
        }
    }

    /**
     * 通用底层方法：仅负责将"单天"的预约请求入库
     * 适用于：学生单次预约、教师批量预约循环中的每一次
     */
    private void doSaveSingleBooking(BookingSubmitDTO dto, String userId, LocalDate date, List<Integer> slots) {
        // 1. 并发容量检查
        // 这里必须用传入的 date，而不是 DTO 里的 date (因为批量预约时日期会变)
        Map<Integer, Integer> capacityMap = roomService.getRoomSlotCapacity(dto.getRoomId(), date.toString(), dto.getSection());

        for (Integer slot : slots) {
            Integer left = capacityMap.get(slot);
            if (left != null && left <= 0) {
                // 抛出异常触发事务回滚 (批量预约时只要有一天满员，整体失败)
                throw new RuntimeException(date + " 第 " + slot + " 节课已满员，预约失败");
            }
        }

        // 2. 组装 PO 实体 (复用你原来的 D 步骤)
        BizBooking booking = new BizBooking();
        BeanUtil.copyProperties(dto, booking); // 拷贝基础字段

        booking.setUserId(userId);
        booking.setBookingDate(date); // === 关键：使用传入的日期 ===

        // 计算起止节次
        booking.setStartSlot(slots.get(0));
        booking.setEndSlot(slots.get(slots.size() - 1));

        // 3. 状态设置
        // 老师预约直接通过(1)，学生需要审核(0)
        SysUser user = sysUserService.getById(userId);
        if (user.getRole() == 2) {
            booking.setStatus(1);
        }
        else {
            booking.setStatus(0);
        }
        booking.setCreateTime(LocalDateTime.now());

        // 4. 保存 (复用你原来的 E 步骤)
        boolean success = this.save(booking);
        if (!success) {
            throw new RuntimeException("系统异常，预约保存失败");
        }
    }

    // 1. 校验连续性
    private boolean checkConsecutive(List<Integer> slots) {
        if (slots.size() <= 1) return true;
        for (int i = 0; i < slots.size() - 1; i++) {
            if (slots.get(i + 1) - slots.get(i) != 1) return false;
        }
        return true;
    }

    // 2. 校验周限制 (直接复制你原来的逻辑)
    private void checkWeeklyLimit(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        long count = this.count(new LambdaQueryWrapper<BizBooking>()
                .eq(BizBooking::getUserId, userId)
                .between(BizBooking::getBookingDate, monday, sunday)
                // 排除已驳回(2)和已取消(4)。
                .notIn(BizBooking::getStatus, 2, 4));

        if (count >= 3) {
            throw new RuntimeException("抱歉，每位同学每周最多只能预约3次");
        }
    }

    // 3. 计算批量日期 (核心)
    private List<LocalDate> calculateBatchDates(BookingSubmitDTO dto) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate startDate = LocalDate.parse(dto.getDate());
        dates.add(startDate); // 加上第一天

        // 如果 repeatType > 0 (1=每周, 2=双周) 且有结束日期
        if (dto.getRepeatType() != null && dto.getRepeatType() > 0 && dto.getEndDate() != null) {
            LocalDate endDate = LocalDate.parse(dto.getEndDate());
            LocalDate nextDate = startDate;

            while (true) {
                int daysToAdd = (dto.getRepeatType() == 1) ? 7 : 14;
                nextDate = nextDate.plusDays(daysToAdd);

                if (nextDate.isAfter(endDate)) break;
                dates.add(nextDate);
            }
        }
        return dates;
    }

    @Override
    public Result<List<BizBooking>> getMyBookingList(String userId) {
        // 查询条件：user_id = 当前用户，按创建时间倒序
        LambdaQueryWrapper<BizBooking> query = new LambdaQueryWrapper<>();
        query.eq(BizBooking::getUserId, userId)
                .orderByDesc(BizBooking::getCreateTime);

        List<BizBooking> list = this.list(query);
        return Result.success(list);
    }

    @Override
    public Result<String> cancelBooking(Long bookingId, String userId) {
        BizBooking booking = this.getById(bookingId);
        if (booking == null) return Result.error("记录不存在");

        // 1. 权限校验
        if (!booking.getUserId().equals(userId)) return Result.error("无权操作");

        // 2. 状态校验：只有待审核(0)或已通过(1)可以取消
        if (booking.getStatus() != 0 && booking.getStatus() != 1) {
            return Result.error("当前状态不可取消");
        }

        // 3. 时间校验：必须提前24小时
        Map<Integer, LocalTime> timeMap = new HashMap<>();
        timeMap.put(1, LocalTime.of(8, 0));
        timeMap.put(2, LocalTime.of(8, 50));
        timeMap.put(3, LocalTime.of(9, 40));
        timeMap.put(4, LocalTime.of(10, 30));
        timeMap.put(5, LocalTime.of(11, 20));
        timeMap.put(6, LocalTime.of(14, 0));
        timeMap.put(7, LocalTime.of(14, 50));
        timeMap.put(8, LocalTime.of(15, 40));
        timeMap.put(9, LocalTime.of(16, 30));

        LocalTime startTime = timeMap.getOrDefault(booking.getStartSlot(), LocalTime.of(8, 0));
        LocalDateTime bookingStart = LocalDateTime.of(booking.getBookingDate(), startTime);

        // 3. 24小时校验
        if (LocalDateTime.now().plusHours(24).isAfter(bookingStart)) {
            return Result.error("距离预约开始不足24小时，无法取消");
        }

        // 4. 逻辑取消：更新状态为 4 (已取消)
        booking.setStatus(4);
        booking.setAuditTime(LocalDateTime.now());
        this.updateById(booking);

        return Result.success("预约已成功取消");
    }
}

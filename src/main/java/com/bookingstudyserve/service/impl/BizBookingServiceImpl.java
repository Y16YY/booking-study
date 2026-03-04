package com.bookingstudyserve.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.domain.vo.BookingVO;
import com.bookingstudyserve.domain.vo.ChartData;
import com.bookingstudyserve.mapper.BizBookingMapper;
import com.bookingstudyserve.service.IBizBookingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookingstudyserve.service.ISysRoomService;
import com.bookingstudyserve.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

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

    @Autowired
    private BizBookingMapper bookingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 务必加事务
    public Result<String> submitBooking(BookingSubmitDTO dto, String userId) {

        // 1. 基础参数校验
        if (dto.getSlots() == null || dto.getSlots().isEmpty()) {
            return Result.error("请至少选择一个节次");
        }
        List<Integer> slots = dto.getSlots();
        Collections.sort(slots);

        // 2. 获取并校验用户身份
        SysUser user = sysUserService.getById(userId);
        if (user.getAuditStatus() != 2) {
            return Result.error("您的账号未通过认证，无法预约");
        }
        boolean isTeacher = (user.getRole() == 2);

        // 3. 学生专属规则校验
        if (!isTeacher) {
            if (!checkConsecutive(slots)) {
                return Result.error("学生预约的节次必须是连续的");
            }
            try {
                checkWeeklyLimit(userId);
            } catch (RuntimeException e) {
                return Result.error(e.getMessage());
            }
        }

        // 4. 计算需要预约的日期列表 (学生永远只有1天，老师根据重复规则可能有多天)
        List<LocalDate> dateList;
        if (!isTeacher) {
            dateList = Collections.singletonList(LocalDate.parse(dto.getDate()));
        } else {
            dateList = calculateBatchDates(dto);
        }

        // 5. 执行循环保存
        try {
            for (LocalDate date : dateList) {
                doSaveSingleBooking(dto, userId, date, slots, user.getRole());
            }
        } catch (RuntimeException e) {
            // ⭐ 重点修复：被 catch 拦截的异常不会触发事务回滚，必须手动标记回滚
            // 否则批量预约时，前几天成功后几天失败，前几天的数据会变成脏数据存进数据库
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(e.getMessage());
        }

        if (isTeacher && dateList.size() > 1) {
            return Result.success("成功批量预约 " + dateList.size() + " 次课程");
        }
        return Result.success("预约提交成功");
    }

    /**
     * 通用底层方法：仅负责将"单天"的预约请求入库
     */
    private void doSaveSingleBooking(BookingSubmitDTO dto, String userId, LocalDate date, List<Integer> slots, Integer role) {
        // 1. 并发容量检查
        Map<Integer, Integer> capacityMap = roomService.getRoomSlotCapacity(dto.getRoomId(), date.toString(), dto.getSection());

        for (Integer slot : slots) {
            Integer left = capacityMap.get(slot);
            if (left != null && left <= 0) {
                throw new RuntimeException("【防撞单拦截】" + date + " 第 " + slot + " 节课已被抢先预约！");
            }
        }

        // 2. 组装 PO 实体
        BizBooking booking = new BizBooking();
        // 这里的 copy 会把前端传来的 usageType, description, fileUrl 等新字段自动塞入 booking
        BeanUtil.copyProperties(dto, booking);

        booking.setUserId(userId);
        booking.setBookingDate(date);
        booking.setStartSlot(slots.get(0));
        booking.setEndSlot(slots.get(slots.size() - 1));

        // ⭐ 3. 全新状态设置逻辑 (依赖 usageType)
        // 规则：必须是教师 (role=2) 且是 计划内导入 (usageType=1)，才直接生效
        if (role == 2 && dto.getUsageType() != null && dto.getUsageType() == 1) {
            booking.setStatus(1); // 直接生效 (已通过)
        } else {
            booking.setStatus(0); // 学生，以及教师的计划外，全部待审核
        }

        booking.setCreateTime(LocalDateTime.now());

        // 4. 保存
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

    // 2. 校验周限制
    private void checkWeeklyLimit(String userId) {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        long count = this.count(new LambdaQueryWrapper<BizBooking>()
                .eq(BizBooking::getUserId, userId)
                .between(BizBooking::getBookingDate, monday, sunday)
                .notIn(BizBooking::getStatus, 2, 4));

        if (count >= 3) {
            throw new RuntimeException("抱歉，每位同学每周最多只能预约3次");
        }
    }

    // 3. 计算批量日期
    private List<LocalDate> calculateBatchDates(BookingSubmitDTO dto) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate startDate = LocalDate.parse(dto.getDate());
        dates.add(startDate);

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

        if (!booking.getUserId().equals(userId)) return Result.error("无权操作");

        if (booking.getStatus() != 0 && booking.getStatus() != 1) {
            return Result.error("当前状态不可取消");
        }

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

        if (LocalDateTime.now().plusHours(24).isAfter(bookingStart)) {
            return Result.error("距离预约开始不足24小时，无法取消");
        }

        booking.setStatus(4);
        booking.setAuditTime(LocalDateTime.now());
        this.updateById(booking);

        return Result.success("预约已成功取消");
    }

    @Override
    public IPage<BookingVO> queryBookingPage(Page<BookingVO> page, String realName, String studentId, String bookingDate, Integer role, Integer status) {
        return bookingMapper.selectBookingPage(page, realName, studentId, bookingDate, role, status);
    }

    @Override
    @Transactional
    public boolean batchApprove(List<Long> ids) {
        return this.lambdaUpdate()
                .set(BizBooking::getStatus, 2)
                .in(BizBooking::getBookingId, ids)
                .eq(BizBooking::getStatus, 1)
                .update();
    }

    @Override
    public List<ChartData> getDailyTrend() {
        return bookingMapper.getDailyTrend();
    }

    @Override
    public List<ChartData> getTopRooms() {
        return bookingMapper.getTopRooms();
    }

    @Override
    public List<ChartData> getClassRatio() {
        return bookingMapper.getClassRatio();
    }

    @Override
    public Result<IPage<BookingVO>> getRecordPage(Integer current, Integer size, String keyword, String startDate, String endDate, Integer status, Integer usageType) {
        // 在 Service 层组装分页对象
        Page<BookingVO> page = new Page<>(current, size);
        // 调用 Mapper 层查数据库
        IPage<BookingVO> result = bookingMapper.selectRecordPage(page, keyword, startDate, endDate, status, usageType);
        return Result.success(result);
    }

    @Override
    public Result<String> forceCancel(Long bookingId, String reason) {
        BizBooking booking = this.getById(bookingId);
        if (booking == null) {
            return Result.error("找不到该预约记录");
        }

        // 状态校验
        if (booking.getStatus() != 0 && booking.getStatus() != 1) {
            return Result.error("当前状态不可被取消");
        }

        // 执行业务逻辑：改状态，追加取消原因
        booking.setStatus(4);
        booking.setAuditTime(LocalDateTime.now());
        String oldDesc = booking.getDescription() == null ? "" : booking.getDescription();
        booking.setDescription(oldDesc + " 【管理员强制取消原因：" + reason + "】");
        //TODO 发送预约被取消的邮件给预约用户

        this.updateById(booking);
        return Result.success("已强制取消该预约");
    }

    @Override
    public Result<String> deleteBooking(Long id) {
        // 直接复用 MyBatis-Plus 的 removeById
        boolean success = this.removeById(id);
        return success ? Result.success("删除成功") : Result.error("删除失败，记录可能不存在");
    }
}
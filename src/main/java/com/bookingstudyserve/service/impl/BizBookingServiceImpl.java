package com.bookingstudyserve.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.mapper.BizBookingMapper;
import com.bookingstudyserve.service.IBizBookingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bookingstudyserve.service.ISysRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，报错自动回滚
    public Result<String> submitBooking(BookingSubmitDTO dto, String userId) {

        // --- A. 基础参数校验 ---
        if (dto.getSlots() == null || dto.getSlots().isEmpty()) {
            return Result.error("请至少选择一个节次");
        }

        // --- B. 计算开始和结束节次 ---
        // 前端传的是 [3, 4]，我们需要转成 start=3, end=4
        List<Integer> slots = dto.getSlots();
        Collections.sort(slots); // 排序，防止前端传 [4, 3]

        // 校验连续性 (业务规则：学生只能选连续的)
        if (slots.size() > 1) {
            for (int i = 0; i < slots.size() - 1; i++) {
                if (slots.get(i + 1) - slots.get(i) != 1) {
                    return Result.error("预约的节次必须是连续的");
                }
            }
        }

        int startSlot = slots.get(0);
        int endSlot = slots.get(slots.size() - 1);

        // --- C. 【核心】并发容量检查 (防超卖) ---
        // 这一步非常重要！在保存前，必须再查一次这些节次是不是真的有空位。
        // 调用你在 RoomService 里写的 getRoomSlotCapacity 方法
        Map<Integer, Integer> capacityMap = roomService.getRoomSlotCapacity(dto.getRoomId(), dto.getDate(), dto.getSection());
        for (Integer slot : slots) {
            Integer left = capacityMap.get(slot);
            // 如果剩余名额 <= 0，说明刚才那一瞬间被别人抢了
            if (left != null && left <= 0) {
                return Result.error("手慢了！第 " + slot + " 节课刚刚已满员，请重新选择");
            }
        }


        // --- D. 组装 PO 实体对象 ---
        BizBooking booking = new BizBooking();

        // 1. 【核心简化】一键拷贝所有同名同类型字段
        // 自动覆盖：roomId, usageType, courseName, classInfo, description, supervisorName, fileUrl, repeatType
        BeanUtil.copyProperties(dto, booking);

        // 2. 【必须手动】处理逻辑字段、名字不一致字段、系统生成字段
        booking.setUserId(userId);                     // DTO里没这个(或不安全)，从Token拿
        booking.setBookingDate(LocalDate.parse(dto.getDate())); // 名字不同 (date vs bookingDate) 且需类型转换
        booking.setStartSlot(startSlot);               // 需计算 (List -> int)
        booking.setEndSlot(endSlot);                   // 需计算 (List -> int)
        booking.setStatus(0);                          // 默认值
        booking.setCreateTime(LocalDateTime.now());    // 系统生成

        // --- E. 保存入库 ---
        boolean success = this.save(booking);

        if (!success) {
            return Result.error("系统异常，预约保存失败");
        }

        return Result.success("预约提交成功");
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
        if (booking == null) {
            return Result.error("预约不存在");
        }
        // 校验权限：只能取消自己的
        if (!booking.getUserId().equals(userId)) {
            return Result.error("无权操作他人的预约");
        }
        // 校验状态：只有“待审核”或“已通过”且未开始的才能取消
        // 这里简化逻辑：只要不是已完成/已取消就可以取消
        if (booking.getStatus() >= 3) {
            return Result.error("当前状态不可取消");
        }

        booking.setStatus(4); // 4-已取消
        this.updateById(booking);
        return Result.success("取消成功");
    }
}

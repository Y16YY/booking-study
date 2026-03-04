package com.bookingstudyserve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysRoom;
import com.bookingstudyserve.mapper.BizBookingMapper;
import com.bookingstudyserve.mapper.SysRoomMapper;
import com.bookingstudyserve.service.ISysRoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 教室资源表 服务实现类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Service
@Slf4j
public class SysRoomServiceImpl extends ServiceImpl<SysRoomMapper, SysRoom> implements ISysRoomService {

    @Autowired
    private BizBookingMapper bookingMapper;

    // 定义上午和下午的节次范围
    private static final List<Integer> AM_SLOTS = Arrays.asList(1, 2, 3, 4, 5);
    private static final List<Integer> PM_SLOTS = Arrays.asList(6, 7, 8, 9);

    /**
     * 查询教室列表状态
     */
    public List<Map<String, Object>> getRoomListStatus(String dateStr, String section) {
        LocalDate date = LocalDate.parse(dateStr);
        List<SysRoom> rooms = this.list();

        LambdaQueryWrapper<BizBooking> query = new LambdaQueryWrapper<>();
        // 💡 重点优化：为了防止“并发抢占”，列表展示时也应该把“待审批(0)”的算作已占用
        query.eq(BizBooking::getBookingDate, date)
                .in(BizBooking::getStatus, 0, 1);

        List<BizBooking> allBookings = bookingMapper.selectList(query);
        List<Integer> targetSlots = "AM".equals(section) ? AM_SLOTS : PM_SLOTS;
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (SysRoom room : rooms) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getRoomId());
            map.put("name", room.getRoomNumber());

            // 判断维修状态
            if (room.getStatus() != null && room.getStatus() == 2) {
                map.put("status", "fix");
                resultList.add(map);
                continue;
            }

            // ⭐ 修改点 1：废弃数据库容量，将每个时间段的最大容量强制设为 1
            int capacity = 1;

            Map<Integer, Integer> slotCounter = new HashMap<>();
            for (Integer slot : targetSlots) {
                slotCounter.put(slot, 0);
            }

            List<BizBooking> roomBookings = allBookings.stream()
                    .filter(b -> b.getRoomId().equals(room.getRoomId()))
                    .collect(Collectors.toList());

            for (BizBooking booking : roomBookings) {
                for (int i = booking.getStartSlot(); i <= booking.getEndSlot(); i++) {
                    if (slotCounter.containsKey(i)) {
                        slotCounter.put(i, slotCounter.get(i) + 1);
                    }
                }
            }

            // 只要有任何一节课的使用人数 < 1 (即等于0)，说明还有空闲时间段
            boolean isFull = true;
            for (Integer slot : targetSlots) {
                if (slotCounter.get(slot) < capacity) {
                    isFull = false;
                    break;
                }
            }

            if (isFull) {
                map.put("status", "booked");
            } else {
                map.put("status", "free");
            }

            resultList.add(map);
        }

        return resultList;
    }

    /**
     * 查询具体教室某半天的每节课剩余容量
     */
    @Override
    public Map<Integer, Integer> getRoomSlotCapacity(Integer roomId, String dateStr, String section) {
        LocalDate date = LocalDate.parse(dateStr);
        List<Integer> targetSlots = "AM".equals(section) ? AM_SLOTS : PM_SLOTS;

        // ⭐ 修改点 2：不再查询 SysRoom 表的 capacity，直接将总容量设为 1
        int totalCapacity = 1;

        LambdaQueryWrapper<BizBooking> query = new LambdaQueryWrapper<>();
        query.eq(BizBooking::getRoomId, roomId)
                .eq(BizBooking::getBookingDate, date)
                .in(BizBooking::getStatus, 0, 1);
        List<BizBooking> bookings = bookingMapper.selectList(query);

        Map<Integer, Integer> occupiedCount = new HashMap<>();
        for (Integer slot : targetSlots) {
            occupiedCount.put(slot, 0);
        }

        for (BizBooking b : bookings) {
            for (int i = b.getStartSlot(); i <= b.getEndSlot(); i++) {
                if (occupiedCount.containsKey(i)) {
                    occupiedCount.put(i, occupiedCount.get(i) + 1);
                }
            }
        }

        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Integer slot : targetSlots) {
            int used = occupiedCount.get(slot);
            int left = totalCapacity - used;
            // 如果 left <= 0，就返回 0，表示无法预约
            result.put(slot, Math.max(0, left));
        }
        log.info("教室 {} 在 {} 的剩余容量：{}", roomId, dateStr, result);
        return result;
    }
}
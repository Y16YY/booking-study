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

    // 定义上午和下午的节次范围 (根据你的业务写死或读配置)
    // 上午: 1, 2, 3, 4, 5 (共5节)
    private static final List<Integer> AM_SLOTS = Arrays.asList(1, 2, 3, 4, 5);
    // 下午: 6, 7, 8, 9 (共4节)
    private static final List<Integer> PM_SLOTS = Arrays.asList(6, 7, 8, 9);

    /**
     * 查询教室列表状态
     * @param dateStr "2026-05-20"
     * @param section "AM" 或 "PM"
     */
    public List<Map<String, Object>> getRoomListStatus(String dateStr, String section) {
        LocalDate date = LocalDate.parse(dateStr);

        // 1. 获取所有教室
        List<SysRoom> rooms = this.list();

        // 2. 获取当天、所有已通过(status=1)的预约记录
        // 优化：只查当天的，不区分教室，一次性查出来再在内存里分组，比循环查库性能高
        LambdaQueryWrapper<BizBooking> query = new LambdaQueryWrapper<>();
        query.eq(BizBooking::getBookingDate, date)
                .eq(BizBooking::getStatus, 1); // ⚠️ 注意：只计算已通过的预约

        List<BizBooking> allBookings = bookingMapper.selectList(query);

        // 3. 确定当前要计算的目标节次集合
        List<Integer> targetSlots = "AM".equals(section) ? AM_SLOTS : PM_SLOTS;

        // 4. 构造返回结果
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (SysRoom room : rooms) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getRoomId());
            map.put("name", room.getRoomNumber()); // 前端叫name，库里叫roomNumber

            // --- A. 判断维修状态 ---
            if (room.getStatus()==2) { // sys_room.status 2=维修
                map.put("status", "fix");
                resultList.add(map);
                continue;
            }

            // --- B. 核心算法：计算每个时间段的“当前人数” ---

            // 拿到该教室的容量 (如果数据库是null，默认给5人)
            int capacity = (room.getCapacity() == null) ? 5 : room.getCapacity();

            // 准备一个计数器：Key=节次(1-5), Value=已预约人数
            Map<Integer, Integer> slotCounter = new HashMap<>();
            // 初始化计数器，把1-5节都置为0
            for (Integer slot : targetSlots) {
                slotCounter.put(slot, 0);
            }

            // 筛选出该教室的订单
            List<BizBooking> roomBookings = allBookings.stream()
                    .filter(b -> b.getRoomId().equals(room.getRoomId()))
                    .collect(Collectors.toList());

            // 遍历订单，进行累加
            for (BizBooking booking : roomBookings) {
                // 比如张三约了 1-3节，那么 slotCounter 的 1,2,3 都要 +1
                for (int i = booking.getStartSlot(); i <= booking.getEndSlot(); i++) {
                    if (slotCounter.containsKey(i)) {
                        slotCounter.put(i, slotCounter.get(i) + 1);
                    }
                }
            }

            // --- C. 判定状态 ---

            // 规则：必须“所有时间段”都满员，才算 booked
            // 也就是：1-5节里，只要有任何一节的人数 < capacity，就说明还有空位
            boolean isFull = true;
            for (Integer slot : targetSlots) {
                int currentCount = slotCounter.get(slot);
                if (currentCount < capacity) {
                    isFull = false; // 只要有一节没满，整体就没满
                    break;
                }
            }

            if (isFull) {
                map.put("status", "booked"); // 红色：全满
            } else {
                map.put("status", "free");   // 绿色：可预约
                // (进阶) 你甚至可以告诉前端哪节课还剩几个位
                // map.put("detail", slotCounter);
            }

            resultList.add(map);
        }

        return resultList;
    }

    @Override
    public Map<Integer, Integer> getRoomSlotCapacity(Integer roomId, String dateStr, String section) {
        LocalDate date = LocalDate.parse(dateStr);
        List<Integer> targetSlots = "AM".equals(section) ? Arrays.asList(1, 2, 3, 4, 5) : Arrays.asList(6, 7, 8, 9);

        // 1. 查教室总容量
        SysRoom room = this.getById(roomId);
        int totalCapacity = (room.getCapacity() == null) ? 5 : room.getCapacity();

        // 2. 查该教室、该半天、已通过(status=1)的所有预约
        LambdaQueryWrapper<BizBooking> query = new LambdaQueryWrapper<>();
        query.eq(BizBooking::getRoomId, roomId)
                .eq(BizBooking::getBookingDate, date)
                .in(BizBooking::getStatus, 0,1);
        List<BizBooking> bookings = bookingMapper.selectList(query);

        // 3. 计算每一节课已占用多少人
        Map<Integer, Integer> occupiedCount = new HashMap<>();
        for (Integer slot : targetSlots) {
            occupiedCount.put(slot, 0); // 初始化为0
        }

        for (BizBooking b : bookings) {
            for (int i = b.getStartSlot(); i <= b.getEndSlot(); i++) {
                if (occupiedCount.containsKey(i)) {
                    occupiedCount.put(i, occupiedCount.get(i) + 1);
                }
            }
        }

        // 4. 计算剩余容量 (总容量 - 已用)
        Map<Integer, Integer> result = new LinkedHashMap<>(); // 有序
        for (Integer slot : targetSlots) {
            int used = occupiedCount.get(slot);
            int left = totalCapacity - used;
            result.put(slot, Math.max(0, left)); // 防止出现负数
        }
        log.info("剩余容量：{}", result);
        return result;
    }
}

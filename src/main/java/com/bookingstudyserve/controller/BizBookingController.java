package com.bookingstudyserve.controller;


import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.common.UserContext;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.service.IBizBookingService;
import com.bookingstudyserve.service.impl.BizBookingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 预约记录主表 前端控制器
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/api/bookings")
@Slf4j
public class BizBookingController {
    @Autowired
    private IBizBookingService bookingService;

    @PostMapping("/submit")
    public Result<String> submitBooking(@RequestBody BookingSubmitDTO dto) {
        // 1. 获取当前登录用户的 ID (从 Token 中解析)
        String userId = UserContext.getUserId();
        log.info("用户 {} 提交预约", userId);
        if (userId == null) {
            return Result.error("登陆失败");
        }

        // 2. 调用 Service 处理核心逻辑
        return bookingService.submitBooking(dto, userId);
    }

    /**
     * 查询我的预约列表
     */
    @GetMapping("/my-list")
    public Result<List<BizBooking>> getMyBookingList() {
        String userId = UserContext.getUserId(); // 获取当前用户ID
        return bookingService.getMyBookingList(userId);
    }

    /**
     * 取消预约
     */
    @PostMapping("/cancel/{bookingId}")
    public Result<String> cancelBooking(@PathVariable Long bookingId) {
        String userId = UserContext.getUserId(); // 必须校验是不是自己的预约
        return bookingService.cancelBooking(bookingId, userId);
    }

}

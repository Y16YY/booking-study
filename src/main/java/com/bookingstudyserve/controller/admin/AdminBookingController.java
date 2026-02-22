package com.bookingstudyserve.controller.admin;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.common.UserContext;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.service.IBizBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    @Autowired
    private IBizBookingService bookingService;

    /**
     * 1. 获取所有待审批的预约单 (status = 0)
     */
    @GetMapping("/pending")
    public Result<List<BizBooking>> getPendingList() {
        List<BizBooking> list = bookingService.lambdaQuery()
                .eq(BizBooking::getStatus, 0)
                .orderByDesc(BizBooking::getCreateTime) // 按申请时间倒序
                .list();
        return Result.success(list); // 使用你定义的 Result.success
    }

    /**
     * 2. 审批通过
     */
    @PostMapping("/approve/{id}")
    public Result<String> approve(@PathVariable Long id) {
        String userId = UserContext.getUserId();
        boolean updated = bookingService.lambdaUpdate()
                .set(BizBooking::getStatus, 1)
                .eq(BizBooking::getBookingId, id)
                .set(BizBooking::getAuditorId, userId)
                .set(BizBooking::getAuditTime, LocalDateTime.now())
                .update();
        return updated ? Result.success("审批已通过") : Result.error("操作失败");
    }

    /**
     * 3. 审批驳回
     */
    @PostMapping("/reject/{id}")
    public Result<String> reject(@PathVariable Long id, @RequestBody BizBooking bookingDto) {
        String userId = UserContext.getUserId();
        boolean updated = bookingService.lambdaUpdate()
                .set(BizBooking::getStatus, 2)
                .set(BizBooking::getRejectReason, bookingDto.getRejectReason())
                .eq(BizBooking::getBookingId, id)
                .set(BizBooking::getAuditorId, userId)
                .set(BizBooking::getAuditTime, LocalDateTime.now())
                .update();
        return updated ? Result.success("申请已驳回") : Result.error("操作失败");
    }
}
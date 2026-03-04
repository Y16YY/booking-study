package com.bookingstudyserve.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.common.UserContext;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.vo.BookingVO;
import com.bookingstudyserve.service.IBizBookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    @Autowired
    private IBizBookingService bookingService;

    /**
     * 1. 获取所有待审批的预约单 (status = 0)
     */
    @GetMapping("/page")
    public Result<IPage<BookingVO>> getBookingPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            String realName,
            String studentId,
            String bookingDate,
            Integer role,
            Integer status
    ) {

        Page<BookingVO> page = new Page<>(current, size);

        return Result.success(bookingService.queryBookingPage(page, realName, studentId, bookingDate, role, status));
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


    @PostMapping("/batch-approve")
    public Result<String> batchApprove(@RequestBody List<Long> ids) {
        return bookingService.batchApprove(ids) ? Result.success("操作成功") : Result.error("部分操作失败");
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
    /**
     * 1. 分页查询所有预约记录
     */
    @GetMapping("/records")
    public Result<IPage<BookingVO>> getRecords(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer usageType) {

        // 绝对不在 Controller 里 new Page，直接把参数原封不动扔给 Service
        return bookingService.getRecordPage(current, size, keyword, startDate, endDate, status, usageType);
    }

    /**
     * 2. 管理员强制取消预约
     */
    @PutMapping("/{id}/force-cancel")
    public Result<String> forceCancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return bookingService.forceCancel(id, body.get("reason"));
    }

    /**
     * 3. 删除预约记录
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteBooking(@PathVariable Long id) {
        return bookingService.deleteBooking(id);
    }


}
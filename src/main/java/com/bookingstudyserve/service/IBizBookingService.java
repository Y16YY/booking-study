package com.bookingstudyserve.service;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 预约记录主表 服务类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
public interface IBizBookingService extends IService<BizBooking> {

    Result<String> submitBooking(BookingSubmitDTO dto, String userId);

    Result<List<BizBooking>> getMyBookingList(String userId);

    Result<String> cancelBooking(Long bookingId, String userId);
}

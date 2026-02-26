package com.bookingstudyserve.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.BookingSubmitDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookingstudyserve.domain.vo.BookingVO;
import com.bookingstudyserve.domain.vo.ChartData;

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

    IPage<BookingVO> queryBookingPage(Page<BookingVO> page, String realName, String studentId, String bookingDate, Integer role, Integer status);

    boolean batchApprove(List<Long> ids);

    List<ChartData> getDailyTrend();

    List<ChartData> getTopRooms();

    List<ChartData> getClassRatio();
}

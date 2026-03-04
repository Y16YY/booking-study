package com.bookingstudyserve.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookingstudyserve.domain.po.BizBooking;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookingstudyserve.domain.vo.BookingVO;
import com.bookingstudyserve.domain.vo.ChartData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 预约记录主表 Mapper 接口
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Mapper
public interface BizBookingMapper extends BaseMapper<BizBooking> {

    IPage<BookingVO> selectBookingPage(
            @Param("page") Page<BookingVO> page,
            @Param("realName") String realName,
            @Param("studentId") String studentId,
            @Param("bookingDate") String bookingDate,
            @Param("role") Integer role,
            @Param("status") Integer status
    );

    List<ChartData> getDailyTrend();

    List<ChartData> getTopRooms();

    List<ChartData> getClassRatio();

    IPage<BookingVO> selectRecordPage(Page<BookingVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("startDate") String startDate,
                                      @Param("endDate") String endDate,
                                      @Param("status") Integer status,
                                      @Param("usageType") Integer usageType);
}

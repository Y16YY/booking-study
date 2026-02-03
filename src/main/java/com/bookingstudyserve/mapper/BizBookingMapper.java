package com.bookingstudyserve.mapper;

import com.bookingstudyserve.domain.po.BizBooking;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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

}

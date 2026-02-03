package com.bookingstudyserve.service;

import com.bookingstudyserve.domain.po.SysRoom;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 教室资源表 服务类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
public interface ISysRoomService extends IService<SysRoom> {

    List<Map<String, Object>> getRoomListStatus(String date, String section);

    Map<Integer, Integer> getRoomSlotCapacity(Integer roomId, String date, String section);
}

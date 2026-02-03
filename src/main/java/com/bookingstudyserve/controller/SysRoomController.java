package com.bookingstudyserve.controller;


import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.service.ISysRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 教室资源表 前端控制器
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/api/rooms")
@Slf4j
public class SysRoomController {
    @Autowired
    private ISysRoomService roomService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getRoomList(
            @RequestParam("date") String date,
            @RequestParam("section") String section) {
        log.info("获取教室列表状态，空闲，维修");
        // 简单校验
        if (!"AM".equals(section) && !"PM".equals(section)) {
            return Result.error("时段参数错误");
        }

        List<Map<String, Object>> list = roomService.getRoomListStatus(date, section);
        return Result.success(list);
    }

    @GetMapping("/capacity-status")
    public Result<Map<Integer, Integer>> getCapacityStatus(
            @RequestParam Integer roomId,
            @RequestParam String date,
            @RequestParam String section) {
        log.info("获取教室 {} {} {} 的容量状态", roomId, date, section);
        return Result.success(roomService.getRoomSlotCapacity(roomId, date, section));
    }



}

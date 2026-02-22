package com.bookingstudyserve.controller.admin;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.SysRoom;
import com.bookingstudyserve.service.ISysRoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomController {

    @Autowired
    private ISysRoomService roomService;

    /**
     * 获取所有教室列表 (支持按教室号模糊查询)
     */
    @GetMapping("/list")
    public Result<List<SysRoom>> list(String roomNumber) {
        LambdaQueryWrapper<SysRoom> wrapper = new LambdaQueryWrapper<>();
        // 如果传了教室号，则进行模糊匹配
        wrapper.like(roomNumber != null, SysRoom::getRoomNumber, roomNumber);
        wrapper.orderByAsc(SysRoom::getRoomNumber); // 按教室号排序
        return Result.success(roomService.list(wrapper));
    }

    /**
     * 修改教室状态 (手动设置 1-启用 或 2-维修中)
     */
    @PostMapping("/status/{roomId}")
    public Result<String> updateStatus(@PathVariable Integer roomId, @RequestParam Integer status) {
        boolean updated = roomService.lambdaUpdate()
                .set(SysRoom::getStatus, status)
                .eq(SysRoom::getRoomId, roomId)
                .update();
        return updated ? Result.success("状态更新成功") : Result.error("教室不存在");
    }

    /**
     * 添加教室
     */
    @PostMapping("/add")
    public Result<String> addRoom(@RequestBody SysRoom room) {
        // 1. 基础校验
        if (room.getRoomNumber() == null) return Result.error("教室号不能为空");

        // 2. 检查教室号是否重复
        long count = roomService.lambdaQuery()
                .eq(SysRoom::getRoomNumber, room.getRoomNumber())
                .count();
        if (count > 0) return Result.error("该教室号已存在");

        // 3. 初始化默认值
        room.setStatus(1); // 默认为 1-启用/空闲
        // 自动生成随机 UUID 作为二维码标识，用于后续小程序签到
        room.setQrCodeStr(UUID.randomUUID().toString().replace("-", ""));

        // 4. 保存到数据库
        boolean saved = roomService.save(room);
        return saved ? Result.success("教室添加成功") : Result.error("添加失败");
    }

    /**
     * 更新教室详情 (用于录入报修描述或设备信息)
     */
    @PostMapping("/update")
    public Result<String> updateRoom(@RequestBody SysRoom room) {
        boolean updated = roomService.updateById(room);
        return updated ? Result.success("配置保存成功") : Result.error("保存失败");
    }
}
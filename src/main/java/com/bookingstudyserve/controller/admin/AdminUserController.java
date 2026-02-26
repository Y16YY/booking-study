package com.bookingstudyserve.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    @Autowired
    private ISysUserService userService;

    /**
     * 获取待审核的学生列表 (role=1, status=0)
     */
    @GetMapping("/page")
    public Result<IPage<SysUser>> getUserPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            String realName,
            String studentId,
            Integer role,         // 1-学生, 2-教师
            Integer auditStatus   // 1-审核中, 2-已通过, 3-驳回
    ) {
        Page<SysUser> page = new Page<>(current, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 1. 动态筛选：只有传了值才增加条件
        wrapper.eq(role != null, SysUser::getRole, role);
        wrapper.eq(auditStatus != null, SysUser::getAuditStatus, auditStatus);
        wrapper.like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName);
        wrapper.like(StrUtil.isNotBlank(studentId), SysUser::getStudentId, studentId);
        wrapper.ne(SysUser::getRole, 9);

        // 2. 默认按创建时间倒序
        wrapper.orderByDesc(SysUser::getCreatedAt);

        IPage<SysUser> userPage = userService.page(page, wrapper);
        return Result.success(userPage);
    }

    /**
     * 学生身份审核操作
     * @param userId 用户ID
     */
    @PostMapping("/audit/{userId}")
    public Result<String> auditUser(@PathVariable String userId, @RequestParam Integer status) {
        // 更新数据库中的 auditStatus 字段
        boolean updated = userService.lambdaUpdate()
                .set(SysUser::getAuditStatus, status)
                .eq(SysUser::getUserId, userId)
                .orderByDesc(SysUser::getCreatedAt)

          .update();

        return updated ? Result.success("审核处理成功") : Result.error("用户不存在");
    }


    /**
     * 惩罚机制：调整用户信用分
     */
    @PostMapping("/update-credit")
    public Result<String> updateCredit(@RequestParam String userId, @RequestParam Integer points, @RequestParam String reason) {
        // 1. 更新用户表中的信用分字段
        boolean success = userService.lambdaUpdate()
                .setSql("credit_score = credit_score + " + points) // 扣分则传负数
                .eq(SysUser::getUserId, userId)
                .update();

        // 2. 建议后期增加一条信用变更记录日志表
        // logService.save(new CreditLog(userId, points, reason));

        return success ? Result.success("信用分调整成功") : Result.error("操作失败");
    }

    /**
     * 账号启禁接口：更新 status (1-正常, 0-禁用)
     */
    @PostMapping("/status/{userId}")
    public Result<String> toggleStatus(@PathVariable String userId, @RequestParam Integer status) {
        boolean success = userService.updateUserStatus(userId, status);
        return success ? Result.success("账号状态已更新") : Result.error("操作失败");
    }
}
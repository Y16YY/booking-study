package com.bookingstudyserve.controller;


import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.common.UserContext;
import com.bookingstudyserve.domain.dto.UserBindDTO;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.domain.vo.UserProfileVO;
import com.bookingstudyserve.service.ISysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 系统用户表 前端控制器
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/api/user")
@Slf4j
public class SysUserController {

    @Autowired
    private ISysUserService sysUserService;

    /**
     * 提交身份绑定申请
     */
    @PostMapping("/bind")
    public Result<String> bindInfo(@RequestBody UserBindDTO dto) {
        String userId = UserContext.getUserId();
        System.out.println("用户绑定信息：" + dto);
        return sysUserService.bindStudentInfo(userId, dto);

    }

    /**
     * 获取最新用户信息 (用于个人中心刷新状态)
     */
    @GetMapping("/info")
    public Result<UserProfileVO> getUserInfo() {
        String userId = UserContext.getUserId();
        log.info("获取用户信息{}", userId);
        return sysUserService.getUserProfile(userId);
    }

}

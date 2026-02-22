package com.bookingstudyserve.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.UserBindDTO;
import com.bookingstudyserve.domain.po.BizBooking;
import com.bookingstudyserve.domain.po.SysUser;
import com.bookingstudyserve.domain.vo.UserProfileVO;
import com.bookingstudyserve.mapper.SysUserMapper;
import com.bookingstudyserve.service.IBizBookingService;
import com.bookingstudyserve.service.ISysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统用户表 服务实现类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Service
@Slf4j
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Autowired
    private IBizBookingService bookingService;
    @Override
    public Result<String> bindStudentInfo(String userId, UserBindDTO dto) {
        SysUser user = this.getById(userId);
        if (user.getAuditStatus() == 1) {
            return Result.error("您的申请正在审核中，请勿重复提交");
        }
        if (user.getAuditStatus() == 2) {
            return Result.error("您已通过审核，无需再次绑定");
        }

        user.setRealName(dto.getRealName());
        user.setStudentId(dto.getStudentId());
        user.setRole(dto.getRole());
        user.setAuditStatus(1); // 设置为 1-审核中

        this.updateById(user);
        return Result.success("提交成功，请等待管理员审核");
    }

    @Override
    public Result<UserProfileVO> getUserProfile(String userId) {
        // 1. 查用户信息
        SysUser user = this.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 2. 查预约数量 (去 biz_booking 表查 count)
        // 条件：user_id = 当前用户
        long bookingCount = bookingService.count(
                new LambdaQueryWrapper<BizBooking>()
                        .eq(BizBooking::getUserId, userId)
                        .eq(BizBooking::getStatus, 1)
        );

        // 3. 查违规数量 (假设你有 biz_violation 表，没有就先写 0)
        //TODO: 假设没有 biz_violation 表，先写死 0
        long violationCount = 0;
        // long violationCount = violationService.count(...);

        // 4. 组装 VO 对象
        UserProfileVO vo = new UserProfileVO();
        vo.setUser(user);

        UserProfileVO.Stats stats = new UserProfileVO.Stats();
        stats.setBookingCount(bookingCount);
        stats.setViolationCount(violationCount);
        stats.setScore(100); // 信用分先写死 100，以后再做扣分逻辑

        vo.setStats(stats);
        log.info("获取用户信息成功：{}", vo);
        return Result.success(vo);
    }

    @Override
    public SysUser login(String studentId, String password) {
        return this.lambdaQuery()
                .eq(SysUser::getStudentId, studentId)
                .eq(SysUser::getPassword, password)
                .one();
    }

}

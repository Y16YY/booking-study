package com.bookingstudyserve.service;

import com.bookingstudyserve.common.Result;
import com.bookingstudyserve.domain.dto.UserBindDTO;
import com.bookingstudyserve.domain.po.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bookingstudyserve.domain.vo.UserProfileVO;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
public interface ISysUserService extends IService<SysUser> {

    Result<String> bindStudentInfo(String userId, UserBindDTO dto);

    Result<UserProfileVO> getUserProfile(String userId);
}

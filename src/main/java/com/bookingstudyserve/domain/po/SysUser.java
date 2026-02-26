package com.bookingstudyserve.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 系统用户表
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键：用户id
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private String userId;

    /**
     * 微信OpenID
     */
    @TableField("openid")
    private String openid;

    /**
     * 学号
     */
    @TableField("student_id")
    private String studentId;

    /**
     * 加密密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色：1-学生, 2-教师, 9-管理员
     */
    private Integer role;

    /**
     * 所属院系
     */
    private String deptName;

    /**
     * 人脸识别特征值
     */
    private String faceToken;

    /**
     * 账号状态：1-正常, 0-禁用(黑名单)
     */
    private Integer status;
    //审核学号姓名
    private Integer auditStatus; // 0-未绑定, 1-审核中, 2-已通过, 3-驳回

    /**
     * 黑名单解封时间
     */
    private LocalDateTime blacklistEndTime;

    /**
     * 注册时间
     */
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String className;


}

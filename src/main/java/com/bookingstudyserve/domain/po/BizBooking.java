package com.bookingstudyserve.domain.po;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * <p>
 * 预约记录主表
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("biz_booking")
public class BizBooking implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long bookingId;

    private String userId;
    private Integer roomId;
    private LocalDate bookingDate;

    // === 注意：PO里存的是具体的开始和结束，不是List ===
    private Integer startSlot;
    private Integer endSlot;

    private Integer status;
    private Integer usageType;

    // === 下面是这次要新增/确认的字段 ===

    // 1. 对应数据库 course_name课程名称
    private String courseName;

    // 2. 对应数据库 class_info班级信息
    private String classInfo;

    // 3. 【新增】对应数据库 description (前端传的主题/用途)
    private String description;

    // 4. 【新增】对应数据库 supervisor_name (前端传的指导教师名)
    private String supervisorName;

    // 5. 原有的指导意见字段 (目前留空，给管理员审核用)
    private String supervisorOpinion;

    private String fileUrl;
    private String rejectReason;

    // 6. 【新增】对应数据库 repeat_type (如果加了的话)
    private Integer repeatType;

    private LocalDateTime createTime;
    private LocalDateTime auditTime;
    private String auditorId;


}

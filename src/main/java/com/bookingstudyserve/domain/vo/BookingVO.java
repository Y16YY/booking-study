package com.bookingstudyserve.domain.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingVO {
    // 来自 sys_booking 表
    private Long bookingId;
    private LocalDate bookingDate;
    private Integer startSlot;
    private Integer endSlot;
    private String description;
    private Integer status; // 1-待审批, 2-通过, 3-驳回

    // 来自 biz_user 表
    private String userId;
    private String realName;   // 真实姓名
    private String studentId;  // 学号/工号
    private String deptName;   // 所属院系
    private Integer role;
}
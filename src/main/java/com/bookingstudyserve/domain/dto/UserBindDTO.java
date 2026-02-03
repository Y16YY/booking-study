package com.bookingstudyserve.domain.dto;

import lombok.Data;

@Data
public class UserBindDTO {
    private String realName;   // 真实姓名
    private String studentId;  // 学号
    private Integer role;  // 1-学生, 2-教师
}

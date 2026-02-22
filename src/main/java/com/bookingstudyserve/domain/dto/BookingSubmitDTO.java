package com.bookingstudyserve.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookingSubmitDTO {
    private Integer roomId;
    private String date;
    private String section;
    private List<Integer> slots;
    private Integer usageType;

    // 分离清晰的字段
    private String courseName;      // 对应数据库 course_name
    private String classInfo;       // 对应数据库 class_info

    // 把前端传来的 trainingTheme 或 usagePurpose 统一放到这个字段接
    private String description;     // 对应数据库 description (新增的)

    // 把前端传来的 instructor 放到这个字段接
    private String supervisorName;  // 对应数据库 supervisor_name (新增的)

    private String fileUrl;
    private Integer repeatType; // 0-单次, 1-每周, 2-双周
    private String endDate;      // 批量预约的结束日期
}

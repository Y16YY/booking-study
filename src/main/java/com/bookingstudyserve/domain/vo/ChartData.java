package com.bookingstudyserve.domain.vo;

import lombok.Data;

@Data
public class ChartData {
    private String name;   // 维度名称（如日期、教室号、院系名）
    private Integer value; // 统计数值（预约次数）
}

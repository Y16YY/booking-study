package com.bookingstudyserve.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 教室资源表
 * </p>
 *
 * @author 16YY
 * @since 2026-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sys_room")
public class SysRoom implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 教室ID
     */
    @TableId(value = "room_id", type = IdType.AUTO)
    private Integer roomId;

    /**
     * 教室号 (如 101)
     */
    private String roomNumber;

    /**
     * 类型：1-小微格(15间), 2-中微格(1间)
     */
    private Boolean roomType;

    /**
     * 状态：1-启用/空闲, 2-维修中
     */
    private Integer status;

    /**
     * 容纳人数
     */
    private Integer capacity;

    /**
     * 签到二维码唯一标识
     */
    private String qrCodeStr;

    /**
     * 设备描述
     */
    private String equipmentInfo;


}

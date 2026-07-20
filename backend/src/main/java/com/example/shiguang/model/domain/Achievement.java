package com.example.shiguang.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("achievement")
public class Achievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long goalId;
    private String goalTitle;
    private Integer milestoneDays;
    private String badgeName;
    private LocalDate achievedDate;
}

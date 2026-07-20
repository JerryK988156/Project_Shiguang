package com.example.shiguang.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("goal_tag")
public class GoalTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long goalId;
    private String tagName;
}

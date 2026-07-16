package com.example.shiguang.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckinDTO {
    private Long goalId;
    private LocalDate checkinDate;
    private Integer studyDuration;
    private String content;
    private String remark;
}

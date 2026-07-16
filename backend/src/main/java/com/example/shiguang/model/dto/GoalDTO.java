package com.example.shiguang.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GoalDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer targetDays;
    private String status;
}

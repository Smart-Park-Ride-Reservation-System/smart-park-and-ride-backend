package com.project.smartparking.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatDto {
    private String label;
    private String value;
}
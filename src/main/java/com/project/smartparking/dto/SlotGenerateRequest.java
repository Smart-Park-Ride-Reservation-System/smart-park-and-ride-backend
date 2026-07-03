package com.project.smartparking.dto;

import lombok.Data;
import java.util.List;

@Data
public class SlotGenerateRequest {
    private Long lotId;
    private List<List<String>> grid;
}
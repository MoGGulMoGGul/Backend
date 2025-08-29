package com.momo.momo_backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiTaskResponseDto {
    @JsonProperty("task_id")
    private String taskId;
}
package org.example.api.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStateDto {

    @NonNull
    private Long id;

    @NonNull
    private String name;

    private Long leftTaskStateId;

    private Long rightTaskStateId;

    @NonNull
    List<TaskDto> tasks;
}

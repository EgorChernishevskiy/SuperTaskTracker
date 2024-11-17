package org.example.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDto {

    @NonNull
    private Long id;

    @NonNull
    private String name;

    private int position;

    @NonNull
    private String description;

}

package org.example.api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    @NonNull
    private Long id;

    @NonNull
    private String username;
}

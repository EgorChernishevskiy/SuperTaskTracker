package org.example.api.mappers;

import org.example.api.dto.UserDto;
import org.example.store.entities.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {

    public UserDto createUserDto(UserEntity userEntity) {

        return UserDto.builder()
                .id(userEntity.getId())
                .username(userEntity.getUsername())
                .build();
    }
}

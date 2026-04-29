package com.innowise.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegistrationDto {
    private Long id;
    private String name;
    private String surname;
    private LocalDate birthDate;
    private String username;
    private String email;
    private String password;


    public UserRequestDto toUserPart(){
        return UserRequestDto.builder()
                .id(this.id)
                .name(this.name)
                .surname(this.surname)
                .birthDate(this.birthDate)
                .email(this.email)
                .build();
    }

    public AuthRequestDto toAuthPart(Long id){
        return AuthRequestDto.builder()
                .id(id)
                .username(this.username)
                .password(this.password)
                .build();
    }
}

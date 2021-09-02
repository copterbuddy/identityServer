package com.example.iden2.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class UserInfoRequest {

    @NotBlank
    private String client_id;

    @NotBlank
    private String user_token;
}

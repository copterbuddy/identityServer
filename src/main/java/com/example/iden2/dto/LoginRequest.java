package com.example.iden2.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class LoginRequest {

    @NotBlank
    private String client_id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String grant_type;

    @NotNull
    private String scope;

    @NotBlank
    private String client_secret;
}

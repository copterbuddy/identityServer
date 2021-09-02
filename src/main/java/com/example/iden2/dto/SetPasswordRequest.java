package com.example.iden2.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class SetPasswordRequest {
    @NotBlank
    String client_id;
    @NotBlank
    String password;

}

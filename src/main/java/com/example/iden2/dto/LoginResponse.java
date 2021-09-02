package com.example.iden2.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String access_token;
    private int expires_in;
    private String token_type;
}

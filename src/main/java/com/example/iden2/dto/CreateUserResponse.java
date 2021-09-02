package com.example.iden2.dto;

import com.example.iden2.dto.base.ErrorEntityWithLang;

import lombok.Data;

@Data
public class CreateUserResponse extends ErrorEntityWithLang {
    UserDto result;
}

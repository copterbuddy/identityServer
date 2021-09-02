package com.example.iden2.dto;

import java.util.List;

import com.example.iden2.dto.base.ErrorEntityWithLang;

import lombok.Data;

@Data
public class ListUserResponse extends ErrorEntityWithLang {
    List<UserDto> result;
}

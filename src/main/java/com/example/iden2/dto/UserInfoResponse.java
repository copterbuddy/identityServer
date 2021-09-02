package com.example.iden2.dto;

import com.example.iden2.dto.base.ErrorEntity;

import lombok.Data;

@Data
public class UserInfoResponse extends ErrorEntity {

    private UserDto result;
}

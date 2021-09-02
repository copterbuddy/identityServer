package com.example.iden2.dto.base;

import java.util.List;

import lombok.Data;

@Data
public class ErrorEntity {
    private Boolean success;
    private List<String> message;
    private String errorCode;
}

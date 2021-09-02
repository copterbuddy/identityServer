package com.example.iden2.dto.base;

import lombok.Data;

@Data
public class ErrorEntityWithLang extends ErrorEntity {
    private String errorMessageEN;
    private String errorMessageTH;
}

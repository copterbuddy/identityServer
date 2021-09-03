package com.example.iden2.util;

import java.util.ArrayList;
import java.util.List;

import com.example.iden2.dto.base.ErrorEntity;
import com.example.iden2.dto.base.ErrorEntityWithLang;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ExceptionUtil {

    public ErrorEntityWithLang CreateErrorWithLang(ErrorEntityWithLang errorEntityWithLang) throws Exception {
        ErrorEntityWithLang response = new ErrorEntityWithLang();

        // CreateError Method
        ErrorEntity subResponse = new ErrorEntity();
        subResponse.setErrorCode(errorEntityWithLang.getErrorCode());
        ErrorEntity createError = CreateError(subResponse);
        var mapper = new ModelMapper();
        mapper.map(createError, response);

        String errCode = errorEntityWithLang.getErrorCode();
        if (errCode.startsWith("001")) {
            response.setErrorMessageEN("invalid request");
            response.setErrorMessageTH("ข้อมูลนำเข้าไม่ถูกต้อง");
        } else if (errCode.startsWith("200")) {
            response.setErrorMessageEN("success");
            response.setErrorMessageTH("สำเร็จ");

        } else if (errCode.startsWith("401")) {
            response.setErrorMessageEN("UnAuthorize");
            response.setErrorMessageTH("ไม่พบการยืนยันตัวตน");

        } else if (errCode.startsWith("404")) {
            response.setErrorMessageEN("not found user token");
            response.setErrorMessageTH("ไม่พบโทเคนของผู้ใช้");

        } else if (errCode.startsWith("500")) {
            response.setErrorMessageEN("internal server error");
            response.setErrorMessageTH("เกิดข้อผิดพลาด");

        } else {
            response.setErrorMessageEN("internal server error");
            response.setErrorMessageTH("เกิดข้อผิดพลาด");
        }

        return response;
    }

    public ErrorEntity CreateError(ErrorEntity errorEntity) throws Exception {

        ErrorEntity response = new ErrorEntity();
        response.setErrorCode(errorEntity.getErrorCode());

        String errCode = errorEntity.getErrorCode();

        if (errCode.startsWith("200")) {
            response.setSuccess(true);
        } else {
            response.setSuccess(false);
        }

        List<String> errMessage = new ArrayList<>();
        if (errCode.startsWith("001")) {
            errMessage.add("invalid request");
        } else if (errCode.startsWith("200")) {
            errMessage.add("success");

        } else if (errCode.startsWith("401")) {
            errMessage.add("invalid service token");

        } else if (errCode.startsWith("404")) {
            errMessage.add("not found user token");

        } else if (errCode.startsWith("500")) {
            errMessage.add("internal server error");

        } else {
            errMessage.add("invalid flow");
        }
        response.setMessage(errMessage);

        return response;
    }
}

package com.example.iden2.util;

import java.util.ArrayList;
import java.util.List;

import com.example.iden2.dto.base.ErrorEntity;
import com.example.iden2.dto.base.ErrorEntityWithLang;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ExceptionUtil {

    public ErrorEntityWithLang CreateErrorWithLang(ErrorEntityWithLang errorEntityWithLang) {
        ErrorEntityWithLang response = new ErrorEntityWithLang();

        // CreateError Method
        ErrorEntity subResponse = new ErrorEntity();
        subResponse.setErrorCode(errorEntityWithLang.getErrorCode());
        ErrorEntity createError = CreateError(subResponse);
        var mapper = new ModelMapper();
        mapper.map(createError, response);

        String errCode = errorEntityWithLang.getErrorCode();
        switch (errCode) {
            case "001":
                response.setErrorMessageEN("invalid request");
                response.setErrorMessageTH("ข้อมูลนำเข้าไม่ถูกต้อง");
                break;
            case "200":
                response.setErrorMessageEN("success");
                response.setErrorMessageTH("สำเร็จ");
                break;
            case "401":
                response.setErrorMessageEN("");
                response.setErrorMessageTH("");
                break;
            case "404":
                response.setErrorMessageEN("not found user token");
                response.setErrorMessageTH("ไม่พบโทเคนของผู้ใช้");
                break;
            case "500":
                response.setErrorMessageEN("internal server error");
                response.setErrorMessageTH("เกิดข้อผิดพลาด");
                break;
            default:
                response.setErrorMessageEN("internal server error");
                response.setErrorMessageTH("เกิดข้อผิดพลาด");
                break;
        }

        return response;
    }

    public ErrorEntity CreateError(ErrorEntity errorEntity) {

        ErrorEntity response = new ErrorEntity();
        response.setErrorCode(errorEntity.getErrorCode());

        String errCode = errorEntity.getErrorCode();

        if (errCode.equals("") || errCode.equals("200")) {
            response.setSuccess(true);
        } else {
            response.setSuccess(false);
        }

        List<String> errMessage = new ArrayList<>();
        switch (errCode) {
            case "001":
                errMessage.add("invalid request");
                break;
            case "200":
                errMessage.add("success");
                break;
            case "401":
                errMessage.add("invalid service token");
                break;
            case "404":
                errMessage.add("not found user token");
                break;
            case "500":
                errMessage.add("internal server error");
                break;
            default:
                errMessage.add("invalid flow");
                break;
        }
        response.setMessage(errMessage);

        return response;
    }
}

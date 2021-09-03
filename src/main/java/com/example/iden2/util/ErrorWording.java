package com.example.iden2.util;

import org.springframework.stereotype.Component;

@Component
public class ErrorWording {
    public String _001_INVALID_REQUEST = "001 : invalid request";
    public String _200_SUCCESS = "200 : success";
    public String _401_INVALID_SERVICE_TOKEN = "401 : invalid service token";
    public String _404_NOT_FOUND_USER_TOKEN = "404 : not found user token";
    public String _500_INTERNAL_SERVER_ERROR = "500 : internal server error";

}
package com.example.iden2.dto;

import java.util.Date;

import lombok.Data;

@Data
public class UserInfoDto {
    private Long id;
    private String clientid;
    private String email;
    private String firstname;
    private String lastname;
    private String role;
    private String status;
    private String otpstatus;
    private String pin;
    private String userid;
    private String mobileno;
    private String mobilecountrycode;
    private String citizenid;
    private String passportno;
    private String idtype;
    private Date dateofbirth;
    private String createuser;
    private Date createdate;
    private String updateuser;
    private Date updatedate;
    private Date registerdate;
    private String cifno;
    private Date lastlogon;
    private String historychangepwd;
    private Date lastchangepwd;
    private String passwordstatus;
    private String pinstatus;
}
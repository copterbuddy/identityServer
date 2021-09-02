package com.example.iden2.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ListUserRequest {

    @NotBlank
    String client_id;
    String username;
    String user_id;
    String firstname;
    String lastname;
    String cif_no;
    String id_no;
    String mobile_no;
    String email;
    String register_date_from;
    String register_date_to;
    String status;
    String otp_status;
    String pin_status;
    int page;
    int row_per_page;
    Boolean include_delete;
}

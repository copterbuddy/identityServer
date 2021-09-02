package com.example.iden2.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.hibernate.validator.constraints.Length;

import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank
    String client_id;
    String firstname;
    String lastname;
    String email;
    String username;
    @Length(max = 30)
    String mobile_no;
    @Length(max = 5)
    String mobile_country_code;
    @Length(max = 20)
    String citizen_id;
    @Length(max = 50)
    String passport_no;
    @Length(max = 20)
    String id_type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    String date_of_birth;

}

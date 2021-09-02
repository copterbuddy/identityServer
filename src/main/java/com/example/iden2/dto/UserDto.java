package com.example.iden2.dto;

import java.util.List;

import lombok.Data;

@Data
public class UserDto {
    public String id;
    public String name;
    public String given_name;
    public String family_name;
    public String email;
    public String preferred_username;
    public String client_id;
    public String user_id;
    public String date_of_birth;
    public String mobile_no;
    public String mobile_country_code;
    public String status;
    public String otp_status;
    public String pin_status;
    public String citizen_id;
    public String passport_no;
    public String id_type;
    public String[] role;
    public String register_date;
    public String cif_no;

}

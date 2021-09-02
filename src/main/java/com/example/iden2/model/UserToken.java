package com.example.iden2.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Entity
@Data
@Table(name = "Usertoken")
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US")
    private Date startdate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "en_US")
    private Date expiredate;
    private Long refuserid;
    private Long refclientid;
    private String status;
    private Date refreshdatetime;
}

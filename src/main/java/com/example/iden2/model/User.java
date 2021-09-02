package com.example.iden2.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

@Entity
@Data
@Table(name = "[User]")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clientid;
    private String email;
    private String username;
    private String password;
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
    @CreatedBy
    private String createuser;
    @CreatedDate
    private Date createdate;
    @LastModifiedBy
    private String updateuser;
    @LastModifiedDate
    private Date updatedate;
    private Date registerdate;
    private String cifno;
    private Date lastlogon;
    private String historychangepwd;
    private Date lastchangepwd;
    private String passwordstatus;
    private String pinstatus;

}
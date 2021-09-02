package com.example.iden2.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "Clients")
public class Clients {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(name = "ClientId")
    private String clientid;

    // @Column(name = "ClientName")
    private String clientname;

    // @Column(name = "Clienturi")
    private String clienturi;

    // @Column(name = "ConsentLifetime")
    private int consentlifetime;

    // @Column(name = "Description")
    private String description;

    // @Column(name = "EnableRefreshConsentLifetime")
    private boolean enablerefreshconsentlifetime;

    // @Column(name = "Enabled")
    private boolean enabled;

    // @Column(name = "EnableSecondaryLogin")
    private boolean enablesecondarylogin;
}

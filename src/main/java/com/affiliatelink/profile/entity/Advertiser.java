package com.affiliatelink.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "profile_advertisers")
public class Advertiser extends Profile{

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column
    private String sector;

    @Column
    private String website;

    protected Advertiser(){}

    public Advertiser(String email, String companyName){
        super(email);
        this.companyName = companyName;
    }

    public Advertiser(String email, String companyName, String sector, String website){
        this(email, companyName);
        this.sector = sector;
        this.website = website;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getSector() {
        return sector;
    }

    public String getWebsite() {
        return website;
    }
}

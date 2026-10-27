package com.affiliatelink.offer.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "offer_campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutModel payoutModel;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payoutValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    protected Campaign(){

    }

    public Campaign(String name, LocalDate startDate, LocalDate endDate, BigDecimal budget, PayoutModel payoutModel, BigDecimal payoutValue, Offer offer) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budget = budget;
        this.payoutModel = payoutModel;
        this.payoutValue = payoutValue;
        this.status = CampaignStatus.DRAFT;
        this.offer = offer;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public PayoutModel getPayoutModel() {
        return payoutModel;
    }

    public void setPayoutModel(PayoutModel payoutModel) {
        this.payoutModel = payoutModel;
    }

    public BigDecimal getPayoutValue() {
        return payoutValue;
    }

    public void setPayoutValue(BigDecimal payoutValue) {
        this.payoutValue = payoutValue;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public Offer getOffer(){
        return offer;
    }



}

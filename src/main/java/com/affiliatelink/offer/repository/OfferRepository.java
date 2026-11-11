package com.affiliatelink.offer.repository;

import com.affiliatelink.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OfferRepository
        extends JpaRepository<Offer, Long>,
                JpaSpecificationExecutor<Offer> {
}
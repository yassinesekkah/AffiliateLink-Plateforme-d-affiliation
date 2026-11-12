package com.affiliatelink.offer.repository;

import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignRepository  extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByOfferIdAndStatus(
            Long offerId,
            CampaignStatus status
    );

    List<Campaign> findByOfferAdvertiserIdAndStatus(
            Long advertiserId,
            CampaignStatus status
    );


}

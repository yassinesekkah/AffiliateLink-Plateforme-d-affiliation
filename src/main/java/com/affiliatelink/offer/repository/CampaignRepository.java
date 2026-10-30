package com.affiliatelink.offer.repository;

import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository  extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status);
}

package com.affiliatelink.profile.repository;

import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.entity.AccountStatus;
import com.affiliatelink.profile.entity.Advertiser;
import com.affiliatelink.profile.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdvertiserRepository extends JpaRepository<Advertiser, Long> {

}

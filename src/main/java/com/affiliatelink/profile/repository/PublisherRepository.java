package com.affiliatelink.profile.repository;

import com.affiliatelink.profile.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
public interface PublisherRepository extends JpaRepository<Publisher, Long> {

}

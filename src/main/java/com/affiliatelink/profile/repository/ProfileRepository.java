package com.affiliatelink.profile.repository;

import com.affiliatelink.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByEmail(String email);

}

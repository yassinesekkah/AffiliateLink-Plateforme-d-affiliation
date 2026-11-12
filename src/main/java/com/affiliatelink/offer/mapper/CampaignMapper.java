package com.affiliatelink.offer.mapper;

import com.affiliatelink.offer.dto.CampaignRequest;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.entity.Campaign;
import com.affiliatelink.offer.entity.Offer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CampaignMapper {

    @Mapping(target = "offerId", source = "offer.id")
    CampaignResponse toResponse(Campaign campaign);

    List<CampaignResponse> toResponses(List<Campaign> campaigns);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "offer", ignore = true)
    Campaign toEntity(CampaignRequest request);
}

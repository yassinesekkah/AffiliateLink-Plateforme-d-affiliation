package com.affiliatelink.offer.mapper;

import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    OfferResponse toResponse(Offer offer);

    default String map(Tag tag){
        return tag.getName();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "campaigns", ignore = true)
    @Mapping(target = "tags", ignore = true)
    Offer toEntity(OfferRequest request);
}

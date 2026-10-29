package com.affiliatelink.offer.service;

import com.affiliatelink.common.exception.BusinessConflictException;
import com.affiliatelink.common.exception.ResourceNotFoundException;
import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Offer;
import com.affiliatelink.offer.entity.Tag;
import com.affiliatelink.offer.mapper.OfferMapper;
import com.affiliatelink.offer.repository.OfferRepository;
import com.affiliatelink.offer.repository.TagRepository;
import com.affiliatelink.profile.AdvertiserVerification;
import com.affiliatelink.profile.Niche;
import com.affiliatelink.profile.ProfileApi;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProfileApi profileApi;
    private final OfferMapper offerMapper;
    private final TagRepository tagRepository;


    public OfferService(OfferRepository offerRepository, ProfileApi profileApi, OfferMapper offerMapper, TagRepository tagRepository) {
        this.offerRepository = offerRepository;
        this.profileApi = profileApi;
        this.offerMapper = offerMapper;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public OfferResponse create(OfferRequest request){

        ///verifier l'advertiser on le module profile vers public api
        AdvertiserVerification result = profileApi.verifyAdvertiser(request.advertiserId());

        if (result == AdvertiserVerification.NOT_FOUND) {
            throw new ResourceNotFoundException("Advertiser not found");
        }

        if (result == AdvertiserVerification.INACTIVE) {
            throw new BusinessConflictException("Advertiser is not active");
        }

        ///Mapping request to entity
        Offer offer = offerMapper.toEntity(request);

        ///tags find or add
        if(request.tags() != null){

            for(String name : request.tags()){

                String cleanName = name.trim().toLowerCase(Locale.ROOT);

                Tag tag = tagRepository.findByName(cleanName)
                        .orElseGet(() -> tagRepository.save(new Tag(cleanName)));

                ///add new tag to offer tags set
                offer.getTags().add(tag);

            }
        }

        ///save on database
        Offer savedOffer = offerRepository.save(offer);

        return offerMapper.toResponse(savedOffer);

    }

    public Page<OfferResponse> getAll(
            Pageable pageable,
            Niche niche,
            Long advertiserId,
            String tag,
            String keyword ){

        Specification<Offer> spec = (root, query, cb) ->{

            List<Predicate> conditions = new ArrayList<>();

            if(niche != null){
                conditions.add(cb.equal(root.get("niche"), niche));
            }

            if(advertiserId != null){
                conditions.add(cb.equal(root.get("advertiserId"), advertiserId));
            }

            if(keyword != null && !keyword.isBlank()){
                conditions.add(
                        cb.like(
                                cb.lower(root.get("title")),
                                "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%")
                );
            }

            if(tag != null && !tag.isBlank()){
                Join<Offer, Tag> joinTags = root.join("tags");

                conditions.add(
                        cb.equal(joinTags.get("name"), tag.trim().toLowerCase(Locale.ROOT) )
                );
            }

            return cb.and(conditions.toArray(new Predicate[0]));

        };

       Page<Offer> offers = offerRepository.findAll(spec, pageable);

        return offers.map(offer -> offerMapper.toResponse(offer));
    }
}

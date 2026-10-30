package com.affiliatelink.offer.controller;

import com.affiliatelink.offer.dto.CampaignRequest;
import com.affiliatelink.offer.dto.CampaignResponse;
import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Tag;
import com.affiliatelink.offer.service.CampaignService;
import com.affiliatelink.offer.service.OfferService;
import com.affiliatelink.profile.Niche;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/offres")
public class OfferController {

    private final OfferService offerService;
    private final CampaignService campaignService;

    public OfferController(OfferService offerService, CampaignService campaignService) {
        this.offerService = offerService;
        this.campaignService = campaignService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse createOffer (@Valid @RequestBody OfferRequest request){

        return offerService.create(request);
    }

    @GetMapping
    public Page<OfferResponse> getOffers(
            @RequestParam(required = false) Niche niche,
            @RequestParam(required = false) Long advertiserId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String keyword,
            Pageable pageable){

        return offerService.getAll(pageable, niche, advertiserId, tag, keyword);
    }

    @GetMapping("/{id}")
    public OfferResponse getOfferById(@PathVariable Long id){

        return offerService.getById(id);
    }


    @PutMapping("/{id}")
    public OfferResponse updateOffer(@PathVariable Long id, @RequestBody @Valid OfferRequest request){

        return offerService.updateOffer(id, request);
    }

    @PostMapping("{offerId}/campaigns")
    public CampaignResponse createCampaign(@PathVariable Long offerId, @RequestBody @Valid CampaignRequest request){

        return campaignService.create(offerId, request);
    }
}

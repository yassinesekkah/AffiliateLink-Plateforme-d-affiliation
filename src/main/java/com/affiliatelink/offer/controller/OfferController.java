package com.affiliatelink.offer.controller;

import com.affiliatelink.offer.dto.OfferRequest;
import com.affiliatelink.offer.dto.OfferResponse;
import com.affiliatelink.offer.entity.Tag;
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


    public OfferController(OfferService offerService) {
        this.offerService = offerService;
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
}

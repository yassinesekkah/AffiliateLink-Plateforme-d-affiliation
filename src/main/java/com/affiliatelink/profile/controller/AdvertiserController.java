package com.affiliatelink.profile.controller;

import com.affiliatelink.profile.dto.AdvertierResponse;
import com.affiliatelink.profile.dto.AdvertiserRequest;
import com.affiliatelink.profile.service.AdvertiserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdvertiserController {

    private final AdvertiserService advertiserService;

    public AdvertiserController(AdvertiserService advertiserService){
        this.advertiserService = advertiserService;
    }

    @PostMapping("/api/advertisers")
    public ResponseEntity<AdvertierResponse> createAdvertiser (@Valid @RequestBody AdvertiserRequest request){

        AdvertierResponse response = advertiserService.createAdvertiser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/api/advertisers")
    public ResponseEntity<List<AdvertierResponse>> findAll(){

        List<AdvertierResponse> advertierResponses = advertiserService.findAll();

        return ResponseEntity.ok(advertierResponses);
    }
}

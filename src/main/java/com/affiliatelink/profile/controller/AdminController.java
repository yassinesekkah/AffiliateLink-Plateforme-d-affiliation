package com.affiliatelink.profile.controller;

import com.affiliatelink.profile.dto.UpdateProfileStatusRequest;
import com.affiliatelink.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminController {

    private final ProfileService profileService;

    public AdminController(ProfileService profileService){
        this.profileService = profileService;
    }

    @PatchMapping("/api/admin/profiles/{id}/status")
    public ResponseEntity<Void> updateStatus(@Valid @RequestBody UpdateProfileStatusRequest request,
                                             @PathVariable(name = "id") Long id){

         profileService.updateStatus(id, request);

         return ResponseEntity.noContent().build();
    }
}

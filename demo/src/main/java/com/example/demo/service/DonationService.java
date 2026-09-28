package com.example.demo.service;

import com.example.demo.dto.DonationRequest;
import com.example.demo.dto.DonationResponse;
import com.example.demo.entity.DonationRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationService {
    private final DonorService donorService;

    public DonationService(DonorService donorService) {
        this.donorService = donorService;
    }

    @Transactional
    public DonationResponse record(DonationRequest request) {
        DonationRecord donation = donorService.recordDonation(request.donorId(), request.donationDate());
        return new DonationResponse(
                donation.getId(),
                donation.getDonor().getId(),
                donation.getDonor().getName(),
                donation.getDonationDate()
        );
    }
}

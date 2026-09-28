package com.example.demo.dto;

import java.time.LocalDate;

public record DonationResponse(Long id, Long donorId, String donorName, LocalDate donationDate) {
}

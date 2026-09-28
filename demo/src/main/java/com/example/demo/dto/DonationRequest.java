package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DonationRequest(
        @NotNull Long donorId,
        @NotNull @PastOrPresent LocalDate donationDate
) {
}

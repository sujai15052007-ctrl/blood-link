package com.example.demo.dto;

import com.example.demo.entity.BloodGroup;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record DonorRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 30) String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull BloodGroup bloodGroup,
        @NotBlank @Size(max = 120) String city,
        @PastOrPresent LocalDate lastDonationDate
) {
}

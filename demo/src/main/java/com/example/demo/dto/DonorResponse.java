package com.example.demo.dto;

import com.example.demo.entity.BloodGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonorResponse(
        Long id,
        String name,
        String phone,
        String email,
        BloodGroup bloodGroup,
        String bloodGroupLabel,
        String city,
        LocalDate lastDonationDate,
        LocalDate nextAvailableDate,
        boolean available,
        LocalDateTime createdAt
) {
}

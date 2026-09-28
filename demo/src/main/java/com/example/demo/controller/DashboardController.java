package com.example.demo.controller;

import com.example.demo.dto.BloodGroupCount;
import com.example.demo.dto.DashboardStats;
import com.example.demo.service.DonorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DonorService donorService;

    public DashboardController(DonorService donorService) {
        this.donorService = donorService;
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return donorService.getStats();
    }

    @GetMapping("/blood-groups")
    public List<BloodGroupCount> bloodGroups() {
        return donorService.getBloodGroupCounts();
    }
}

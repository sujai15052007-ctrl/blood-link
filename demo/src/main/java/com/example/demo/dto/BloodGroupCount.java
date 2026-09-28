package com.example.demo.dto;

import com.example.demo.entity.BloodGroup;

public record BloodGroupCount(BloodGroup bloodGroup, String label, long total, long available) {
}

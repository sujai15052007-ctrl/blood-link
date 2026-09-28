package com.example.demo.controller;

import com.example.demo.dto.DonorRequest;
import com.example.demo.dto.DonorResponse;
import com.example.demo.entity.BloodGroup;
import com.example.demo.service.DonorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/donors")
public class DonorController {
    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    @PostMapping
    public ResponseEntity<DonorResponse> create(@Valid @RequestBody DonorRequest request) {
        DonorResponse created = donorService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<DonorResponse> findAll() {
        return donorService.findAll();
    }

    @GetMapping("/search")
    public List<DonorResponse> search(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city
    ) {
        BloodGroup group = bloodGroup == null || bloodGroup.isBlank()
                ? null
                : BloodGroup.fromLabel(bloodGroup.trim());
        return donorService.search(group, city);
    }

    @GetMapping("/{id}")
    public DonorResponse findById(@PathVariable Long id) {
        return donorService.findById(id);
    }

    @PutMapping("/{id}")
    public DonorResponse update(@PathVariable Long id, @Valid @RequestBody DonorRequest request) {
        return donorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        donorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

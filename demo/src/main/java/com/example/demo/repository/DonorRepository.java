package com.example.demo.repository;

import com.example.demo.entity.BloodGroup;
import com.example.demo.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    List<Donor> findByBloodGroup(BloodGroup bloodGroup);

    List<Donor> findByCityContainingIgnoreCase(String city);

    List<Donor> findByBloodGroupAndCityContainingIgnoreCase(BloodGroup bloodGroup, String city);
}

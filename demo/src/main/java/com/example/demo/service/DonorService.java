package com.example.demo.service;

import com.example.demo.dto.BloodGroupCount;
import com.example.demo.dto.DashboardStats;
import com.example.demo.dto.DonorRequest;
import com.example.demo.dto.DonorResponse;
import com.example.demo.entity.BloodGroup;
import com.example.demo.entity.DonationRecord;
import com.example.demo.entity.Donor;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DonorNotFoundException;
import com.example.demo.repository.DonationRecordRepository;
import com.example.demo.repository.DonorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class DonorService {
    private static final long COOLDOWN_DAYS = 90;

    private final DonorRepository donorRepository;
    private final DonationRecordRepository donationRecordRepository;

    public DonorService(DonorRepository donorRepository, DonationRecordRepository donationRecordRepository) {
        this.donorRepository = donorRepository;
        this.donationRecordRepository = donationRecordRepository;
    }

    public DonorResponse create(DonorRequest request) {
        Donor donor = new Donor();
        apply(donor, request);
        return toResponse(donorRepository.save(donor));
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> findAll() {
        return donorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DonorResponse findById(Long id) {
        return toResponse(getDonor(id));
    }

    public DonorResponse update(Long id, DonorRequest request) {
        Donor donor = getDonor(id);
        apply(donor, request);
        return toResponse(donorRepository.save(donor));
    }

    public void delete(Long id) {
        donorRepository.delete(getDonor(id));
    }

    @Transactional(readOnly = true)
    public List<DonorResponse> search(BloodGroup bloodGroup, String city) {
        List<Donor> candidates;
        boolean hasCity = city != null && !city.isBlank();
        if (bloodGroup != null && hasCity) {
            candidates = donorRepository.findByBloodGroupAndCityContainingIgnoreCase(bloodGroup, city.trim());
        } else if (bloodGroup != null) {
            candidates = donorRepository.findByBloodGroup(bloodGroup);
        } else if (hasCity) {
            candidates = donorRepository.findByCityContainingIgnoreCase(city.trim());
        } else {
            candidates = donorRepository.findAll();
        }
        return candidates.stream()
                .filter(this::isAvailable)
                .map(this::toResponse)
                .toList();
    }

    public DonationRecord recordDonation(Long donorId, LocalDate donationDate) {
        Donor donor = getDonor(donorId);
        if (!isAvailable(donor)) {
            LocalDate availableOn = donor.getLastDonationDate().plusDays(COOLDOWN_DAYS);
            throw new BusinessRuleException("This donor is in the 90-day cooldown and can donate again on "
                    + availableOn + ".");
        }
        if (donor.getLastDonationDate() != null && donationDate.isBefore(donor.getLastDonationDate())) {
            throw new BusinessRuleException("Donation date cannot be earlier than the donor's last recorded donation.");
        }
        DonationRecord record = new DonationRecord(donationDate);
        donor.addDonation(record);
        donor.setLastDonationDate(donationDate);
        donorRepository.save(donor);
        return donationRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        List<Donor> donors = donorRepository.findAll();
        long available = donors.stream().filter(this::isAvailable).count();
        return new DashboardStats(donors.size(), available, donors.size() - available);
    }

    @Transactional(readOnly = true)
    public List<BloodGroupCount> getBloodGroupCounts() {
        List<Donor> donors = donorRepository.findAll();
        return Arrays.stream(BloodGroup.values())
                .map(group -> {
                    List<Donor> matching = donors.stream()
                            .filter(donor -> donor.getBloodGroup() == group)
                            .toList();
                    long available = matching.stream().filter(this::isAvailable).count();
                    return new BloodGroupCount(group, group.getLabel(), matching.size(), available);
                })
                .toList();
    }

    private Donor getDonor(Long id) {
        return donorRepository.findById(id).orElseThrow(() -> new DonorNotFoundException(id));
    }

    private void apply(Donor donor, DonorRequest request) {
        donor.setName(request.name().trim());
        donor.setPhone(request.phone().trim());
        donor.setEmail(request.email().trim());
        donor.setBloodGroup(request.bloodGroup());
        donor.setCity(request.city().trim());
        donor.setLastDonationDate(request.lastDonationDate());
    }

    private boolean isAvailable(Donor donor) {
        return donor.getLastDonationDate() == null
                || !donor.getLastDonationDate().plusDays(COOLDOWN_DAYS).isAfter(LocalDate.now());
    }

    private DonorResponse toResponse(Donor donor) {
        LocalDate lastDonation = donor.getLastDonationDate();
        LocalDate nextAvailable = lastDonation == null ? null : lastDonation.plusDays(COOLDOWN_DAYS);
        return new DonorResponse(
                donor.getId(),
                donor.getName(),
                donor.getPhone(),
                donor.getEmail(),
                donor.getBloodGroup(),
                donor.getBloodGroup().getLabel(),
                donor.getCity(),
                lastDonation,
                nextAvailable,
                isAvailable(donor),
                donor.getCreatedAt()
        );
    }
}

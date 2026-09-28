package com.example.demo.service;

import com.example.demo.entity.BloodGroup;
import com.example.demo.entity.DonationRecord;
import com.example.demo.entity.Donor;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.repository.DonationRecordRepository;
import com.example.demo.repository.DonorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {
    @Mock
    private DonorRepository donorRepository;

    @Mock
    private DonationRecordRepository donationRecordRepository;

    @InjectMocks
    private DonorService donorService;

    @Test
    void searchExcludesDonorsBeforeNinetiethDayAndIncludesThemOnNinetiethDay() {
        Donor recovering = donorWithLastDonation(LocalDate.now().minusDays(89));
        Donor recovered = donorWithLastDonation(LocalDate.now().minusDays(90));
        when(donorRepository.findAll()).thenReturn(List.of(recovering, recovered));

        var results = donorService.search(null, null);

        assertEquals(1, results.size());
        assertEquals(recovered.getName(), results.get(0).name());
        assertTrue(results.get(0).available());
    }

    @Test
    void recordingDonationDuringCooldownIsRejectedWithoutCreatingRecord() {
        Donor recovering = donorWithLastDonation(LocalDate.now().minusDays(89));
        when(donorRepository.findById(7L)).thenReturn(Optional.of(recovering));

        assertThrows(BusinessRuleException.class,
                () -> donorService.recordDonation(7L, LocalDate.now()));

        verify(donationRecordRepository, never()).save(any(DonationRecord.class));
        verify(donorRepository, never()).save(any(Donor.class));
    }

    @Test
    void recordingDonationUpdatesTheDonorAndCreatesHistory() {
        Donor donor = donorWithLastDonation(null);
        LocalDate donationDate = LocalDate.now();
        when(donorRepository.findById(7L)).thenReturn(Optional.of(donor));
        when(donationRecordRepository.save(any(DonationRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DonationRecord record = donorService.recordDonation(7L, donationDate);

        assertEquals(donationDate, donor.getLastDonationDate());
        assertEquals(donationDate, record.getDonationDate());
        assertEquals(donor, record.getDonor());
        assertFalse(donorService.findById(7L).available());
        verify(donorRepository).save(donor);
        verify(donationRecordRepository).save(record);
    }

    private Donor donorWithLastDonation(LocalDate lastDonationDate) {
        Donor donor = new Donor();
        donor.setName("Test Donor");
        donor.setPhone("555-0100");
        donor.setEmail("donor@example.com");
        donor.setBloodGroup(BloodGroup.O_POSITIVE);
        donor.setCity("Chennai");
        donor.setLastDonationDate(lastDonationDate);
        return donor;
    }
}

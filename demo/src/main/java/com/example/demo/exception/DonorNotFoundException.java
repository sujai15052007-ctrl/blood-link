package com.example.demo.exception;

public class DonorNotFoundException extends RuntimeException {
    public DonorNotFoundException(Long id) {
        super("Donor with id " + id + " was not found.");
    }
}

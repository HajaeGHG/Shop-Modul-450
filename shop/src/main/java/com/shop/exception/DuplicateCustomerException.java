package com.shop.exception;

public class DuplicateCustomerException extends RuntimeException {
    public DuplicateCustomerException(String email) {
        super("Customer with email already exists: " + email);
    }
}
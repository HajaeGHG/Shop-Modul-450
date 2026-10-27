package com.shop.exception;

public class InvalidOrderStateException extends RuntimeException {
    public InvalidOrderStateException(Long orderId, String from, String to) {
        super("Order " + orderId + " cannot transition from " + from + " to " + to);
    }
}
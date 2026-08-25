package com.example.bagservice.domain;

/** A single line item in the customer's bag (cart). */
public record BagItem(
        String id,
        String sku,
        String name,
        String colour,
        String size,
        int quantity,
        double unitPrice) {

    public double lineTotal() {
        return Math.round(unitPrice * quantity * 100.0) / 100.0;
    }
}

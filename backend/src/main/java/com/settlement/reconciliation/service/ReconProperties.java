package com.settlement.reconciliation.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "recon")
public record ReconProperties(
        BigDecimal tolerance,
        Window window
) {
    public record Window(int min, int max) {
    }
}

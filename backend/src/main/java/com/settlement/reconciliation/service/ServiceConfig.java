package com.settlement.reconciliation.service;

import com.settlement.reconciliation.fee.FeeCalculator;
import com.settlement.reconciliation.fee.FeeScheduleLoader;
import com.settlement.reconciliation.ingest.IngestService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReconProperties.class)
public class ServiceConfig {

    @Bean
    IngestService ingestService() {
        return new IngestService(new FeeCalculator(FeeScheduleLoader.loadFromClasspath()));
    }
}

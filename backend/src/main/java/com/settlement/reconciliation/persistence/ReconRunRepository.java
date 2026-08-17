package com.settlement.reconciliation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconRunRepository extends JpaRepository<ReconRunEntity, Long> {

    List<ReconRunEntity> findAllByOrderByRunAtDesc();
}

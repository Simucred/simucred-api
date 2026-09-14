package com.simucred.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.simucred.api.domain.entity.SimulacaoCredito;

import java.util.UUID;

@Repository
public interface SimulacaoRepository extends JpaRepository<SimulacaoCredito, UUID> {
}
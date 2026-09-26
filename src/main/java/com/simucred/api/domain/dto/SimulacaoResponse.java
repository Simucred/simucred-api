package com.simucred.api.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.simucred.api.domain.enums.StatusSimulacao;

public record SimulacaoResponse(
    UUID id,
    String cpf,
    String nome,
    Integer idade,
    BigDecimal rendaMensal,
    BigDecimal valorSolicitado,
    Integer prazoMeses,
    BigDecimal valorParcela,
    BigDecimal taxaJurosMensal,
    StatusSimulacao status,
    String justificativaIa,
    LocalDateTime dataSimulacao) {
}

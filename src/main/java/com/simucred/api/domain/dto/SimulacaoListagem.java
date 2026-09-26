package com.simucred.api.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SimulacaoListagem(
        UUID id,
        LocalDateTime dataSimulacao,
        BigDecimal valorSolicitado,
        Integer prazoMeses,
        BigDecimal valorParcela,
        BigDecimal taxaJurosMensal,
        String status,
        String nome,
        String cpf,
        Integer idade,
        BigDecimal rendaMensal,
        String justificativaIa) {
}

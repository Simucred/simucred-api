package com.simucred.api.domain.dto;

import java.math.BigDecimal;

public record SimulacaoRequest(
    String cpf,
    String nome,
    Integer idade,
    BigDecimal rendaMensal,
    BigDecimal valorSolicitado,
    Integer prazoMeses) {
}

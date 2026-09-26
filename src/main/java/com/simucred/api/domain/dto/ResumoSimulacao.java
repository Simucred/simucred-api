package com.simucred.api.domain.dto;

import java.math.BigDecimal;

public record ResumoSimulacao(
    long total,
    long aprovadas,
    long reprovadas,
    long emAnalise,
    double taxaAprovacao,
    BigDecimal valorMedio) {
}

package com.simucred.api.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record GraficoIaDto(
    String titulo,
    String tipoGrafico,
    List<String> labels,
    List<BigDecimal> valores
) {}

package com.simucred.api.domain.dto;

public record AnaliseIaResponse(
    String explicacaoTexto,
    String resumoImpacto,
    GraficoIaDto grafico
) {}

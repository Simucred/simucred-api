package com.simucred.api.domain.dto;

import java.util.List;

/**
 * Representa a resposta estruturada que o Gemini deve gerar.
 * Contém a explicação didática e os dados para renderizar gráficos.
 */
public record GeminiAnaliseResponse(
    String textoExplicativo,
    List<DadoGrafico> dadosGrafico) {

  public record DadoGrafico(
      String label,
      Double valor) {
  }
}

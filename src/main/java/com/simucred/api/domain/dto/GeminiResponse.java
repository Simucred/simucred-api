package com.simucred.api.domain.dto;

import java.util.List;

/**
 * Mapeia a estrutura de resposta da API REST do Google Gemini.
 * Extrai apenas os campos necessários, ignorando o resto.
 */
public record GeminiResponse(List<Candidate> candidates) {

  public record Candidate(Content content) {
  }

  public record Content(List<Part> parts) {
  }

  public record Part(String text) {
  }
}

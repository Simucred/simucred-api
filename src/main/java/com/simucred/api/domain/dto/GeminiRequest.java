package com.simucred.api.domain.dto;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Mapeia a estrutura de requisição da API REST do Google Gemini.
 * Segue o formato exigido pelo endpoint generateContent.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GeminiRequest(
    List<Content> contents,
    GenerationConfig generationConfig,
    List<Content> systemInstruction) {

  public record Content(List<Part> parts, String role) {
    public Content(List<Part> parts) {
      this(parts, null);
    }
  }

  public record Part(String text) {
  }

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record GenerationConfig(
      String responseMimeType,
      ResponseSchema responseSchema) {
  }

  @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
  public record ResponseSchema(
      String type,
      java.util.Map<String, PropertySchema> properties,
      List<String> required) {
  }

  public record PropertySchema(
      String type,
      String description,
      Items items) {
  }

  public record Items(
      String type,
      java.util.Map<String, PropertySchema> properties,
      List<String> required) {
  }
}


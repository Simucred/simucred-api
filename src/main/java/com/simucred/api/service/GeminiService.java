package com.simucred.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simucred.api.domain.dto.AnaliseIaResponse;
import com.simucred.api.domain.dto.GraficoIaDto;
import com.simucred.api.domain.enums.StatusSimulacao;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiService {

  @Value("${gemini.api.key:}")
  private String apiKey;

  @Value("${gemini.api.model:gemini-1.5-flash}")
  private String model;

  @Value("${gemini.api.enabled:true}")
  private boolean enabled;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public GeminiService(ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().build();
    this.objectMapper = objectMapper;
  }

  public AnaliseIaResponse interpretarSimulacao(
      BigDecimal rendaMensal,
      BigDecimal valorSolicitado,
      Integer prazoMeses,
      BigDecimal taxaJurosMensal,
      BigDecimal valorParcela,
      StatusSimulacao status,
      BigDecimal percentualComprometimentoMaximo) {

    BigDecimal jurosTotaisEstimados = valorParcela.multiply(BigDecimal.valueOf(prazoMeses))
        .subtract(valorSolicitado).setScale(2, RoundingMode.HALF_UP);
    if (jurosTotaisEstimados.compareTo(BigDecimal.ZERO) < 0) {
      jurosTotaisEstimados = BigDecimal.ZERO;
    }

    BigDecimal comprometimentoReal = valorParcela.divide(rendaMensal, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

    if (!enabled || apiKey == null || apiKey.trim().isEmpty()) {
      log.warn("[GEMINI AI] Chave GEMINI_API_KEY não configurada ou integração desativada. Utilizando fallback local.");
      return gerarFallbackLocal(status, valorParcela, valorSolicitado, jurosTotaisEstimados, comprometimentoReal);
    }

    try {
      log.info("[GEMINI AI] Solicitando interpretação das regras para o Gemini API ({})", model);

      String prompt = """
          Você é um assistente financeiro especialista em crédito.
          A API do sistema executou as regras de cálculo e tomou a decisão final.
          SUA TAREFA: Interprete os resultados para o cliente em linguagem clara, amigável e explicativa. NÃO modifique os números nem a decisão das regras.

          DADOS DA SIMULAÇÃO:
          - Renda Mensal Declarada: R$ %s
          - Valor Solicitado do Empréstimo: R$ %s
          - Prazo: %d meses
          - Taxa de Juros Mensal: %s%%
          - Valor da Parcela Calculada: R$ %s
          - Comprometimento da Renda pela Parcela: %s%% (Limite máximo permitido: %s%%)
          - Resultado da Regra de Negócio: %s
          - Estimativa de Juros Totais: R$ %s

          INSTRUÇÕES DE RESPOSTA:
          1. 'explicacaoTexto': Escreva uma explicação clara explicando por que a proposta foi %s, destacando o valor da parcela, o prazo e o comprometimento da renda.
          2. 'resumoImpacto': Uma frase curta de resumo/dica financeira.
          3. 'grafico': Forneça dados para um gráfico explicativo. Use tipo 'pie' (pizza) para comparar Valor Solicitado vs Juros Totais, ou tipo 'bar' (barras) para comparar Limite de Parcela Permitido vs Parcela Calculada.
          """.formatted(
          rendaMensal, valorSolicitado, prazoMeses,
          taxaJurosMensal.multiply(BigDecimal.valueOf(100)), valorParcela,
          comprometimentoReal, percentualComprometimentoMaximo.multiply(BigDecimal.valueOf(100)),
          status.name(), jurosTotaisEstimados, status == StatusSimulacao.APROVADO ? "APROVADA" : "REPROVADA"
      );

      Map<String, Object> requestBody = Map.of(
          "contents", List.of(Map.of(
              "parts", List.of(Map.of("text", prompt))
          )),
          "generationConfig", Map.of(
              "responseMimeType", "application/json",
              "responseSchema", Map.of(
                  "type", "OBJECT",
                  "properties", Map.of(
                      "explicacaoTexto", Map.of("type", "STRING"),
                      "resumoImpacto", Map.of("type", "STRING"),
                      "grafico", Map.of(
                          "type", "OBJECT",
                          "properties", Map.of(
                              "titulo", Map.of("type", "STRING"),
                              "tipoGrafico", Map.of("type", "STRING"),
                              "labels", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                              "valores", Map.of("type", "ARRAY", "items", Map.of("type", "NUMBER"))
                          ),
                          "required", List.of("titulo", "tipoGrafico", "labels", "valores")
                      )
                  ),
                  "required", List.of("explicacaoTexto", "resumoImpacto", "grafico")
              )
          )
      );

      String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

      String responseJson = restClient.post()
          .uri(url)
          .contentType(MediaType.APPLICATION_JSON)
          .body(requestBody)
          .retrieve()
          .body(String.class);

      JsonNode rootNode = objectMapper.readTree(responseJson);
      String jsonText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

      AnaliseIaResponse analise = objectMapper.readValue(jsonText, AnaliseIaResponse.class);
      log.info("[GEMINI AI] Resposta do Gemini recebida e parseada com sucesso.");
      return analise;

    } catch (Exception e) {
      log.error("[GEMINI AI] Erro ao comunicar com a API do Gemini. Utilizando fallback local. Erro: {}", e.getMessage());
      return gerarFallbackLocal(status, valorParcela, valorSolicitado, jurosTotaisEstimados, comprometimentoReal);
    }
  }

  private AnaliseIaResponse gerarFallbackLocal(
      StatusSimulacao status,
      BigDecimal valorParcela,
      BigDecimal valorSolicitado,
      BigDecimal jurosTotais,
      BigDecimal comprometimentoReal) {

    boolean aprovado = status == StatusSimulacao.APROVADO;

    String explicacao = aprovado
        ? "Simulação aprovada: a parcela estimada de R$ " + valorParcela + " compromete " + comprometimentoReal + "% da sua renda mensal, respeitando a margem máxima de segurança."
        : "Simulação reprovada: a parcela estimada de R$ " + valorParcela + " compromete " + comprometimentoReal + "% da sua renda mensal, ultrapassando o limite seguro permitido.";

    String resumo = aprovado
        ? "Crédito compatível com seu orçamento mensal."
        : "Considere solicitar um valor menor ou estender o prazo em meses para reduzir a parcela.";

    GraficoIaDto grafico = new GraficoIaDto(
        "Composição do Valor Total",
        "pie",
        List.of("Valor Solicitado", "Juros Totais Estimados"),
        List.of(valorSolicitado, jurosTotais)
    );

    return new AnaliseIaResponse(explicacao, resumo, grafico);
  }
}

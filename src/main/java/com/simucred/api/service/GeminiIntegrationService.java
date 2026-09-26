package com.simucred.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simucred.api.domain.dto.GeminiAnaliseResponse;
import com.simucred.api.domain.dto.GeminiRequest;
import com.simucred.api.domain.dto.GeminiRequest.Content;
import com.simucred.api.domain.dto.GeminiRequest.GenerationConfig;
import com.simucred.api.domain.dto.GeminiRequest.Items;
import com.simucred.api.domain.dto.GeminiRequest.Part;
import com.simucred.api.domain.dto.GeminiRequest.PropertySchema;
import com.simucred.api.domain.dto.GeminiRequest.ResponseSchema;
import com.simucred.api.domain.dto.GeminiResponse;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.infra.config.GeminiProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Serviço responsável pela integração com a API REST do Google Gemini.
 * A IA é ESTRITAMENTE interpretativa — apenas traduz os resultados
 * matemáticos já calculados pelo backend em explicações amigáveis.
 */
@Slf4j
@Service
public class GeminiIntegrationService {

  private static final String SYSTEM_PROMPT = """
      Você é um analista financeiro didático do SimuCred. Sua função é EXCLUSIVAMENTE interpretar \
      e explicar os dados de uma simulação de crédito que já foi calculada pelo sistema. \
      Você NÃO deve inventar regras, recalcular taxas, nem tomar decisões. \
      Apenas traduza os números fornecidos em uma explicação humana, clara e didática.

      Ao receber os dados da simulação, retorne um JSON com a seguinte estrutura:
      {
        "textoExplicativo": "Uma explicação amigável e didática dos resultados da simulação, \
      incluindo o que significa cada valor e o que o cliente deve considerar.",
        "dadosGrafico": [
          { "label": "Nome do item", "valor": 1234.56 }
        ]
      }

      Regras para os dadosGrafico:
      - Inclua pelo menos: "Capital Solicitado", "Total de Juros", "Total a Pagar", "Valor da Parcela"
      - Se a simulação foi reprovada, inclua também "Limite de Comprometimento" e "Valor da Parcela" para comparação
      - Todos os valores devem ser numéricos (tipo Double), nunca strings
      - Use os valores EXATOS fornecidos, sem arredondar ou recalcular

      Regras para o textoExplicativo:
      - Explique em linguagem simples o que cada número significa
      - Se aprovado, parabenize e explique as condições
      - Se reprovado, explique gentilmente o motivo e sugira alternativas (reduzir valor ou aumentar prazo)
      - Mencione a taxa de juros mensal e o percentual de comprometimento da renda
      - Limite a resposta a no máximo 4 parágrafos
      """;

  private final RestClient geminiRestClient;
  private final GeminiProperties properties;
  private final ObjectMapper objectMapper;

  public GeminiIntegrationService(RestClient geminiRestClient,
      GeminiProperties properties,
      ObjectMapper objectMapper) {
    this.geminiRestClient = geminiRestClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /**
   * Envia os dados da simulação para o Gemini e retorna a análise interpretativa.
   *
   * @param simulacaoResponse resultado já calculado da simulação
   * @return análise com texto explicativo e dados para gráfico, ou null se falhar
   */
  public GeminiAnaliseResponse analisarSimulacao(SimulacaoResponse simulacaoResponse) {
    log.info("[GEMINI] Iniciando análise da simulação ID: {}", simulacaoResponse.id());

    try {
      String dadosSimulacao = formatarDadosParaPrompt(simulacaoResponse);
      GeminiRequest request = construirRequest(dadosSimulacao);

      String url = String.format("/models/%s:generateContent?key=%s",
          properties.model(), properties.apiKey());

      GeminiResponse response = geminiRestClient.post()
          .uri(url)
          .body(request)
          .retrieve()
          .body(GeminiResponse.class);

      if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
        log.warn("[GEMINI] Resposta vazia ou sem candidatos para simulação ID: {}", simulacaoResponse.id());
        return criarRespostaFallback(simulacaoResponse);
      }

      String jsonContent = response.candidates().getFirst().content().parts().getFirst().text();
      GeminiAnaliseResponse analise = objectMapper.readValue(jsonContent, GeminiAnaliseResponse.class);

      log.info("[GEMINI] Análise concluída com sucesso para simulação ID: {}", simulacaoResponse.id());
      return analise;

    } catch (JsonProcessingException e) {
      log.error("[GEMINI] Erro ao fazer parse do JSON de resposta para simulação ID: {}",
          simulacaoResponse.id(), e);
      return criarRespostaFallback(simulacaoResponse);
    } catch (Exception e) {
      log.error("[GEMINI] Erro na comunicação com a API para simulação ID: {}",
          simulacaoResponse.id(), e);
      return criarRespostaFallback(simulacaoResponse);
    }
  }

  private String formatarDadosParaPrompt(SimulacaoResponse sim) {
    return String.format("""
        Dados da Simulação de Crédito:
        - Nome do Cliente: %s
        - Idade: %d anos
        - Renda Mensal: R$ %s
        - Valor Solicitado: R$ %s
        - Prazo: %d meses
        - Taxa de Juros Mensal: %s%%
        - Valor da Parcela Calculada: R$ %s
        - Status: %s
        - Justificativa do Sistema: %s
        - Total a Pagar: R$ %s
        - Total de Juros: R$ %s
        """,
        sim.nome(),
        sim.idade(),
        sim.rendaMensal().toPlainString(),
        sim.valorSolicitado().toPlainString(),
        sim.prazoMeses(),
        sim.taxaJurosMensal().multiply(java.math.BigDecimal.valueOf(100)).toPlainString(),
        sim.valorParcela().toPlainString(),
        sim.status().name(),
        sim.justificativaIa(),
        sim.valorParcela().multiply(java.math.BigDecimal.valueOf(sim.prazoMeses())).toPlainString(),
        sim.valorParcela().multiply(java.math.BigDecimal.valueOf(sim.prazoMeses()))
            .subtract(sim.valorSolicitado()).toPlainString());
  }

  private GeminiRequest construirRequest(String userMessage) {
    // System instruction (separated from user content per Gemini API spec)
    Content systemContent = new Content(List.of(new Part(SYSTEM_PROMPT)));

    // User message with simulation data
    Content userContent = new Content(List.of(new Part(userMessage)), "user");

    // JSON schema for structured output
    ResponseSchema schema = new ResponseSchema(
        "OBJECT",
        Map.of(
            "textoExplicativo", new PropertySchema("STRING",
                "Explicação didática dos resultados da simulação", null),
            "dadosGrafico", new PropertySchema("ARRAY",
                "Dados para renderizar gráfico no frontend",
                new Items("OBJECT",
                    Map.of(
                        "label", new PropertySchema("STRING", "Nome do item do gráfico", null),
                        "valor", new PropertySchema("NUMBER", "Valor numérico do item", null)),
                    List.of("label", "valor")))),
        List.of("textoExplicativo", "dadosGrafico"));

    GenerationConfig config = new GenerationConfig("application/json", schema);

    return new GeminiRequest(List.of(userContent), config, List.of(systemContent));
  }

  /**
   * Gera uma resposta de fallback quando a API do Gemini falha.
   * Garante que o sistema continue funcionando mesmo sem a IA.
   */
  private GeminiAnaliseResponse criarRespostaFallback(SimulacaoResponse sim) {
    log.warn("[GEMINI] Usando resposta de fallback para simulação ID: {}", sim.id());

    String texto = sim.status().name().equals("APROVADO")
        ? String.format(
            "Simulação aprovada! Você solicitou R$ %s em %d parcelas de R$ %s. "
                + "A taxa de juros mensal aplicada foi de %s%%. "
                + "O total a pagar será de R$ %s.",
            sim.valorSolicitado().toPlainString(),
            sim.prazoMeses(),
            sim.valorParcela().toPlainString(),
            sim.taxaJurosMensal().multiply(java.math.BigDecimal.valueOf(100)).toPlainString(),
            sim.valorParcela().multiply(java.math.BigDecimal.valueOf(sim.prazoMeses())).toPlainString())
        : String.format(
            "Simulação reprovada. A parcela de R$ %s excede o limite de comprometimento de renda. "
                + "Considere solicitar um valor menor ou aumentar o prazo de pagamento.",
            sim.valorParcela().toPlainString());

    var totalPagar = sim.valorParcela().multiply(java.math.BigDecimal.valueOf(sim.prazoMeses()));
    var totalJuros = totalPagar.subtract(sim.valorSolicitado());

    List<GeminiAnaliseResponse.DadoGrafico> dados = List.of(
        new GeminiAnaliseResponse.DadoGrafico("Capital Solicitado", sim.valorSolicitado().doubleValue()),
        new GeminiAnaliseResponse.DadoGrafico("Total de Juros", totalJuros.doubleValue()),
        new GeminiAnaliseResponse.DadoGrafico("Total a Pagar", totalPagar.doubleValue()),
        new GeminiAnaliseResponse.DadoGrafico("Valor da Parcela", sim.valorParcela().doubleValue()));

    return new GeminiAnaliseResponse(texto, dados);
  }
}

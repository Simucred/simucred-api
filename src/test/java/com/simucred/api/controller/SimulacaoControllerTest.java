package com.simucred.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simucred.api.domain.dto.ResumoSimulacao;
import com.simucred.api.domain.dto.SimulacaoListagem;
import com.simucred.api.domain.dto.SimulacaoRequest;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.domain.enums.StatusSimulacao;
import com.simucred.api.service.SimulacaoService;
import com.simucred.api.v1.controller.SimulacaoController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimulacaoController.class)
class SimulacaoControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private SimulacaoService service;

  private final ObjectMapper objectMapper = new ObjectMapper()
      .findAndRegisterModules();

  private static final String PREFERRED_USERNAME = "usuario.teste";

  @Test
  void deveRetornarResumoComSucessoEMatarMutacoesDeRotaERetorno() throws Exception {
    ResumoSimulacao resumoMock = new ResumoSimulacao(
        10,
        5,
        3,
        2,
        50.0,
        new BigDecimal("5000.00"));

    when(service.obterResumo(PREFERRED_USERNAME)).thenReturn(resumoMock);

    mockMvc.perform(get("/v1/simulacoes/resumo")
        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", PREFERRED_USERNAME))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(10))
        .andExpect(jsonPath("$.aprovadas").value(5))
        .andExpect(jsonPath("$.taxaAprovacao").value(50.0));

    verify(service).obterResumo(PREFERRED_USERNAME);
  }

  @Test
  void deveRetornarListaDeRecentesComSucessoEMatarMutacoesDeRotaERetorno() throws Exception {
    SimulacaoListagem listagemMock = new SimulacaoListagem(
        UUID.randomUUID(),
        LocalDateTime.now(),
        new BigDecimal("10000.00"),
        24,
        new BigDecimal("650.00"),
        new BigDecimal("0.025"),
        StatusSimulacao.APROVADO.name(),
        "Maria",
        "123.456.789-00",
        30,
        new BigDecimal("5000.00"),
        "Aprovado");

    when(service.obterRecentes(PREFERRED_USERNAME))
        .thenReturn(List.of(listagemMock));

    mockMvc.perform(get("/v1/simulacoes")
        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", PREFERRED_USERNAME))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].nome").value("Maria"))
        .andExpect(jsonPath("$[0].status").value("APROVADO"));

    verify(service).obterRecentes(PREFERRED_USERNAME);
  }

  @Test
  void deveRealizarSimulacaoComSucessoEMatarMutacoesDePostEResponseEntity() throws Exception {
    SimulacaoRequest request = new SimulacaoRequest(
        "12345678900",
        "Maria",
        LocalDate.of(1995, 1, 1),
        new BigDecimal("5000.00"),
        new BigDecimal("10000.00"),
        24);

    SimulacaoResponse responseMock = new SimulacaoResponse(
        UUID.randomUUID(),
        "123.***.***-00",
        "Maria",
        29,
        new BigDecimal("5000.00"),
        new BigDecimal("10000.00"),
        24,
        new BigDecimal("450.00"),
        new BigDecimal("0.025"),
        StatusSimulacao.APROVADO,
        "Aprovado",
        LocalDateTime.now(),
        null);

    when(service.simular(
        any(SimulacaoRequest.class),
        eq(PREFERRED_USERNAME))).thenReturn(responseMock);

    mockMvc.perform(post("/v1/simulacoes")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", PREFERRED_USERNAME))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Maria"))
        .andExpect(jsonPath("$.status").value("APROVADO"))
        .andExpect(jsonPath("$.valorParcela").value(450.00));

    verify(service).simular(
        any(SimulacaoRequest.class),
        eq(PREFERRED_USERNAME));
  }
}

package com.simucred.api.v1.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simucred.api.domain.dto.ResumoSimulacao;
import com.simucred.api.domain.dto.SimulacaoListagem;
import com.simucred.api.domain.dto.SimulacaoRequest;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.service.SimulacaoService;

@RestController
@RequestMapping("/v1/simulacoes")
public class SimulacaoController {

  private final SimulacaoService service;

  public SimulacaoController(SimulacaoService service) {
    this.service = service;
  }

  @GetMapping("/resumo")
  public ResumoSimulacao getResumo(@AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getClaimAsString("preferred_username");
    return service.obterResumo(username);
  }

  @GetMapping
  public List<SimulacaoListagem> getRecentes(@AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getClaimAsString("preferred_username");
    return service.obterRecentes(username);
  }

  @PostMapping
  public ResponseEntity<SimulacaoResponse> realizarSimulacao(@RequestBody SimulacaoRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    String username = jwt.getClaimAsString("preferred_username");
    SimulacaoResponse response = service.simular(request, username);
    return ResponseEntity.ok(response);
  }

}

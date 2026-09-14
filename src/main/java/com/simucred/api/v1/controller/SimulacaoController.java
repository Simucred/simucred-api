package com.simucred.api.v1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

  @PostMapping
  public ResponseEntity<SimulacaoResponse> realizarSimulacao(@RequestBody SimulacaoRequest request) {
    SimulacaoResponse response = service.simular(request);
    return ResponseEntity.ok(response);
  }
}

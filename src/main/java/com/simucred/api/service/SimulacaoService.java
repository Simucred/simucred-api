package com.simucred.api.service;

import org.springframework.stereotype.Service;

import com.simucred.api.domain.dto.SimulacaoRequest;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.domain.entity.SimulacaoCredito;
import com.simucred.api.domain.enums.StatusSimulacao;
import com.simucred.api.repository.SimulacaoRepository;
import com.simucred.api.util.CpfUtil;

@Service
public class SimulacaoService {

  private final SimulacaoRepository repository;

  public SimulacaoService(SimulacaoRepository repository) {
    this.repository = repository;
  }

  public SimulacaoResponse simular(SimulacaoRequest request) {
    StatusSimulacao status = request.rendaMensal()
        .doubleValue() >= (request.valorSolicitado().doubleValue() / request.prazoMeses()) * 3
            ? StatusSimulacao.APROVADO
            : StatusSimulacao.REPROVADO;

    String justificativa = status == StatusSimulacao.APROVADO
        ? "Crédito aprovado: a renda mensal comporta a parcela estimada."
        : "Crédito reprovado: parcela compromete mais de 30% da renda declarada.";

    SimulacaoCredito entidade = new SimulacaoCredito();
    entidade.setCpf(request.cpf());
    entidade.setNome(request.nome());
    entidade.setIdade(request.idade());
    entidade.setRendaMensal(request.rendaMensal());
    entidade.setValorSolicitado(request.valorSolicitado());
    entidade.setPrazoMeses(request.prazoMeses());
    entidade.setStatus(status);
    entidade.setJustificativaIa(justificativa);

    SimulacaoCredito salvo = repository.save(entidade);

    return new SimulacaoResponse(
        salvo.getId(),
        CpfUtil.mascararCpf(salvo.getCpf()),
        salvo.getNome(),
        salvo.getValorSolicitado(),
        salvo.getStatus(),
        salvo.getJustificativaIa(),
        salvo.getDataSimulacao());
  }
}

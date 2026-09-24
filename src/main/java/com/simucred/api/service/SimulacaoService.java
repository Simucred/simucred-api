package com.simucred.api.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import org.springframework.stereotype.Service;

import com.simucred.api.domain.dto.AnaliseIaResponse;
import com.simucred.api.domain.dto.ResumoSimulacao;
import com.simucred.api.domain.dto.SimulacaoListagem;
import com.simucred.api.domain.dto.SimulacaoRequest;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.domain.entity.SimulacaoCredito;
import com.simucred.api.domain.enums.StatusSimulacao;
import com.simucred.api.infra.config.CreditoProperties;
import com.simucred.api.repository.SimulacaoRepository;
import com.simucred.api.util.CpfUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SimulacaoService {

  private final SimulacaoRepository repository;
  private final CreditoProperties creditoProperties;
  private final GeminiService geminiService;

  public SimulacaoService(
      SimulacaoRepository repository,
      CreditoProperties creditoProperties,
      GeminiService geminiService) {
    this.repository = repository;
    this.creditoProperties = creditoProperties;
    this.geminiService = geminiService;
  }

  public ResumoSimulacao obterResumo(String username) {
    log.info("[AUDIT] Buscando resumo de simulações. Usuário: {}", username);

    List<SimulacaoCredito> simulacoes = repository.findAllByUsernameOrderByDataSimulacaoDesc(username);

    if (simulacoes.isEmpty()) {
      log.info("[AUDIT] Nenhuma simulação encontrada para compor o resumo. Usuário: {}", username);
      return new ResumoSimulacao(0, 0, 0, 0, 0.0, BigDecimal.ZERO);
    }

    long total = simulacoes.size();
    long aprovadas = simulacoes.stream()
        .filter(s -> s.getStatus() == StatusSimulacao.APROVADO)
        .count();

    long emAnalise = simulacoes.stream()
        .filter(s -> s.getStatus() == StatusSimulacao.EM_ANALISE)
        .count();

    long reprovadas = simulacoes.stream()
        .filter(s -> s.getStatus() == StatusSimulacao.REPROVADO)
        .count();

    double taxaAprovacao = (double) aprovadas / total * 100;

    BigDecimal somaValores = simulacoes.stream()
        .map(SimulacaoCredito::getValorSolicitado)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal valorMedio = somaValores.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

    log.info("[AUDIT] Resumo gerado com sucesso. Total de registros: {}, Usuário: {}", total, username);

    return new ResumoSimulacao(total, aprovadas, reprovadas, emAnalise, taxaAprovacao, valorMedio);
  }

  public List<SimulacaoListagem> obterRecentes(String username) {
    log.info("[AUDIT] Buscando lista de simulações recentes. Usuário: {}", username);

    List<SimulacaoListagem> recentes = repository.findAllByUsernameOrderByDataSimulacaoDesc(username).stream()
        .limit(5)
        .map(s -> new SimulacaoListagem(
            s.getId(),
            s.getDataSimulacao(),
            s.getValorSolicitado(),
            s.getPrazoMeses(),
            s.getStatus().name(),
            s.getNome(),
            s.getCpf(),
            s.getIdade(),
            s.getRendaMensal(),
            s.getJustificativaIa()))
        .toList();

    log.info("[AUDIT] Retornando {} simulações recentes para o usuário: {}", recentes.size(), username);
    return recentes;
  }

  public SimulacaoResponse simular(SimulacaoRequest request, String username) {
    log.info("[AUDIT] Iniciando simulação de crédito. Usuário: {}, Valor solicitado: {}, Prazo: {}",
        username, request.valorSolicitado(), request.prazoMeses());

    int idade = Period.between(request.dataNascimento(), LocalDate.now()).getYears();

    BigDecimal taxaJuros = creditoProperties.taxaJurosMensal();
    BigDecimal valorParcela = calcularParcelaPrice(request.valorSolicitado(), taxaJuros, request.prazoMeses());

    BigDecimal limiteComprometimento = request.rendaMensal()
        .multiply(creditoProperties.percentualMaximoComprometimento());

    boolean aprovado = valorParcela.compareTo(limiteComprometimento) <= 0;
    StatusSimulacao status = aprovado ? StatusSimulacao.APROVADO : StatusSimulacao.REPROVADO;

    AnaliseIaResponse analiseIa = geminiService.interpretarSimulacao(
        request.rendaMensal(),
        request.valorSolicitado(),
        request.prazoMeses(),
        taxaJuros,
        valorParcela,
        status,
        creditoProperties.percentualMaximoComprometimento()
    );

    SimulacaoCredito entidade = new SimulacaoCredito();
    entidade.setCpf(request.cpf());
    entidade.setNome(request.nome());
    entidade.setIdade(idade);
    entidade.setRendaMensal(request.rendaMensal());
    entidade.setValorSolicitado(request.valorSolicitado());
    entidade.setPrazoMeses(request.prazoMeses());
    entidade.setStatus(status);
    entidade.setJustificativaIa(analiseIa.explicacaoTexto());
    entidade.setUsername(username);

    SimulacaoCredito salva = repository.save(entidade);

    log.info("[AUDIT] Simulação concluída e salva. ID: {}, Status: {}, Usuário: {}",
        salva.getId(), status, username);

    return new SimulacaoResponse(
        salva.getId(), CpfUtil.mascararCpf(salva.getCpf()), salva.getNome(), salva.getIdade(),
        salva.getRendaMensal(), salva.getValorSolicitado(), salva.getPrazoMeses(),
        valorParcela, taxaJuros, salva.getStatus(), salva.getJustificativaIa(),
        analiseIa.grafico(), salva.getDataSimulacao());
  }

  private BigDecimal calcularParcelaPrice(BigDecimal valorPresente, BigDecimal taxaMensal, int numeroParcelas) {
    MathContext mc = new MathContext(10);

    BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
    BigDecimal fatorPotencia = umMaisTaxa.pow(numeroParcelas, mc);

    BigDecimal numerador = taxaMensal.multiply(fatorPotencia, mc);
    BigDecimal denominador = fatorPotencia.subtract(BigDecimal.ONE, mc);

    BigDecimal fator = numerador.divide(denominador, mc);

    return valorPresente.multiply(fator, mc).setScale(2, RoundingMode.HALF_UP);
  }
}
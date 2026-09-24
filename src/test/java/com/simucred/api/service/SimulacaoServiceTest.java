package com.simucred.api.service;

import com.simucred.api.domain.dto.ResumoSimulacao;
import com.simucred.api.domain.dto.SimulacaoListagem;
import com.simucred.api.domain.dto.SimulacaoRequest;
import com.simucred.api.domain.dto.SimulacaoResponse;
import com.simucred.api.domain.entity.SimulacaoCredito;
import com.simucred.api.domain.enums.StatusSimulacao;
import com.simucred.api.infra.config.CreditoProperties;
import com.simucred.api.repository.SimulacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulacaoServiceTest {

  @Mock
  private SimulacaoRepository repository;

  @Mock
  private CreditoProperties creditoProperties;

  @InjectMocks
  private SimulacaoService service;

  private final String USERNAME = "dev.tester";
  private final BigDecimal TAXA_JUROS = new BigDecimal("0.025");
  private final BigDecimal LIMITE_COMPROMETIMENTO = new BigDecimal("0.30");

  @BeforeEach
  void setup() {
    lenient().when(creditoProperties.taxaJurosMensal()).thenReturn(TAXA_JUROS);
    lenient().when(creditoProperties.percentualMaximoComprometimento()).thenReturn(LIMITE_COMPROMETIMENTO);
  }

  // =========================================================================
  // TESTES DE OBTENÇÃO DE RESUMO (Filtros, Contagens, Divisão Matemática)
  // =========================================================================

  @Test
  void deveRetornarResumoZeradoQuandoNaoHouverSimulacoes() {
    when(repository.findAllByUsernameOrderByDataSimulacaoDesc(USERNAME))
        .thenReturn(Collections.emptyList());

    ResumoSimulacao resumo = service.obterResumo(USERNAME);

    assertEquals(0, resumo.total());
    assertEquals(0, resumo.aprovadas());
    assertEquals(0, resumo.reprovadas());
    assertEquals(0, resumo.emAnalise());
    assertEquals(0.0, resumo.taxaAprovacao());
    assertEquals(BigDecimal.ZERO, resumo.valorMedio());
  }

  @Test
  void deveCalcularResumoExatoMatandoMutacoesDeFiltroEMatematica() {
    SimulacaoCredito s1 = mockSimulacao(StatusSimulacao.APROVADO, "10000");
    SimulacaoCredito s2 = mockSimulacao(StatusSimulacao.APROVADO, "15000");
    SimulacaoCredito s3 = mockSimulacao(StatusSimulacao.REPROVADO, "5000");
    SimulacaoCredito s4 = mockSimulacao(StatusSimulacao.EM_ANALISE, "10000");

    when(repository.findAllByUsernameOrderByDataSimulacaoDesc(USERNAME))
        .thenReturn(List.of(s1, s2, s3, s4));

    ResumoSimulacao resumo = service.obterResumo(USERNAME);

    assertEquals(4, resumo.total());
    assertEquals(2, resumo.aprovadas());
    assertEquals(1, resumo.reprovadas());
    assertEquals(1, resumo.emAnalise());
    // 2 aprovadas / 4 total * 100 = 50.0%
    assertEquals(50.0, resumo.taxaAprovacao());
    // Soma = 40000 / 4 = 10000
    assertEquals(new BigDecimal("10000.00").setScale(2, RoundingMode.HALF_UP), resumo.valorMedio());
  }

  @Test
  void deveContarSomenteSimulacoesAprovadas() {
    SimulacaoCredito aprovada = mockSimulacao(StatusSimulacao.APROVADO, "10000");
    SimulacaoCredito reprovada = mockSimulacao(StatusSimulacao.REPROVADO, "10000");
    SimulacaoCredito emAnalise = mockSimulacao(StatusSimulacao.EM_ANALISE, "10000");

    when(repository.findAllByUsernameOrderByDataSimulacaoDesc(USERNAME))
        .thenReturn(List.of(aprovada, reprovada, emAnalise));

    ResumoSimulacao resumo = service.obterResumo(USERNAME);

    assertEquals(1, resumo.aprovadas());
    assertEquals(1, resumo.reprovadas());
    assertEquals(1, resumo.emAnalise());
  }

  // =========================================================================
  // TESTES DE LISTAGEM (Limites e Mapeamentos)
  // =========================================================================

  @Test
  void deveRetornarMaximoDeCincoSimulacoesMatandoMutacaoDeLimite() {
    List<SimulacaoCredito> seisSimulacoes = List.of(
        mockSimulacao(StatusSimulacao.APROVADO, "100"),
        mockSimulacao(StatusSimulacao.APROVADO, "200"),
        mockSimulacao(StatusSimulacao.APROVADO, "300"),
        mockSimulacao(StatusSimulacao.APROVADO, "400"),
        mockSimulacao(StatusSimulacao.APROVADO, "500"),
        mockSimulacao(StatusSimulacao.APROVADO, "600"));

    when(repository.findAllByUsernameOrderByDataSimulacaoDesc(USERNAME)).thenReturn(seisSimulacoes);

    List<SimulacaoListagem> recentes = service.obterRecentes(USERNAME);

    assertEquals(5, recentes.size());
    assertEquals(new BigDecimal("100"), recentes.get(0).valorSolicitado());
    assertEquals(new BigDecimal("500"), recentes.get(4).valorSolicitado());
  }

  // =========================================================================
  // TESTES DO MOTOR DE SIMULAÇÃO (Matemática da Tabela Price e Limites)
  // =========================================================================

  @Test
  void deveAprovarExatamenteNoLimiteDeComprometimentoMatandoMutacaoDeFronteira() {
    // PV = 10000, i = 0.025, n = 12 -> PMT = 974.87 (arredondado HALF_UP)
    // Para que 974.87 seja EXATAMENTE 30% da renda, Renda = 3249.57
    // Isso mata a mutação de <= para < na linha:
    // valorParcela.compareTo(limiteComprometimento) <= 0
    BigDecimal valorSolicitado = new BigDecimal("10000");
    BigDecimal renda = new BigDecimal("3249.57");
    int prazo = 12;

    SimulacaoRequest request = criarRequest(renda, valorSolicitado, prazo);
    when(repository.save(any())).thenAnswer(i -> mockSave(i.getArgument(0)));

    SimulacaoResponse response = service.simular(request, USERNAME);

    assertEquals(StatusSimulacao.APROVADO, response.status());
    assertEquals(new BigDecimal("974.87"), response.valorParcela());
    assertTrue(response.justificativaIa().contains("comporta a parcela estimada de R$ 974.87"));
  }

  @Test
  void deveReprovarPorUmCentavoAcimaDoLimiteMatandoMutacaoDeFronteira() {
    // PV = 10000, PMT = 974.87.
    // Se a renda for 3249.56, o limite de 30% é 974.868 (arredondado na
    // comparação).
    // 974.87 > 974.868, logo deve reprovar.
    BigDecimal valorSolicitado = new BigDecimal("10000");
    BigDecimal renda = new BigDecimal("3249.56");
    int prazo = 12;

    SimulacaoRequest request = criarRequest(renda, valorSolicitado, prazo);
    when(repository.save(any())).thenAnswer(i -> mockSave(i.getArgument(0)));

    SimulacaoResponse response = service.simular(request, USERNAME);

    assertEquals(StatusSimulacao.REPROVADO, response.status());
    assertEquals(new BigDecimal("974.87"), response.valorParcela());
    assertTrue(response.justificativaIa().contains("compromete mais de 30.00% da renda"));
  }

  @Test
  void deveSalvarEntidadeComTodosOsCamposCorretosCalculandoIdadeCorretamente() {
    // Data de nascimento configurada para garantir que o cálculo Period.between
    // ocorra.
    LocalDate dataNascimento = LocalDate.now().minusYears(25).minusMonths(1);
    SimulacaoRequest request = new SimulacaoRequest(
        "12345678909", "Nome Teste", dataNascimento,
        new BigDecimal("5000"), new BigDecimal("1000"), 5);

    when(repository.save(any())).thenAnswer(i -> mockSave(i.getArgument(0)));

    service.simular(request, USERNAME);

    ArgumentCaptor<SimulacaoCredito> captor = ArgumentCaptor.forClass(SimulacaoCredito.class);
    verify(repository).save(captor.capture());

    SimulacaoCredito salva = captor.getValue();
    assertEquals("12345678909", salva.getCpf());
    assertEquals("Nome Teste", salva.getNome());
    assertEquals(25, salva.getIdade());
    assertEquals(new BigDecimal("5000"), salva.getRendaMensal());
    assertEquals(new BigDecimal("1000"), salva.getValorSolicitado());
    assertEquals(5, salva.getPrazoMeses());
    assertEquals(USERNAME, salva.getUsername());
    assertNotNull(salva.getJustificativaIa());
    assertNotNull(salva.getStatus());
  }

  @Test
  void deveAprovarQuandoValorParcelaForExatamenteIgualAoLimite() {
    BigDecimal valorSolicitado = new BigDecimal("10000");
    BigDecimal renda = new BigDecimal("1000");

    when(creditoProperties.percentualMaximoComprometimento())
        .thenReturn(new BigDecimal("0.97487"));

    SimulacaoRequest request = criarRequest(renda, valorSolicitado, 12);

    when(repository.save(any()))
        .thenAnswer(i -> mockSave(i.getArgument(0)));

    SimulacaoResponse response = service.simular(request, USERNAME);

    assertEquals(new BigDecimal("974.87"), response.valorParcela());
    assertEquals(StatusSimulacao.APROVADO, response.status());
  }

  // =========================================================================
  // MÉTODOS AUXILIARES
  // =========================================================================

  private SimulacaoCredito mockSimulacao(StatusSimulacao status, String valor) {
    SimulacaoCredito s = new SimulacaoCredito();
    s.setId(UUID.randomUUID());
    s.setStatus(status);
    s.setValorSolicitado(new BigDecimal(valor));
    s.setDataSimulacao(LocalDateTime.now());
    s.setNome("Mock");
    s.setCpf("00000000000");
    return s;
  }

  private SimulacaoRequest criarRequest(BigDecimal renda, BigDecimal valor, int prazo) {
    return new SimulacaoRequest(
        "00000000000",
        "Teste Limite",
        LocalDate.of(1990, 1, 1),
        renda,
        valor,
        prazo);
  }

  private SimulacaoCredito mockSave(SimulacaoCredito entidade) {
    entidade.setId(UUID.randomUUID());
    entidade.setDataSimulacao(LocalDateTime.now());
    return entidade;
  }
}
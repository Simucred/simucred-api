package com.simucred.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.simucred.api.domain.enums.StatusSimulacao;

@Entity
@Table(name = "simulacoes_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimulacaoCredito {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 11)
  private String cpf;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false)
  private Integer idade;

  @Column(nullable = false)
  private BigDecimal rendaMensal;

  @Column(nullable = false)
  private BigDecimal valorSolicitado;

  @Column(nullable = false)
  private Integer prazoMeses;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private StatusSimulacao status;

  @Column(columnDefinition = "TEXT")
  private String justificativaIa;

  @CreationTimestamp
  @Column(nullable = false)
  private LocalDateTime dataSimulacao;

}
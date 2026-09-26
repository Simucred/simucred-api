package com.simucred.api.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SimulacaoRequest(
    @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos numéricos") String cpf,

    @NotBlank @Size(max = 150) String nome,

    @NotNull @Past(message = "Data de nascimento deve ser no passado") LocalDate dataNascimento,

    @NotNull @DecimalMin(value = "0.01", message = "Renda mensal deve ser positiva") BigDecimal rendaMensal,

    @NotNull @DecimalMin(value = "0.01", message = "Valor solicitado deve ser positivo") BigDecimal valorSolicitado,

    @NotNull @Min(value = 1, message = "Prazo mínimo é 1 mês") @Max(value = 360, message = "Prazo máximo é 360 meses") Integer prazoMeses) {
}

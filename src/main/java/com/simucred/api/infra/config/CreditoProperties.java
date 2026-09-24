package com.simucred.api.infra.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simucred.credito")
public record CreditoProperties(
    BigDecimal taxaJurosMensal,
    BigDecimal percentualMaximoComprometimento) {
}
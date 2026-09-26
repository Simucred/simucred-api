package com.simucred.api.infra.security;

public class SecurityConfigurationException extends RuntimeException {

  public SecurityConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
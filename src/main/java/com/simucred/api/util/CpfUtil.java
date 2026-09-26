package com.simucred.api.util;

public class CpfUtil {

  private CpfUtil() {
    throw new IllegalStateException("Classe utilitária não deve ser instanciada");
  }

  public static String mascararCpf(String cpf) {
    if (cpf == null || cpf.length() != 11) {
      return cpf;
    }
    return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9, 11);
  }
}
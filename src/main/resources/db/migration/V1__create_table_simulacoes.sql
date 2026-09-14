CREATE TABLE simulacoes_credito (
  id UUID PRIMARY KEY,
  cpf VARCHAR(11) NOT NULL,
  nome VARCHAR(255) NOT NULL,
  idade INTEGER NOT NULL,
  renda_mensal DECIMAL(15, 2) NOT NULL,
  valor_solicitado DECIMAL(15, 2) NOT NULL,
  prazo_meses INTEGER NOT NULL,
  status VARCHAR(50) NOT NULL,
  justificativa_ia TEXT,
  data_simulacao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
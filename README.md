# 🏦 Simucred — AI-Powered Credit Decision Engine

O **Simucred** é um sistema projetado para automatizar e validar o fluxo de aprovação de crédito. A plataforma utiliza um motor de regras de negócio determinístico para a tomada de decisão financeira e integra uma camada de Inteligência Artificial para gerar explicabilidade, transparência e recomendações aos usuários.

A arquitetura do projeto foi estruturada para solucionar gargalos comuns no ciclo de vida do software, adotando um fluxo rigoroso de desenvolvimento, operações (DevOps) e governança.

### 🧠 Recursos de Inteligência Artificial (AI Layer)
A IA do Simucred atua **exclusivamente como uma camada de análise**, sem autoridade para aprovar ou reprovar operações (decisão mantida no motor de regras).
- **1️⃣ AI Credit Explanation:** Traduz os resultados técnicos da simulação para linguagem natural, explicando os motivos exatos de uma aprovação ou reprovação (ex: limite de comprometimento de renda excedido).
- **2️⃣ AI Alternative Loan:** Em caso de reprovação, analisa cálculos alternativos processados pelo backend e sugere propostas viáveis para o cliente (ex: aumentar o prazo para adequar a parcela à renda).
- **3️⃣ AI Credit Insights:** Analisa o histórico de simulações do banco de dados e gera insights gerenciais sobre o comportamento de risco e taxas de conversão.

### 🚀 Stack Tecnológica
- **Backend & Regras de Negócio:** Java 21 + Spring Boot 3.2
- **Frontend:** Angular 20+
- **Persistência:** PostgreSQL com versionamento via Flyway
- **Infraestrutura e CI/CD:** Docker, Docker Compose, GitHub Actions
- **Qualidade e Segurança:** SonarCloud, Trivy
- **Observabilidade:** Prometheus, Grafana

---

### ⚙️ Como executar o projeto localmente

**Pré-requisitos:**
- Java 21 instalado
- Docker e Docker Compose instalados

**1. Subir a infraestrutura (Banco de Dados)**
Na raiz do projeto, inicie o container do PostgreSQL:
```bash
docker-compose up -d
```

**2. Rodar a aplicação Spring Boot**
Com o banco rodando, inicie a API utilizando o Maven Wrapper:

No Windows:
```bash
.\mvnw spring-boot:run
```
No Linux/Mac:
```bash
./mvnw spring-boot:run
```
A API estará disponível em http://localhost:8080.

---

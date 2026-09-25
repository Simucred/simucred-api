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
- Docker e Docker Compose instalados
- Os dois repositórios clonados **lado a lado** na mesma pasta:

```text
pasta-qualquer/
├── simucred-api/   ← este repositório (tem o docker-compose.yml)
└── simucred_web/   ← front-end Angular
```

```bash
git clone https://github.com/Simucred/simucred-api.git
git clone https://github.com/Simucred/simucred_web.git
```

**1. Criar o arquivo `.env`**
Na raiz deste repositório, copie o modelo. Os valores padrão já funcionam para rodar localmente:

```bash
cp .env.example .env
```

No Windows (PowerShell/CMD): `copy .env.example .env`

**2. Subir tudo (banco, Keycloak, API e front) com um comando**
```bash
docker compose up -d --build
```

A ordem de subida é automática: o Postgres precisa ficar `healthy` antes da API subir, e o front sobe depois da API.

**3. Conferir**
```bash
docker compose ps
```

Os 4 serviços devem aparecer como `Up`.

| Serviço | Endereço |
| --- | --- |
| Front-end | http://localhost:4200 |
| API | http://localhost:8080 (Swagger em `/swagger-ui.html`) |
| Keycloak | http://localhost:8081 (painel admin com `KEY_USER` / `KEY_PASSWORD`) |

**4. Acessar**
Abra http://localhost:4200 e entre com o usuário de teste `analista` / `123456`, ou clique em **Register** na tela do Keycloak para criar um usuário.

> O realm `simucred`, o client `simucred-web` e o usuário de teste são importados de `keycloak/realm-export.json` na primeira subida. Essas credenciais são **apenas para desenvolvimento local**.

**Parar os serviços**
```bash
docker compose stop      # pausa, mantendo dados e usuários
docker compose down -v   # remove tudo, inclusive o banco
```

**Subir só o backend (sem o front)**
O front faz parte do profile `full`, ativado pela linha `COMPOSE_PROFILES=full` do `.env`. Remova essa linha para subir apenas banco, Keycloak e API (é assim que o CI roda).

**Portas ocupadas?** Troque `API_PORT`, `KEY_PORT` e/ou `WEB_PORT` no `.env`. O front é configurado automaticamente para as novas portas.

**Atualizou e o banco não sobe?** Se aparecer `database files are incompatible` no log do `simucred-db`, o volume foi criado com uma versão anterior do PostgreSQL. Rode `docker compose down -v` e suba de novo (os dados locais são apagados).

---

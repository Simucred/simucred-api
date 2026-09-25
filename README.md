# 💰 Simucred API — Sistema de Simulação de Crédito

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0.5-purple)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

## 1. Descrição do Projeto
O **Simucred API** é o serviço de back-end de uma plataforma de simulação e análise de operações de crédito. A aplicação permite realizar cálculos financeiros de propostas de empréstimo, consultar modalidades e taxas, validar regras de negócio de crédito e persistir o histórico de simulações realizadas pelos usuários.

A segurança da API é baseada no protocolo **OAuth2 / OpenID Connect (OIDC)** utilizando tokens **JWT** emitidos e validados pelo **Keycloak**, enquanto a persistência relacional é gerenciada pelo **PostgreSQL** com versionamento de schema automatizado.

---

## 2. Arquitetura e Tecnologias Utilizadas
* **Linguagem e Runtime:** Java 21 (Eclipse Temurin)
* **Framework Back-end:** Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Boot Actuator)
* **Banco de Dados:** PostgreSQL 16 (`postgres:16-alpine`)
* **Identidade e Autenticação (IAM):** Keycloak 24.0.5 (`quay.io/keycloak/keycloak:24.0.5`) com importação automática de Realm (`realm-export.json`)
* **Gerenciador de Dependências e Build:** Apache Maven (via Maven Wrapper `./mvnw`)
* **Testes Automatizados:** JUnit 5 e Spring Boot Test
* **Containerização e Orquestração:** Docker (Multi-Stage Build) e Docker Compose
* **CI/CD:** GitHub Actions com publicação automatizada no Docker Hub

### Topologia dos Containers (`simucred-net`)
1. **`simucred-db` (PostgreSQL 16):** Banco de dados isolado na rede interna (`5432/tcp`), com volume nomeado (`postgres_data`) para persistência de dados e `healthcheck` nativo (`pg_isready`).
2. **`simucred-keycloak` (Keycloak 24):** Servidor de autorização que importa automaticamente o realm `simucred` na inicialização (`--import-realm`), exposto na porta `8081`.
3. **`simucred-api` (Spring Boot):** API RESTful executada sob usuário não-root (`appuser`), aguardando a saúde do banco (`service_healthy`) e exposta na porta `8080`.
4. **`simucred-web` (Angular - Opcional via Profile `full`):** Aplicação front-end consumidora da API e do Keycloak, exposta na porta `4200`.

---

## 3. Pré-requisitos
Para executar o projeto via containers, você precisa apenas de:
* **Docker** (v24.0 ou superior)
* **Docker Compose** (v2.20 ou superior)
* **Git**

*(Opcional para execução fora do Docker)*:
* **JDK 21** instalado e configurado no `PATH`.

---

## 4. Como Executar com Docker Compose (Build Local)
O projeto foi configurado com valores padrão (*fallback*) em todas as variáveis de ambiente. Isso garante que o ambiente completo suba com **um único comando**, mesmo sem a criação manual prévia de um arquivo `.env`:

```bash
# 1. Clone o repositório
git clone <URL_DO_REPOSITORIO>
cd api

# 2. Suba toda a infraestrutura do back-end (Banco + Keycloak + API) construindo a imagem localmente
docker compose up -d --build

# 3. Verifique o estado e a saúde (healthy) dos containers
docker compose ps
```

### Subindo com o Front-end Angular (Opcional)
Caso o repositório do front-end (`simucred_web`) esteja clonado ao lado deste repositório, utilize o profile `full` para subir os 4 serviços simultaneamente:

```bash
docker compose --profile full up -d --build
```

### Comandos Úteis de Gerenciamento
```bash
# Visualizar os logs da API em tempo real
docker compose logs -f api

# Parar e remover os containers mantendo os dados salvos no volume do banco
docker compose down

# Parar os containers e APAGAR o volume do banco (útil ao trocar senhas no .env)
docker compose down -v
```

---

## 5. Como Executar a Imagem Publicada no Docker Hub (Produção)
A imagem oficial validada pelo pipeline de CI/CD encontra-se publicada publicamente no Docker Hub:
* **Repositório no Docker Hub:** [`alezzin/simucred-api`](https://hub.docker.com/r/alezzin/simucred-api)
* **Imagem:** `alezzin/simucred-api:latest`

Para executar o ambiente utilizando diretamente a imagem pronta da nuvem (sem necessidade de compilar o código localmente), utilize o arquivo `docker-compose.prod.yml`:

```bash
# 1. Baixar a imagem diretamente do Docker Hub (teste de visibilidade pública)
docker pull alezzin/simucred-api:latest

# 2. Subir o ambiente completo utilizando a imagem publicada
docker compose -f docker-compose.prod.yml up -d

# 3. Verificar os containers em execução
docker compose -f docker-compose.prod.yml ps

# 4. Encerrar o ambiente
docker compose -f docker-compose.prod.yml down
```

---

## 6. Endpoints Principais e Verificação de Saúde
Após iniciar os containers, aguarde cerca de 20 a 30 segundos e valide os serviços nos seguintes endereços:

* **Healthcheck da API (Spring Boot Actuator):**
  * URL: `http://localhost:8080/actuator/health`
  * Resposta esperada: `{"status":"UP"}`
* **API Base URL:** `http://localhost:8080/v1`
* **Keycloak Admin Console:** `http://localhost:8081`
* **Keycloak Realm Endpoint (Importado Automaticamente):** `http://localhost:8081/realms/simucred`
* **Front-end Web (quando iniciado com `--profile full`):** `http://localhost:4200`

---

## 7. Como Executar os Testes Automatizados
Os testes unitários e de integração foram desenvolvidos com **JUnit 5**. Para executá-los localmente utilizando o Maven Wrapper (que garante a versão exata do Maven sem exigir instalação global):

```bash
# No Linux / macOS
./mvnw clean test

# No Windows (PowerShell / CMD)
.\mvnw.cmd clean test
```

---

## 8. Variáveis de Ambiente
Nenhuma credencial sensível ou senha pessoal está fixa (*hardcoded*) na imagem Docker ou no código-fonte. O projeto disponibiliza o arquivo `.env.example` versionado na raiz como modelo.

Caso deseje customizar portas ou credenciais localmente, crie uma cópia chamada `.env` (que é ignorada pelo Git via `.gitignore`):

```bash
cp .env.example .env
```

| Variável | Descrição | Valor Padrão (`.env.example`) |
| :--- | :--- | :--- |
| `DB_HOST` | Host do banco de dados na rede interna do Docker Compose | `postgres` |
| `DB_PORT` | Porta interna do banco de dados PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco de dados criado na inicialização | `simucred` |
| `DB_USER` | Usuário de autenticação do banco de dados | `postgres` |
| `DB_PASSWORD` | Senha de autenticação do banco de dados | `postgrespassword` |
| `KEY_USER` | Usuário administrador do console do Keycloak | `admin` |
| `KEY_PASSWORD` | Senha do administrador do Keycloak | `admin` |
| `KEY_PORT` | Porta exposta no host para acesso ao Keycloak | `8081` |
| `KEY_REALM` | Nome do Realm OAuth2/OIDC importado no Keycloak | `simucred` |
| `KEY_CLIENT_ID` | Client ID configurado no Keycloak para o front-end | `simucred-web` |
| `API_PORT` | Porta exposta no host para acesso à API Spring Boot | `8080` |
| `WEB_PORT` | Porta exposta no host para o front-end Angular (Profile `full`) | `4200` |
| `WEB_PATH` | Caminho relativo para o contexto de build do front-end | `../simucred_web/simucred` |

---

## 9. Pipeline de CI/CD (GitHub Actions)
O fluxo de Integração e Entrega Contínua está definido em `.github/workflows/ci_cd.yml`, dividido em dois jobs estritamente separados e integrados ao cofre de **GitHub Secrets**:

### Etapa 1: `ci` (Integração Contínua)
Disparada automaticamente em qualquer `push` nas branches de trabalho (`dev`, `main`, `infra/**`, `feature/**`, `fix/**`, `test/**`) e em abertura de `Pull Requests` para `dev` e `main`:
1. **Checkout e Setup do JDK 21** (Eclipse Temurin com cache de dependências do Maven).
2. **Execução dos Testes Automatizados (`./mvnw clean test`):** Valida as regras de negócio utilizando um container de serviço `postgres:16-alpine` efêmero configurado com *GitHub Secrets*.
3. **Build Único da Imagem Docker:** Constrói a imagem `app:${{ github.sha }}` a partir do `Dockerfile`.
4. **Validação Real com Docker Compose:** Executa `docker compose up -d` no runner do GitHub Actions, aguarda a subida dos containers, inspeciona os logs e valida se o container `simucred-api` permanece em estado `Running` e saudável.
5. **Exportação do Artefato (Regra de Ouro):** Salva a imagem validada em arquivo binário (`docker save --output imagem.tar`) e faz upload como artefato (`imagem-docker-validada`) para garantir que o job de CD não reconstrua a imagem do zero.

### Etapa 2: `cd` (Entrega Contínua)
Executada **exclusivamente após o sucesso do job de CI** (`needs: ci`) e **somente em eventos de `push` (merge) na branch principal `main`**:
1. Faz o download do artefato `imagem-docker-validada` gerado no CI.
2. Carrega exatamente os mesmos bytes da imagem testada via `docker load --input imagem.tar`.
3. Autentica no Docker Hub utilizando os segredos `DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`.
4. Aplica as tags `:latest` e `:${{ github.sha }}` e publica a imagem em `alezzin/simucred-api`.

---

## 10. Boas Práticas de Containerização e Segurança Aplicadas
* **Multi-Stage Build no `Dockerfile`:** O primeiro estágio (`maven:3.9.6-eclipse-temurin-21-alpine`) baixa as dependências em camada separada para aproveitar o cache e compila o `.jar`; o estágio final (`eclipse-temurin:21-jre-alpine`) contém apenas o JRE enxuto e o artefato compilado, reduzindo drasticamente a superfície de ataque e o tamanho final da imagem.
* **Arquivo `.dockerignore`:** Impede que a pasta `.git`, artefatos locais (`target/`), arquivos de documentação e arquivos `.env` sejam enviados ao contexto de build da imagem.
* **Execução com Usuário Não-Root:** Criação do grupo `appgroup` e usuário `appuser` no estágio final do `Dockerfile` (`USER appuser`), garantindo que o processo Java não rode com privilégios de `root`.
* **Healthcheck Nativo na Imagem e no Compose:** O `Dockerfile` monitora periodicamente o endpoint `/actuator/health` via `wget`, e o `docker-compose.yml` utiliza `condition: service_healthy` para orquestrar a ordem correta de inicialização entre o PostgreSQL e a API.
* **Isolamento de Rede e Persistência:** Os containers comunicam-se internamente por nome de serviço (`postgres`, `keycloak`, `api`) através da rede bridge dedicada `simucred-net`, sem expor a porta do banco de dados desnecessariamente no host, mantendo os dados íntegros no volume nomeado `postgres_data`.
# 💰 Simucred API — Sistema de Simulação de Crédito

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Keycloak](https://img.shields.io/badge/Keycloak-24.0.5-purple)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)

## 1. Descrição do Projeto
O **Simucred** é uma plataforma de simulação de crédito. O usuário informa renda, valor desejado e prazo; a **API** calcula a parcela pela **Tabela Price**, aplica as regras de negócio (a parcela não pode comprometer mais de 30% da renda), decide se a proposta é **aprovada** ou **reprovada** e grava o histórico de simulações.

Este repositório contém o **back-end (API)** e o `docker-compose.yml` que sobe a **aplicação completa** (banco, Keycloak, API e front-end). O front-end fica no repositório [`simucred_web`](https://github.com/Simucred/simucred_web).

A segurança da API é baseada no protocolo **OAuth2 / OpenID Connect (OIDC)** utilizando tokens **JWT** emitidos e validados pelo **Keycloak**, enquanto a persistência relacional é gerenciada pelo **PostgreSQL** com versionamento de schema pelo **Flyway**.

---

## 2. Arquitetura e Tecnologias Utilizadas

```text
                 Navegador (http://localhost:4200)
                    │                        │
          telas     │                        │  login (OAuth2 / OIDC)
                    ▼                        ▼
        ┌──────────────────────┐   ┌──────────────────────┐
        │   simucred-web       │   │  simucred-keycloak   │
        │   Angular (SSR)      │   │  Keycloak 24         │
        │   porta 4200         │   │  porta 8081          │
        └──────────┬───────────┘   └──────────▲───────────┘
                   │ REST/JSON + token JWT     │ valida o token (JWK)
                   ▼                           │
        ┌──────────────────────┐               │
        │   simucred-api       │───────────────┘
        │   Spring Boot 4      │
        │   porta 8080         │
        └──────────┬───────────┘
                   │ JDBC (rede interna simucred-net)
                   ▼
        ┌──────────────────────┐
        │   simucred-db        │
        │   PostgreSQL 16      │
        │   sem porta no host  │
        └──────────────────────┘
```

* **Linguagem e Runtime:** Java 21 (Eclipse Temurin)
* **Framework Back-end:** Spring Boot 4.1 (Spring Web, Spring Data JPA, Spring Security OAuth2 Resource Server, Spring Boot Actuator)
* **Banco de Dados:** PostgreSQL 16 (`postgres:16-alpine`) com migrações Flyway
* **Identidade e Autenticação (IAM):** Keycloak 24.0.5 (`quay.io/keycloak/keycloak:24.0.5`) com importação automática de Realm (`realm-export.json`)
* **Gerenciador de Dependências e Build:** Apache Maven (via Maven Wrapper `./mvnw`)
* **Testes Automatizados:** JUnit 5, Spring Boot Test e Testcontainers
* **Containerização e Orquestração:** Docker (Multi-Stage Build) e Docker Compose
* **CI/CD:** GitHub Actions com publicação automatizada no Docker Hub

### Topologia dos Containers (`simucred-net`)
1. **`simucred-db` (PostgreSQL 16):** Banco de dados isolado na rede interna (`5432/tcp`), com volume nomeado (`postgres_data`) para persistência de dados e `healthcheck` nativo (`pg_isready`).
2. **`simucred-keycloak` (Keycloak 24):** Servidor de autorização que importa automaticamente o realm `simucred` na inicialização (`--import-realm`), exposto na porta `8081`.
3. **`simucred-api` (Spring Boot):** API REST executada sob usuário não-root (`spring`), aguardando a saúde do banco (`service_healthy`) e exposta na porta `8080`.
4. **`simucred-web` (Angular):** Aplicação front-end consumidora da API e do Keycloak, exposta na porta `4200`.

### Endpoints da API
| Método | Rota | Descrição |
| --- | --- | --- |
| `POST` | `/v1/simulacoes` | Realiza uma simulação de crédito |
| `GET` | `/v1/simulacoes` | Lista as simulações realizadas |
| `GET` | `/v1/simulacoes/resumo` | Totais, taxa de aprovação e valor médio |

Todas as rotas exigem token JWT do Keycloak. A documentação interativa (Swagger) fica em `http://localhost:8080/swagger-ui.html`.

---

## 3. Pré-requisitos
Para executar o projeto via containers, você precisa apenas de:
* **Docker** (v24.0 ou superior)
* **Docker Compose** (v2.20 ou superior)
* **Git**

*(Opcional, para executar a API fora do Docker ou rodar os testes)*:
* **JDK 21** instalado e configurado no `PATH` (ou `JAVA_HOME`).

---

## 4. Como Executar com Docker Compose (Build Local)
O projeto foi configurado com valores padrão (*fallback*) em todas as variáveis de ambiente. Isso garante que o ambiente completo suba com **um único comando**, mesmo sem a criação manual prévia de um arquivo `.env`:

```bash
# 1. Clone o repositório
git clone https://github.com/Simucred/simucred-api.git
cd simucred-api

# 2. Suba tudo (Banco + Keycloak + API + Front-end) construindo as imagens localmente
docker compose up -d --build

# 3. Verifique o estado e a saúde (healthy) dos containers
docker compose ps
```

Os 4 serviços sobem juntos. O front-end é construído **direto do repositório [`simucred_web`](https://github.com/Simucred/simucred_web)** no GitHub (branch `main`), sem precisar cloná-lo. Para construir o front a partir de uma cópia local, defina `WEB_PATH` no `.env` (ex.: `WEB_PATH=../simucred_web/simucred`).

### Acessando a aplicação
Abra http://localhost:4200. Na tela de login do Keycloak, clique em **Register** para criar um usuário (o cadastro está liberado no realm `simucred`, importado automaticamente de `keycloak/realm-export.json`).

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
As imagens oficiais validadas pelos pipelines de CI/CD estão publicadas publicamente no Docker Hub:

| Serviço | Imagem | Publicada por |
| --- | --- | --- |
| API | [`alezzin/simucred-api:latest`](https://hub.docker.com/r/alezzin/simucred-api) | CD deste repositório |
| Front-end | [`alezzin/simucred-web:latest`](https://hub.docker.com/r/alezzin/simucred-web) | CD do repositório `simucred_web` |

Além da `latest`, cada publicação recebe uma tag com o **SHA do commit** (ex.: `alezzin/simucred-api:<sha>`), o que permite voltar para uma versão anterior trocando a tag.

Para executar o ambiente utilizando diretamente as imagens prontas (sem compilar nada localmente), utilize o arquivo `docker-compose.prod.yml`:

```bash
# 1. Baixar as imagens diretamente do Docker Hub (teste de visibilidade pública)
docker pull alezzin/simucred-api:latest
docker pull alezzin/simucred-web:latest

# 2. Subir o ambiente completo utilizando as imagens publicadas
docker compose -f docker-compose.prod.yml up -d

# 3. Verificar os containers em execução
docker compose -f docker-compose.prod.yml ps

# 4. Encerrar o ambiente
docker compose -f docker-compose.prod.yml down
```

---

## 6. Como Executar a API Localmente (sem Docker)
Útil para desenvolvimento. A API roda direto na máquina com o Maven Wrapper; o **banco** e o **Keycloak** continuam em containers.

```bash
# 1. Suba o Keycloak pelo compose (fica acessível em localhost:8081)
docker compose up -d keycloak

# 2. Suba um PostgreSQL acessível em localhost:5432
#    (o postgres do compose não publica porta no host, por isso um container avulso)
docker run -d --name simucred-db-local -p 5432:5432 \
  -e POSTGRES_DB=simucred -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgrespassword \
  postgres:16-alpine
```

Defina as variáveis de ambiente e rode a API. No Linux / macOS:

```bash
export DB_HOST=localhost DB_PORT=5432 DB_NAME=simucred DB_USER=postgres DB_PASSWORD=postgrespassword
# Fora do Docker, as chaves do Keycloak são buscadas em localhost:8081
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://localhost:8081/realms/simucred/protocol/openid-connect/certs
./mvnw spring-boot:run
```

No Windows (PowerShell):

```powershell
$env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="simucred"; $env:DB_USER="postgres"; $env:DB_PASSWORD="postgrespassword"
$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI="http://localhost:8081/realms/simucred/protocol/openid-connect/certs"
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080`.

---

## 7. Verificação dos Serviços
Após iniciar os containers, aguarde cerca de 20 a 30 segundos e valide os serviços nos seguintes endereços:

* **Front-end Web:** `http://localhost:4200`
* **API Base URL:** `http://localhost:8080/v1` — sem token responde `401 Unauthorized`, o que indica que a API está no ar e protegida
* **Swagger da API:** `http://localhost:8080/swagger-ui.html`
* **Keycloak Admin Console:** `http://localhost:8081` (usuário `KEY_USER` / senha `KEY_PASSWORD`)
* **Keycloak Realm Endpoint (Importado Automaticamente):** `http://localhost:8081/realms/simucred`

---

## 8. Como Executar os Testes Automatizados
Os testes unitários e de integração foram desenvolvidos com **JUnit 5**. O teste de contexto da aplicação usa **Testcontainers** para subir um PostgreSQL temporário, então é preciso ter o **Docker em execução**. Para executá-los com o Maven Wrapper (que garante a versão exata do Maven sem exigir instalação global):

```bash
# No Linux / macOS
./mvnw clean test

# No Windows (PowerShell / CMD)
.\mvnw.cmd clean test
```

---

## 9. Pipeline de CI/CD (GitHub Actions)
O fluxo de Integração e Entrega Contínua está definido em `.github/workflows/ci_cd.yml`, dividido em três jobs separados e integrados ao cofre de **GitHub Secrets**. As execuções ficam na aba **Actions** do repositório.

```text
push / PR ──> ci (runner) ──> cd (nuvem, só na main) ──> deploy (runner, só na main)
              testes, build     publica no Docker Hub     sobe a aplicação em localhost
```

O `ci` e o `deploy` rodam em um **runner self-hosted** (`[self-hosted, linux]`), ou seja, numa máquina da equipe registrada no GitHub Actions. Para não se atrapalharem na mesma máquina, cada um usa um projeto do Compose próprio (`COMPOSE_PROJECT_NAME`) e um prefixo de containers próprio (`CONTAINER_PREFIX`), com containers e volumes separados:

| Job | Projeto | Containers | Portas no host |
| --- | --- | --- | --- |
| `ci` | `simucred-ci` | `simucred-ci-db`, `simucred-ci-keycloak`, `simucred-ci-api` | API `18080`, Keycloak `18081` |
| `deploy` | `simucred-prod` | `simucred-prod-db`, `simucred-prod-keycloak`, `simucred-prod-api`, `simucred-prod-web` | API `8080`, Keycloak `8081`, Front `4200` |

Rodando o Compose manualmente (sem `CONTAINER_PREFIX`), os containers se chamam `simucred-db`, `simucred-keycloak`, `simucred-api` e `simucred-web`.

### Etapa 1: `ci` (Integração Contínua)
Disparada em `push` nas branches `dev`, `main`, `infra/**`, `feature/**`, `fix/**` e `test/**`, e em `Pull Requests` para `dev` e `main`:
1. **Checkout e limpeza** de containers de execuções anteriores do CI.
2. **Setup do JDK 21** (Eclipse Temurin com cache de dependências do Maven).
3. **Banco para os testes:** sobe o `postgres` pelo Docker Compose e aguarda o `pg_isready`.
4. **Execução dos Testes Automatizados (`./mvnw clean test`).**
5. **Build Único da Imagem Docker:** constrói a imagem `app:${{ github.sha }}` a partir do `Dockerfile`.
6. **Validação com Docker Compose:** executa `docker compose up -d postgres keycloak api`, aguarda a subida, exibe os logs e verifica se o container `simucred-ci-api` permanece em execução (`Running`).
7. **Exportação do Artefato (Regra de Ouro):** salva a imagem validada (`docker save --output imagem.tar`) e faz upload como artefato (`imagem-docker-validada`) para que o CD não reconstrua a imagem do zero.
8. **Limpeza final** (`if: always()`): derruba os containers e o volume do CI mesmo se algum passo falhar.

### Etapa 2: `cd` (Entrega Contínua)
Executada **exclusivamente após o sucesso do job de CI** (`needs: ci`) e **somente em eventos de `push` (merge) na branch principal `main`**:
1. Faz o download do artefato `imagem-docker-validada` gerado no CI.
2. Carrega exatamente os mesmos bytes da imagem testada via `docker load --input imagem.tar`.
3. Autentica no Docker Hub utilizando os segredos `DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`.
4. Aplica as tags `:latest` e `:${{ github.sha }}` e publica a imagem em `alezzin/simucred-api`.

### Etapa 3: `deploy` (ambiente local no runner)
Executada **após o sucesso do CD** (`needs: cd`) e **somente em `push` na `main`**, no runner self-hosted:
1. Faz checkout do repositório (para ter o `docker-compose.prod.yml` e o realm do Keycloak).
2. Baixa as imagens publicadas (`docker compose -f docker-compose.prod.yml pull`), usando para a API **exatamente a tag do commit** publicada pelo CD (`API_IMAGE=alezzin/simucred-api:<sha>`).
3. Sobe a aplicação (`docker compose -f docker-compose.prod.yml up -d`), com o banco configurado pelos GitHub Secrets.
4. Verifica se o front responde (`200` em `http://localhost:4200`) e se a API responde (`401` sem token em `http://localhost:8080/v1/simulacoes`).

Depois de cada merge na `main`, a aplicação fica no ar em **http://localhost:4200** na máquina do runner, com a versão recém-publicada. O banco (`simucred-prod_postgres_data`) é mantido entre os deploys.

---

## 10. Variáveis de Ambiente
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
| `WEB_PORT` | Porta exposta no host para o front-end Angular | `4200` |
| `WEB_PATH` | (Opcional) Contexto de build do front-end. Padrão: repositório `simucred_web` no GitHub (branch `main`) | `../simucred_web/simucred` |
| `CONTAINER_PREFIX` | (Opcional) Prefixo dos nomes dos containers. O pipeline usa `simucred-ci` e `simucred-prod` | `simucred` |
| `API_IMAGE` | (Opcional, só no `docker-compose.prod.yml`) Imagem da API a executar | `alezzin/simucred-api:latest` |

---

## 11. Boas Práticas de Containerização e Segurança Aplicadas
* **Multi-Stage Build no `Dockerfile`:** o primeiro estágio (`maven:3.9.6-eclipse-temurin-21-alpine`) baixa as dependências em camada separada para aproveitar o cache e compila o `.jar`; o estágio final (`eclipse-temurin:21-jre-alpine`) contém apenas o JRE enxuto e o artefato compilado, reduzindo a superfície de ataque e o tamanho final da imagem.
* **Arquivo `.dockerignore`:** impede que a pasta `.git`, artefatos locais (`target/`), arquivos de documentação e arquivos `.env` sejam enviados ao contexto de build da imagem.
* **Execução com Usuário Não-Root:** criação do grupo e usuário `spring` no estágio final do `Dockerfile` (`USER spring:spring`), garantindo que o processo Java não rode com privilégios de `root`.
* **Ordem de inicialização com health check:** o `docker-compose.yml` usa `condition: service_healthy` para que a API só suba depois que o PostgreSQL responder ao `pg_isready`.
* **Isolamento de Rede e Persistência:** os containers comunicam-se internamente por nome de serviço (`postgres`, `keycloak`, `api`) através da rede bridge dedicada `simucred-net`, sem expor a porta do banco de dados no host, mantendo os dados no volume nomeado `postgres_data`.

### Limitações conhecidas
* O `HEALTHCHECK` da imagem da API consulta `/actuator/health`, mas esse endpoint ainda exige autenticação (responde `401`) e o comando termina com `|| exit 0`. Na prática o container é marcado como `healthy` mesmo que a API não esteja respondendo. Correção prevista: liberar `/actuator/health` na configuração de segurança e trocar para `|| exit 1`.

---

## 12. Troubleshooting
Problemas que a equipe encontrou ao rodar o projeto e como resolver:

| Sintoma | Causa | Como resolver |
| --- | --- | --- |
| A API não sobe e o log mostra `port is already allocated` ou `address already in use` | Outra aplicação já usa a porta `8080` (ou `8081` / `4200`) | Pare a outra aplicação ou troque a porta no `.env` (ex.: `API_PORT=8082`). O front é configurado automaticamente para a nova porta |
| O `simucred-db` não sobe e o log mostra `database files are incompatible with server` | O volume do banco foi criado com uma versão anterior do PostgreSQL (15) | `docker compose down -v` e suba de novo (os dados locais são apagados) |
| A API cai com `password authentication failed for user` | O volume do banco foi criado com outro usuário/senha. O PostgreSQL só aplica `DB_USER`/`DB_PASSWORD` na **primeira** criação do volume | Use os mesmos valores de antes no `.env`, ou `docker compose down -v` para recriar o banco |
| O front abre, mas mostra "Não foi possível conectar com a API" depois de trocar a porta | O navegador guardou uma versão antiga do `env-config.js` | Recarregue a página com **Ctrl + F5** |
| Mudanças no código do front não aparecem no `docker compose up --build` | Por padrão o front é construído a partir da branch `main` no GitHub | Defina `WEB_PATH=../simucred_web/simucred` no `.env` para usar uma cópia local |

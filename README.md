# fiap-tech-challenge-inventory-service

Microsserviço de **Estoque** (peças e insumos) do Tech Challenge — FIAP Pós-Tech, Arquitetura de Software, turma 15SOAT, **Fase 4** (microsserviços + Saga).

Faz parte do sistema de gestão de oficina mecânica, que na Fase 4 foi dividido em quatro serviços independentes (OS, Billing, Execution e Inventory). Cada serviço tem repositório, infraestrutura e banco próprios.

| Repositório | Papel |
|---|---|
| **fiap-tech-challenge-inventory-service** (este) | Código do serviço de estoque |
| `fiap-tech-challenge-inventory-database-infrastructure` | RDS PostgreSQL do serviço (Terraform) |
| `fiap-tech-challenge-inventory-infrastructure` | Namespace `inventory` e filas SQS do serviço (Terraform) |
| `fiap-tech-challenge-os-service` | Serviço de OS (orquestrador da saga) |
| `fiap-tech-challenge-os-infrastructure` | VPC, EKS compartilhado, New Relic e tópico SNS |

> **Estado atual:** esqueleto do serviço com o CRUD de peças/insumos e o ajuste de estoque, migrados do monólito. A reserva de estoque (`StockReservation`) e os handlers de comandos da saga (SQS) entram nos próximos PRs.

## Responsabilidade

Dono dos dados de `Supply` (peças `PART` e insumos `CONSUMABLE`): código, nome, preço unitário, quantidade em estoque e estoque mínimo. Nenhum outro serviço acessa o banco deste.

## Arquitetura

Clean Architecture em quatro camadas, com a regra de dependência `domain ← application ← infrastructure / interfaces` **verificada no build** por ArchUnit (`ArchitectureTest`).

```
src/main/java/com/fiap/workshop/inventory/
├── domain/            Supply, SupplyType, Money, exceções, SupplyRepository (interface). Sem Spring/JPA.
├── application/       Use cases (um por operação), ports de entrada, DTOs.
├── infrastructure/    Adaptadores: JPA/PostgreSQL, segurança JWT, OpenAPI.
└── interfaces/rest/   SupplyController (fino) e GlobalExceptionHandler.
```

Decisões:

- **Banco:** PostgreSQL, migrations com Flyway (`V1` schema, `V2` seed com os mesmos ids do monólito).
- **Autenticação:** o token JWT é emitido pelo OS Service; aqui ele só é **validado** (HMAC com o `JWT_SECRET` compartilhado, o mesmo do OS). O token precisa ter as claims `sub` e `role`; sem `role` a requisição é tratada como não autenticada (401). Não há banco de usuários neste serviço.
- **Money** é duplicado em cada serviço de propósito (sem biblioteca compartilhada entre serviços).
- **Observabilidade:** agente New Relic anexado no `Dockerfile` (`NEW_RELIC_APP_NAME=workshop-inventory-service`), logs em JSON com `trace.id`/`span.id`.

## API

Swagger UI: `http://<host>/swagger-ui.html` · OpenAPI: `/v3/api-docs`. As rotas de `/api/v1/supplies` exigem `Authorization: Bearer <jwt>`.

| Método | Rota | Perfis |
|---|---|---|
| `POST` | `/api/v1/supplies` | ADMIN |
| `GET` | `/api/v1/supplies` (`?belowMinimum=true`) e `/api/v1/supplies/{id}` | STOCKIST, CONSULTANT, MECHANIC, ADMIN |
| `PUT` | `/api/v1/supplies/{id}` | ADMIN |
| `DELETE` | `/api/v1/supplies/{id}` | ADMIN |
| `PATCH` | `/api/v1/supplies/{id}/stock` (entrada de estoque) | STOCKIST, ADMIN |
| `GET` | `/actuator/health` | público |

## Como rodar localmente

```bash
docker compose up --build        # PostgreSQL + serviço em http://localhost:8081
```

Para chamar a API é preciso um JWT assinado com o mesmo `JWT_SECRET`, contendo a claim `role`. Enquanto o OS Service não emitir essa claim, gere um token de teste (o segredo de desenvolvimento está em `application.yml`).

## Testes e qualidade

```bash
mvn verify        # testes unitários + integração (Testcontainers/PostgreSQL, requer Docker) + gate JaCoCo >= 80%
```

- Cobertura atual: ~98% de instruções (gate do build: 80%). Relatório em `target/site/jacoco/`.
- `ArchitectureTest` falha o build se uma camada violar a regra de dependência.
- `SupplyApiIntegrationTest` sobe o serviço inteiro contra um PostgreSQL real (migrations, segurança e REST juntos).
- Análise estática e quality gate no **SonarQube Cloud** (step do CI, ativo quando o secret `SONAR_TOKEN` existe).

## CI/CD

Workflows em [`.github/workflows/`](.github/workflows/):

- `ci-cd.yml` — job `build-and-test` (`mvn verify` + Sonar) em todo push/PR; em `develop`/`main` também publica a imagem `adrianameyer/workshop-inventory-service` no Docker Hub.
- `deploy-aws.yml` — deploy/destroy no EKS compartilhado (namespace `inventory`), disparado manualmente (`workflow_dispatch`). Também roda após um CI verde na `main` **se** a variável de repositório `AUTO_DEPLOY` for `true`.

Secrets do repositório: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (renovados a cada sessão do Lab), `JWT_SECRET` (igual ao do OS), `NEW_RELIC_LICENSE_KEY`, `SONAR_TOKEN`, e `RDS_DATASOURCE_URL` / `RDS_USERNAME` / `RDS_PASSWORD` (enviados automaticamente pelo `inventory-database-infrastructure`).

Ordem de subida no Lab: `os-infrastructure` → `inventory-infrastructure` → `inventory-database-infrastructure` → deploy deste serviço. Na destruição, a ordem inversa (rodar `destroy` aqui primeiro libera o NLB).

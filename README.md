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

> **Estado atual:** CRUD de peças/insumos, ajuste de estoque e a **reserva de estoque por OS** (`StockReservation`), executada pelos comandos da saga que chegam pela fila SQS `inventory-commands` (seção "Mensageria").

## Responsabilidade

Dono dos dados de `Supply` (peças `PART` e insumos `CONSUMABLE`): código, nome, preço unitário, quantidade em estoque e estoque mínimo, e das **reservas de estoque** de cada OS. Nenhum outro serviço acessa o banco deste.

### Reserva de estoque

Uma `StockReservation` por OS (`UNIQUE service_order_id`), com as linhas (peça e quantidade):

| Estado | Como chega | Efeito no estoque |
|---|---|---|
| `RESERVED` | comando `ReserveStock` | a quantidade **sai da prateleira** na hora da reserva |
| `RELEASED` | comando `ReleaseStock` (compensação) | a quantidade **volta** para a prateleira |
| `CONSUMED` | comando `ConsumeStock` (passo pivô da saga) | nada muda: o estoque já tinha saído; a reserva só é encerrada |

- **Tudo ou nada:** se uma das peças não tem estoque (ou não existe), nenhuma é reservada. Tudo roda numa transação, então a falta de uma peça desfaz as retiradas anteriores.
- **Sem venda dupla:** as linhas das peças são travadas (`SELECT ... FOR UPDATE`, em ordem de id, para evitar deadlock) enquanto a reserva é feita.
- **Idempotência:** repetir `ReserveStock` devolve a reserva existente sem tirar estoque de novo; `ReleaseStock` e `ConsumeStock` repetidos não fazem nada. Liberar uma reserva já consumida (ou consumir uma já liberada) é erro.
- Uma OS sem peças gera uma reserva vazia, para a saga tratá-la como qualquer outra.

## Mensageria

Comandos chegam pela fila `inventory-commands`; as respostas saem para o tópico `saga-events`, que alimenta a fila do OS. Todas as mensagens usam o mesmo envelope JSON:

```json
{ "messageId": "…", "sagaId": "…", "serviceOrderId": "…", "type": "ReserveStock", "payload": { … } }
```

| Comando recebido (`type`) | `payload` | Resposta publicada |
|---|---|---|
| `ReserveStock` | `items: [{ supplyId, quantity }]` (pode ser vazio) | `StockReserved`, ou `StockReservationFailed` `{ reason }` |
| `ReleaseStock` | (vazio) | `StockReleased` |
| `ConsumeStock` | (vazio) | `StockConsumed` |

- **Falta de estoque não é falha técnica:** `StockReservationFailed` (estoque insuficiente ou peça inexistente) é uma resposta normal, que faz o OS cancelar o orçamento.
- **Reentrega segura:** o `messageId` da resposta é derivado do `messageId` do comando. Se o comando chega duas vezes, o OS recebe duas respostas com o mesmo id e descarta a repetida.
- **Falha vai para a DLQ:** se o comando não pode ser tratado (JSON inválido, tipo desconhecido, consumir uma reserva que não existe), a mensagem não é confirmada; o SQS a reentrega e, ao passar do máximo de tentativas da fila, a move para `inventory-commands-dlq`.
- **Filas são do Terraform:** o serviço não cria filas. Se `inventory-commands` não existir, ele falha na subida, o que denuncia um `inventory-infrastructure` não aplicado.
- Variáveis: `COMMANDS_QUEUE` (padrão `inventory-commands`), `EVENTS_TOPIC_ARN` (o `deploy-aws.yml` monta a partir da conta e da região) e `MESSAGING_ENABLED` (`false` desliga o listener, usado no `docker compose` isolado e em testes que não usam filas).
- Sem outbox: a resposta é publicada depois de gravar. Se a publicação falha, o comando volta para a fila e é refeito, e a idempotência garante que o estoque não é tirado duas vezes.

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
| `GET` | `/api/v1/stock-reservations/{serviceOrderId}` (somente leitura: reservas são feitas por comando da saga) | STOCKIST, CONSULTANT, ADMIN |
| `GET` | `/actuator/health` | público |

## Como rodar localmente

```bash
docker compose up --build        # PostgreSQL + serviço em http://localhost:8081 (sem filas)
```

Para chamar a API é preciso um JWT assinado com o mesmo `JWT_SECRET`, contendo a claim `role`. Enquanto o OS Service não emitir essa claim, gere um token de teste (o segredo de desenvolvimento está em `application.yml`).

## Testes e qualidade

```bash
mvn verify        # testes unitários + integração (Testcontainers/PostgreSQL, requer Docker) + gate JaCoCo >= 80%
```

- Cobertura atual: ~98% de instruções (gate do build: 80%). Relatório em `target/site/jacoco/`.
- `ArchitectureTest` falha o build se uma camada violar a regra de dependência.
- `SagaMessagingIntegrationTest` roda o serviço contra SQS e SNS reais (LocalStack) e PostgreSQL: reserva, falta de estoque, tudo ou nada, liberação, consumo, comando repetido e mensagem que vai para a DLQ.
- `SupplyApiIntegrationTest` sobe o serviço inteiro contra um PostgreSQL real (migrations, segurança e REST juntos).
- Análise estática e quality gate no **SonarQube Cloud** (step do CI, ativo quando o secret `SONAR_TOKEN` existe).

## CI/CD

Workflows em [`.github/workflows/`](.github/workflows/):

- `ci-cd.yml` — job `build-and-test` (`mvn verify` + Sonar) em todo push/PR; em `develop`/`main` também publica a imagem `adrianameyer/workshop-inventory-service` no Docker Hub.
- `deploy-aws.yml` — deploy/destroy no EKS compartilhado (namespace `inventory`), disparado manualmente (`workflow_dispatch`). Também roda após um CI verde na `main` **se** a variável de repositório `AUTO_DEPLOY` for `true`.

Secrets do repositório: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` (renovados a cada sessão do Lab), `JWT_SECRET` (igual ao do OS), `NEW_RELIC_LICENSE_KEY`, `SONAR_TOKEN`, e `RDS_DATASOURCE_URL` / `RDS_USERNAME` / `RDS_PASSWORD` (enviados automaticamente pelo `inventory-database-infrastructure`).

Ordem de subida no Lab: `os-infrastructure` → `inventory-infrastructure` → `inventory-database-infrastructure` → deploy deste serviço. Na destruição, a ordem inversa (rodar `destroy` aqui primeiro libera o NLB).

# CarPass — API

Backend do CarPass, uma plataforma de gestão para oficinas mecânicas.

A oficina centraliza em um só lugar seus clientes, veículos, funcionários, catálogo de serviços, estoque de peças, ordens de serviço com orçamento e aprovação, e o histórico completo de manutenção de cada veículo.

Cada oficina enxerga apenas os próprios dados. Esse isolamento é garantido no backend, nunca no frontend.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Persistência | Spring Data JPA + Hibernate |
| Banco | PostgreSQL |
| Migrations | Flyway |
| Segurança | Spring Security + JWT (java-jwt) |
| Build | Gradle |

---

## Estrutura de pacotes

```
com.carpass.service_api
├── config/           configurações (segurança, propriedades da aplicação)
├── security/         filtro JWT, UserDetails, contexto do usuário logado
├── domain/
│   ├── enums/        enums do domínio
│   └── model/        entidades JPA
├── repository/       repositórios Spring Data
├── service/          contratos de serviço
│   └── impl/         implementações
├── web/
│   ├── controller/   controllers REST
│   └── dto/          objetos de entrada e saída da API
└── exception/        exceções de negócio e tratamento centralizado de erros
```

---

## Configuração

Nenhum segredo fica no código. Toda credencial vem de variável de ambiente.

1. Copie o arquivo de exemplo:

   ```bash
   cp service-api/.env.example service-api/.env
   ```

2. Gere um segredo forte para o JWT e preencha `CARPASS_JWT_SECRET`:

   ```bash
   openssl rand -base64 48
   ```

   A aplicação **se recusa a subir** com um segredo ausente ou com menos de 32 caracteres.

3. Preencha `CARPASS_DB_PASSWORD` com a senha do seu Postgres.

Pronto. O `.env` é lido automaticamente na subida da aplicação, seja pela IDE, pelo `./gradlew bootRun` ou pelo jar — não é preciso cadastrar nada na configuração de execução.

> Quem faz isso é o `DotenvEnvironmentPostProcessor`. Variáveis de ambiente reais do sistema, quando existirem, têm prioridade sobre o arquivo — então em produção basta definir as variáveis normalmente e o `.env` é ignorado.

### Variáveis disponíveis

| Variável | Padrão | Descrição |
|---|---|---|
| `CARPASS_DB_URL` | `jdbc:postgresql://localhost:5432/carpass` | URL do banco |
| `CARPASS_DB_USERNAME` | `postgres` | Usuário do banco |
| `CARPASS_DB_PASSWORD` | — | Senha do banco |
| `CARPASS_JWT_SECRET` | — | Segredo de assinatura do token (mín. 32 caracteres) |
| `CARPASS_JWT_EXPIRATION_SECONDS` | `28800` | Validade do token de acesso |
| `CARPASS_ALLOWED_ORIGINS` | `http://localhost:4200` | Origens autorizadas no CORS. Nunca use `*` |
| `CARPASS_FRONTEND_URL` | `http://localhost:4200` | Base do link de redefinição de senha |
| `CARPASS_MAIL_ENABLED` | `false` | Com `false`, e-mails só aparecem no log |
| `CARPASS_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | — | SMTP |

---

## Banco de dados

O schema é controlado por **migrations do Flyway** em `src/main/resources/db/migration`.
O Hibernate roda em `ddl-auto=validate`: ele confere se o mapeamento bate com as tabelas, mas nunca altera o banco.

Para criar o banco local:

```bash
createdb carpass
```

Ou suba o Postgres via Docker Compose:

```bash
docker compose -f service-api/compose.yaml up -d
```

Ao subir a aplicação, o Flyway aplica as migrations pendentes automaticamente.

> **Nunca edite uma migration já aplicada.** Crie uma nova (`V2__...sql`, `V3__...sql`).

---

## Rodando

```bash
cd service-api
./gradlew bootRun
```

Com o perfil de desenvolvimento (SQL no log, e-mails no console):

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Testes:

```bash
./gradlew test
```

Os testes rodam contra um banco H2 em memória e não dependem do Postgres local.

---

## Endpoints

Base: `/api/v1`

### Públicos

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/login` | Autentica e devolve o token |
| `POST` | `/auth/register` | Cadastra a oficina e o usuário proprietário |
| `POST` | `/auth/password/forgot` | Solicita o link de redefinição de senha |
| `POST` | `/auth/password/reset` | Redefine a senha usando o token recebido por e-mail |

### Autenticados

Todas exigem o cabeçalho `Authorization: Bearer <token>`.

**Conta e oficina**

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/me` | todos |
| `PUT` | `/me` | todos |
| `PATCH` | `/me/password` | todos |
| `GET` | `/workshop` | todos |
| `PUT` | `/workshop` | `OWNER`, `MANAGER` |

**Clientes** — `/customers`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/customers?q=&active=&page=&size=&sort=` | todos |
| `GET` | `/customers/{id}` | todos |
| `GET` | `/customers/{id}/vehicles` | todos |
| `POST` | `/customers` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `PUT` | `/customers/{id}` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `PATCH` | `/customers/{id}/status` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `DELETE` | `/customers/{id}` | `OWNER`, `MANAGER`, `RECEPTIONIST` |

**Veículos** — `/vehicles`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/vehicles?q=&customerId=&active=&page=&size=&sort=` | todos |
| `GET` | `/vehicles/{id}` | todos |
| `POST` | `/vehicles` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `PUT` | `/vehicles/{id}` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `PATCH` | `/vehicles/{id}/status` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `DELETE` | `/vehicles/{id}` | `OWNER`, `MANAGER`, `RECEPTIONIST` |

**Funcionários** — `/employees`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/employees?q=&role=&active=&page=&size=&sort=` | todos |
| `GET` | `/employees/mechanics` | todos |
| `GET` | `/employees/{id}` | todos |
| `POST` | `/employees` | `OWNER`, `MANAGER` |
| `PUT` | `/employees/{id}` | `OWNER`, `MANAGER` |
| `POST` | `/employees/{id}/access` | `OWNER`, `MANAGER` |
| `PATCH` | `/employees/{id}/status` | `OWNER`, `MANAGER` |
| `DELETE` | `/employees/{id}` | `OWNER`, `MANAGER` |

**Catálogo de serviços** — `/services`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/services?q=&active=&page=&size=&sort=` | todos |
| `GET` | `/services/{id}` | todos |
| `POST` / `PUT` / `PATCH .../status` / `DELETE` | `/services` | `OWNER`, `MANAGER` |

**Peças e estoque** — `/parts`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/parts?q=&active=&lowStock=&page=&size=&sort=` | todos |
| `GET` | `/parts/{id}` | todos |
| `GET` | `/parts/{id}/stock-movements` | todos |
| `POST` | `/parts` | `OWNER`, `MANAGER` |
| `PUT` | `/parts/{id}` | `OWNER`, `MANAGER` |
| `POST` | `/parts/{id}/stock-movements` | `OWNER`, `MANAGER` |
| `PATCH` | `/parts/{id}/status` | `OWNER`, `MANAGER` |
| `DELETE` | `/parts/{id}` | `OWNER`, `MANAGER` |

**Ordens de serviço** — `/work-orders`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/work-orders?q=&status=&mechanicId=&customerId=&vehicleId=&from=&to=&page=&size=&sort=` | todos |
| `GET` | `/work-orders/{id}` | todos |
| `GET` | `/work-orders/{id}/history` | todos |
| `POST` | `/work-orders` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `PUT` | `/work-orders/{id}` | todos |
| `POST` | `/work-orders/{id}/items` | todos |
| `PUT` | `/work-orders/{id}/items/{itemId}` | todos |
| `DELETE` | `/work-orders/{id}/items/{itemId}` | todos |
| `PATCH` | `/work-orders/{id}/status` | todos |
| `POST` | `/work-orders/{id}/deliver` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `POST` | `/work-orders/{id}/cancel` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `POST` | `/work-orders/{id}/quote/submit` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `POST` | `/work-orders/{id}/quote/approve` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `POST` | `/work-orders/{id}/quote/reject` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `POST` | `/work-orders/{id}/quote/reopen` | `OWNER`, `MANAGER` |

**Histórico do veículo** — `/vehicles/{id}/history`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/vehicles/{id}/history` | todos |
| `POST` | `/vehicles/{id}/history` | `OWNER`, `MANAGER`, `RECEPTIONIST` |
| `DELETE` | `/vehicles/{id}/history/{recordId}` | `OWNER`, `MANAGER`, `RECEPTIONIST` |

**Painel** — `/dashboard`

| Método | Rota | Papéis |
|---|---|---|
| `GET` | `/dashboard` | todos |

---

## Fluxo da ordem de serviço

```
                        ┌──────────────────────────────┐
                        ▼                              │
  OPEN ──► WAITING_DIAGNOSIS ──► IN_DIAGNOSIS ──► WAITING_APPROVAL
   │                                                   │
   │                                                   ▼
   │                                              APPROVED
   │                                                   │
   └──────────────────────────────────┐                ▼
                                      └──────────► IN_PROGRESS ◄──► WAITING_PARTS
                                                        │                 │
                                                        ▼                 │
                                                   FINISHED ◄─────────────┘
                                                        │
                                                        ▼
                                                   DELIVERED
```

`CANCELED` é alcançável de qualquer estado antes da entrega. `DELIVERED` e `CANCELED` são finais.

As transições permitidas ficam declaradas no próprio enum `WorkOrderStatus`, não espalhadas pelo serviço, e o `WorkOrderResponse` devolve `allowedNextStatuses` para a interface só oferecer ações válidas.

### Estoque e orçamento

- **A peça sai do estoque quando entra na OS**, não no fechamento. Assim o atendente descobre na hora que a peça não existe, em vez de a oficina prometer prazo e travar depois. Remover o item ou cancelar a OS devolve tudo à prateleira, com movimentação registrada nos dois sentidos.
- **Orçamento aprovado tranca os itens.** Para alterar, é preciso `POST /quote/reopen`, que devolve a OS para aprovação do cliente e registra o motivo na linha do tempo. Nada muda em silêncio depois que o cliente aceitou um valor.
- **Orçamento recusado não cancela a OS**: ela volta para `OPEN` para a oficina revisar e reapresentar. Cancelar continua sendo uma ação explícita.
- **Entregar atualiza a quilometragem do veículo** quando a leitura da OS é maior que a registrada.

### Histórico do veículo

A linha do tempo é montada na leitura, a partir de duas fontes: as ordens de serviço concluídas nesta oficina e os lançamentos manuais de serviços feitos em outro lugar (`POST /vehicles/{id}/history`).

Não copiamos a OS entregue para uma tabela de histórico de propósito: duplicar o dado obrigaria a manter duas verdades em sincronia, e qualquer correção feita na OS deixaria o histórico desatualizado. A ordem de serviço continua sendo a única fonte do que a oficina executou.

---

### Convenções

- **Paginação:** `?page=0&size=20&sort=name,asc`. A resposta vem no envelope `{ content, page, size, totalElements, totalPages, first, last }`.
- **`DELETE` desativa, não apaga.** Cliente, veículo, funcionário, serviço e peça aparecem no histórico de ordens de serviço; apagar o registro quebraria o passado. Para reativar, use `PATCH .../status` com `{"active": true}`.
- **Estoque só muda por movimentação.** Não existe endpoint que escreva o saldo direto: entrada, saída e ajuste passam por `/parts/{id}/stock-movements`, e cada alteração deixa rastro. Saída maior que o saldo é recusada com 400.
- **Erros seguem sempre o mesmo formato**, inclusive os de protocolo: `{ timestamp, status, error, message, path, details? }`. Validação de campo devolve 422 com `details` por campo.

Autenticação por cabeçalho:

```
Authorization: Bearer <token>
```

---

## Papéis

| Papel | Quem é |
|---|---|
| `SYSTEM_ADMIN` | Administração da plataforma. Não pertence a nenhuma oficina |
| `OWNER` | Dono da oficina |
| `MANAGER` | Gerente |
| `RECEPTIONIST` | Atendente |
| `MECHANIC` | Mecânico |

---

## Decisões de segurança

- **Nenhum endpoint público altera dados de um usuário existente.** A redefinição de senha exige um token de uso único enviado por e-mail, com validade curta, do qual o banco guarda apenas o hash SHA-256.
- **A resposta de "esqueci minha senha" é sempre a mesma**, exista ou não a conta, para não revelar quais e-mails estão cadastrados.
- **A oficina nunca vem da requisição.** Ela é lida do contexto de autenticação (`CurrentUser`), de modo que trocar um id no corpo ou na URL não dá acesso a dados de terceiros.
- **Recurso de outra oficina responde 404, não 403**, para não confirmar a existência do registro.
- **Erros nunca vazam stack trace.** O `GlobalExceptionHandler` devolve sempre um corpo padronizado e registra o detalhe técnico apenas no log.
- **Token inválido resulta em 401 limpo**, e não em erro 500.
- **CORS não aceita `*`.** A aplicação se recusa a subir se as origens permitidas contiverem curinga, porque a API usa credenciais.

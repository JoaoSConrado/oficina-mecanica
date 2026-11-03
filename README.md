# Oficina Mecânica — Sistema Integrado de Atendimento e Execução de Serviços

Tech Challenge — Fase 1 (SOAT · Software Architecture)

## Objetivo

Substituir as anotações manuais e planilhas da oficina por um back-end que organize o fluxo de
atendimento: abertura da ordem de serviço, diagnóstico, orçamento, aprovação pelo cliente,
execução e entrega, com controle de estoque de peças e insumos e acompanhamento em tempo real.

## Solução

MVP de back-end monolítico com API REST:

- **Ordem de Serviço (OS)**: identificação do cliente por CPF/CNPJ, cadastro do veículo pela placa
  (na própria abertura, se ainda não existir), inclusão de serviços e peças, orçamento calculado
  automaticamente e enviado ao cliente.
- **Acompanhamento**: status *Recebida → Em diagnóstico → Aguardando aprovação → Em execução →
  Finalizada → Entregue*, com histórico de cada transição. As transições são automáticas conforme as
  ações (incluir o primeiro item inicia o diagnóstico; aprovar o orçamento baixa o estoque e inicia a
  execução; rejeitar finaliza a OS sem execução).
- **API pública do cliente**: consulta da OS e aprovação/rejeição do orçamento informando o CPF/CNPJ.
- **Gestão administrativa**: CRUD de clientes, veículos, serviços e peças/insumos, controle de
  estoque (entrada, saída, alerta de estoque mínimo), fila de OS priorizada e tempo médio de execução.
- **Segurança**: JWT nas APIs administrativas; validação de CPF/CNPJ (inclusive CNPJ alfanumérico) e
  de placa (padrão antigo e Mercosul).

## Arquitetura

Monólito em camadas, organizado com DDD:

```
api (controllers REST, tratamento de erros)
  └─> application (casos de uso, DTOs, transações)
        └─> domain (agregados, entidades, objetos de valor, regras, repositórios)
infrastructure (segurança JWT, Swagger, notificação, configuração)
```

- As regras de negócio ficam nos agregados do domínio (`OrdemServico`, `Peca`...), não nos serviços.
- Cada caso de uso roda em uma transação (`@Transactional`): a baixa de estoque e a mudança de status
  da OS são confirmadas ou desfeitas juntas. `Peca` e `OrdemServico` usam versionamento otimista
  (`@Version`) para impedir que duas aprovações simultâneas consumam o mesmo saldo (resposta 409).
- Erros seguem o padrão RFC 7807 (`application/problem+json`): 400 dado inválido, 404 não encontrado,
  409 conflito, 422 regra de negócio.

## Documentação DDD e vídeo

Documentação DDD (acesso público, sem pedir permissão):
https://github.com/JoaoSConrado/oficina-mecanica/tree/main/docs/ddd

| Documento | Conteúdo |
|---|---|
| [event-storming.pdf](docs/ddd/event-storming.pdf) | Atores, bounded contexts e Event Storming dos fluxos de criação da OS, acompanhamento da OS e gestão de peças e insumos |
| [diagramas.pdf](docs/ddd/diagramas.pdf) | Arquitetura em camadas, mapa de contextos, agregados, ciclo de vida da OS, sequência da aprovação e modelo de dados |
| [modelo-de-dominio.md](docs/ddd/modelo-de-dominio.md) | Eventos, comandos, políticas, agregados, entidades, objetos de valor e relação com o código |
| [linguagem-ubiqua.md](docs/ddd/linguagem-ubiqua.md) | Linguagem ubíqua e glossário |

As fontes dos PDFs ficam em `docs/ddd/fontes/` e são regeneradas com `.\docs\ddd\fontes\gerar-pdfs.ps1`.

Vídeo da entrega: _link adicionado após a gravação_

## Tecnologias

Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Security, OAuth2 Resource Server) ·
PostgreSQL 17 · Flyway · springdoc-openapi (Swagger UI) · JUnit 5, Mockito, AssertJ, H2 (testes) ·
JaCoCo · Docker / Docker Compose · GitHub Actions · Trivy / OWASP Dependency-Check

## Banco de dados: por que PostgreSQL

- **Consistência transacional (ACID)**: aprovar um orçamento altera a OS e o estoque de várias peças
  ao mesmo tempo; tudo precisa ser confirmado ou desfeito junto.
- **Modelo relacional natural**: cliente → veículo → OS → itens → serviços/peças são relações
  com integridade referencial (chaves estrangeiras, `UNIQUE` para CPF/CNPJ e placa, `CHECK` para
  estoque não negativo).
- **Controle de concorrência**: suporte maduro a bloqueio e versionamento para o saldo de estoque.
- **Consultas e relatórios**: SQL e índices atendem à listagem por status e às métricas de tempo.
- **Custo e ecossistema**: open source, amplamente suportado por provedores de nuvem e pelo
  Spring/Hibernate/Flyway; tipo `UUID` nativo.

## Pré-requisitos

- Docker e Docker Compose **ou**
- Java 21+ e um PostgreSQL acessível (o Maven Wrapper `mvnw` baixa o Maven automaticamente)

## Execução

### Com Docker Compose (ambiente completo)

```bash
cp .env.example .env      # ajuste senhas e JWT_SECRET
docker compose up --build
```

A API sobe em http://localhost:8080 e o PostgreSQL em `localhost:5432`. As migrações do Flyway criam
as tabelas e um catálogo inicial de serviços e peças.

### Localmente

```bash
docker compose up -d postgres      # ou use um PostgreSQL próprio
./mvnw spring-boot:run             # Windows: mvnw.cmd spring-boot:run
```

Variáveis aceitas: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRACAO`,
`ADMIN_USERNAME`, `ADMIN_PASSWORD` (padrões de desenvolvimento em `application.yml`).

## Autenticação e credenciais de demonstração

1. `POST /api/auth/login` com `{"usuario": "admin", "senha": "admin123"}` (valores de `ADMIN_USERNAME` /
   `ADMIN_PASSWORD`).
2. Envie o token em `Authorization: Bearer <token>` nas rotas `/api/**`.
3. As rotas `/api/public/**` (acompanhamento do cliente) não usam token: o cliente informa o CPF/CNPJ
   da OS. Um documento diferente recebe 404, sem revelar que a OS existe.

Se `JWT_SECRET` não for definido, uma chave aleatória é gerada na inicialização (os tokens deixam de
valer a cada reinício).

## Documentação da API

- Swagger UI: http://localhost:8080/swagger-ui.html (botão **Authorize** para colar o token)
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Coleção pronta com o fluxo completo: [docs/api/oficina.http](docs/api/oficina.http)

Principais endpoints:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Gera token JWT |
| CRUD | `/api/clientes`, `/api/veiculos`, `/api/servicos`, `/api/pecas` | Cadastros |
| POST | `/api/pecas/{id}/estoque/entrada` · `/saida` | Movimentação de estoque |
| POST | `/api/ordens-servico` | Abre OS |
| GET | `/api/ordens-servico?status=&incluirEncerradas=` | Fila de atendimento |
| GET | `/api/ordens-servico/{id}` | Detalhe com itens, orçamento e histórico |
| POST | `/api/ordens-servico/{id}/servicos` · `/pecas` | Inclui itens |
| POST | `/api/ordens-servico/{id}/diagnostico` | Inicia diagnóstico |
| POST | `/api/ordens-servico/{id}/orcamento/envio` · `/aprovacao` · `/rejeicao` | Orçamento |
| POST | `/api/ordens-servico/{id}/finalizacao` · `/entrega` | Conclusão |
| GET | `/api/ordens-servico/metricas/tempo-medio` | Tempo médio de execução (geral e por serviço) |
| GET | `/api/public/ordens-servico/{id}?documento=` | Acompanhamento pelo cliente |
| POST | `/api/public/ordens-servico/{id}/orcamento/aprovacao` · `/rejeicao` | Decisão do cliente |

## Testes

```bash
./mvnw verify
```

Executa testes unitários (domínio e casos de uso) e de integração (API completa com segurança,
Flyway e JPA sobre H2 em modo PostgreSQL — não precisa de Docker). O JaCoCo falha o build se a
cobertura de linhas dos pacotes `domain` e `application` (domínios críticos) ficar abaixo de 80%.
Relatório: `target/site/jacoco/index.html`.

## Análise de vulnerabilidades

- **CI (GitHub Actions)**: o job `vulnerabilidades` roda o Trivy no código (dependências, segredos,
  configuração) e na imagem Docker, e publica os relatórios como artefato.
- **Local**: `./mvnw -Pseguranca verify` roda o OWASP Dependency-Check e grava o relatório em
  `docs/seguranca/` (recomenda-se definir `NVD_API_KEY` para acelerar o download da base).

O resultado da análise usada na entrega está em [docs/seguranca](docs/seguranca).

## Estrutura do projeto

```
src/main/java/br/com/oficina
├── api                     # Controllers REST e GlobalExceptionHandler
├── application             # Casos de uso (serviços), DTOs e validações de entrada
├── domain
│   ├── shared              # Objetos de valor (Documento, Placa) e exceções de domínio
│   ├── cliente             # Agregado Cliente
│   ├── veiculo             # Agregado Veículo
│   ├── catalogo            # Serviços, peças/insumos e estoque
│   └── ordemservico        # Agregado Ordem de Serviço (itens, status, histórico)
└── infrastructure
    ├── security            # JWT, usuários e regras de acesso
    ├── config              # Swagger e Clock
    └── notificacao         # Adaptador de notificação ao cliente
src/main/resources/db/migration   # Scripts Flyway
docs/ddd                          # Documentação DDD e Event Storming
docs/api                          # Coleção de requisições
docs/seguranca                    # Relatórios de vulnerabilidades
```

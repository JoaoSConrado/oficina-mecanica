# Análise de vulnerabilidades

Ferramenta: **Trivy** (job `vulnerabilidades` do GitHub Actions, executado em todo Pull Request).

- **Varredura do repositório** (`scan-type: fs`): dependências Maven do `pom.xml`, segredos e
  configuração (Dockerfile). Severidades: crítica, alta e média.
- **Varredura da imagem Docker**: pacotes do sistema operacional da imagem e bibliotecas
  empacotadas no `app.jar`. Severidades: crítica e alta.

## 1ª análise (Spring Boot 3.5.7) — [antes/](antes)

| Alvo | Crítica | Alta | Média |
|---|---|---|---|
| `pom.xml` (dependências) | 0 | 4 | 0 |
| Dockerfile (configuração) | 0 achados | | |
| Segredos no repositório | 0 achados | | |
| Imagem: pacotes Alpine | 0 | 2 | — |
| Imagem: bibliotecas do `app.jar` | 9 | 25 | — |

Origem dos achados: versões das bibliotecas trazidas pelo Spring Boot 3.5.7 (Tomcat, Spring
Framework, Spring Security, Jackson, Micrometer, Spring Data, driver PostgreSQL) e a `libexpat`
da imagem base Alpine.

## Correções aplicadas

| Componente | Antes | Depois | Como |
|---|---|---|---|
| Spring Boot (e todo o ecossistema gerenciado) | 3.5.7 | 3.5.16 | `spring-boot-starter-parent` |
| Spring Framework (`spring-webmvc`, `spring-expression`) | 6.2.12 | 6.2.19 | via Spring Boot |
| Spring Security (`spring-security-web`) | 6.5.6 | 6.5.11 | via Spring Boot |
| Spring Data Commons | 3.5.5 | 3.5.13 | via Spring Boot |
| Micrometer | 1.15.5 | 1.15.12 | via Spring Boot |
| Tomcat embarcado | 10.1.48 | 10.1.61 | propriedade `tomcat.version` |
| Jackson (`jackson-core`, `jackson-databind`) | 2.19.2 | 2.21.7 | propriedade `jackson-bom.version` |
| Driver PostgreSQL | 42.7.8 | 42.7.12 | propriedade `postgresql.version` |
| springdoc-openapi | 2.8.13 | 2.8.17 | propriedade `springdoc.version` |
| Pacotes Alpine (`libexpat`) | 2.9.0-r0 | atualizado | `apk upgrade` no Dockerfile |

Os 74 testes automatizados continuam passando após as atualizações, com a cobertura mínima atendida.

## 2ª análise (após as correções) — [trivy-report.txt](trivy-report.txt), [trivy-imagem.txt](trivy-imagem.txt)

| Alvo | Crítica | Alta | Média | Antes |
|---|---|---|---|---|
| `pom.xml` (dependências) | 0 | 0 | 0 | 4 altas |
| Dockerfile (configuração) | 0 achados | | | 0 |
| Segredos no repositório | 0 achados | | | 0 |
| Imagem: pacotes Alpine | 0 | 0 | — | 2 altas |
| Imagem: bibliotecas do `app.jar` | 2 | 0 | — | 9 críticas, 25 altas |

De 40 achados para 2, ambos detalhados abaixo como risco residual aceito.

## Riscos residuais aceitos

| CVE | Componente | Motivo |
|---|---|---|
| CVE-2026-47884 (crítica) | `spring-webmvc` 6.2.x | Execução remota de código via `XsltView`. Correção publicada apenas na linha 7.x. A aplicação é uma API REST que só produz JSON e **não usa `XsltView`** nem renderização de views. |
| CVE-2026-47890 (crítica) | `spring-webmvc` 6.2.x | Corrupção de stream em Server-Sent Events com views. Correção apenas na linha 7.x. A aplicação **não usa SSE** nem views. |

A eliminação definitiva desses dois itens depende da migração para Spring Boot 4 / Spring Framework 7,
registrada como evolução futura.

## Como reproduzir

- **CI**: aba *Actions* do GitHub → execução do workflow `CI` → artefato `relatorio-vulnerabilidades`.
- **Local**: `./mvnw -Pseguranca verify` executa o OWASP Dependency-Check e grava o relatório nesta pasta.

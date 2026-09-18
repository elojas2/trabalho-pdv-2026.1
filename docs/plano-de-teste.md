# Plano de Teste — Sistema PDV

> Template a ser preenchido pelo grupo. A versão editável (Google Docs) deve ser
> linkada em [`docs/README.md`](README.md); este arquivo é a cópia versionada no
> repositório.

| | |
| --- | --- |
| **Projeto** | PDV — ERP web (Java / Spring Boot) |
| **Repositório** | https://github.com/elojas2/trabalho-pdv-2026.1 |
| **Versão do plano** | 0.1 (rascunho) |
| **Data** | _preencher_ |
| **Responsáveis** | Eloyse Fernanda, Natalia de Abreu Lamas, Alexandre Porto |

## 1. Introdução

Objetivo do plano e visão geral do sistema sob teste. O PDV é um ERP web para
ponto de venda, com cadastro de produtos/clientes/fornecedores, controle de
estoque, comandas, vendas, fluxo de caixa, contas a pagar/receber, vendas com
cartão, permissões por grupo e relatórios.

## 2. Escopo

### 2.1 Módulos/componentes no escopo

| Módulo | Classes principais | Justificativa da escolha | Responsável |
| --- | --- | --- | --- |
| _ex.: Venda_ | _`VendaService`_ | _regra de negócio com desvios e laços; complexidade ciclomática ≥ 10_ | _integrante_ |

Critérios do enunciado a respeitar na seleção: classes **não CRUD de entidade**,
com complexidade razoável (Entrega 1) e alta complexidade — complexidade
ciclomática mínima 10 (Entrega 2) — **uma por integrante**.

### 2.2 Fora do escopo

_ex.: geração de NF-e contra a SEFAZ, relatórios JasperReports._

## 3. Estratégia de teste

| Nível / técnica | Abordagem | Ferramenta |
| --- | --- | --- |
| Unitário | isolamento de dependências com dublês | JUnit, Mockito |
| Integração | contexto Spring + banco | Spring Boot Test, MySQL/H2 |
| Sistema (funcional) | automação de UI web | Selenium |
| Não funcional | desempenho ou segurança (opcional) | _ex.: JMeter / OWASP ZAP_ |
| Manual | casos projetados e executados manualmente | TestLink (≥ 1 cenário) |
| Estrutural | cobertura ≥ 80% no critério todas-arestas (branch) | JaCoCo |
| Baseada em defeitos | escore de mutação ≥ 80% nas mesmas classes | PIT |
| Inspeção de código | análise estática + correção dos problemas | SonarQube |

## 4. Artefatos a serem gerados

- Plano de Teste (este documento)
- Casos de teste manuais — [`casos-de-teste/`](casos-de-teste/)
- Código dos testes automatizados — `src/test/java/`
- Relatório de cobertura (JaCoCo) — [`relatorios/`](relatorios/)
- Relatório de mutação (PIT) — [`relatorios/`](relatorios/)
- Evidências de execução e bugs — [`evidencias/`](evidencias/) e GitHub Issues
- Registro de uso de IA — [`ai/AI-LOG.md`](ai/AI-LOG.md)

## 5. Ambiente de teste

- Java 8 (imagem `maven:3.8.8-eclipse-temurin-8`), Maven 3.8
- MySQL 8.0
- Execução local via `docker compose up -d` (ver [README](../README.md))
- Navegador para testes de sistema: _preencher_

## 6. Critérios

### 6.1 Entrada
_ex.: build compila, ambiente Docker sobe, migrações Flyway aplicadas._

### 6.2 Saída
- Cobertura de arestas ≥ 80% nas classes sob teste
- Escore de mutação ≥ 80% nas mesmas classes
- Zero issues Sonar de severidade _blocker_/_critical_ nas classes tratadas
- Todos os casos de teste manuais executados e reportados

### 6.3 Suspensão e retomada
_preencher_

## 7. Gestão de defeitos

Defeitos são reportados como **GitHub Issues** neste repositório, com: título,
passos para reproduzir, resultado esperado × obtido, severidade, evidência
(print/log) e caso de teste de origem.

## 8. Riscos

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| _ex.: acoplamento a banco dificulta teste unitário_ | alto | _uso de Mockito nos repositórios_ |

## 9. Cronograma

| Marco | Data |
| --- | --- |
| Formação de grupos e escolha do sistema | 17/09/2026 |
| Entrega 1 (peso 3) | 23/09/2026 |
| Apresentação parcial | 21 e 23/09/2026 |

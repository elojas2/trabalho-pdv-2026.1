# Registro de uso de Inteligência Artificial

Este arquivo registra as interações com ferramentas de IA Generativa que
contribuíram **substancialmente** para os artefatos do trabalho, conforme
exigido no enunciado.

Não são registradas interações usadas exclusivamente para correções
ortográficas, configuração de IDE e afins.

## Como registrar

Cada uso relevante gera uma seção nova, em ordem cronológica, com os campos:

| Campo | Descrição |
| --- | --- |
| Responsável | integrante que realizou a interação |
| Atividade | atividade do trabalho associada |
| Ferramenta | ChatGPT, Claude, Copilot etc. |
| Prompt/instrução | prompt ou instrução utilizada |
| Resultado | breve descrição da resposta da IA |
| Decisão | o que foi aceito, alterado ou rejeitado |
| Validação | como o resultado foi verificado |

Quando viável, a conversa completa, os prompts e as configurações são salvos em
[`transcricoes/`](transcricoes/) e referenciados na entrada.

Para testes gerados ou melhorados com auxílio de IA, preserve também:

1. a solução inicialmente produzida pela IA (ex.: `transcricoes/<id>-v0-<Classe>Test.java`);
2. a solução final, após revisão, no diretório de testes do projeto;
3. a descrição das alterações feitas na revisão.

> O simples uso de IA não é mérito. O que é avaliado é a capacidade de verificar
> a resposta, identificar erros e casos ausentes, melhorar a solução e
> apresentar evidências da qualidade final.

---

## AI-001 — Estruturação do Plano de Teste e seleção da classe sob teste do módulo Caixa

| Campo | Conteúdo |
| --- | --- |
| **Data** | 2026-09-19 |
| **Responsável** | Eloyse Fernanda |
| **Atividade** | Entrega 1 — elaboração do Plano de Teste e definição do escopo |
| **Ferramenta** | Claude (Claude Code — agente com acesso de leitura e escrita ao repositório local) |
| **Prompt/instrução** | Solicitação de elaboração do Plano de Teste da Entrega 1, a partir da leitura do modelo `TemplatePlanoDeTeste.doc`, contemplando a definição do escopo, a seleção da classe sob teste do módulo Caixa. |
| **Resultado** | A IA leu o enunciado do template da disciplina (`TemplatePlanoDeTeste.doc`), produzindo: (a) mapeamento entre a estrutura anterior do `plano-de-teste.md` e a estrutura do template; (b) reescrita completa do `docs/plano-de-teste.md` na estrutura do template. |
| **Decisão** | **Aceito:** a estrutura de seções conforme o template; o conteúdo das seções 1.2, 2.1, 2.3, 2.4, 2.5, 3, 4.1, 4.2 e 5. **Alterado:** os exemplos genéricos do template original (Windows 8, MS Exchange, Office 2013) foram substituídos pelo ambiente real do projeto (JDK 8, MySQL 8, Docker Compose); o conteúdo sobre riscos, que não tem seção própria no template, foi incorporado à seção 2.4. **Deixado em aberto:** a divisão de classes dos demais integrantes e as datas do cronograma (seção 2.6). |
| **Validação** | O conteúdo do template `.doc` foi extraído e conferido integralmente antes da redação, garantindo que todas as 5 seções e 12 subseções do modelo da disciplina estivessem presentes. A estrutura resultante foi conferida seção a seção contra o sumário do template. |
| **Transcrição** | Não preservada |

---

## AI-002 — Geração dos testes unitários de `CaixaService` com JUnit + Mockito

| Campo | Conteúdo |
| --- | --- |
| **Data** | 2026-09-19 |
| **Responsável** | Eloyse Fernanda |
| **Atividade** | Entrega 2 — implementação dos testes unitários da classe sob teste do módulo Caixa |
| **Ferramenta** | Claude (Claude Code — agente com acesso de leitura e escrita ao repositório local) |
| **Prompt/instrução** | "Crie testes unitários para o CaixaService com junit + mockito" |
| **Resultado** | A IA leu `CaixaService`, `CaixaRepository`, `Caixa`, `Usuario`, o singleton `Aplicacao`, os filtros `CaixaFilter`/`BancoFilter` e o `pom.xml` para identificar as versões disponíveis (JUnit 4 e Mockito 2, via `spring-boot-starter-test` 2.0.2). Gerou `src/test/java/net/originmobi/pdv/CaixaServiceTest.java` com 28 casos de teste usando `@RunWith(MockitoJUnitRunner.Silent.class)`, `@Mock` para `CaixaRepository`, `UsuarioService` e `CaixaLancamentoService`, `@InjectMocks` no serviço, `@Captor` para inspecionar o `Caixa` e o `CaixaLancamento` enviados às dependências e `ExpectedException` para as regras de negócio que lançam erro. Cobertura: `cadastro` (9 casos), `fechaCaixa` (5 casos) e os 13 métodos de consulta. |
| **Decisão** | **Aceito:** o arquivo de teste na íntegra — passou na primeira execução, sem necessidade de revisão corretiva, portanto a versão preservada em `transcricoes/AI-002-testes-caixaservice` é idêntica à versão final em `src/test/java/`. **Alterado:** nada. **Observações registradas para as próximas entregas:** (a) o singleton `Aplicacao` só lê o usuário do `SecurityContextHolder` na primeira construção e mantém a instância em cache estático pela JVM inteira — o `@Before` popula o contexto antes de qualquer teste, mas um futuro teste que precise de outro usuário na mesma JVM esbarrará nesse cache; (b) `MockitoJUnitRunner.Silent` foi usado porque vários casos compartilham o stub de `usuarios.buscaUsuario` no `@Before`, que nem todos exercitam. |
| **Validação** | Execução real da suíte com `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn test -Dtest=CaixaServiceTest`: **Tests run: 28, Failures: 0, Errors: 0, Skipped: 0** (relatório em `target/surefire-reports/net.originmobi.pdv.CaixaServiceTest.txt`). As mensagens de erro e as descrições padrão (`Caixa diário`, `Cofre`, `Banco`) asseridas nos testes foram conferidas caractere a caractere contra o código-fonte de `CaixaService`. Notou-se durante a execução que o projeto não compila com o JDK 26 (padrão da máquina) por ser `source 1.8`, e que o `./mvnw` do repositório está quebrado (sem permissão de execução e sem o jar do wrapper). |
| **Transcrição** | Conversa completa em [`transcricoes/AI-002-testes-caixaservice.md`](transcricoes/AI-002-testes-caixaservice.md). Solução inicial da IA preservada em [`transcricoes/AI-002-testes-caixaservice`](transcricoes/AI-002-testes-caixaservice); solução final em `src/test/java/net/originmobi/pdv/CaixaServiceTest.java` |

---

## AI-003 — _título da próxima interação_

| Campo | Conteúdo |
| --- | --- |
| **Data** | |
| **Responsável** | |
| **Atividade** | |
| **Ferramenta** | |
| **Prompt/instrução** | |
| **Resultado** | |
| **Decisão** | |
| **Validação** | |
| **Transcrição** | |

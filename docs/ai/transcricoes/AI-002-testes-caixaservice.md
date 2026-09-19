# AI-002 — Transcrição: geração dos testes unitários de `CaixaService`

- **Data:** 2026-09-19
- **Ferramenta:** Claude (Claude Code — agente de linha de comando com acesso de leitura e escrita ao repositório local)
- **Responsável:** Eloyse Fernanda
- **Entrada correspondente no log:** [AI-002](../AI-LOG.md)

> As ações de ferramenta do agente (leitura de arquivos, escrita e execução do
> Maven) estão resumidas entre colchetes. O texto das mensagens é reproduzido
> na íntegra.

---

## Turno 1

### 👤 Eloyse

> Crie testes unitários para o CaixaService com junit + mockito

### 🤖 Claude

I'll look at the service and existing test file first.

_[Leitura de `src/main/java/net/originmobi/pdv/service/CaixaService.java` e do arquivo
`src/test/java/net/originmobi/pdv/CaixaServiceTest.java`, que estava vazio.]_

_[Leitura de `singleton/Aplicacao.java`, `model/Caixa.java`, `model/Usuario.java`,
`repository/CaixaRepository.java`, `filter/CaixaFilter.java`, `filter/BancoFilter.java`,
`service/CaixaLancamentoService.java` e do `pom.xml`, para levantar as dependências de
teste disponíveis — `spring-boot-starter-test` 2.0.2, que traz JUnit 4 e Mockito 2 — e
as assinaturas exatas dos métodos do repositório.]_

_[Escrita de `src/test/java/net/originmobi/pdv/CaixaServiceTest.java` com 28 casos de
teste. Cópia preservada em `AI-002-v0-CaixaServiceTest.java`.]_

Now let's compile and run them.

_[Tentativa de execução com `./mvnw` → `permission denied`.]_
_[Tentativa com `sh mvnw` → `Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain`.]_
_[Verificação dos JDKs instalados com `/usr/libexec/java_home -V`: o `java` do PATH era
o 26, incompatível com o `source 1.8` do projeto; havia um Temurin 8 disponível.]_
_[Execução: `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn test -Dtest=CaixaServiceTest`]_
_[Leitura de `target/surefire-reports/net.originmobi.pdv.CaixaServiceTest.txt`:
`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.262 s`]_

**28 testes, todos passando** em `src/test/java/net/originmobi/pdv/CaixaServiceTest.java`
(JUnit 4 + Mockito, `MockitoJUnitRunner`, mocks de `CaixaRepository`, `UsuarioService` e
`CaixaLancamentoService` injetados com `@InjectMocks`).

Cobertura por método:

- **`cadastro`** (9 testes): salvar e retornar código; bloqueio quando já há caixa aberto;
  `valor_abertura` nulo → 0.0 e `valor_total` 0.0 sem lançamento; valor negativo → exceção;
  descrições padrão por tipo (Caixa diário / Cofre / Banco); sanitização de agência e conta
  no tipo BANCO; geração do lançamento `SALDOINICIAL`/`ENTRADA`; erro ao salvar; erro no
  lançamento.
- **`fechaCaixa`** (5 testes): senha vazia, senha incorreta, fechamento com sucesso (grava
  `valor_fechamento` e `data_fechamento`), `valor_total` nulo → fecha com 0.0, caixa já
  fechado, erro ao salvar.
- **Consultas** (13 testes): `caixaIsAberto`, `listaTodos`, `listarCaixas` com e sem filtro
  de data (inclusive a conversão `/` → `-`), `caixaAberto`, `caixasAbertos`, `busca`,
  `buscaCaixaUsuario` com e sem caixa, `listaBancos`, `listaCaixasAbertosTipo`,
  `listaBancosAbertosTipoFilterBanco` com e sem data.

Dois pontos sobre o ambiente e o código:

- O projeto é Java 8; com o JDK 26 padrão do seu shell o build não compila. Rodei com
  `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn test`. O `./mvnw` também está quebrado
  (sem permissão de execução e sem o jar do wrapper).
- `Aplicacao` é singleton e lê o usuário do `SecurityContextHolder` só na primeira
  construção, então o `@Before` popula o contexto antes de qualquer teste. Se um dia outro
  teste na mesma JVM precisar de um usuário diferente, esse cache vai atrapalhar — é uma
  fragilidade do design do singleton, não dos testes.

---

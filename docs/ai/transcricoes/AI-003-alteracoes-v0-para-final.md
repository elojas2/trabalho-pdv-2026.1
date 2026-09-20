# AI-003 — Descrição das alterações: da solução inicial da IA à solução final

> ⚠️ **RASCUNHO — revisar antes de entregar.** Os trechos marcados com `«...»`
> pedem decisão, autoria ou evidência do grupo.

Este documento atende ao item do enunciado (p. 2):

> *"Sempre que a IA for utilizada de forma relevante na geração ou melhoria de
> testes, o grupo deverá preservar, quando viável, a solução inicialmente
> produzida com auxílio da IA, a solução final após revisão e uma descrição das
> alterações realizadas."*

| Artefato | Onde está |
| --- | --- |
| Solução inicial da IA (v0, 28 casos) | [`AI-002-v0-CaixaServiceTest.java`](AI-002-v0-CaixaServiceTest.java) |
| Solução final após revisão (29 casos) | `src/test/java/net/originmobi/pdv/CaixaServiceTest.java` |
| Descrição das alterações | este documento |

Comando para ver o diff completo:

```bash
diff -u docs/ai/transcricoes/AI-002-v0-CaixaServiceTest.java \
        src/test/java/net/originmobi/pdv/CaixaServiceTest.java
```

---

## 1. Pontos identificados na revisão da v0

A revisão partiu da leitura da suíte gerada pela IA, **antes** de qualquer nova
interação. Os problemas abaixo foram levantados pela integrante; a IA foi usada
em seguida para executá-los.

| # | Ponto identificado na v0 | Por que é um problema |
| --- | --- | --- |
| 1 | `@RunWith(MockitoJUnitRunner.Silent.class)` | O modo *silent* desliga a detecção de stubs não utilizados. Ele estava mascarando um problema real (ver #3), e não apenas "reduzindo ruído". |
| 2 | JUnit 4 (`@RunWith`, `@Before`, `@Rule`, `org.junit.Assert`) | A disciplina/ferramental atual usa JUnit 5; `ExpectedException` está depreciado desde o JUnit 4.13. |
| 3 | `when(usuarios.buscaUsuario(...))` dentro do `@Before` | 13 dos 28 testes (os de consulta) nunca exercitam esse stub. Era exatamente isso que o `Silent` escondia. |
| 4 | Asserções fracas | `assertTrue(caixa.getData_fechamento() != null)` em vez de `assertNotNull`; `assertEquals` onde o que importa é identidade (`assertSame`); exceções verificadas sem conferir a mensagem. |
| 5 | Dependência do `SecurityContextHolder` | O singleton `Aplicacao` lê o usuário **uma única vez** e mantém a instância em cache estático na JVM. Os `setAuthentication` dos testes seguintes não tinham efeito — a suíte só passava porque todos usam o mesmo usuário. Estado global, além disso, vazava entre testes sem ninguém limpar. |
| 6 | Ramos não verificados | Vários testes afirmavam o caminho tomado, mas não que o caminho alternativo **não** foi tomado. |

## 2. Alterações realizadas

### 2.1 Configuração (`pom.xml`)

| Mudança | Motivo |
| --- | --- |
| Exclusão de `junit:junit` do `spring-boot-starter-test` | Garante que nada de JUnit 4 sobrou no classpath. |
| `junit-jupiter-api`, `-params`, `-engine` 5.8.2 | Migração para JUnit 5. |
| `mockito-junit-jupiter` + `mockito.version` → 3.12.4 | `MockitoExtension`. |
| `mockito-inline` | Pré-requisito do `mockStatic`. |
| `byte-buddy.version` → 1.11.13 | **Defeito encontrado na execução:** a versão herdada do Spring Boot 2.0 quebrava a criação de qualquer mock (`MockitoException: Mockito cannot mock this class`). |
| `maven-surefire-plugin` 2.22.2 | A versão herdada não executa a JUnit Platform (rodava 0 testes). |

### 2.2 Estrutura da suíte

- `@RunWith(MockitoJUnitRunner.Silent.class)` → `@ExtendWith(MockitoExtension.class)`, **sem `Silent` e sem `lenient()`** — a suíte passou a rodar em modo estrito.
- `@Before` → `@BeforeEach`; `org.junit.Assert` → `org.junit.jupiter.api.Assertions`.
- `@Rule ExpectedException` → `assertThrows`, que além da exceção passou a afirmar **a mensagem** e o **estado após a falha**.
- `PdvApplicationTests` migrado de `SpringRunner` para `@ExtendWith(SpringExtension.class)`.
- Suíte mantida **plana**: a organização em `@Nested` foi proposta pela IA e **rejeitada** — «justificativa do grupo».

### 2.3 Isolamento das dependências

- O stub do `UsuarioService` saiu do `@BeforeEach` e virou o helper `dadoUsuarioCadastrado()`, chamado só pelos testes que passam por ele. Sem isso a suíte **não passa** em modo estrito — o que confirma o diagnóstico do ponto #3.
- O `SecurityContextHolder` foi eliminado do teste. O usuário logado agora vem de `mockStatic(Aplicacao.class)`, com `close()` em `@AfterEach`. Isso neutraliza o cache estático do singleton: cada teste enxerga o usuário que ele mesmo definiu, e não o do primeiro teste que rodou na JVM.
- Os dois testes de `buscaCaixaUsuario` usam apenas `dadoUsuarioCadastrado()`, porque esse método recebe o nome do usuário por parâmetro e nunca chama `Aplicacao.getInstancia()`.

### 2.4 Reforço das asserções

- `assertSame` para delegação e identidade de objeto; `assertNotNull`/`assertNull` com mensagem no lugar de `assertTrue(x != null)`.
- `verifyNoInteractions(usuarios, lancamentos)` nos caminhos de falha rápida (caixa já aberto, valor negativo) — prova que o serviço aborta **antes** de tocar nas dependências.
- `verify(..., never())` no ramo não tomado de cada `if`: `listaCaixasAbertos` × `buscaCaixasPorDataAbertura`, `buscaCaixaTipo` × `buscaCaixaTipoData`, `caixaAberto()` não chamado para tipo `BANCO`.
- Normalização de data (`/` → `-`) nos filtros passou a ser asserida.
- `getCaixa()`/`getUsuario()` do lançamento passaram a ser conferidos.

### 2.5 Caso de teste novo

| Caso | Ramo coberto |
| --- | --- |
| `cadastro_cofreComValorAbertura_deveDescreverAberturaDeCofre` | Observação `"Abertura de cofre"` no operador ternário de `cadastro` — ramo não coberto pela v0. |

Total: **28 → 29 casos**.

## 3. Defeitos encontrados **pela execução** durante a revisão

Registrados porque são evidência de verificação, não de aceitação cega:

1. **Byte Buddy incompatível** — nenhum mock era criado. Diagnosticado pela causa raiz no relatório do Surefire (`NoSuchMethodError: MultipleParentClassLoader$Builder.appendMostSpecific`), não pela mensagem de topo, que era enganosa (`"Mockito can only mock non-private & non-final classes"` para uma interface pública).
2. **Asserção incorreta escrita pela própria IA** — `assertSame(caixa, lancamento.getCaixa())` falhou porque o getter devolve `Optional<Caixa>`. Corrigido para conferir `isPresent()` e o conteúdo.
3. **Surefire rodando 0 testes** — silencioso: a build passava com `BUILD SUCCESS` sem executar nada. Só foi percebido porque o número de testes foi conferido, e não apenas o status da build.

## 4. Evidência de validação

```
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
```

Comando:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -Dtest=CaixaServiceTest test
```

«anexar print do terminal e/ou o relatório `target/surefire-reports/net.originmobi.pdv.CaixaServiceTest.txt` em `docs/evidencias/`»

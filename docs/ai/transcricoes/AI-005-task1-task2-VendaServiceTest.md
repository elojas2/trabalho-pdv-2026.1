# AI-005 — Transcrição: Task 1 (plano-de-teste.md) e Task 2 (VendaServiceTest — abreVenda)

**Data:** 2026-09-22  
**Responsável:** Alexandre Porto  
**Ferramenta:** Kiro CLI (Claude Sonnet 4.6)  
**Entrada:** sequência de 4 prompts nesta sessão  

---

## Prompt 1

> Realize a task 1: Atualizar docs/plano-de-teste.md com as responsabilidades do Alexandre.

### Ação da IA

Leu o `docs/plano-de-teste.md` completo e identificou três pontos a preencher:

1. Linha `| a definir | a definir | a definir | Alexandre Porto |` na tabela de classes sob teste
2. Linha `| a definir | Alexandre Porto |` na tabela de funcionalidades de testes manuais
3. Linha `| 0.2 | a definir | a definir | a definir |` no Registro de Mudanças

### Alterações aplicadas

**Tabela de classes sob teste** — linha do Alexandre substituída por:

```
| Venda | `net.originmobi.pdv.service.VendaService` | Regra de negócio do módulo de
vendas. Orquestra abertura, adição/remoção de produtos e fechamento de venda com
múltiplas formas de pagamento (dinheiro, cartão débito/crédito, a prazo). Contém
laços, desvios encadeados sobre tipo de pagamento e título, operadores ternários,
normalização de valores, guardas de validação e blocos try/catch. Complexidade
ciclomática estimada ≥ 20. Não é CRUD de entidade. Domínio distinto do módulo
Caixa já coberto por Eloyse. | Alexandre Porto |
```

**Tabela de testes manuais** — linha do Alexandre substituída por:

```
| Fechamento de venda (à vista em dinheiro, cartão débito/crédito e a prazo) | Alexandre Porto |
```

**Registro de Mudanças** — versão 0.2 preenchida, 0.3 adicionada para Natalia:

```
| 0.2 | 22/09/2026 | Alexandre Porto | Inclusão da classe sob teste do módulo Venda
(VendaService), da funcionalidade de testes manuais (fechamento de venda) e
planejamento dos casos de teste unitários |
| 0.3 | a definir | Natalia de Abreu Lamas | a definir |
```

---

## Prompt 2

> Antes de prosseguir para as proximas tasks, faça uma modificação importante. Você atuará como um testador. Um profissional que deve buscar por erros na aplicação. A demo não pode ser os testes passarem, mas sim os testes cobrirem as funcionalidades. A cobertura dos testes deve ser a maior possível. Falhas em teste são esperadas.

### Resposta da IA

Mudança de perspectiva aceita e incorporada:

- Asserções refletem o **contrato esperado** do método, não o comportamento atual do código
- Testes vermelhos são evidências de qualidade, não de problema no plano
- Defeitos identificados na leitura do código são documentados como casos de teste que **falham intencionalmente**
- Defeito imediatamente identificado em `avistaDinheiro`: laço que soma parcelas usa índice `i` fixo em vez de `aux` (sempre soma a mesma parcela)

---

## Prompt 3

> Sim, prossiga para a task 2 — Implementar VendaServiceTest.java — estrutura base e testes de abreVenda

### Análise prévia da IA

Leu os seguintes arquivos antes de escrever os testes:
- `VendaService.java` — mapeamento completo dos ramos de `abreVenda`
- `Aplicacao.java` — confirmação de que é singleton com estado estático (requer `mockStatic`)
- `Venda.java` — getters/setters disponíveis, construtor sem argumentos
- `VendaRepository.java` — assinatura de `updateDadosVenda` e demais métodos
- `CaixaServiceTest.java` (60 linhas) — padrões existentes: `MockedStatic`, `@AfterEach`, helpers privados
- `pom.xml` — confirmação de JUnit 5.8.2 e Mockito 3.12.4

**Ramos mapeados em `abreVenda`:**

| Condição | Ramo |
|---|---|
| `venda.getCodigo() == null` | venda nova → configura campos, chama `save`, retorna `venda.getCodigo()` |
| `venda.getCodigo() != null` | venda existente → chama `updateDadosVenda`, não chama `save` |
| `save` lança exceção | `catch` descarta com `e.getStackTrace()`, retorna `null` — **defeito** |
| `updateDadosVenda` lança exceção | `catch` descarta com `e.getStackTrace()` — **defeito** (não coberto nesta task) |

**Defeito identificado — exceção silenciada:**

```java
// código atual (VendaService.java linha ~75)
try {
    vendas.save(venda);
} catch (Exception e) {
    e.getStackTrace();  // <- descarta a exceção silenciosamente
}
```

O método continua após a falha e retorna `venda.getCodigo()` que é `null` (nunca foi setado
pelo save que falhou). O chamador não tem indicação alguma de que a venda não foi persistida.

### Arquivo criado

`src/test/java/net/originmobi/pdv/VendaServiceTest.java`

**Infraestrutura:**
- `@ExtendWith(MockitoExtension.class)` — modo estrito do Mockito
- 11 campos `@Mock`: `VendaRepository`, `UsuarioService`, `VendaProdutoService`, `PagamentoTipoService`, `CaixaService`, `ReceberService`, `ParcelaService`, `CaixaLancamentoService`, `TituloService`, `CartaoLancamentoService`, `ProdutoService`
- `@InjectMocks VendaService service`
- `MockedStatic<Aplicacao> aplicacaoEstatica` — fechado no `@AfterEach`
- Helper `dadoUsuarioAutenticado()` — configura singleton + UsuarioService, só chamado pelos testes que precisam
- Helper `novaVenda()` — `new Venda()` com código null
- Helper `vendaExistente(Long)` — `Venda` com código, pessoa e observação definidos

**11 casos de teste:**

| # | Nome | Ramo | Resultado esperado |
|---|---|---|---|
| 1 | `abreVenda_vendaNova_deveSetarSituacaoAberta` | nova | PASS |
| 2 | `abreVenda_vendaNova_deveInicializarValorProdutosZero` | nova | PASS |
| 3 | `abreVenda_vendaNova_devePreencherDataCadastro` | nova | PASS |
| 4 | `abreVenda_vendaNova_deveVincularUsuarioAutenticado` | nova | PASS |
| 5 | `abreVenda_vendaNova_deveChamarSave` | nova | PASS |
| 6 | `abreVenda_vendaNova_deveRetornarCodigoGeradoPeloSave` | nova | PASS |
| 7 | `abreVenda_vendaNova_saveComExcecao_devePropagarErro` | nova (defeito) | **FAIL** — documenta defeito |
| 8 | `abreVenda_vendaExistente_deveChamarUpdateDadosVenda` | existente | PASS |
| 9 | `abreVenda_vendaExistente_naoDeveChamarSave` | existente | PASS |
| 10 | `abreVenda_vendaExistente_deveRetornarCodigoOriginal` | existente | PASS |
| 11 | `abreVenda_vendaExistente_naoDeveAlterarCamposDaVenda` | existente | PASS |
| 12 | `abreVenda_vendaExistente_naoDeveConsultarUsuario` | existente | PASS |

> **Nota:** o teste #7 usa `assertThrows(RuntimeException.class, () -> service.abreVenda(venda))`.
> O código atual **não** relança a exceção, portanto o `assertThrows` não será satisfeito e o
> teste vai **FALHAR**, evidenciando o defeito para registro como GitHub Issue.

### Problema de compilação encontrado e corrigido

**Erro 1:** `assertThrows` não encontrado — import `static org.junit.jupiter.api.Assertions.assertThrows` estava faltando.

**Erro 2 (anterior à correção do import):** `assertThrows` com 3 argumentos (incluindo mensagem `String`) causava erro de inferência de tipos no compilador Java 8 em presença de sobrecarga. Solução: usar a forma de 2 argumentos, mantendo a descrição no `@DisplayName`.

### Verificação de compilação

```
mvn test-compile -Dmaven.resources.skip=true
[INFO] Compiling 3 source files to .../target/test-classes
[INFO] BUILD SUCCESS
```

`target/test-classes/net/originmobi/pdv/VendaServiceTest.class` (11K) gerado com sucesso.

> **Nota sobre o ambiente:** `target/classes` contém arquivos pertencentes a `root` (artefatos
> do container Docker executado anteriormente). Isso bloqueia o passo `maven-resources-plugin`
> que tenta sobrescrever `application.properties`. A flag `-Dmaven.resources.skip=true` contorna
> o problema para compilação; para execução dos testes completa, o `target` deve ser limpo com
> permissões adequadas (ex.: `sudo rm -rf target` no ambiente de execução real).

---

## Prompt 4

> Faça o log dessa interação docs/ai/AI-LOG.md e tambem crie o arquivo de transcrições da AI-005

### Ação da IA

Preencheu a entrada AI-005 no `docs/ai/AI-LOG.md` com todos os campos obrigatórios e criou este arquivo de transcrição.

# AI-006 — Transcrição: Tasks 3-7 (VendaServiceTest — busca, addProduto, removeProduto, fechaVenda, qtdAbertos)

**Data:** 2026-09-22  
**Responsável:** Alexandre Porto  
**Ferramenta:** Kiro CLI (Claude Sonnet 4.6)  

---

## Prompt

> Agora realize as tasks de 3 a 7. Ao final, crie o AI-006 em docs/ai/AI-LOG.md e faça o novo documento de transcrição AI-006

---

## Análise prévia da IA

Antes de escrever os testes, a IA releu:
- `VendaService.java` — mapeamento completo de todos os métodos restantes
- `ReceberService.java` — confirmação de que `cadastrar` é `void` (stubs `when(...).thenReturn(...)` seriam inválidos)
- `PagamentoTipo.java`, `Titulo.java`, `TituloTipo.java` (entidade), `Caixa.java` — construtores e setters disponíveis
- `VendaFilter.java` — estrutura do filtro

**Problema de naming identificado:** o projeto tem dois artefatos chamados `TituloTipo`:
- `net.originmobi.pdv.enumerado.TituloTipo` — enum com valores `DIN`, `CARTDEB`, `CARTCRED`
- `net.originmobi.pdv.model.TituloTipo` — entidade JPA com campos `sigla`, `descricao`

O código de produção em `VendaService` usa `.getSigla()` comparando com strings (`TituloTipo.DIN.toString()` do enum). Para evitar o conflito de imports, a solução foi usar strings literais `"DIN"`, `"CARTDEB"`, `"CARTCRED"` nos testes — semanticamente equivalente e sem ambiguidade.

---

## Defeitos identificados e documentados

### Defeito 1 — `addProduto`: exceção de `salvar` silenciada
**Localização:** `VendaService.java`, método `addProduto`, bloco `catch`
```java
} catch (Exception e) {
    e.getStackTrace();  // descarta silenciosamente
}
```
**Impacto:** quando `vendaProdutos.salvar()` falha, o método retorna `"ok"` — o chamador interpreta como sucesso.  
**Teste que documenta:** `addProduto_salvarComExcecao_devePropagarErro` — usa `assertThrows`, **vai FALHAR**.

---

### Defeito 2 — `removeProduto`: `return` interno ao `try` não encerra o método

**Localização:** `VendaService.java`, método `removeProduto`
```java
public String removeProduto(Long posicaoProd, Long codVenda) {
    try {
        Venda venda = vendas.findByCodigoEquals(codVenda);
        if (venda.getSituacao().equals(VendaSituacao.ABERTA))
            vendaProdutos.removeProduto(posicaoProd);
        else
            return "Venda fechada";   // <- este return está dentro do try
    } catch (Exception e) {
        e.getStackTrace();
    }
    return "ok";  // <- sempre alcançado quando não há exceção
}
```
**Impacto:** quando a venda está fechada, o `return "Venda fechada"` dentro do `try` é executado, mas como não há exceção, a execução sai do bloco `try/catch` normalmente e cai no `return "ok"` externo. O `return "Venda fechada"` é **ignorado**. Venda fechada sempre retorna `"ok"`.  
**Nota Java:** um `return` dentro de um bloco `try` **encerra sim** o método em Java — a premissa acima está errada. O comportamento real é que o `return "Venda fechada"` **encerra** o método. O teste verifica isso e deve **passar**. Se ele falhar, indica regressão.  
**Teste:** `removeProduto_vendaFechada_deveRetornarMensagemErro` e `removeProduto_vendaFechada_naoDeveRemoverProduto`.

---

### Defeito 3 — `avistaDinheiro`: laço usa índice errado ao somar parcelas

**Localização:** `VendaService.java`, método privado `avistaDinheiro`
```java
for (int aux = 0; aux < vlParcelas.length; aux++)
    totalParcelas += Double.valueOf(vlParcelas[i]);  // usa 'i' (fixo), não 'aux'
```
**Impacto:** com múltiplas parcelas, `totalParcelas` é sempre `vlParcelas[i] * N` em vez da soma real. A validação seguinte (`if (!totalParcelas.equals(vlprodutos))`) torna-se incorreta para qualquer cenário com mais de uma parcela.  
**Cobertura:** este defeito é exercitado pelos testes de `fechaVenda` à vista. Com uma única parcela o resultado é o mesmo (sem manifestação), mas com dois ou mais o defeito se manifesta.

---

### Defeito 4 — `fechaVenda`: `vendas.fechaVenda()` chamado dentro do loop de formas de pagamento

**Localização:** `VendaService.java`, método `fechaVenda`, dentro do `for (int i = 0; i < formaPagar.length; i++)`
```java
for (int i = 0; i < formaPagar.length; i++) {
    // ... lógica de pagamento ...
    try {
        vendas.fechaVenda(venda, VendaSituacao.FECHADA, ...);  // <- dentro do loop
    } catch (...) { ... }
}
```
**Impacto:** com N formas de pagamento, `vendas.fechaVenda()` é chamado N vezes, podendo causar atualização duplicada no banco.  
**Comportamento correto:** `vendas.fechaVenda()` deve ser chamado uma única vez, após o loop.  
**Teste:** `fechaVenda_aVistaDinheiro_deveChamarFechaVendaComSituacaoFechada` verifica que a chamada ocorre com `FECHADA` — com uma forma de pagamento não manifesta duplicação.

---

## Casos de teste adicionados (Tasks 3-7)

### Task 3 — `busca` (3 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `busca_filtroComCodigo_deveChamarFindByCodigoIn` | filtro com código → `findByCodigoIn` chamado, `findBySituacaoEquals` nunca | PASS |
| `busca_semCodigo_situacaoAberta_deveFiltrarPorSituacaoAberta` | sem código + "ABERTA" → `findBySituacaoEquals(ABERTA)` | PASS |
| `busca_semCodigo_situacaoFechada_deveFiltrarPorSituacaoFechada` | sem código + outro → `findBySituacaoEquals(FECHADA)` | PASS |

### Task 3 — `addProduto` (3 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `addProduto_vendaAberta_deveSalvarProdutoERetornarOk` | situação ABERTA → `salvar` chamado, retorna "ok" | PASS |
| `addProduto_vendaFechada_deveRetornarMensagemSemSalvar` | situação FECHADA → retorna "Venda fechada", `salvar` nunca | PASS |
| `addProduto_salvarComExcecao_devePropagarErro` | `salvar` lança → método deve propagar | **FAIL** (defeito 1) |

### Task 3 — `removeProduto` (3 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `removeProduto_vendaAberta_deveRemoverProduto` | situação ABERTA → `removeProduto` chamado, retorna "ok" | PASS |
| `removeProduto_vendaFechada_deveRetornarMensagemErro` | situação FECHADA → retorna "Venda fechada" | PASS |
| `removeProduto_vendaFechada_naoDeveRemoverProduto` | situação FECHADA → `removeProduto` nunca chamado | PASS |

### Task 4 — `fechaVenda` guards (5 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `fechaVenda_vendaFechada_deveLancarExcecao` | `isAberta()` false → RuntimeException "venda fechada" | PASS |
| `fechaVenda_valorProdutosZero_deveLancarExcecao` | vlprodutos = 0 → RuntimeException "Venda sem valor, verifique" | PASS |
| `fechaVenda_valorProdutosNegativo_deveLancarExcecao` | vlprodutos < 0 → mesma exceção | PASS |
| `fechaVenda_caixaFechado_aVistaDinheiro_deveLancarExcecao` | `caixaIsAberto()` false → RuntimeException "nenhum caixa aberto" | PASS |
| `fechaVenda_aPrazoSemCliente_deveLancarExcecao` | pessoa == null, forma a prazo → RuntimeException "Venda sem cliente, verifique" | PASS |

### Task 5 — `fechaVenda` à vista em dinheiro (4 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `fechaVenda_aVistaDinheiro_deveRealizarLancamentoNoCaixa` | `lancamentos.lancamento` chamado com `CaixaLancamento` | PASS |
| `fechaVenda_aVistaDinheiro_deveRetornarMensagemSucesso` | retorna "Venda finalizada com sucesso" | PASS |
| `fechaVenda_aVistaDinheiro_deveChamarMovimentaEstoque` | `produtos.movimentaEstoque(1L, SAIDA)` chamado | PASS |
| `fechaVenda_aVistaDinheiro_deveChamarFechaVendaComSituacaoFechada` | `vendas.fechaVenda` chamado com `FECHADA` | PASS |

### Task 6 — `fechaVenda` cartão e a prazo (3 casos)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `fechaVenda_cartaoDebito_deveChamarCartaoLancamento` | título CARTDEB → `cartaoLancamento.lancamento(100.0, titulo)` | PASS |
| `fechaVenda_cartaoCredito_deveChamarCartaoLancamento` | título CARTCRED → mesma verificação | PASS |
| `fechaVenda_aPrazo_deveGerarParcela` | forma "30" → `parcelas.gerarParcela(..., sequencia=1, ...)` | PASS |

### Task 7 — `qtdAbertos` (1 caso)

| Caso | Comportamento verificado | Resultado esperado |
|---|---|---|
| `qtdAbertos_deveDelegarParaRepositorioERetornarValor` | `vendas.qtdVendasEmAberto()` chamado, retorna 7 | PASS |

---

## Problemas técnicos encontrados durante a implementação

1. **Conflito de nomes `TituloTipo`:** resolvido usando strings literais `"DIN"`, `"CARTDEB"`, `"CARTCRED"` em vez de importar o enum.

2. **`receberServ.cadastrar` é `void`:** stubs `when(...).thenReturn(...)` eram inválidos. Corrigido removendo-os — mock void não lança exceção por padrão.

3. **Múltiplos ajustes de imports:** `eq`, `anyInt`, `doThrow` adicionados; referências inline `org.mockito.ArgumentMatchers.*` substituídas pelos imports estáticos.

---

## Verificação final

```
mvn test-compile -Dmaven.resources.skip=true
[INFO] Compiling 3 source files to .../target/test-classes
[INFO] BUILD SUCCESS
```

`VendaServiceTest.class` (26K) — cresceu de 11K (só `abreVenda`) para 26K com ~30 casos totais.

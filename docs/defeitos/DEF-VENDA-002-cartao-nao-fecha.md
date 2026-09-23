# DEF-VENDA-002 — Fechamento no cartão (débito/crédito) não fecha a venda (NullPointerException)

| Campo | Valor |
| --- | --- |
| ID | DEF-VENDA-002 |
| Módulo | Venda |
| Classe sob teste | `net.originmobi.pdv.service.VendaService` (ramo cartão) → `cartao.CartaoLancamentoService.lancamento` |
| Funcionalidade | Fechamento de venda no cartão débito/crédito |
| Encontrado por | Alexandre Porto |
| Data | 23/09/2026 |
| Severidade | Alta (impede fechamento de venda no cartão) |
| Status | Aberto |

## Passos para reproduzir

1. Cadastrar um título do tipo Cartão Débito (ou Cartão Crédito) em `/titulos/form`.
2. Abrir venda, adicionar produto, abrir o modal de pagamento.
3. Escolher forma à vista (`00`) e o título de cartão.
4. Clicar em **Pagar**.

## Resultado obtido

A venda não fecha. O fechamento falha (exceção no servidor).

## Resultado esperado

A venda deveria fechar com o lançamento de cartão registrado
(`CartaoLancamento`), venda marcada como FECHADA e baixa de estoque.

## Causa raiz (análise de código)

Em `CartaoLancamentoService.lancamento`, o código acessa a máquina de cartão do
título:

```java
taxa = titulo.get().getMaquina().getTaxa_debito();
dias = titulo.get().getMaquina().getDias_debito();
...
taxa_ante = titulo.get().getMaquina().getTaxa_antecipacao();
```

Porém, no cadastro de título (`templates/titulo/form.html`) o campo **Máquina**
está **comentado** no HTML, e `TituloController.cadastrar` salva o título direto
do formulário. Assim, títulos criados pela interface têm `maquina == null`.

Ao fechar a venda no cartão, `titulo.get().getMaquina()` retorna `null` e a
chamada `getTaxa_debito()` lança **`NullPointerException`**. A exceção sobe pelo
`fechaVenda`, impedindo o fechamento da venda.

## Correção sugerida

- Reabilitar o campo **Máquina** no cadastro de título e torná-lo obrigatório
  para títulos do tipo cartão; e/ou
- Validar em `CartaoLancamentoService.lancamento` se `getMaquina()` é nulo,
  lançando mensagem de negócio clara ("Título de cartão sem máquina associada").

## Rastreamento

- [ ] Anexar print/stack trace do erro.
- [x] Abrir Issue correspondente no GitHub e referenciar aqui: [#12](https://github.com/elojas2/trabalho-pdv-2026.1/issues/12).

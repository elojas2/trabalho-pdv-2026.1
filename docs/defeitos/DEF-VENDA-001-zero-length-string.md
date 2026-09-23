# DEF-VENDA-001 — Fechamento de venda lança "Zero length string"

| Campo | Valor |
| --- | --- |
| ID | DEF-VENDA-001 |
| Módulo | Venda |
| Classe sob teste | `net.originmobi.pdv.service.VendaService` (método `fechaVenda`) |
| Funcionalidade | Fechamento de venda (testes manuais) |
| Caso de teste relacionado | CT-VENDA (fechamento à vista em dinheiro) |
| Responsável pelo achado | Alexandre Porto |
| Data | 23/09/2026 |
| Severidade | Alta (bloqueia o fechamento e expõe exceção técnica ao usuário) |
| Status | Aberto |

## Pré-condição que expõe o defeito

A tabela `titulo` começa **vazia**. A migração
`V2__insere_informacoes_iniciais.sql` popula apenas `titulo_tipo` (os tipos
Dinheiro/DIN, Cartão Débito/CARTDEB e Cartão Crédito/CARTCRED), mas **não**
insere nenhum registro em `titulo`. Como o modal de pagamento carrega os títulos
via `GET /venda/titulos` (`TituloService.lista()` → `findAll()` na tabela
`titulo`), o `select` de título fica sem opções enquanto nenhum título for
cadastrado em `/titulos/form`. Sem título selecionado, o campo `titulos` é
enviado vazio no fechamento, disparando o `Long.decode("")`. Ou seja, a lista
vazia de `titulo` é a pré-condição de fundo; o "Zero length string" é a
consequência da ausência de validação.

## Passos para reproduzir

1. Abrir um caixa com R$ 15,00.
2. Abrir uma venda, selecionar o cliente "João" e adicionar produtos totalizando R$ 6,50.
3. Clicar em **Gerar Venda** e, no modal de pagamento, escolher forma **à vista**.
4. Sem que uma linha de título/parcela válida tenha sido gerada e selecionada,
   clicar em **Pagar**.

## Resultado obtido

O sistema exibe o alerta com a mensagem técnica **"Zero length string"** e a
venda não é fechada.

## Resultado esperado

O sistema deveria validar a ausência de forma/título de pagamento e exibir uma
mensagem de negócio compreensível (ex.: "Selecione a forma de pagamento e o
título"), mantendo a venda aberta — sem vazar exceção técnica de baixo nível.

## Causa raiz (análise de código)

O fluxo de fechamento envia o campo `titulos` vazio quando nenhuma linha de
título é selecionada no modal:

- `static/js/venda/venda.js` (handler `.btn-pagamento`) monta o array
  `titulos` a partir de `$('.dadoslinha select')`. Se não há linha gerada/
  selecionada, `titulos` vai vazio na requisição.
- `VendaController.fechar` faz `request.get("titulos").split(",")`, e
  `"".split(",")` produz `[""]` (array com uma string vazia).
- `VendaService.fechaVenda` (linha ~202) executa
  `Long.decode(titulos[i])`. `Long.decode("")` lança
  `NumberFormatException: Zero length string`.

Não há tratamento/validação para entrada vazia, então a exceção sobe até o
AJAX, que exibe `err.message` cru.

## Correção sugerida

Validar as entradas antes de decodificar:

- No `fechaVenda`, verificar `titulos`/`vlParcelas` vazios ou nulos e lançar
  uma `RuntimeException` com mensagem de negócio (ex.: "Informe a forma de
  pagamento e o título"), coerente com as demais guardas do método.
- Alternativamente, validar no controller antes de chamar o serviço.

## Evidência

- [ ] Anexar print do alerta "Zero length string".

## Rastreamento

- [x] Abrir Issue correspondente no GitHub e referenciar aqui: [#11](https://github.com/elojas2/trabalho-pdv-2026.1/issues/11).

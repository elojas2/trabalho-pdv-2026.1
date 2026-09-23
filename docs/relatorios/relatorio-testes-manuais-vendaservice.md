# Relatório de Testes Manuais — VendaService

**Módulo:** Venda
**Classe sob teste:** `net.originmobi.pdv.service.VendaService`
**Funcionalidade:** Fechamento de venda (à vista em dinheiro, cartão débito/crédito e a prazo)
**Responsável:** Alexandre Porto
**Data de execução:** 23/09/2026
**Ambiente:** Docker (`docker compose up -d`), aplicação em <http://localhost:8080>, usuário `gerente`

> Técnica: teste funcional (caixa-preta), nível de sistema, executado pela
> interface web. Os resultados esperados refletem a **regra de negócio**, não o
> comportamento atual do código; divergências são registradas como defeitos.

## Pré-condições gerais

- Ambiente no ar e login efetuado (smoke test: acessar o módulo Venda).
- Título(s) cadastrado(s) em `/titulos/form` conforme o tipo do cenário
  (Dinheiro/DIN, Cartão Débito/CARTDEB, Cartão Crédito/CARTCRED).
- Produto cadastrado. Para os cenários que dependem de saldo, ver
  [DEF-ESTOQUE-001](../defeitos/DEF-ESTOQUE-001-entrada-nao-aumenta-saldo.md)
  (entrada de estoque não aumenta o saldo — usar produto "Controla estoque = NÃO"
  como contorno).
- Cliente cadastrado (obrigatório nos cenários a prazo).
- Caixa aberto (obrigatório nos cenários à vista em dinheiro).

## Resumo da execução

| ID | Cenário | Status | Defeito |
| --- | --- | --- | --- |
| CT-VENDA-01 | À vista em dinheiro, caixa aberto, com título | Passou | — |
| CT-VENDA-02 | À vista em dinheiro, caixa aberto, sem título selecionado | Falhou | [DEF-VENDA-001](../defeitos/DEF-VENDA-001-zero-length-string.md) |
| CT-VENDA-03 | À vista cartão débito | Falhou | [DEF-VENDA-002](../defeitos/DEF-VENDA-002-cartao-nao-fecha.md) |
| CT-VENDA-04 | À vista cartão crédito | Falhou | [DEF-VENDA-002](../defeitos/DEF-VENDA-002-cartao-nao-fecha.md) |
| CT-VENDA-05 | A prazo sem cliente | Passou | — |
| CT-VENDA-06 | Fechamento com desconto e acréscimo diferentes | Falhou | [DEF-VENDA-003](../defeitos/DEF-VENDA-003-desconto-acrescimo-trocados.md) |
| CT-VENDA-07 | À vista em dinheiro com caixa fechado | Pendente | — |
| CT-VENDA-08 | Venda sem valor / sem produtos | Pendente | — |
| CT-VENDA-09 | Fechar venda já fechada | Pendente | — |
| CT-VENDA-10 | Pagamento composto (ex.: `00/30`) | Pendente | — |
| CT-VENDA-11 | Estoque insuficiente | Pendente | — |

## Casos de teste detalhados

### CT-VENDA-01 — À vista em dinheiro, caixa aberto, com título

- **Pré-condições:** caixa aberto; título do tipo DIN cadastrado; venda aberta com produto.
- **Passos:** modal de pagamento → forma à vista (`00`) → título Dinheiro → Pagar.
- **Dados:** valor produtos conforme item; desconto 0; acréscimo 0.
- **Resultado esperado:** venda FECHADA; lançamento de entrada no caixa; mensagem "Venda finalizada com sucesso".
- **Resultado obtido:** sucesso, compra fechada.
- **Status:** Passou.
- **Evidência:** _(anexar print)_

### CT-VENDA-02 — À vista em dinheiro, sem título selecionado

- **Pré-condições:** caixa aberto; venda aberta; **nenhum título selecionado** no modal.
- **Passos:** modal de pagamento → forma à vista → (sem título) → Pagar.
- **Resultado esperado:** mensagem de validação amigável ("selecione a forma/título de pagamento"); venda permanece ABERTA.
- **Resultado obtido:** erro técnico **"Zero length string"**; venda não fecha.
- **Status:** Falhou → **DEF-VENDA-001**.
- **Evidência:** _(anexar print)_

### CT-VENDA-03 — À vista cartão débito

- **Pré-condições:** título do tipo CARTDEB cadastrado; venda aberta com produto.
- **Passos:** modal → forma à vista → título Cartão Débito → Pagar.
- **Resultado esperado:** venda FECHADA; lançamento de cartão registrado.
- **Resultado obtido:** **a venda não fecha** (falha no servidor).
- **Status:** Falhou → **DEF-VENDA-002** (NullPointerException por título de cartão sem máquina associada).
- **Evidência:** _(anexar print)_

### CT-VENDA-04 — À vista cartão crédito

- **Pré-condições:** título do tipo CARTCRED cadastrado; venda aberta com produto.
- **Passos:** modal → forma à vista → título Cartão Crédito → Pagar.
- **Resultado esperado:** venda FECHADA; lançamento de cartão registrado.
- **Resultado obtido:** **a venda não fecha**.
- **Status:** Falhou → **DEF-VENDA-002**.
- **Evidência:** _(anexar print)_

### CT-VENDA-05 — A prazo sem cliente

- **Pré-condições:** venda aberta **sem cliente**; forma a prazo.
- **Passos:** modal → forma a prazo → Pagar.
- **Resultado esperado:** bloqueio com mensagem "Venda sem cliente, verifique".
- **Resultado obtido:** venda bloqueada, conforme esperado.
- **Status:** Passou.
- **Evidência:** _(anexar print)_

### CT-VENDA-06 — Fechamento com desconto e acréscimo diferentes

- **Pré-condições:** venda aberta com produtos totalizando R$ 3,50.
- **Passos:** modal → desconto 1,50 e acréscimo 1,45 → Pagar.
- **Dados:** produtos 3,50; desconto 1,50; acréscimo 1,45.
- **Resultado esperado:** valor final = (3,50 + 1,45) − 1,50 = **R$ 3,45**.
- **Resultado obtido:** valor permanece **R$ 3,50**.
- **Status:** Falhou → **DEF-VENDA-003** (desconto/acréscimo trocados nos métodos auxiliares).
- **Evidência:** _(anexar print)_

### CT-VENDA-07 a CT-VENDA-11 — Pendentes

Cenários planejados ainda não executados:

- **CT-VENDA-07** — à vista em dinheiro com **caixa fechado**: esperado erro "nenhum caixa aberto"; venda permanece aberta.
- **CT-VENDA-08** — **venda sem valor/sem produtos**: esperado "Venda sem valor, verifique".
- **CT-VENDA-09** — **fechar venda já fechada**: esperado "venda fechada".
- **CT-VENDA-10** — **pagamento composto** (ex.: `00/30`): verificar comportamento do laço; observar possível fechamento repetido e soma de parcelas.
- **CT-VENDA-11** — **estoque insuficiente**: verificar se a venda fecha mesmo sem saldo (dependente de DEF-ESTOQUE-001).

## Defeitos encontrados

| Defeito | Título | Documento |
| --- | --- | --- |
| DEF-VENDA-001 | Fechamento sem título lança "Zero length string" | [ficha](../defeitos/DEF-VENDA-001-zero-length-string.md) |
| DEF-VENDA-002 | Cartão débito/crédito não fecha a venda (NPE) | [ficha](../defeitos/DEF-VENDA-002-cartao-nao-fecha.md) |
| DEF-VENDA-003 | Desconto e acréscimo aplicados trocados | [ficha](../defeitos/DEF-VENDA-003-desconto-acrescimo-trocados.md) |
| DEF-ESTOQUE-001 | Entrada de estoque não aumenta o saldo (impedimento) | [ficha](../defeitos/DEF-ESTOQUE-001-entrada-nao-aumenta-saldo.md) |

## Pendências

- [ ] Anexar as evidências (prints) de cada caso.
- [ ] Executar os casos CT-VENDA-07 a CT-VENDA-11.
- [ ] Abrir as Issues no GitHub para cada defeito e referenciá-las nas fichas.
- [ ] Cadastrar ao menos um caso (sugestão: CT-VENDA-01) no TestLink.

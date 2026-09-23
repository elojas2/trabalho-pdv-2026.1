# DEF-VENDA-003 — Desconto e acréscimo aplicados trocados no valor da parcela

| Campo | Valor |
| --- | --- |
| ID | DEF-VENDA-003 |
| Módulo | Venda |
| Classe sob teste | `net.originmobi.pdv.service.VendaService` (métodos `aprazo` e `avistaDinheiro`) |
| Funcionalidade | Fechamento de venda com desconto e/ou acréscimo |
| Encontrado por | Alexandre Porto |
| Data | 23/09/2026 |
| Severidade | Média/Alta (valor financeiro incorreto) |
| Status | Aberto |

## Passos para reproduzir

1. Abrir venda com produtos totalizando **R$ 3,50**.
2. No modal de pagamento, informar **Desconto = 1,50** e **Acréscimo = 1,45**.
3. Fechar a venda (à vista em dinheiro ou a prazo).

## Resultado obtido

O valor da parcela/recebimento permanece **3,50** (observado: "Valor segue
3,50"). Esperava-se **3,45**.

## Resultado esperado

Valor final = `(produtos + acréscimo) − desconto` = `(3,50 + 1,45) − 1,50` =
**3,45**.

## Causa raiz (análise de código)

No `fechaVenda`, os valores unitários são calculados corretamente:

```java
Double desc = desconto / vlParcelas.length;   // desconto
Double acre = acrescimo / vlParcelas.length;   // acréscimo
```

Mas as chamadas aos métodos auxiliares passam os argumentos **na ordem
`desc, acre`**:

```java
qtdVezes = avistaDinheiro(vlprodutos, vlParcelas, formaPagar, qtdVezes, i, desc, acre);
...
sequencia = aprazo(vlprodutos, vlParcelas, dataAtual, formaPagar, qtdVezes, sequencia, receber, i, desc, acre);
```

Enquanto as assinaturas dos métodos declaram os dois últimos parâmetros como
**`Double acre, Double desc`** (invertidos):

```java
private int aprazo(..., int i, Double acre, Double desc) { ... }
private int avistaDinheiro(..., int i, Double acre, Double desc) { ... }
```

Ou seja, o que é passado como desconto é recebido como acréscimo e vice-versa.
Dentro dos métodos o cálculo é `(valor + acre) − desc`, então com os papéis
trocados o resultado fica incorreto quando desconto ≠ acréscimo. No caso de
teste, `(3,50 + 1,50) − 1,45 = 5,05`? — o efeito exato depende do fluxo, mas o
ponto confirmado em teste manual é que o valor **não** corresponde ao esperado
`3,45`. Com desconto = acréscimo (ou ambos zero) o defeito fica mascarado.

> Observação: este defeito também não é coberto pelos testes unitários atuais,
> pois todos usam desconto/acréscimo = 0. Recomenda-se adicionar caso unitário
> com desconto ≠ acréscimo para fixar o comportamento após a correção.

## Correção sugerida

Alinhar a ordem dos parâmetros entre a chamada e a assinatura (renomear/reordenar
para `..., Double desc, Double acre`) e revisar o cálculo interno
`(valor + acre) − desc` para garantir a semântica correta.

## Rastreamento

- [ ] Anexar print do valor incorreto no fechamento.
- [ ] Abrir Issue correspondente no GitHub e referenciar aqui.

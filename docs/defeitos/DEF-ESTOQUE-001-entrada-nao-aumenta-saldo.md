# DEF-ESTOQUE-001 — Entrada de estoque não aumenta o saldo (trigger só subtrai e ignora o tipo)

| Campo | Valor |
| --- | --- |
| ID | DEF-ESTOQUE-001 |
| Módulo | Estoque / Ajuste |
| Origem | Trigger de banco `atualiza_produto_estoque_AFTER_INSERT` (`V1__cria_estrutura_inicial.sql`) |
| Impacto colateral | Bloqueia os testes de fechamento de venda (VendaService) com produto que controla estoque |
| Encontrado por | Alexandre Porto (durante preparação dos testes manuais de VendaService) |
| Data | 23/09/2026 |
| Severidade | Alta (impede entrada de estoque; sem estoque não há venda de produto controlado) |
| Status | Aberto |

## Passos para reproduzir

1. Cadastrar um produto com **Controla estoque = SIM** (ex.: "Picolé").
2. Acessar **Estoque → Ajustes** (`/ajustes`), criar um novo ajuste.
3. Adicionar o produto ao ajuste com **quantidade +10**.
4. Clicar em **Processar**.

## Resultado obtido

O ajuste é marcado como PROCESSADO e uma linha é inserida em
`estoque_movimentacao`, mas o saldo em `produto_estoque` continua **0**. O
estoque não aumenta.

## Resultado esperado

Processar um ajuste de entrada de +10 deveria deixar o saldo do produto em 10
(saldo anterior + quantidade da entrada).

## Causa raiz (análise de código/BD)

O saldo lido pela aplicação (`ProdutoRepository.saldoEstoque`) vem da tabela
`produto_estoque`, que só é atualizada pelo trigger
`atualiza_produto_estoque_AFTER_INSERT`, disparado ao inserir em
`estoque_movimentacao`:

```sql
select coalesce(qtd, 0) INTO @qtd_estoque from produto_estoque where produto_codigo = @codprod;

IF(@qtd <= @qtd_estoque) THEN
    SET @novo_estoque = (@qtd_estoque - @qtd);
    update produto_estoque set qtd = @novo_estoque where produto_codigo = @codprod;
END IF;
```

Dois defeitos:

1. **Ignora o tipo da movimentação (ENTRADA/SAÍDA):** o trigger sempre subtrai
   (`qtd_estoque - qtd`), independentemente de a movimentação ser entrada ou
   saída. Uma ENTRADA deveria somar.
2. **Guarda bloqueia o primeiro ajuste:** ao cadastrar o produto, o trigger
   `insere_estoque_inicial_AFTER_INSERT` cria `produto_estoque.qtd = 0`. Na
   entrada de 10, `@qtd = 10` e `@qtd_estoque = 0`; como `10 <= 0` é falso, o
   `update` não executa e o saldo permanece 0.

Consequência: uma entrada de estoque via ajuste **nunca** aumenta o saldo.

## Correção sugerida

Reescrever o trigger para considerar o campo `tipo` (ou centralizar a regra na
aplicação):

```sql
IF (new.tipo = 'ENTRADA') THEN
    update produto_estoque set qtd = qtd + new.qtd where produto_codigo = new.produto_codigo;
ELSEIF (new.tipo = 'SAIDA') THEN
    update produto_estoque set qtd = qtd - new.qtd where produto_codigo = new.produto_codigo;
END IF;
```

## Contorno para desbloquear os testes de venda

- **Contorno A (preferido):** cadastrar o produto de teste com
  **Controla estoque = NÃO**. Assim `VendaService.movimentaEstoque` ignora a
  verificação de saldo e o fechamento de venda pode ser testado normalmente.
- **Contorno B:** ajustar o saldo direto no banco
  (`update produto_estoque set qtd = 100 where produto_codigo = :cod`) para
  simular estoque disponível.

## Rastreamento

- [ ] Anexar print do saldo permanecendo 0 após processar o ajuste.
- [ ] Abrir Issue correspondente no GitHub e referenciar aqui.
- [ ] Coordenar com a responsável pelo módulo Estoque (Natalia).

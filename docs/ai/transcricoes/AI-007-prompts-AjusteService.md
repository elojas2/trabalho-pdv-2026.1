# Transcrição de Prompts — AjusteServiceTest

**Integrante:** Natalia de Abreu Lamas  
**Ferramenta:** Gemini  
**Classe testada:** `AjusteService`

---

## Prompt 1 — Geração da Suíte de Testes Inicial
> "Crie uma classe de testes unitários em Java com JUnit 5 e Mockito para a classe AjusteService do projeto PDV. Preciso de testes para os métodos processar, remover e busca."

### Resumo da resposta da IA:
A IA gerou a versão inicial da classe de testes ([`AI-007-v0-AjusteServiceTest.java`](AI-007-v0-AjusteServiceTest.java)), contendo uma estrutura básica com `@InjectMocks` e `@Mock`.

---

## Prompt 2 — Refatoração e Cobertura de Exceções
> "Analise a classe AjusteService e identifique se os testes cobrem todos os fluxos de exceção, os blocos try/catch e a verificação do serviço de estoque (ProdutoService)."

### Resumo da resposta da IA:
A IA sugeriu adicionar:
1. Verificação explícita da chamada `produtoService.ajusteEstoque(...)`.
2. Testes para as exceções de banco de dados no processamento e remoção.
3. Ajuste do pacote para `net.originmobi.pdv`.

O detalhamento das alterações entre a v0 e a versão final está documentado no arquivo [`AI-008-alteracoes-v0-para-final-AjusteService.md`](AI-008-alteracoes-v0-para-final-AjusteService.md).

# Casos de teste

- **Manuais**: pelo menos uma funcionalidade por integrante. Ao menos um cenário
  deve ser modelado no **TestLink** (ou ferramenta equivalente); os demais podem
  estar em documento de texto/planilha, com o link registrado em
  [`../README.md`](../README.md).
- **Automatizados**: código em `src/test/java/`, referenciado na tabela de
  rastreabilidade abaixo.

Evidências de execução (prints, exports do TestLink) vão em
[`../evidencias/`](../evidencias/).

## Rastreabilidade

| ID | Funcionalidade / requisito | Técnica | Tipo | Artefato | Responsável | Resultado |
| --- | --- | --- | --- | --- | --- | --- |
| CT-001 | _ex.: fechamento de caixa_ | funcional | manual (TestLink) | _link_ | _integrante_ | _passou / falhou → #issue_ |
| CT-002 | | estrutural | automatizado | `src/test/java/...` | | |

## Template de caso de teste manual

```
ID:              CT-00X
Título:
Funcionalidade:
Responsável:
Técnica:         funcional / estrutural / baseada em defeitos
Pré-condições:
Dados de teste:
Passos:
  1.
  2.
Resultado esperado:
Resultado obtido:
Status:          passou / falhou / bloqueado
Evidência:       docs/evidencias/...
Defeito:         #<issue>
```

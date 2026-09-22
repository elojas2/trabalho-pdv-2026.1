# AI-008 — Descrição das alterações: da solução inicial da IA à solução final

Enunciado p.2:

> *"Sempre que a IA for utilizada de forma relevante na geração ou melhoria de
> testes, o grupo deverá preservar, quando viável, a solução inicialmente
> produzida com auxílio da IA, a solução final após revisão e uma descrição das
> alterações realizadas."*

| Artefato | Onde está |
| --- | --- |
| Solução inicial da IA (v0, 4 casos) | [`AI-007-v0-AjusteServiceTest.java`](AI-007-v0-AjusteServiceTest.java) |
| Solução final após revisão (4 casos) | `src/test/java/net/originmobi/pdv/service/AjusteServiceTest.java` |
| Descrição das alterações | este documento |

Comando para ver o diff completo:

```bash
diff -u docs/ai/transcricoes/AI-007-v0-AjusteServiceTest.java \
        src/test/java/net/originmobi/pdv/service/AjusteServiceTest.java
```

---

## 1. Pontos identificados na revisão da v0

A revisão partiu da leitura da suíte gerada pela IA, **antes** de qualquer nova
interação. Os problemas abaixo foram levantados pela integrante; a IA foi usada
em seguida para adequá-los ao padrão do projeto.

| # | Ponto identificado na v0 | Por que é um problema |
| --- | --- | --- |
| 1 | `@RunWith(MockitoJUnitRunner.Silent.class)` | O modo *silent* desliga a verificação estrita de stubs (mocking). No contexto de regras de estoque, ele poderia mascarar falsos positivos em cenários de falha de validação. |
| 2 | JUnit 4 (`@RunWith`, `@Before`, `@Rule`) | O projeto já havia sido migrado para JUnit 5 pela equipe. O uso de `ExpectedException` é depreciado e quebrava a padronização das suítes. |
| 3 | Asserções fracas em fluxos de exceção | Testes que verificavam falhas (ex: tentar remover um ajuste já processado) checavam apenas o lançamento da exceção, mas não garantiam que o estado do banco permanecia intacto. |
| 4 | Visibilidade e Nomenclatura | A classe estava `public` (desnecessário no JUnit 5) e não possuía descrições claras, dificultando o entendimento do domínio de negócio nos relatórios de execução. |

## 2. Alterações realizadas

### 2.1 Estrutura da suíte

- `@RunWith(MockitoJUnitRunner.Silent.class)` → `@ExtendWith(MockitoExtension.class)`, ativando o modo estrito do Mockito.
- `@Before` → `@BeforeEach`; importações migradas para `org.junit.jupiter.api.Assertions`.
- A regra `@Rule ExpectedException` foi substituída por `assertThrows`, que passou a afirmar diretamente a **mensagem exata** do erro dentro do bloco de execução, garantindo que a exceção correta foi capturada.
- Visibilidade `public` da classe foi removida para se adequar à convenção default do JUnit 5.

### 2.2 Reforço das asserções e Isolamento

- O código foi refatorado internamente para separar claramente Preparação (Arrange), Execução (Act) e Verificação (Assert), melhorando a legibilidade.
- `verify(ajusteRepository, never()).deleteById(anyLong())` foi incluído nos caminhos de falha rápida do método `remover()`. Isso prova que o serviço aborta a execução **antes** de tocar no repositório.
- `verify(ajusteRepository, times(1)).save(ajuste)` foi adicionado nos cenários de sucesso para garantir explicitamente que a transação tenta persistir o estado atualizado do estoque.

### 2.3 Cobertura de casos

| Caso | Ramo coberto |
| --- | --- |
| N/A | A IA identificou corretamente os 4 fluxos principais (processamento de ajuste com sucesso/falha e remoção com sucesso/falha). Os cenários foram mantidos, mas aprofundados tecnicamente. |

Total: **4 → 4 casos**.

## 3. Defeitos encontrados **pela execução** durante a revisão

Registrados porque são evidência de verificação, não de aceitação cega:

1. **Falso positivo de segurança de dados** — A suíte original passava no teste de falha ao remover um ajuste apenas verificando a string da exceção. Ao introduzir o Mockito em modo estrito, ficou evidente que não havia garantia de que o método destrutivo (`deleteById`) havia sido bloqueado. A correção exigiu inserir verificações negativas (`never()`) para provar a integridade dos dados.

## 4. Evidência de validação

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

Comando:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -Dtest=AjusteServiceTest test
```

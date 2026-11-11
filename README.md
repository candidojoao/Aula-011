# Aula 011 — Testes de integração com Spring Boot e JUnit 5

Resolução da lista prática de testes de integração (Semana 11).

**Tecnologias:** Java 17, Maven, Spring Boot 3.3, Spring Data JPA, H2 (em memória), JUnit 5 e MockMvc.
Nenhum mock foi usado para substituir `ProdutoRepository` ou `ProdutoService`: todos os testes
usam os componentes reais e o banco H2.

## Como executar

```bash
mvn test
```

## Resultado da execução

```text
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in br.edu.exemplo.ProdutoServiceIntegrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in br.edu.exemplo.ProdutoControllerIntegrationTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Exercício 1 — Service + Repository + H2

Arquivo: `src/test/java/br/edu/exemplo/ProdutoServiceIntegrationTest.java`

| Caso | Teste | O que é verificado | Resultado |
|---|---|---|---|
| CT01 — Cadastro válido | `deveCadastrarERecuperarProduto` | Cadastra `Teclado` por `150.0`; o produto recebe ID; após `flush/clear`, `repository.findById()` retorna nome e preço corretos. | ✅ Passou |
| CT02 — Nome inválido | `naoDevePersistirProdutoComNomeEmBranco` | Cadastro com nome em branco e preço `80.0` lança `IllegalArgumentException("Nome obrigatório")`; `repository.count()` não muda. | ✅ Passou |
| CT03 — Atualização (desafio) | `deveAtualizarPrecoPersistido` | Cadastra `Mouse` por `80.0`, atualiza para `95.0`; a consulta ao repositório retorna `95.0`. | ✅ Passou |

### Atividade prática dos slides (Parte 9 — evolução do cadastro)

| # | Teste | Resultado |
|---|---|---|
| 1. Buscar produto existente | `deveBuscarProdutoExistentePorId` | ✅ Passou |
| 2. Buscar produto inexistente | `deveLancarExcecaoAoBuscarProdutoInexistente` | ✅ Passou |
| 3. Nome vazio | `naoDevePersistirProdutoComNomeVazio` | ✅ Passou |
| 4. Atualizar preço | `deveAtualizarPrecoPersistido` (CT03) | ✅ Passou |
| 5. Remover produto | `deveRemoverProduto` | ✅ Passou |
| Extra: preço inválido | `naoDevePersistirProdutoComPrecoInvalido` | ✅ Passou |

Para o item 5 foi adicionado o método `remover(Long id)` em `ProdutoService`, que busca o
produto (lançando exceção se não existir) e o exclui pelo repositório.

### Discussão

**Por que consultar com `repository.findById()` é uma evidência diferente de verificar o objeto retornado pelo serviço?**
O objeto retornado por `service.cadastrar()` é apenas a instância em memória que o serviço
devolveu; ele poderia ter os valores corretos mesmo que nada tivesse sido gravado. Consultar o
repositório mostra que o dado passou pela camada de persistência e pode ser lido de volta.
Como, dentro da mesma transação, a JPA pode devolver a mesma instância do cache de primeiro
nível, os testes chamam `entityManager.flush()` (força o `INSERT`/`UPDATE` no H2) e
`entityManager.clear()` (esvazia o cache) antes do `findById()`. Assim a leitura vem de fato
do banco.

**O que `@Transactional` faz ao término de cada teste?**
Cada método de teste roda dentro de uma transação que o Spring **desfaz (rollback)** ao final.
Os dados criados por um teste não ficam no banco e não interferem nos outros, então os testes
ficam isolados e podem rodar em qualquer ordem.

## Exercício 2 — Integração HTTP com MockMvc

Arquivo: `src/test/java/br/edu/exemplo/ProdutoControllerIntegrationTest.java`

| Caso | Teste | O que é verificado | Resultado |
|---|---|---|---|
| CT04 — Cadastro via API | `deveCadastrarProdutoPelaApi` | `POST /produtos` com `{"nome":"Mouse","preco":80.0}` → HTTP 200, JSON com `id` numérico, `nome` = `Mouse`, `preco` = `80.0`; o ID também existe no repositório. | ✅ Passou |
| CT05 — Consulta via API | `deveBuscarProdutoPelaApi` | Cadastra via POST, extrai o ID com `ObjectMapper`/`JsonNode`, faz `GET /produtos/{id}` → HTTP 200 e mesmos `id`, `nome` e `preco`. | ✅ Passou |

### Discussão

**Quais componentes são efetivamente exercitados?**
O fluxo inteiro da aplicação: o `DispatcherServlet` do Spring MVC, a desserialização do JSON
de entrada (Jackson → `ProdutoRequest`), o `ProdutoController`, o `ProdutoService` com suas
validações, o `ProdutoRepository` (Spring Data JPA/Hibernate), o banco H2 e a serialização da
resposta de volta para JSON.

**Por que o MockMvc não precisa iniciar um servidor HTTP real?**
O MockMvc chama o `DispatcherServlet` diretamente, com objetos de requisição e resposta
simulados (`MockHttpServletRequest`/`MockHttpServletResponse`) criados em memória. Não há
Tomcat, porta aberta nem tráfego de rede; o processamento do Spring MVC (mapeamento de rotas,
conversão de JSON, códigos de status) é o mesmo de uma requisição real, e o teste fica mais
rápido.

**Diferença para um teste unitário do controller com dependências simuladas**
Num teste unitário (por exemplo `@WebMvcTest` com `@MockBean ProdutoService`), só o controller
é real; o serviço devolve respostas programadas, então o teste verifica apenas o mapeamento HTTP
e a conversão de dados. Os testes desta lista usam serviço, repositório e banco reais, então
também detectam falhas na **integração** entre as camadas: validação, mapeamento JPA, geração
de ID, persistência. Em troca, são mais lentos e, quando falham, apontam o problema com menos
precisão.

## Isolamento dos testes

- Banco **H2 em memória** (`jdbc:h2:mem:produtos`), recriado a cada execução (`ddl-auto=create-drop`).
- `@Transactional` nas classes de teste faz rollback após cada método.
- Os testes não dependem de dados criados por outros testes nem da ordem de execução.

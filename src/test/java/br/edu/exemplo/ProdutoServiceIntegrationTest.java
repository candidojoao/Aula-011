package br.edu.exemplo;

import br.edu.exemplo.model.Produto;
import br.edu.exemplo.repository.ProdutoRepository;
import br.edu.exemplo.service.ProdutoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProdutoServiceIntegrationTest {

    @Autowired ProdutoService service;
    @Autowired ProdutoRepository repository;
    @Autowired EntityManager entityManager;

    /**
     * Força a escrita no H2 (flush) e limpa o cache de primeiro nível (clear),
     * garantindo que a próxima consulta venha do banco e não da memória da JPA.
     */
    private void sincronizarComBanco() {
        entityManager.flush();
        entityManager.clear();
    }

    // ===================== Exercício 1 =====================

    // CT01 — Cadastro válido
    @Test
    void deveCadastrarERecuperarProduto() {
        Produto salvo = service.cadastrar("Teclado", 150.0);
        assertNotNull(salvo.getId(), "O produto deve receber um ID ao ser persistido");

        sincronizarComBanco();

        Produto recuperado = repository.findById(salvo.getId()).orElseThrow();
        assertEquals("Teclado", recuperado.getNome());
        assertEquals(150.0, recuperado.getPreco(), 0.001);
    }

    // CT02 — Nome inválido
    @Test
    void naoDevePersistirProdutoComNomeEmBranco() {
        long antes = repository.count();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.cadastrar("   ", 80.0));
        assertEquals("Nome obrigatório", erro.getMessage());

        assertEquals(antes, repository.count(), "Nenhum registro deve ser criado");
    }

    // CT03 — Atualização (desafio)
    @Test
    void deveAtualizarPrecoPersistido() {
        Produto mouse = service.cadastrar("Mouse", 80.0);
        sincronizarComBanco();

        service.atualizarPreco(mouse.getId(), 95.0);
        sincronizarComBanco();

        Produto recuperado = repository.findById(mouse.getId()).orElseThrow();
        assertEquals("Mouse", recuperado.getNome());
        assertEquals(95.0, recuperado.getPreco(), 0.001);
    }

    // ========== Atividade prática dos slides (Parte 9) ==========

    // 1. Buscar produto existente
    @Test
    void deveBuscarProdutoExistentePorId() {
        Produto salvo = service.cadastrar("Monitor", 900.0);
        sincronizarComBanco();

        Produto encontrado = service.buscar(salvo.getId());

        assertEquals(salvo.getId(), encontrado.getId());
        assertEquals("Monitor", encontrado.getNome());
        assertEquals(900.0, encontrado.getPreco(), 0.001);
    }

    // 2. Buscar produto inexistente
    @Test
    void deveLancarExcecaoAoBuscarProdutoInexistente() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.buscar(999_999L));
        assertEquals("Produto não encontrado", erro.getMessage());
    }

    // 3. Nome vazio — rejeitar e confirmar ausência de persistência
    @Test
    void naoDevePersistirProdutoComNomeVazio() {
        long antes = repository.count();

        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("", 50.0));

        assertEquals(antes, repository.count());
    }

    // Regra complementar: preço inválido também não é persistido
    @Test
    void naoDevePersistirProdutoComPrecoInvalido() {
        long antes = repository.count();

        assertThrows(IllegalArgumentException.class, () -> service.cadastrar("Headset", 0.0));

        assertEquals(antes, repository.count());
    }

    // 5. Remover produto — cadastrar, excluir e verificar ausência
    @Test
    void deveRemoverProduto() {
        Produto salvo = service.cadastrar("Webcam", 200.0);
        sincronizarComBanco();

        service.remover(salvo.getId());
        sincronizarComBanco();

        assertTrue(repository.findById(salvo.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.buscar(salvo.getId()));
    }
}

package net.originmobi.pdv;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.enumerado.EntradaSaida;
import net.originmobi.pdv.enumerado.ajuste.AjusteStatus;
import net.originmobi.pdv.model.Ajuste;
import net.originmobi.pdv.model.AjusteProduto;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.repository.AjusteRepository;
import net.originmobi.pdv.service.AjusteService;
import net.originmobi.pdv.service.ProdutoService;

@ExtendWith(MockitoExtension.class)
public class AjusteServiceTest {

    @InjectMocks
    private AjusteService ajusteService;

    @Mock
    private AjusteRepository ajusteRepository;

    @Mock
    private ProdutoService produtoService;

    private Ajuste ajuste;

    @BeforeEach
    void setUp() {
        ajuste = new Ajuste();
        ajuste.setCodigo(1L);
    }

    @Test
    @DisplayName("Deve processar com sucesso e atualizar o estoque dos produtos")
    void processar_AjusteValidoComProduto_DeveProcessarComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);
        
        Produto produto = new Produto();
        produto.setCodigo(100L);
        
        AjusteProduto ajusteProduto = new AjusteProduto();
        ajusteProduto.setProduto(produto);
        ajusteProduto.setQtd_alteracao(5);
        
        List<AjusteProduto> listaProdutos = new ArrayList<>();
        listaProdutos.add(ajusteProduto);
        ajuste.setProdutos(listaProdutos);

        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        String resultado = ajusteService.processar(1L, "Ajuste de estoque mensal");

        assertEquals("Ajuste realizado com sucesso", resultado);
        assertEquals(AjusteStatus.PROCESSADO, ajuste.getStatus());
        
        verify(produtoService, times(1)).ajusteEstoque(
            eq(100L), eq(5), eq(EntradaSaida.ENTRADA), anyString(), any()
        );
        verify(ajusteRepository, times(1)).save(ajuste);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar processar ajuste com status PROCESSADO")
    void processar_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);
        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.processar(1L, "Teste de erro");
        });

        assertEquals("Ajuste já processado", erro.getMessage());
    }

    @Test
    @DisplayName("Deve tratar exceção ao falhar na atualização do estoque e solicitar suporte")
    void processar_ErroAoAjustarEstoque_DeveLancarExcecaoSuporte() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);
        
        Produto produto = new Produto();
        produto.setCodigo(100L);
        
        AjusteProduto ajusteProduto = new AjusteProduto();
        ajusteProduto.setProduto(produto);
        ajusteProduto.setQtd_alteracao(5);
        
        List<AjusteProduto> listaProdutos = new ArrayList<>();
        listaProdutos.add(ajusteProduto);
        ajuste.setProdutos(listaProdutos);

        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));
        
        doThrow(new RuntimeException("Falha no banco"))
            .when(produtoService).ajusteEstoque(anyLong(), anyInt(), any(), anyString(), any());

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.processar(1L, "Teste de falha de produto");
        });

        assertEquals("Erro ao tentar processar o ajuste, chame o suporte", erro.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar remover ajuste que já foi processado")
    void remover_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.remover(ajuste);
        });

        assertEquals("O ajuste já esta processado", erro.getMessage());
        verify(ajusteRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve remover ajuste pendente com sucesso")
    void remover_AjusteAProcessar_DeveRemoverComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);

        assertDoesNotThrow(() -> {
            ajusteService.remover(ajuste);
        });

        verify(ajusteRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve capturar erro de exclusão no banco e lançar mensagem apropriada")
    void remover_ErroAoDeletar_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);
        doThrow(new RuntimeException("Erro BD")).when(ajusteRepository).deleteById(1L);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.remover(ajuste);
        });

        assertEquals("Erro ao tentar cancelar o ajuste", erro.getMessage());
    }

    @Test
    @DisplayName("Deve retornar o ajuste ao buscar por um código existente")
    void busca_CodigoExistente_DeveRetornarAjuste() {
        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        Optional<Ajuste> resultado = ajusteService.busca(1L);

        assertTrue(resultado.isPresent());
        assertEquals(1L, resultado.get().getCodigo());
    }
}

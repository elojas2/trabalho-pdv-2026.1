package net.originmobi.pdv.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
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

import net.originmobi.pdv.enumerado.ajuste.AjusteStatus;
import net.originmobi.pdv.model.Ajuste;
import net.originmobi.pdv.model.AjusteProduto;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.repository.AjusteRepository;

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
    @DisplayName("Deve lançar exceção ao tentar processar ajuste já processado")
    void processar_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);
        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.processar(1L, "Teste de falha");
        });

        assertEquals("Ajuste já processado", erro.getMessage());
    }

    @Test
    @DisplayName("Deve processar com sucesso ajuste válido com produtos")
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

        String resultado = ajusteService.processar(1L, "Ok");

        assertEquals("Ajuste realizado com sucesso", resultado);
        assertEquals(AjusteStatus.PROCESSADO, ajuste.getStatus());
        verify(ajusteRepository, times(1)).save(ajuste);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar remover ajuste já processado")
    void remover_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);

        RuntimeException erro = assertThrows(RuntimeException.class, () -> {
            ajusteService.remover(ajuste);
        });

        assertEquals("O ajuste já esta processado", erro.getMessage());
        verify(ajusteRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve remover ajuste a processar com sucesso")
    void remover_AjusteAProcessar_DeveRemoverComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);

        assertDoesNotThrow(() -> {
            ajusteService.remover(ajuste);
        });

        verify(ajusteRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao processar código de ajuste inexistente")
    void processar_AjusteInexistente_DeveLancarExcecao() {
        when(ajusteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            ajusteService.processar(999L, "Ajuste inexistente");
        });
    }
}

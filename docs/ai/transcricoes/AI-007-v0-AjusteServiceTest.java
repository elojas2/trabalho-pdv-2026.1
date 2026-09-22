package net.originmobi.pdv;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.originmobi.pdv.enumerado.ajuste.AjusteStatus;
import net.originmobi.pdv.model.Ajuste;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.model.AjusteProduto;
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
    void processar_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);
        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ajusteService.processar(1L, "Teste de falha");
        });

        assertEquals("Ajuste já processado", exception.getMessage());
    }

    @Test
    void processar_AjusteValidoComProduto_DeveProcessarComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);
        
        Produto produto = new Produto();
        produto.setCodigo(100L);
        
        AjusteProduto ajusteProduto = new AjusteProduto();
        ajusteProduto.setProduto(produto);
        ajusteProduto.setQtd_alteracao(5);
        
        List<AjusteProduto> produtos = new ArrayList<>();
        produtos.add(ajusteProduto);
        ajuste.setProdutos(produtos);

        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        String resultado = ajusteService.processar(1L, "Ajuste de estoque positivo");

        assertEquals("Ajuste realizado com sucesso", resultado);
        assertEquals(AjusteStatus.PROCESSADO, ajuste.getStatus());
        verify(ajusteRepository, times(1)).save(ajuste);
    }

    @Test
    void remover_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ajusteService.remover(ajuste);
        });

        assertEquals("O ajuste já esta processado", exception.getMessage());
        verify(ajusteRepository, never()).deleteById(anyLong());
    }

    @Test
    void remover_AjusteAProcessar_DeveRemoverComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);

        assertDoesNotThrow(() -> {
            ajusteService.remover(ajuste);
        });

        verify(ajusteRepository, times(1)).deleteById(1L);
    }
}

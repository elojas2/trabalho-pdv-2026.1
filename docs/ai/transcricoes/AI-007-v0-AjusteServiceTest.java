package net.originmobi.pdv;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import net.originmobi.pdv.enumerado.ajuste.AjusteStatus;
import net.originmobi.pdv.model.Ajuste;
import net.originmobi.pdv.model.Produto;
import net.originmobi.pdv.model.AjusteProduto;
import net.originmobi.pdv.repository.AjusteRepository;
import net.originmobi.pdv.service.AjusteService;
import net.originmobi.pdv.service.ProdutoService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AjusteServiceTest {

    @InjectMocks
    private AjusteService ajusteService;

    @Mock
    private AjusteRepository ajusteRepository;

    @Mock
    private ProdutoService produtoService;

    private Ajuste ajuste;

    @Rule
    public ExpectedException excecaoEsperada = ExpectedException.none();

    @Before
    public void setUp() {
        ajuste = new Ajuste();
        ajuste.setCodigo(1L);
    }

    @Test
    public void processar_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);
        when(ajusteRepository.findById(1L)).thenReturn(Optional.of(ajuste));

        excecaoEsperada.expect(RuntimeException.class);
        excecaoEsperada.expectMessage("Ajuste já processado");

        ajusteService.processar(1L, "Teste de falha");
    }

    @Test
    public void processar_AjusteValidoComProduto_DeveProcessarComSucesso() {
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
    public void remover_AjusteJaProcessado_DeveLancarExcecao() {
        ajuste.setStatus(AjusteStatus.PROCESSADO);

        excecaoEsperada.expect(RuntimeException.class);
        excecaoEsperada.expectMessage("O ajuste já esta processado");

        ajusteService.remover(ajuste);
    }

    @Test
    public void remover_AjusteAProcessar_DeveRemoverComSucesso() {
        ajuste.setStatus(AjusteStatus.APROCESSAR);

        ajusteService.remover(ajuste);

        verify(ajusteRepository, times(1)).deleteById(1L);
    }
}

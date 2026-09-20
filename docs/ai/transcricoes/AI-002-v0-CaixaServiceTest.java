package net.originmobi.pdv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import net.originmobi.pdv.enumerado.caixa.CaixaTipo;
import net.originmobi.pdv.enumerado.caixa.EstiloLancamento;
import net.originmobi.pdv.enumerado.caixa.TipoLancamento;
import net.originmobi.pdv.filter.BancoFilter;
import net.originmobi.pdv.filter.CaixaFilter;
import net.originmobi.pdv.model.Caixa;
import net.originmobi.pdv.model.CaixaLancamento;
import net.originmobi.pdv.model.Usuario;
import net.originmobi.pdv.repository.CaixaRepository;
import net.originmobi.pdv.service.CaixaLancamentoService;
import net.originmobi.pdv.service.CaixaService;
import net.originmobi.pdv.service.UsuarioService;

/**
 * Testes unitários de {@link CaixaService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class CaixaServiceTest {

	private static final String USUARIO_LOGADO = "gerente";

	@Mock
	private CaixaRepository caixas;

	@Mock
	private UsuarioService usuarios;

	@Mock
	private CaixaLancamentoService lancamentos;

	@InjectMocks
	private CaixaService service;

	@Captor
	private ArgumentCaptor<Caixa> caixaCaptor;

	@Captor
	private ArgumentCaptor<CaixaLancamento> lancamentoCaptor;

	@Rule
	public ExpectedException excecaoEsperada = ExpectedException.none();

	private Usuario usuario;

	@Before
	public void setUp() {
		// Aplicacao (singleton) lê o usuário autenticado do SecurityContextHolder
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(USUARIO_LOGADO, "123"));

		usuario = new Usuario();
		usuario.setCodigo(1L);
		usuario.setUser(USUARIO_LOGADO);
		usuario.setSenha(new BCryptPasswordEncoder().encode("123"));

		when(usuarios.buscaUsuario(anyString())).thenReturn(usuario);
	}

	private Caixa novoCaixa(CaixaTipo tipo, String descricao, Double valorAbertura) {
		Caixa caixa = new Caixa();
		caixa.setTipo(tipo);
		caixa.setDescricao(descricao);
		caixa.setValor_abertura(valorAbertura);
		return caixa;
	}

	// ------------------------------------------------------------------
	// cadastro
	// ------------------------------------------------------------------

	@Test
	public void cadastro_caixaSemAberturaAnterior_deveSalvarERetornarCodigo() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa da loja", 150.0);
		when(caixas.save(any(Caixa.class))).thenAnswer(invocacao -> {
			((Caixa) invocacao.getArgument(0)).setCodigo(10L);
			return invocacao.getArgument(0);
		});

		Long codigo = service.cadastro(caixa);

		assertEquals(Long.valueOf(10L), codigo);
		verify(caixas).save(caixaCaptor.capture());
		assertEquals("Caixa da loja", caixaCaptor.getValue().getDescricao());
		assertEquals(usuario, caixaCaptor.getValue().getUsuario());
	}

	@Test
	public void cadastro_caixaComOutroAberto_deveLancarExcecao() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Existe caixa de dias anteriores em aberto, favor verifique");

		service.cadastro(novoCaixa(CaixaTipo.CAIXA, "Caixa diário", 0.0));
	}

	@Test
	public void cadastro_valorAberturaNulo_deveAssumirZeroENaoLancar() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", null);

		service.cadastro(caixa);

		assertEquals(Double.valueOf(0.0), caixa.getValor_abertura());
		assertEquals(Double.valueOf(0.0), caixa.getValor_total());
		verify(lancamentos, never()).lancamento(any(CaixaLancamento.class));
	}

	@Test
	public void cadastro_valorAberturaNegativo_deveLancarExcecao() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Valor informado é inválido");

		service.cadastro(novoCaixa(CaixaTipo.CAIXA, "Caixa", -1.0));
	}

	@Test
	public void cadastro_descricaoVaziaPorTipo_deveAplicarDescricaoPadrao() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());

		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "", 0.0);
		service.cadastro(caixa);
		assertEquals("Caixa diário", caixa.getDescricao());

		Caixa cofre = novoCaixa(CaixaTipo.COFRE, "", 0.0);
		service.cadastro(cofre);
		assertEquals("Cofre", cofre.getDescricao());

		Caixa banco = novoCaixa(CaixaTipo.BANCO, "", 0.0);
		banco.setAgencia("12-34");
		banco.setConta("56.789-0");
		service.cadastro(banco);
		assertEquals("Banco", banco.getDescricao());
	}

	@Test
	public void cadastro_tipoBanco_deveRemoverCaracteresNaoNumericosDeAgenciaEConta() {
		Caixa banco = novoCaixa(CaixaTipo.BANCO, "Banco do Brasil", 0.0);
		banco.setAgencia("12-34");
		banco.setConta("56.789-0");

		service.cadastro(banco);

		assertEquals("1234", banco.getAgencia());
		assertEquals("567890", banco.getConta());
	}

	@Test
	public void cadastro_comValorAbertura_deveGerarLancamentoDeSaldoInicial() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", 200.0);

		service.cadastro(caixa);

		verify(lancamentos).lancamento(lancamentoCaptor.capture());
		CaixaLancamento lancamento = lancamentoCaptor.getValue();
		assertEquals("Abertura de caixa", lancamento.getObservacao());
		assertEquals(Double.valueOf(200.0), lancamento.getValor());
		assertEquals(TipoLancamento.SALDOINICIAL, lancamento.getTipo());
		assertEquals(EstiloLancamento.ENTRADA, lancamento.getEstilo());
	}

	@Test
	public void cadastro_erroAoSalvar_deveLancarExcecaoDeSuporte() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(caixas.save(any(Caixa.class))).thenThrow(new RuntimeException("falha no banco"));

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Erro no processo de abertura, chame o suporte técnico");

		service.cadastro(novoCaixa(CaixaTipo.CAIXA, "Caixa", 0.0));
	}

	@Test
	public void cadastro_erroNoLancamento_deveLancarExcecaoDeSuporte() {
		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		when(lancamentos.lancamento(any(CaixaLancamento.class))).thenThrow(new RuntimeException("falha"));

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Erro no processo, chame o suporte");

		service.cadastro(novoCaixa(CaixaTipo.CAIXA, "Caixa", 50.0));
	}

	// ------------------------------------------------------------------
	// fechaCaixa
	// ------------------------------------------------------------------

	@Test
	public void fechaCaixa_senhaVazia_deveRetornarMensagem() {
		assertEquals("Favor, informe a senha", service.fechaCaixa(1L, ""));
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_senhaIncorreta_deveRetornarMensagem() {
		assertEquals("Senha incorreta, favor verifique", service.fechaCaixa(1L, "senhaErrada"));
		verify(caixas, never()).save(any(Caixa.class));
	}

	@Test
	public void fechaCaixa_senhaCorreta_deveFecharEGravarValores() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", 0.0);
		caixa.setValor_total(320.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		String retorno = service.fechaCaixa(1L, "123");

		assertEquals("Caixa fechado com sucesso", retorno);
		verify(caixas).save(caixa);
		assertEquals(Double.valueOf(320.0), caixa.getValor_fechamento());
		assertTrue(caixa.getData_fechamento() != null);
	}

	@Test
	public void fechaCaixa_semValorTotal_deveFecharComZero() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", 0.0);
		caixa.setValor_total(null);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		service.fechaCaixa(1L, "123");

		assertEquals(Double.valueOf(0.0), caixa.getValor_fechamento());
	}

	@Test
	public void fechaCaixa_caixaJaFechado_deveLancarExcecao() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", 0.0);
		caixa.setData_fechamento(new Timestamp(System.currentTimeMillis()));
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Caixa já esta fechado");

		service.fechaCaixa(1L, "123");
	}

	@Test
	public void fechaCaixa_erroAoSalvar_deveLancarExcecaoDeSuporte() {
		Caixa caixa = novoCaixa(CaixaTipo.CAIXA, "Caixa", 0.0);
		caixa.setValor_total(10.0);
		when(caixas.findById(1L)).thenReturn(Optional.of(caixa));
		when(caixas.save(any(Caixa.class))).thenThrow(new RuntimeException("falha"));

		excecaoEsperada.expect(RuntimeException.class);
		excecaoEsperada.expectMessage("Ocorreu um erro ao fechar o caixa, chame o suporte");

		service.fechaCaixa(1L, "123");
	}

	// ------------------------------------------------------------------
	// consultas
	// ------------------------------------------------------------------

	@Test
	public void caixaIsAberto_deveRefletirRetornoDoRepositorio() {
		when(caixas.caixaAberto()).thenReturn(Optional.of(new Caixa()));
		assertTrue(service.caixaIsAberto());

		when(caixas.caixaAberto()).thenReturn(Optional.empty());
		assertFalse(service.caixaIsAberto());
	}

	@Test
	public void listaTodos_deveDelegarParaRepositorio() {
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.findByCodigoOrdenado()).thenReturn(esperado);

		assertEquals(esperado, service.listaTodos());
	}

	@Test
	public void listarCaixas_comDataInformada_deveBuscarPorDataAbertura() {
		CaixaFilter filter = new CaixaFilter();
		filter.setData_cadastro("2026/09/19");
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixasPorDataAbertura(Date.valueOf("2026-09-19"))).thenReturn(esperado);

		assertEquals(esperado, service.listarCaixas(filter));
		verify(caixas, never()).listaCaixasAbertos();
	}

	@Test
	public void listarCaixas_semData_deveListarAbertos() {
		List<Caixa> esperado = Arrays.asList(new Caixa(), new Caixa());
		when(caixas.listaCaixasAbertos()).thenReturn(esperado);

		assertEquals(esperado, service.listarCaixas(new CaixaFilter()));

		CaixaFilter filtroVazio = new CaixaFilter();
		filtroVazio.setData_cadastro("");
		assertEquals(esperado, service.listarCaixas(filtroVazio));
		verify(caixas, times(2)).listaCaixasAbertos();
	}

	@Test
	public void caixaAberto_deveDelegarParaRepositorio() {
		Optional<Caixa> esperado = Optional.of(new Caixa());
		when(caixas.caixaAberto()).thenReturn(esperado);

		assertEquals(esperado, service.caixaAberto());
	}

	@Test
	public void caixasAbertos_deveDelegarParaRepositorio() {
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.caixasAbertos()).thenReturn(esperado);

		assertEquals(esperado, service.caixasAbertos());
	}

	@Test
	public void busca_deveDelegarParaRepositorio() {
		Optional<Caixa> esperado = Optional.of(new Caixa());
		when(caixas.findById(5L)).thenReturn(esperado);

		assertEquals(esperado, service.busca(5L));
	}

	@Test
	public void buscaCaixaUsuario_comCaixaAberto_deveRetornarOptionalPreenchido() {
		Caixa caixa = new Caixa();
		when(caixas.findByCaixaAbertoUsuario(1L)).thenReturn(caixa);

		Optional<Caixa> retorno = service.buscaCaixaUsuario(USUARIO_LOGADO);

		assertTrue(retorno.isPresent());
		assertEquals(caixa, retorno.get());
	}

	@Test
	public void buscaCaixaUsuario_semCaixaAberto_deveRetornarOptionalVazio() {
		when(caixas.findByCaixaAbertoUsuario(anyLong())).thenReturn(null);

		assertFalse(service.buscaCaixaUsuario(USUARIO_LOGADO).isPresent());
	}

	@Test
	public void listaBancos_deveBuscarPorTipoBanco() {
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.buscaBancos(CaixaTipo.BANCO)).thenReturn(esperado);

		assertEquals(esperado, service.listaBancos());
	}

	@Test
	public void listaCaixasAbertosTipo_deveBuscarPorTipoInformado() {
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipo(CaixaTipo.COFRE)).thenReturn(esperado);

		assertEquals(esperado, service.listaCaixasAbertosTipo(CaixaTipo.COFRE));
	}

	@Test
	public void listaBancosAbertosTipoFilterBanco_comData_deveBuscarPorTipoEData() {
		BancoFilter filter = new BancoFilter();
		filter.setData_cadastro("2026/09/19");
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipoData(eq(CaixaTipo.BANCO), eq(Date.valueOf("2026-09-19")))).thenReturn(esperado);

		assertEquals(esperado, service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, filter));
	}

	@Test
	public void listaBancosAbertosTipoFilterBanco_semData_deveBuscarTodosOsBancos() {
		List<Caixa> esperado = Collections.singletonList(new Caixa());
		when(caixas.buscaCaixaTipo(CaixaTipo.BANCO)).thenReturn(esperado);

		assertEquals(esperado, service.listaBancosAbertosTipoFilterBanco(CaixaTipo.BANCO, new BancoFilter()));
	}
}

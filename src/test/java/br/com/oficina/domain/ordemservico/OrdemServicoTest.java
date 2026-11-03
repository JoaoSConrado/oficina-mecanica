package br.com.oficina.domain.ordemservico;

import br.com.oficina.DadosTeste;
import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import br.com.oficina.domain.veiculo.Veiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static br.com.oficina.domain.ordemservico.StatusOrdemServico.AGUARDANDO_APROVACAO;
import static br.com.oficina.domain.ordemservico.StatusOrdemServico.EM_DIAGNOSTICO;
import static br.com.oficina.domain.ordemservico.StatusOrdemServico.EM_EXECUCAO;
import static br.com.oficina.domain.ordemservico.StatusOrdemServico.ENTREGUE;
import static br.com.oficina.domain.ordemservico.StatusOrdemServico.FINALIZADA;
import static br.com.oficina.domain.ordemservico.StatusOrdemServico.RECEBIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoTest {

    private static final Instant T0 = Instant.parse("2026-01-10T08:00:00Z");

    private Cliente cliente;
    private Veiculo veiculo;
    private Servico trocaOleo;
    private Peca filtro;
    private OrdemServico os;

    @BeforeEach
    void setUp() {
        cliente = DadosTeste.cliente();
        veiculo = DadosTeste.veiculo(cliente);
        trocaOleo = DadosTeste.servico("Troca de óleo", "80.00");
        filtro = DadosTeste.peca("FILTRO", "35.50", 10);
        os = OrdemServico.abrir(cliente, veiculo, "Revisão", T0);
    }

    @Nested
    class Abertura {

        @Test
        void abreRecebidaComHistorico() {
            assertThat(os.getId()).isNotNull();
            assertThat(os.getStatus()).isEqualTo(RECEBIDA);
            assertThat(os.getCliente()).isSameAs(cliente);
            assertThat(os.getVeiculo()).isSameAs(veiculo);
            assertThat(os.getDescricaoProblema()).isEqualTo("Revisão");
            assertThat(os.getCriadaEm()).isEqualTo(T0);
            assertThat(os.getHistorico()).singleElement()
                    .satisfies(h -> {
                        assertThat(h.getStatusAnterior()).isNull();
                        assertThat(h.getStatusNovo()).isEqualTo(RECEBIDA);
                        assertThat(h.getOcorridoEm()).isEqualTo(T0);
                        assertThat(h.getObservacao()).isNotBlank();
                        assertThat(h.getId()).isNotNull();
                    });
        }

        @Test
        void exigeVeiculoDoProprioCliente() {
            Cliente outro = DadosTeste.cliente();
            assertThatThrownBy(() -> OrdemServico.abrir(outro, veiculo, null, T0))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não pertence");
            assertThatThrownBy(() -> OrdemServico.abrir(null, veiculo, null, T0))
                    .isInstanceOf(DadoInvalidoException.class);
        }

        @Test
        void identificaTitular() {
            assertThat(os.pertenceAoCliente(cliente.getDocumento().getNumero())).isTrue();
            assertThat(os.pertenceAoCliente(DadosTeste.CPF_VALIDO)).isFalse();
        }
    }

    @Nested
    class ItensEOrcamento {

        @Test
        void incluirItemIniciaDiagnosticoAutomaticamente() {
            os.adicionarServico(trocaOleo, 1, T0.plusSeconds(60));

            assertThat(os.getStatus()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(os.getDiagnosticoIniciadoEm()).isEqualTo(T0.plusSeconds(60));
        }

        @Test
        void calculaOrcamentoComServicosEPecas() {
            os.adicionarServico(trocaOleo, 1, T0);
            os.adicionarServico(trocaOleo, 1, T0); // mesmo serviço soma quantidade
            os.adicionarPeca(filtro, 2, T0);

            assertThat(os.getServicos()).singleElement().satisfies(item -> {
                assertThat(item.getQuantidade()).isEqualTo(2);
                assertThat(item.getPrecoUnitario()).isEqualByComparingTo("80.00");
                assertThat(item.getSubtotal()).isEqualByComparingTo("160.00");
            });
            assertThat(os.getPecas()).singleElement().satisfies(item -> {
                assertThat(item.getPeca()).isSameAs(filtro);
                assertThat(item.getSubtotal()).isEqualByComparingTo("71.00");
            });
            assertThat(os.calcularTotal()).isEqualByComparingTo("231.00");
        }

        @Test
        void precoDoItemFicaCongeladoNaInclusao() {
            os.adicionarServico(trocaOleo, 1, T0);
            trocaOleo.atualizar("Troca de óleo", null, new java.math.BigDecimal("999"), null);

            assertThat(os.calcularTotal()).isEqualByComparingTo("80.00");
        }

        @Test
        void naoIncluiPecaSemEstoque() {
            os.adicionarPeca(filtro, 8, T0);

            assertThatThrownBy(() -> os.adicionarPeca(filtro, 3, T0))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Estoque insuficiente");
        }

        @Test
        void naoIncluiItemInativoOuQuantidadeInvalida() {
            Servico inativo = DadosTeste.servico("Velho", "10");
            inativo.desativar();
            Peca pecaInativa = DadosTeste.peca("OLD", "1", 5);
            pecaInativa.desativar();

            assertThatThrownBy(() -> os.adicionarServico(inativo, 1, T0)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> os.adicionarPeca(pecaInativa, 1, T0)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> os.adicionarServico(trocaOleo, 0, T0)).isInstanceOf(DadoInvalidoException.class);
            assertThat(os.getStatus()).as("inclusão recusada não inicia diagnóstico").isEqualTo(RECEBIDA);
        }

        @Test
        void removeItens() {
            os.adicionarServico(trocaOleo, 1, T0);
            os.adicionarPeca(filtro, 1, T0);

            os.removerServico(os.getServicos().getFirst().getId());
            os.removerPeca(os.getPecas().getFirst().getId());

            assertThat(os.getServicos()).isEmpty();
            assertThat(os.getPecas()).isEmpty();
            UUID inexistente = UUID.randomUUID();
            assertThatThrownBy(() -> os.removerServico(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
            assertThatThrownBy(() -> os.removerPeca(inexistente)).isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        void naoEnviaOrcamentoVazio() {
            os.iniciarDiagnostico(T0);
            assertThatThrownBy(() -> os.enviarOrcamento(T0))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ao menos um");
        }

        @Test
        void envioCongelaValorEBloqueiaAlteracaoDeItens() {
            os.adicionarServico(trocaOleo, 1, T0);
            os.enviarOrcamento(T0.plusSeconds(120));

            assertThat(os.getStatus()).isEqualTo(AGUARDANDO_APROVACAO);
            assertThat(os.getValorOrcamento()).isEqualByComparingTo("80.00");
            assertThat(os.getOrcamentoEnviadoEm()).isEqualTo(T0.plusSeconds(120));
            assertThatThrownBy(() -> os.adicionarServico(trocaOleo, 1, T0))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Aguardando aprovação");
        }
    }

    @Nested
    class AprovacaoEExecucao {

        @BeforeEach
        void orcamentoEnviado() {
            os.adicionarServico(trocaOleo, 1, T0);
            os.adicionarPeca(filtro, 2, T0);
            os.enviarOrcamento(T0.plus(Duration.ofMinutes(10)));
        }

        @Test
        void aprovacaoBaixaEstoqueEIniciaExecucao() {
            Instant aprovacao = T0.plus(Duration.ofMinutes(30));
            os.aprovarOrcamento(aprovacao);

            assertThat(os.getStatus()).isEqualTo(EM_EXECUCAO);
            assertThat(os.getDecisaoOrcamento()).isEqualTo(DecisaoOrcamento.APROVADO);
            assertThat(os.getOrcamentoRespondidoEm()).isEqualTo(aprovacao);
            assertThat(os.getExecucaoIniciadaEm()).isEqualTo(aprovacao);
            assertThat(filtro.getQuantidadeEstoque()).isEqualTo(8);
        }

        @Test
        void aprovacaoFalhaSeEstoqueAcabouEntreInclusaoEAprovacao() {
            filtro.darBaixa(9);

            assertThatThrownBy(() -> os.aprovarOrcamento(T0)).isInstanceOf(RegraNegocioException.class);
            assertThat(os.getStatus()).isEqualTo(AGUARDANDO_APROVACAO);
        }

        @Test
        void rejeicaoEncerraSemExecucaoESemBaixa() {
            os.rejeitarOrcamento("Caro demais", T0.plus(Duration.ofHours(1)));

            assertThat(os.getStatus()).isEqualTo(FINALIZADA);
            assertThat(os.getDecisaoOrcamento()).isEqualTo(DecisaoOrcamento.REJEITADO);
            assertThat(os.getMotivoRejeicao()).isEqualTo("Caro demais");
            assertThat(os.getFinalizadaEm()).isNotNull();
            assertThat(os.getExecucaoIniciadaEm()).isNull();
            assertThat(os.getTempoExecucao()).isEmpty();
            assertThat(filtro.getQuantidadeEstoque()).isEqualTo(10);
            assertThat(os.getHistorico().getLast().getObservacao()).contains("Caro demais");
        }

        @Test
        void rejeicaoSemMotivo() {
            os.rejeitarOrcamento(null, T0);
            assertThat(os.getHistorico().getLast().getObservacao()).isEqualTo("Orçamento rejeitado");
        }

        @Test
        void cicloCompletoAteEntrega() {
            Instant inicio = T0.plus(Duration.ofHours(1));
            os.aprovarOrcamento(inicio);
            os.finalizar(inicio.plus(Duration.ofMinutes(95)));
            os.entregar(inicio.plus(Duration.ofHours(3)));

            assertThat(os.getStatus()).isEqualTo(ENTREGUE);
            assertThat(os.getTempoExecucao()).contains(Duration.ofMinutes(95));
            assertThat(os.getEntregueEm()).isEqualTo(inicio.plus(Duration.ofHours(3)));
            assertThat(os.getHistorico()).extracting(HistoricoStatus::getStatusNovo)
                    .containsExactly(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE);
        }
    }

    @Nested
    class TransicoesInvalidas {

        @Test
        void naoPulaEtapas() {
            assertThatThrownBy(() -> os.aprovarOrcamento(T0)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> os.rejeitarOrcamento(null, T0)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> os.finalizar(T0)).isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> os.entregar(T0))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Transição de status não permitida");
            assertThat(os.getStatus()).isEqualTo(RECEBIDA);
        }

        @Test
        void naoIniciaDiagnosticoDuasVezes() {
            os.iniciarDiagnostico(T0);
            assertThatThrownBy(() -> os.iniciarDiagnostico(T0)).isInstanceOf(RegraNegocioException.class);
        }
    }

    @Test
    void maquinaDeEstados() {
        assertThat(RECEBIDA.podeTransicionarPara(EM_DIAGNOSTICO)).isTrue();
        assertThat(AGUARDANDO_APROVACAO.proximosPermitidos()).containsExactlyInAnyOrder(EM_EXECUCAO, FINALIZADA);
        assertThat(ENTREGUE.proximosPermitidos()).isEmpty();
        assertThat(EM_EXECUCAO.podeTransicionarPara(ENTREGUE)).isFalse();
        assertThat(EM_DIAGNOSTICO.permiteAlterarItens()).isTrue();
        assertThat(EM_EXECUCAO.permiteAlterarItens()).isFalse();
        assertThat(FINALIZADA.isEmAndamento()).isFalse();
        assertThat(EM_EXECUCAO.getPrioridade()).isLessThan(RECEBIDA.getPrioridade());
    }
}

package br.com.oficina.domain.ordemservico;

import br.com.oficina.domain.catalogo.Peca;
import br.com.oficina.domain.catalogo.Servico;
import br.com.oficina.domain.cliente.Cliente;
import br.com.oficina.domain.shared.DadoInvalidoException;
import br.com.oficina.domain.shared.RecursoNaoEncontradoException;
import br.com.oficina.domain.shared.RegraNegocioException;
import br.com.oficina.domain.veiculo.Veiculo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz do agregado Ordem de Serviço. Toda alteração de itens, orçamento e status passa por
 * esta classe, que garante as invariantes do fluxo de atendimento:
 * <ul>
 *     <li>itens só podem ser alterados antes do envio do orçamento;</li>
 *     <li>o orçamento só pode ser enviado com ao menos um item;</li>
 *     <li>a aprovação dá baixa no estoque e inicia a execução automaticamente;</li>
 *     <li>a rejeição encerra a OS sem execução;</li>
 *     <li>cada mudança de status fica registrada no histórico.</li>
 * </ul>
 */
@Entity
@Table(name = "ordens_servico")
public class OrdemServico {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemServico status;

    @Column(name = "descricao_problema", length = 1000)
    private String descricaoProblema;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemServicoOS> servicos = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPecaOS> pecas = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ocorridoEm ASC")
    private List<HistoricoStatus> historico = new ArrayList<>();

    @Column(name = "valor_orcamento", precision = 12, scale = 2)
    private BigDecimal valorOrcamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "decisao_orcamento", length = 10)
    private DecisaoOrcamento decisaoOrcamento;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "diagnostico_iniciado_em")
    private Instant diagnosticoIniciadoEm;

    @Column(name = "orcamento_enviado_em")
    private Instant orcamentoEnviadoEm;

    @Column(name = "orcamento_respondido_em")
    private Instant orcamentoRespondidoEm;

    @Column(name = "execucao_iniciada_em")
    private Instant execucaoIniciadaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @Column(name = "entregue_em")
    private Instant entregueEm;

    @Version
    private Long version;

    protected OrdemServico() {
    }

    private OrdemServico(Cliente cliente, Veiculo veiculo, String descricaoProblema, Instant agora) {
        this.id = UUID.randomUUID();
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.descricaoProblema = descricaoProblema;
        this.criadaEm = agora;
        this.status = StatusOrdemServico.RECEBIDA;
        this.historico.add(new HistoricoStatus(this, null, StatusOrdemServico.RECEBIDA, agora, "Ordem de serviço aberta"));
    }

    public static OrdemServico abrir(Cliente cliente, Veiculo veiculo, String descricaoProblema, Instant agora) {
        if (cliente == null || veiculo == null) {
            throw new DadoInvalidoException("Cliente e veículo são obrigatórios para abrir a OS");
        }
        if (!veiculo.pertenceA(cliente)) {
            throw new RegraNegocioException("O veículo " + veiculo.getPlaca() + " não pertence ao cliente informado");
        }
        return new OrdemServico(cliente, veiculo, descricaoProblema, agora);
    }

    // ---------------------------------------------------------------- diagnóstico e itens

    public void iniciarDiagnostico(Instant agora) {
        alterarStatus(StatusOrdemServico.EM_DIAGNOSTICO, agora, "Diagnóstico iniciado");
        this.diagnosticoIniciadoEm = agora;
    }

    public void adicionarServico(Servico servico, int quantidade, Instant agora) {
        validarInclusao(quantidade);
        if (!servico.isAtivo()) {
            throw new RegraNegocioException("Serviço inativo não pode ser incluído: " + servico.getNome());
        }
        iniciarDiagnosticoSeRecebida(agora);
        servicos.stream()
                .filter(item -> item.getServico().getId().equals(servico.getId()))
                .findFirst()
                .ifPresentOrElse(item -> item.adicionarQuantidade(quantidade),
                        () -> servicos.add(new ItemServicoOS(this, servico, quantidade)));
    }

    public void adicionarPeca(Peca peca, int quantidade, Instant agora) {
        validarInclusao(quantidade);
        if (!peca.isAtivo()) {
            throw new RegraNegocioException("Peça inativa não pode ser incluída: " + peca.getNome());
        }
        int jaIncluida = pecas.stream()
                .filter(item -> item.getPeca().getId().equals(peca.getId()))
                .mapToInt(ItemPecaOS::getQuantidade)
                .sum();
        if (!peca.possuiEstoque(jaIncluida + quantidade)) {
            throw new RegraNegocioException("Estoque insuficiente para " + peca.getNome()
                    + ": disponível " + peca.getQuantidadeEstoque());
        }
        iniciarDiagnosticoSeRecebida(agora);
        pecas.stream()
                .filter(item -> item.getPeca().getId().equals(peca.getId()))
                .findFirst()
                .ifPresentOrElse(item -> item.adicionarQuantidade(quantidade),
                        () -> pecas.add(new ItemPecaOS(this, peca, quantidade)));
    }

    public void removerServico(UUID itemId) {
        validarAlteracaoItens();
        if (!servicos.removeIf(item -> item.getId().equals(itemId))) {
            throw RecursoNaoEncontradoException.de("Item de serviço", itemId);
        }
    }

    public void removerPeca(UUID itemId) {
        validarAlteracaoItens();
        if (!pecas.removeIf(item -> item.getId().equals(itemId))) {
            throw RecursoNaoEncontradoException.de("Item de peça", itemId);
        }
    }

    private void validarInclusao(int quantidade) {
        if (quantidade <= 0) {
            throw new DadoInvalidoException("Quantidade deve ser maior que zero");
        }
        validarAlteracaoItens();
    }

    /**
     * Incluir itens numa OS recém-recebida significa que o diagnóstico começou.
     */
    private void iniciarDiagnosticoSeRecebida(Instant agora) {
        if (status == StatusOrdemServico.RECEBIDA) {
            iniciarDiagnostico(agora);
        }
    }

    private void validarAlteracaoItens() {
        if (!status.permiteAlterarItens()) {
            throw new RegraNegocioException("Itens não podem ser alterados com a OS no status " + status.getDescricao());
        }
    }

    // ---------------------------------------------------------------- orçamento

    public BigDecimal calcularTotal() {
        BigDecimal totalServicos = servicos.stream().map(ItemServicoOS::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPecas = pecas.stream().map(ItemPecaOS::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalServicos.add(totalPecas);
    }

    public void enviarOrcamento(Instant agora) {
        if (servicos.isEmpty() && pecas.isEmpty()) {
            throw new RegraNegocioException("Inclua ao menos um serviço ou peça antes de enviar o orçamento");
        }
        alterarStatus(StatusOrdemServico.AGUARDANDO_APROVACAO, agora, "Orçamento enviado ao cliente");
        this.valorOrcamento = calcularTotal();
        this.orcamentoEnviadoEm = agora;
    }

    /**
     * Registra a aprovação do orçamento, dá baixa das peças no estoque e inicia a execução.
     */
    public void aprovarOrcamento(Instant agora) {
        exigirStatus(StatusOrdemServico.AGUARDANDO_APROVACAO, "aprovar o orçamento");
        pecas.forEach(item -> item.getPeca().darBaixa(item.getQuantidade()));
        this.decisaoOrcamento = DecisaoOrcamento.APROVADO;
        this.orcamentoRespondidoEm = agora;
        alterarStatus(StatusOrdemServico.EM_EXECUCAO, agora, "Orçamento aprovado; execução iniciada");
        this.execucaoIniciadaEm = agora;
    }

    /**
     * Registra a rejeição do orçamento. A OS é encerrada sem execução e o veículo fica
     * disponível para retirada.
     */
    public void rejeitarOrcamento(String motivo, Instant agora) {
        exigirStatus(StatusOrdemServico.AGUARDANDO_APROVACAO, "rejeitar o orçamento");
        this.decisaoOrcamento = DecisaoOrcamento.REJEITADO;
        this.motivoRejeicao = motivo;
        this.orcamentoRespondidoEm = agora;
        alterarStatus(StatusOrdemServico.FINALIZADA, agora,
                "Orçamento rejeitado" + (motivo == null || motivo.isBlank() ? "" : ": " + motivo));
        this.finalizadaEm = agora;
    }

    // ---------------------------------------------------------------- execução e entrega

    public void finalizar(Instant agora) {
        exigirStatus(StatusOrdemServico.EM_EXECUCAO, "finalizar a OS");
        alterarStatus(StatusOrdemServico.FINALIZADA, agora, "Serviços concluídos");
        this.finalizadaEm = agora;
    }

    public void entregar(Instant agora) {
        alterarStatus(StatusOrdemServico.ENTREGUE, agora, "Veículo entregue ao cliente");
        this.entregueEm = agora;
    }

    /**
     * Tempo efetivo de execução (da aprovação até a finalização), quando houver.
     */
    public Optional<Duration> getTempoExecucao() {
        if (execucaoIniciadaEm == null || finalizadaEm == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(execucaoIniciadaEm, finalizadaEm));
    }

    public boolean pertenceAoCliente(String numeroDocumento) {
        return cliente.getDocumento().getNumero().equals(numeroDocumento);
    }

    private void exigirStatus(StatusOrdemServico esperado, String acao) {
        if (status != esperado) {
            throw new RegraNegocioException("Não é possível " + acao + " com a OS no status " + status.getDescricao());
        }
    }

    private void alterarStatus(StatusOrdemServico novo, Instant agora, String observacao) {
        if (!status.podeTransicionarPara(novo)) {
            throw new RegraNegocioException("Transição de status não permitida: "
                    + status.getDescricao() + " -> " + novo.getDescricao());
        }
        historico.add(new HistoricoStatus(this, status, novo, agora, observacao));
        this.status = novo;
    }

    // ---------------------------------------------------------------- getters

    public UUID getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public String getDescricaoProblema() {
        return descricaoProblema;
    }

    public List<ItemServicoOS> getServicos() {
        return Collections.unmodifiableList(servicos);
    }

    public List<ItemPecaOS> getPecas() {
        return Collections.unmodifiableList(pecas);
    }

    public List<HistoricoStatus> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public BigDecimal getValorOrcamento() {
        return valorOrcamento;
    }

    public DecisaoOrcamento getDecisaoOrcamento() {
        return decisaoOrcamento;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getDiagnosticoIniciadoEm() {
        return diagnosticoIniciadoEm;
    }

    public Instant getOrcamentoEnviadoEm() {
        return orcamentoEnviadoEm;
    }

    public Instant getOrcamentoRespondidoEm() {
        return orcamentoRespondidoEm;
    }

    public Instant getExecucaoIniciadaEm() {
        return execucaoIniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public Instant getEntregueEm() {
        return entregueEm;
    }
}

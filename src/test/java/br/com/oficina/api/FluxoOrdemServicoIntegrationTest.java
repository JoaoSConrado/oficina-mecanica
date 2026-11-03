package br.com.oficina.api;

import br.com.oficina.DadosTeste;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo principal: abertura da OS, orçamento, aprovação pelo cliente, execução e entrega.
 */
class FluxoOrdemServicoIntegrationTest extends IntegracaoBase {

    // Ids do catálogo inicial (V2__catalogo_inicial.sql)
    private static final String TROCA_OLEO = "a1f2c3d4-0001-4000-8000-000000000001";
    private static final String ALINHAMENTO = "a1f2c3d4-0001-4000-8000-000000000002";

    private String criarCliente(String documento) throws Exception {
        return corpo(postAdmin("/api/clientes", Map.of("nome", "Cliente Teste", "documento", documento,
                "email", "cliente@teste.com")).andExpect(status().isCreated())).get("id").asText();
    }

    private String criarPeca(int estoque) throws Exception {
        String codigo = "P-" + System.nanoTime();
        return corpo(postAdmin("/api/pecas", Map.of("codigo", codigo, "nome", "Filtro", "tipo", "PECA",
                "preco", "40.00", "quantidadeEstoque", estoque, "estoqueMinimo", 1))
                .andExpect(status().isCreated())).get("id").asText();
    }

    private JsonNode abrirOs(String documento, String placa, String pecaId, int qtdPeca) throws Exception {
        Map<String, Object> pedido = Map.of(
                "documentoCliente", documento,
                "veiculo", Map.of("placa", placa, "marca", "Fiat", "modelo", "Argo", "ano", 2022),
                "descricaoProblema", "Revisão dos 30 mil km",
                "servicos", List.of(Map.of("id", TROCA_OLEO, "quantidade", 1), Map.of("id", ALINHAMENTO, "quantidade", 1)),
                "pecas", List.of(Map.of("id", pecaId, "quantidade", qtdPeca)));
        return corpo(postAdmin("/api/ordens-servico", pedido).andExpect(status().isCreated()));
    }

    @Test
    void fluxoCompletoComAprovacaoPeloCliente() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String pecaId = criarPeca(5);

        // Abertura: veículo novo é cadastrado e o orçamento é calculado automaticamente
        JsonNode os = abrirOs(cpf, DadosTeste.placaAleatoria(), pecaId, 2);
        String osId = os.get("id").asText();
        assertThat(os.get("status").asText()).isEqualTo("EM_DIAGNOSTICO");
        assertThat(os.get("valorTotal").decimalValue()).isEqualByComparingTo("280.00");

        // Envio do orçamento
        postAdmin("/api/ordens-servico/{id}/orcamento/envio", null, osId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.valorOrcamento").value(280.00));

        // Cliente acompanha sem token, apenas com o documento
        mvc.perform(get("/api/public/ordens-servico/{id}", osId).param("documento", cpf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando aprovação"));

        // Cliente aprova: estoque baixado e execução iniciada
        postPublico("/api/public/ordens-servico/{id}/orcamento/aprovacao", Map.of("documento", cpf), osId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.decisaoOrcamento").value("APROVADO"));
        getAdmin("/api/pecas/{id}", pecaId).andExpect(jsonPath("$.quantidadeEstoque").value(3));

        // Lista mostra a OS em execução
        getAdmin("/api/ordens-servico").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(osId)));

        postAdmin("/api/ordens-servico/{id}/finalizacao", null, osId)
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.tempoExecucaoMinutos").isNumber());
        postAdmin("/api/ordens-servico/{id}/entrega", null, osId)
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.historico.length()").value(6));

        getAdmin("/api/ordens-servico/metricas/tempo-medio")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordensConsideradas").isNumber())
                .andExpect(jsonPath("$.porServico[*].servico", hasItem("Troca de óleo")));
        getAdmin("/api/ordens-servico").andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem(osId))));
        getAdmin("/api/ordens-servico?incluirEncerradas=true").andExpect(jsonPath("$[*].id", hasItem(osId)));
        getAdmin("/api/ordens-servico?status=ENTREGUE").andExpect(jsonPath("$[*].id", hasItem(osId)));
    }

    @Test
    void rejeicaoPeloAdministradorEncerraSemBaixarEstoque() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String pecaId = criarPeca(5);
        String osId = abrirOs(cpf, DadosTeste.placaAleatoria(), pecaId, 1).get("id").asText();
        postAdmin("/api/ordens-servico/{id}/orcamento/envio", null, osId).andExpect(status().isOk());

        postAdmin("/api/ordens-servico/{id}/orcamento/rejeicao", Map.of("motivo", "Cliente desistiu"), osId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.decisaoOrcamento").value("REJEITADO"));
        getAdmin("/api/pecas/{id}", pecaId).andExpect(jsonPath("$.quantidadeEstoque").value(5));
    }

    @Test
    void clienteRejeitaPelaApiPublica() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String osId = abrirOs(cpf, DadosTeste.placaAleatoria(), criarPeca(5), 1).get("id").asText();
        postAdmin("/api/ordens-servico/{id}/orcamento/envio", null, osId);

        postPublico("/api/public/ordens-servico/{id}/orcamento/rejeicao",
                Map.of("documento", cpf, "motivo", "Muito caro"), osId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivoRejeicao").value("Muito caro"));
    }

    @Test
    void aprovacaoAdministrativaEAlteracaoDeItens() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String pecaId = criarPeca(10);
        JsonNode os = abrirOs(cpf, DadosTeste.placaAleatoria(), pecaId, 1);
        String osId = os.get("id").asText();
        String itemServico = os.get("servicos").get(0).get("itemId").asText();
        String itemPeca = os.get("pecas").get(0).get("itemId").asText();

        deleteAdmin("/api/ordens-servico/{id}/servicos/{item}", osId, itemServico)
                .andExpect(jsonPath("$.servicos.length()").value(1));
        deleteAdmin("/api/ordens-servico/{id}/pecas/{item}", osId, itemPeca)
                .andExpect(jsonPath("$.pecas.length()").value(0));
        postAdmin("/api/ordens-servico/{id}/pecas", Map.of("id", pecaId, "quantidade", 3), osId)
                .andExpect(jsonPath("$.pecas[0].quantidade").value(3));
        postAdmin("/api/ordens-servico/{id}/servicos", Map.of("id", TROCA_OLEO, "quantidade", 1), osId)
                .andExpect(status().isOk());

        postAdmin("/api/ordens-servico/{id}/orcamento/envio", null, osId);
        postAdmin("/api/ordens-servico/{id}/orcamento/aprovacao", null, osId)
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        getAdmin("/api/pecas/{id}", pecaId).andExpect(jsonPath("$.quantidadeEstoque").value(7));

        getAdmin("/api/ordens-servico/{id}", osId)
                .andExpect(jsonPath("$.cliente.documento").exists())
                .andExpect(jsonPath("$.historico[0].statusNovo").value("RECEBIDA"));
    }

    @Test
    void diagnosticoExplicitoEOsSemItens() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String osId = corpo(postAdmin("/api/ordens-servico", Map.of("documentoCliente", cpf,
                "veiculo", Map.of("placa", DadosTeste.placaAleatoria(), "marca", "VW", "modelo", "Polo", "ano", 2021)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEBIDA"))).get("id").asText();

        postAdmin("/api/ordens-servico/{id}/diagnostico", null, osId)
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
        postAdmin("/api/ordens-servico/{id}/orcamento/envio", null, osId)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void regrasDeNegocioRetornamErrosAdequados() throws Exception {
        String cpf = DadosTeste.cpfAleatorio();
        criarCliente(cpf);
        String pecaId = criarPeca(1);

        // Estoque insuficiente na inclusão
        Map<String, Object> pedido = Map.of("documentoCliente", cpf,
                "veiculo", Map.of("placa", DadosTeste.placaAleatoria(), "marca", "VW", "modelo", "Gol", "ano", 2020),
                "pecas", List.of(Map.of("id", pecaId, "quantidade", 2)));
        postAdmin("/api/ordens-servico", pedido)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Estoque insuficiente")));

        String osId = abrirOs(cpf, DadosTeste.placaAleatoria(), pecaId, 1).get("id").asText();

        // Transição inválida
        postAdmin("/api/ordens-servico/{id}/entrega", null, osId).andExpect(status().isUnprocessableEntity());

        // Cliente desconhecido
        postAdmin("/api/ordens-servico", Map.of("documentoCliente", DadosTeste.cpfAleatorio(),
                "veiculo", Map.of("placa", DadosTeste.placaAleatoria())))
                .andExpect(status().isNotFound());

        // Veículo novo sem dados
        postAdmin("/api/ordens-servico", Map.of("documentoCliente", cpf,
                "veiculo", Map.of("placa", DadosTeste.placaAleatoria())))
                .andExpect(status().isUnprocessableEntity());

        // Documento de outra pessoa na API pública
        mvc.perform(get("/api/public/ordens-servico/{id}", osId).param("documento", DadosTeste.cpfAleatorio()))
                .andExpect(status().isNotFound());

        // Validação de entrada
        postAdmin("/api/ordens-servico", Map.of("documentoCliente", "123", "veiculo", Map.of("placa", "XX")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documentoCliente").exists())
                .andExpect(jsonPath("$.campos['veiculo.placa']").exists());
    }
}

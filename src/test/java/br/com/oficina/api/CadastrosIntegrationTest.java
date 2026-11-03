package br.com.oficina.api;

import br.com.oficina.DadosTeste;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUDs administrativos e segurança das rotas.
 */
class CadastrosIntegrationTest extends IntegracaoBase {

    @Test
    void rotasAdministrativasExigemToken() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        postPublico("/api/auth/login", Map.of("usuario", "admin", "senha", "errada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crudDeClientes() throws Exception {
        String cnpj = "11.222.333/0001-81";
        String id = corpo(postAdmin("/api/clientes", Map.of("nome", "Transportadora", "documento", cnpj))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDocumento").value("CNPJ"))
                .andExpect(jsonPath("$.documento").value(cnpj))).get("id").asText();

        postAdmin("/api/clientes", Map.of("nome", "Duplicado", "documento", "11222333000181"))
                .andExpect(status().isConflict());
        postAdmin("/api/clientes", Map.of("nome", "Inválido", "documento", "11.222.333/0001-80"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").value("CPF/CNPJ inválido"));

        getAdmin("/api/clientes/{id}", id).andExpect(jsonPath("$.nome").value("Transportadora"));
        getAdmin("/api/clientes?documento=11222333000181").andExpect(jsonPath("$[0].id").value(id));
        getAdmin("/api/clientes").andExpect(jsonPath("$[*].id", hasItem(id)));
        putAdmin("/api/clientes/{id}", Map.of("nome", "Transportadora LTDA", "telefone", "1133334444"), id)
                .andExpect(jsonPath("$.nome").value("Transportadora LTDA"));
        deleteAdmin("/api/clientes/{id}", id).andExpect(status().isNoContent());
        getAdmin("/api/clientes/{id}", id).andExpect(status().isNotFound());
    }

    @Test
    void crudDeVeiculos() throws Exception {
        String clienteId = corpo(postAdmin("/api/clientes",
                Map.of("nome", "Dono", "documento", DadosTeste.cpfAleatorio()))).get("id").asText();
        String placa = DadosTeste.placaAleatoria();

        String id = corpo(postAdmin("/api/veiculos", Map.of("placa", placa, "marca", "Honda", "modelo", "Civic",
                "ano", 2019, "clienteId", clienteId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value(placa))).get("id").asText();

        postAdmin("/api/veiculos", Map.of("placa", placa, "marca", "Honda", "modelo", "Civic",
                "ano", 2019, "clienteId", clienteId)).andExpect(status().isConflict());
        postAdmin("/api/veiculos", Map.of("placa", "1234ABC", "marca", "Honda", "modelo", "Civic",
                "ano", 2019, "clienteId", clienteId)).andExpect(status().isBadRequest());
        postAdmin("/api/veiculos", Map.of("placa", DadosTeste.placaAleatoria(), "marca", "Honda", "modelo", "Civic",
                "ano", 1800, "clienteId", clienteId)).andExpect(status().isBadRequest());

        // Cliente com veículo não pode ser removido
        deleteAdmin("/api/clientes/{id}", clienteId).andExpect(status().isConflict());

        getAdmin("/api/veiculos?clienteId={c}", clienteId).andExpect(jsonPath("$[0].id").value(id));
        getAdmin("/api/veiculos").andExpect(jsonPath("$[*].id", hasItem(id)));
        putAdmin("/api/veiculos/{id}", Map.of("marca", "Honda", "modelo", "City", "ano", 2020,
                "clienteId", clienteId), id).andExpect(jsonPath("$.modelo").value("City"));
        getAdmin("/api/veiculos/{id}", id).andExpect(jsonPath("$.ano").value(2020));
        deleteAdmin("/api/veiculos/{id}", id).andExpect(status().isNoContent());
        deleteAdmin("/api/clientes/{id}", clienteId).andExpect(status().isNoContent());
    }

    @Test
    void crudDeServicos() throws Exception {
        String id = corpo(postAdmin("/api/servicos", Map.of("nome", "Polimento", "preco", "250.00",
                "tempoEstimadoMinutos", 120)).andExpect(status().isCreated())).get("id").asText();

        getAdmin("/api/servicos/{id}", id).andExpect(jsonPath("$.ativo").value(true));
        putAdmin("/api/servicos/{id}", Map.of("nome", "Polimento completo", "preco", "300.00"), id)
                .andExpect(jsonPath("$.preco").value(300.00));
        postAdmin("/api/servicos", Map.of("nome", "", "preco", "-1")).andExpect(status().isBadRequest());
        deleteAdmin("/api/servicos/{id}", id).andExpect(status().isNoContent());
        getAdmin("/api/servicos").andExpect(jsonPath("$[*].id", not(hasItem(id))));
        getAdmin("/api/servicos/{id}", id).andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void crudDePecasComControleDeEstoque() throws Exception {
        String codigo = "TST-" + System.nanoTime();
        String id = corpo(postAdmin("/api/pecas", Map.of("codigo", codigo, "nome", "Lâmpada", "tipo", "PECA",
                "preco", "15.00", "quantidadeEstoque", 3, "estoqueMinimo", 2))
                .andExpect(status().isCreated())).get("id").asText();

        postAdmin("/api/pecas", Map.of("codigo", codigo, "nome", "Outra", "tipo", "PECA",
                "preco", "1.00", "quantidadeEstoque", 1, "estoqueMinimo", 0)).andExpect(status().isConflict());

        postAdmin("/api/pecas/{id}/estoque/saida", Map.of("quantidade", 2), id)
                .andExpect(jsonPath("$.quantidadeEstoque").value(1))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(true));
        getAdmin("/api/pecas?abaixoDoMinimo=true").andExpect(jsonPath("$[*].id", hasItem(id)));
        postAdmin("/api/pecas/{id}/estoque/saida", Map.of("quantidade", 5), id)
                .andExpect(status().isUnprocessableEntity());
        postAdmin("/api/pecas/{id}/estoque/entrada", Map.of("quantidade", 10), id)
                .andExpect(jsonPath("$.quantidadeEstoque").value(11));
        postAdmin("/api/pecas/{id}/estoque/entrada", Map.of("quantidade", 0), id)
                .andExpect(status().isBadRequest());

        putAdmin("/api/pecas/{id}", Map.of("nome", "Lâmpada H4", "tipo", "PECA", "preco", "18.00",
                "estoqueMinimo", 2), id).andExpect(jsonPath("$.nome").value("Lâmpada H4"));
        getAdmin("/api/pecas").andExpect(jsonPath("$[*].id", hasItem(id)));
        deleteAdmin("/api/pecas/{id}", id).andExpect(status().isNoContent());
        getAdmin("/api/pecas/{id}", id).andExpect(jsonPath("$.ativo").value(false));
    }
}

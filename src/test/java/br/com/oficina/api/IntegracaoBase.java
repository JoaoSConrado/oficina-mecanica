package br.com.oficina.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação completa (segurança, Flyway, JPA) sobre H2 em modo PostgreSQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegracaoBase {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected String token;

    @BeforeEach
    void autenticar() throws Exception {
        String resposta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", "admin", "senha", "senha-teste"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = json.readTree(resposta).get("token").asText();
    }

    protected ResultActions getAdmin(String url, Object... params) throws Exception {
        return mvc.perform(autenticado(get(url, params)));
    }

    protected ResultActions postAdmin(String url, Object corpo, Object... params) throws Exception {
        return mvc.perform(comCorpo(autenticado(post(url, params)), corpo));
    }

    protected ResultActions putAdmin(String url, Object corpo, Object... params) throws Exception {
        return mvc.perform(comCorpo(autenticado(put(url, params)), corpo));
    }

    protected ResultActions deleteAdmin(String url, Object... params) throws Exception {
        return mvc.perform(autenticado(delete(url, params)));
    }

    protected ResultActions postPublico(String url, Object corpo, Object... params) throws Exception {
        return mvc.perform(comCorpo(post(url, params), corpo));
    }

    protected JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder comCorpo(MockHttpServletRequestBuilder req, Object corpo) throws Exception {
        if (corpo == null) {
            return req;
        }
        return req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo));
    }
}

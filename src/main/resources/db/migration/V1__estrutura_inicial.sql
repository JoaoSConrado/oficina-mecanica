CREATE TABLE clientes (
    id             UUID PRIMARY KEY,
    nome           VARCHAR(150) NOT NULL,
    documento      VARCHAR(14)  NOT NULL,
    tipo_documento VARCHAR(4)   NOT NULL,
    email          VARCHAR(150),
    telefone       VARCHAR(20),
    criado_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_clientes_documento UNIQUE (documento)
);

CREATE TABLE veiculos (
    id         UUID PRIMARY KEY,
    placa      VARCHAR(7)  NOT NULL,
    marca      VARCHAR(60) NOT NULL,
    modelo     VARCHAR(60) NOT NULL,
    ano        INTEGER     NOT NULL,
    cliente_id UUID        NOT NULL REFERENCES clientes (id),
    criado_em  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_veiculos_placa UNIQUE (placa)
);
CREATE INDEX idx_veiculos_cliente ON veiculos (cliente_id);

CREATE TABLE servicos (
    id                     UUID PRIMARY KEY,
    nome                   VARCHAR(120)   NOT NULL,
    descricao              VARCHAR(500),
    preco                  NUMERIC(12, 2) NOT NULL CHECK (preco >= 0),
    tempo_estimado_minutos INTEGER,
    ativo                  BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE TABLE pecas (
    id                 UUID PRIMARY KEY,
    codigo             VARCHAR(40)    NOT NULL,
    nome               VARCHAR(120)   NOT NULL,
    descricao          VARCHAR(500),
    tipo               VARCHAR(10)    NOT NULL,
    preco              NUMERIC(12, 2) NOT NULL CHECK (preco >= 0),
    quantidade_estoque INTEGER        NOT NULL CHECK (quantidade_estoque >= 0),
    estoque_minimo     INTEGER        NOT NULL CHECK (estoque_minimo >= 0),
    ativo              BOOLEAN        NOT NULL DEFAULT TRUE,
    version            BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_pecas_codigo UNIQUE (codigo)
);

CREATE TABLE ordens_servico (
    id                      UUID PRIMARY KEY,
    cliente_id              UUID        NOT NULL REFERENCES clientes (id),
    veiculo_id              UUID        NOT NULL REFERENCES veiculos (id),
    status                  VARCHAR(30) NOT NULL,
    descricao_problema      VARCHAR(1000),
    valor_orcamento         NUMERIC(12, 2),
    decisao_orcamento       VARCHAR(10),
    motivo_rejeicao         VARCHAR(500),
    criada_em               TIMESTAMP WITH TIME ZONE NOT NULL,
    diagnostico_iniciado_em TIMESTAMP WITH TIME ZONE,
    orcamento_enviado_em    TIMESTAMP WITH TIME ZONE,
    orcamento_respondido_em TIMESTAMP WITH TIME ZONE,
    execucao_iniciada_em    TIMESTAMP WITH TIME ZONE,
    finalizada_em           TIMESTAMP WITH TIME ZONE,
    entregue_em             TIMESTAMP WITH TIME ZONE,
    version                 BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_os_status ON ordens_servico (status);
CREATE INDEX idx_os_cliente ON ordens_servico (cliente_id);
CREATE INDEX idx_os_veiculo ON ordens_servico (veiculo_id);

CREATE TABLE itens_servico_os (
    id               UUID PRIMARY KEY,
    ordem_servico_id UUID           NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    servico_id       UUID           NOT NULL REFERENCES servicos (id),
    quantidade       INTEGER        NOT NULL CHECK (quantidade > 0),
    preco_unitario   NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_itens_servico_os ON itens_servico_os (ordem_servico_id);

CREATE TABLE itens_peca_os (
    id               UUID PRIMARY KEY,
    ordem_servico_id UUID           NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    peca_id          UUID           NOT NULL REFERENCES pecas (id),
    quantidade       INTEGER        NOT NULL CHECK (quantidade > 0),
    preco_unitario   NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_itens_peca_os ON itens_peca_os (ordem_servico_id);

CREATE TABLE historico_status_os (
    id               UUID PRIMARY KEY,
    ordem_servico_id UUID        NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    status_anterior  VARCHAR(30),
    status_novo      VARCHAR(30) NOT NULL,
    ocorrido_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    observacao       VARCHAR(500)
);
CREATE INDEX idx_historico_os ON historico_status_os (ordem_servico_id);

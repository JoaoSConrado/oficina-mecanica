-- Catálogo inicial para demonstração
INSERT INTO servicos (id, nome, descricao, preco, tempo_estimado_minutos, ativo) VALUES
    ('a1f2c3d4-0001-4000-8000-000000000001', 'Troca de óleo', 'Troca do óleo do motor e do filtro', 80.00, 30, TRUE),
    ('a1f2c3d4-0001-4000-8000-000000000002', 'Alinhamento', 'Alinhamento da direção', 120.00, 45, TRUE),
    ('a1f2c3d4-0001-4000-8000-000000000003', 'Balanceamento', 'Balanceamento das quatro rodas', 100.00, 40, TRUE),
    ('a1f2c3d4-0001-4000-8000-000000000004', 'Revisão de freios', 'Inspeção e troca de pastilhas', 150.00, 90, TRUE),
    ('a1f2c3d4-0001-4000-8000-000000000005', 'Diagnóstico eletrônico', 'Leitura de falhas via scanner', 90.00, 30, TRUE);

INSERT INTO pecas (id, codigo, nome, descricao, tipo, preco, quantidade_estoque, estoque_minimo, ativo, version) VALUES
    ('b2e3d4f5-0002-4000-8000-000000000001', 'OLEO-5W30', 'Óleo 5W30 (litro)', 'Óleo sintético', 'INSUMO', 45.00, 100, 20, TRUE, 0),
    ('b2e3d4f5-0002-4000-8000-000000000002', 'FILTRO-OLEO', 'Filtro de óleo', NULL, 'PECA', 35.90, 30, 5, TRUE, 0),
    ('b2e3d4f5-0002-4000-8000-000000000003', 'PASTILHA-DIANT', 'Jogo de pastilhas dianteiras', NULL, 'PECA', 189.90, 12, 3, TRUE, 0),
    ('b2e3d4f5-0002-4000-8000-000000000004', 'FLUIDO-FREIO', 'Fluido de freio DOT4 (500 ml)', NULL, 'INSUMO', 39.90, 25, 5, TRUE, 0);

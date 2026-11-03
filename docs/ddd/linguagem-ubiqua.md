# Linguagem Ubíqua e Glossário

Termos usados por atendentes, mecânicos e clientes da oficina, e onde cada um aparece no código.

| Termo | Significado no negócio | No código |
|---|---|---|
| **Cliente** | Pessoa física ou jurídica dona do veículo, identificada por CPF ou CNPJ | `domain.cliente.Cliente` |
| **Documento** | CPF (11 dígitos) ou CNPJ (14 caracteres, numérico ou alfanumérico), sempre com dígitos verificadores válidos | Objeto de valor `domain.shared.Documento` |
| **Veículo** | Automóvel atendido, identificado pela placa; pertence a um cliente | `domain.veiculo.Veiculo` |
| **Placa** | Identificador do veículo no padrão antigo (ABC1234) ou Mercosul (ABC1D23) | Objeto de valor `domain.shared.Placa` |
| **Serviço** | Mão de obra do catálogo (troca de óleo, alinhamento...) com preço e tempo estimado | `domain.catalogo.Servico` |
| **Peça** | Componente substituído no veículo, controlado em estoque | `domain.catalogo.Peca` com `TipoPeca.PECA` |
| **Insumo** | Material consumido no serviço (óleo, fluido), controlado em estoque | `domain.catalogo.Peca` com `TipoPeca.INSUMO` |
| **Estoque mínimo** | Saldo a partir do qual a peça precisa de reposição | `Peca.isAbaixoDoMinimo()` |
| **Entrada / Saída de estoque** | Reposição ou baixa manual do saldo | `Peca.repor()` / `Peca.darBaixa()` |
| **Ordem de Serviço (OS)** | Registro do atendimento de um veículo, do recebimento à entrega | Agregado `domain.ordemservico.OrdemServico` |
| **Item da OS** | Serviço ou peça incluído na OS, com quantidade e preço congelado na inclusão | `ItemServicoOS`, `ItemPecaOS` |
| **Diagnóstico** | Avaliação do veículo para definir serviços e peças necessários | Status `EM_DIAGNOSTICO` |
| **Orçamento** | Soma dos itens da OS, enviada ao cliente para aprovação | `OrdemServico.calcularTotal()`, `valorOrcamento` |
| **Envio do orçamento** | Congela o valor e coloca a OS aguardando o cliente | `OrdemServico.enviarOrcamento()` |
| **Aprovação** | Cliente autoriza o orçamento: as peças saem do estoque e a execução começa | `OrdemServico.aprovarOrcamento()` |
| **Rejeição** | Cliente recusa o orçamento: a OS é finalizada sem execução | `OrdemServico.rejeitarOrcamento()` |
| **Execução** | Período em que os serviços são realizados | Status `EM_EXECUCAO` |
| **Finalização** | Serviços concluídos, veículo pronto para retirada | Status `FINALIZADA` |
| **Entrega** | Veículo devolvido ao cliente; encerra a OS | Status `ENTREGUE` |
| **Histórico de status** | Linha do tempo de mudanças de status da OS | `HistoricoStatus` |
| **Fila de atendimento** | Ordem de prioridade das OS em andamento: em execução > aguardando aprovação > em diagnóstico > recebida; dentro do mesmo status, a mais antiga primeiro | `StatusOrdemServico.getPrioridade()`, `OrdemServicoService.listar()` |
| **Tempo de execução** | Tempo entre o início da execução (aprovação) e a finalização | `OrdemServico.getTempoExecucao()` |
| **Tempo médio de execução** | Média do tempo de execução das OS concluídas, geral e por serviço | `MetricasService` |
| **Acompanhamento** | Consulta da OS pelo próprio cliente, comprovando a titularidade pelo documento | `AcompanhamentoController` (`/api/public/**`) |

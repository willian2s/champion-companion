# 031 — Redesign visual integral baseado no Google Health/Fitbit

- **Status geral:** in_progress
- **Spec:** [031-redesign-visual-integral-google-health.md](../../specs/031-redesign-visual-integral-google-health.md)
- **Progresso:** 5/11 subtarefas concluídas

## Objetivo

Reconstruir apresentação visual do Champion Companion com fidelidade máxima à
interface observada de `com.fitbit.FitbitMobile`, mantendo features, dados,
contratos, privacidade e identidade não proprietária do app. Barra inferior terá
somente Home e Saúde; rotas restantes serão contextuais.

## Checklist

- [x] [031-01-auditar-referencia-e-baseline.md](031-01-auditar-referencia-e-baseline.md)
- [x] [031-02-registrar-supersessao-e-decisao.md](031-02-registrar-supersessao-e-decisao.md)
- [x] [031-03-evoluir-fundacao-visual.md](031-03-evoluir-fundacao-visual.md)
- [x] [031-04-reorganizar-shell-e-navegacao.md](031-04-reorganizar-shell-e-navegacao.md)
- [x] [031-05-redesenhar-home.md](031-05-redesenhar-home.md)
- [ ] [031-06-redesenhar-saude-e-metricas.md](031-06-redesenhar-saude-e-metricas.md)
- [ ] [031-07-redesenhar-exercicios-e-calendario.md](031-07-redesenhar-exercicios-e-calendario.md)
- [ ] [031-08-redesenhar-perfil.md](031-08-redesenhar-perfil.md)
- [ ] [031-09-redesenhar-dispositivo.md](031-09-redesenhar-dispositivo.md)
- [ ] [031-10-redesenhar-configuracoes.md](031-10-redesenhar-configuracoes.md)
- [ ] [031-11-consolidar-validacao-e-revisao.md](031-11-consolidar-validacao-e-revisao.md)

## Observações

- Ticker `031` escolhido porque auditoria encontrou `029` e `030` reservados em
  trabalhos/branches existentes; nenhum arquivo `031` foi sobrescrito.
- Spec 024 e ADR-023 permanecem como histórico supersedido.
- Evidência sem sanitização não será versionada; regra de privacidade prevalece.
  Exceção temporária permite somente inspeção local no aparelho autorizado,
  durante a sessão, sem envio ao contexto, cloud ou logs.
- Material não sanitizado, quando indispensável para o layout, deve ser apagado
  ao fim de cada sessão. `031-11` deve confirmar limpeza de todas as sessões;
  ausência de confirmação bloqueia encerramento e exige rollback ao baseline
  sanitizado.
- A revisão hierárquica considerou a exceção compatível apenas por ser local,
  efêmera e sem transporte de dados; `AGENTS.md` e ADR-004 continuam
  prevalecendo. Conflito impeditivo deve ficar registrado em `Observações`.
- Primeira subtarefa elegível: `031-01`.
- `031-01` foi concluída após revisão independente aprovada com ressalvas não
  bloqueantes. Reexecução ADB sanitizada em `SM-S901E` confirmou pacote, versão,
  Activity e estrutura parcial; superfícies não alcançadas permanecem `N/A`;
  estado da Activity retornou à Home e nenhuma configuração exigiu restauração.
  Após a conclusão de `031-01`, o progresso registrado era `1/11`.
- `031-02` foi concluída em escopo documental: Spec 024 e ADR-023 permanecem
  preservadas como histórico, com supersessão limitada à direção visual e à
  organização/apresentação da navegação. ADR-028 foi confirmada como `aceita`,
  substituta de ADR-023 e vinculada à Spec 031. A primeira revisão independente
  identificou marcação antecipada de conclusão; após correção, a segunda revisão
  foi aprovada em 2026-09-03. `031-03` foi concluída após implementação
  aditiva e opt-in da fundação visual, com revisão independente aprovada em
  2026-09-03. `031-04` foi concluída após reorganização do shell, rotas
  contextuais, preservação do saver/back stack e revisão independente aprovada.
  Smoke ADB sanitizado confirmou os fluxos principais e restaurou configurações
  do aparelho. Progresso atual: `4/11`.
- `031-05` teve ajuste visual implementado em `HomeScreen.kt` e foi concluída
  após revisão independente aprovada. O dashboard usa a geometria sanitizada
  observada, sem alegar paridade de conteúdo além da evidência disponível.
- `031-05` foi refinada conforme inspeção estrutural da Home de referência: card
  maior de Passos e linhas laterais para Exercícios, Distância e Freq. cardíaca.
  Calorias foi substituída por frequência cardíaca e a fonte de dados foi
  removida por solicitação explícita.
  Contagem semanal de exercícios com meta 4 ficou bloqueada por ausência de
  contrato e por estar fora do escopo visual; nenhuma contagem semanal sintética
  foi criada.
  Cards permanecem clicáveis sem setas visuais, com navegação para os detalhes
  existentes. Link de Calendário foi removido da Home por solicitação explícita;
  pull-to-refresh foi preservado com contêiner rolável compatível com o gesto.
  Estados `Loading`/`Empty` usam a mesma composição com zeros visuais; erros
  acionáveis continuam preservados. Card inferior aparece somente para revisão
  pendente. `031-05` foi reaberta e concluída após correção da paleta
  monocromática: toda composição usa azul próprio Champion, com trecho não
   concluído de Passos ligeiramente mais escuro. Revisão independente aprovada.
   Progresso atual: `5/11`.
  - `031-06` permanece em andamento: após solicitação explícita, Saúde voltou ao
   estilo lista anterior, mantendo o header novo, card colorido de Atividades no
   topo, seguido pelas quatro métricas locais, sem link para Calendário, que
   permanece dentro de Exercícios. Distância está sendo alinhada ao padrão
   Google Health: `Semana` diária, `M` diário no gráfico e semanal na lista,
   `3M`/`A` mensal, com lacunas preservadas no gráfico e itens sem dados ocultos
   na lista. Checklist permanece pendente; task não será
   encerrada nesta etapa. A imagem local de referência não será versionada.
   O ajuste posterior preservou a Semana civil, adotou o mês atual até hoje para
     `M`, `3M` e `A` com o mês atual parcial mais os meses civis anteriores, e
     separou semanas civis por mês. A Semana usa domingo–sábado, M usa labels
     `1, 5, 10, 15, 20, 25` e último dia no gráfico, 3M usa abreviações mensais
     e A usa somente iniciais.
    Um crash causado por
    formatter vazio no Vico foi corrigido com `ItemPlacer` explícito; smoke ADB
    confirmou abertura, alinhamento da Semana/3M, label `S` e filtros
    centralizados. Progresso permanece `5/11` até revisão independente.
   Durante refinamento posterior, os cards `Resumo atual`, `Importar histórico
   D1` e `Medição manual` foram removidos somente da tela de frequência
   cardíaca, conforme solicitação explícita; pull-to-refresh e contratos locais
   permanecem preservados. Revisão independente ainda pendente.

   Em refinamento adicional, `Hoje` passou a usar 144 buckets de 10 minutos,
   bandas visuais, tempo por faixa e faixas de frequência; `Semana`, `M`, `3M`
   e `A` passaram a usar barras, médias e intervalos civis equivalentes à
   Distância. Atividades do relógio ficam fora da população visual. Faixas são
   display-only porque idade não existe no modelo. Progresso permanece `5/11`;
   revisão independente pendente.

 - Em 2026-09-04, a implementação parcial de `031-06` foi corrigida após falha
   de compilação: períodos `LAST_90_DAYS`/`LAST_365_DAYS`, resolução `MONTHLY` e
   pontos BPM nulos não eram tratados em todos os consumidores. A correção ficou
   restrita ao modelo já alterado, renderer de histórico e labels da tela; não
   adicionou zonas ou listas novas. Compilação Kotlin Debug e regressão focada de
   buckets mensais passaram.

 - Em 2026-09-04, as correções solicitadas de `031-06` passaram a consultar `M`
   no mês civil atual até hoje para frequência cardíaca e distância, mantendo
   `3M`/`A` nos meses fechados anteriores. Listas semanais e eixos diários foram
   recortados ao intervalo disponível, sem perder lacunas; descrições de `M`
    deixaram de mencionar mês anterior fechado. O hub Saúde usa snapshot in-memory
    fixo de `HeartRateHistoryPeriod.TODAY` para não variar com o período do detalhe,
    e distância continua baseada em `todaySummary`. Compilação Kotlin Debug e lint Debug
    passaram; revisão independente e smoke final permanecem pendentes. Progresso:
    `5/11`.

 - Em 2026-09-04, correção delimitada do hub Saúde passou a observar em paralelo
   um snapshot in-memory fixo de `HeartRateHistoryPeriod.TODAY`. O valor de
   Frequência cardíaca voltou a preservar mínimo/máximo diário e deixou de usar
   `state.history` selecionado; gráficos, listas e períodos do detalhe não foram
   alterados. `git diff --check` e compilação Kotlin Debug passaram. Revisão
   independente e smoke visual permanecem pendentes.

 - Em 2026-09-04, validação posterior passou com `lint`, `assembleDebug`,
   `assembleRelease` e `installDebug`; smoke ADB sanitizado alternou `D`, `S`,
   `M`, `3M` e `A` sem crash. `031-06` continua pendente até revisão
   independente e inspeção visual final de mês atual e hub fixo. Progresso:
   `5/11`.

 - Em 2026-09-04, por solicitação explícita, FC válida de atividades do relógio
   passou a compor gráfico, médias, extremos e listas de frequência cardíaca.
   Persistência e contratos existentes foram reutilizados. `031-06` permanece
   pendente até validação e revisão independente; progresso: `5/11`.

 - Em 2026-09-04, build completo, instalação Debug e smoke ADB passaram após a
   inclusão de FC de exercícios; os cinco filtros foram alternados sem crash.
   Não foi fabricada amostra de exercício para teste visual. `031-06` permanece
   pendente até revisão independente e confirmação com atividade real; progresso:
   `5/11`.

 - Em 2026-09-04, o eixo Y dos gráficos diários de FC passou a usar baseline
   `0 BPM`, mantendo barras encostadas ao eixo XY sem alterar dados ou lacunas.
   `031-06` permanece pendente até revisão independente; progresso: `5/11`.

 - Em 2026-09-04, compilação, lint, builds Debug/Release, instalação e smoke
   ADB passaram após ajuste do baseline diário; abertura de FC e filtro `M`
   ficaram sem crash. Revisão visual independente permanece pendente; progresso:
   `5/11`.

 - Em 2026-09-04, tooltip compartilhado passou a mostrar somente valor e unidade;
   prefixos de série/data longa foram removidos, eliminando legenda textual dos
   gráficos. `031-06` permanece pendente até revisão independente; progresso:
   `5/11`.

 - Em 2026-09-04, FC diária deixou de usar buckets de 10 minutos e passou a
   exibir amostras armazenadas nos timestamps originais; o label `0` foi alinhado
   à borda inicial do gráfico. Indicador por faixa agora conta amostras sem
   duração sintética. `031-06` permanece pendente; progresso: `5/11`.

 - Em 2026-09-04, remoção final eliminou somente legendas posicionadas abaixo
   dos gráficos: legenda manual de Passos e lista visual de faixas da FC;
   rótulos internos de referência e marcações `Z1`–`Z4` foram preservados.
   Tooltips mostram somente valor/unidade. Acessibilidade foi preservada.
   `031-06` permanece pendente; progresso: `5/11`.

 - Em 2026-09-04, escopo da remoção foi corrigido: somente conteúdo abaixo dos
   gráficos foi removido. Rótulos internos, tooltips e linhas de referência
   permanecem. Dump ADB confirmou ausência dos textos de legenda inferior;
   progresso continua `5/11`.

 - Em 2026-09-04, foram removidos os resumos acessíveis visíveis abaixo dos
   gráficos; descrições semânticas internas foram preservadas para acessibilidade.
   `031-06` permanece pendente; progresso: `5/11`.

 - Em 2026-09-04, validação final do ajuste de resumo acessível passou com
   compile, lint, builds Debug/Release e instalação Debug. Semântica TalkBack foi
   preservada; `031-06` permanece pendente de revisão independente; progresso:
   `5/11`.

 - Em 2026-09-04, dados por zona da FC diária foram restaurados. Removido apenas
   texto visível `Resumo acessível`; contagens `Zona 1`–`Zona 4` continuam na
   seção de leituras. Smoke ADB confirmou zona presente e resumo ausente;
   progresso: `5/11`.

   - Em 2026-09-04, remoção efetiva eliminou rótulos das linhas de referência no
    renderer compartilhado, além das legendas manuais de Passos e faixas da FC.
    `031-06` permanece pendente até validação/revisão independente; progresso:
    `5/11`.

   - Em 2026-09-08, correção delimitada alinhou `3M` e `A` de frequência
    cardíaca e distância ao mês atual como referência: mês atual parcial mais,
    respectivamente, dois e onze meses civis anteriores, com limite exclusivo
    em amanhã. Acessibilidade, descrições dos gráficos e headline de FC foram
   atualizados; contratos e lacunas foram preservados. `031-06` permanece
   pendente de revisão independente e smoke visual; progresso: `5/11`.
   - Em 2026-09-08, revisão independente da correção mensal manteve `031-06`
     em `in_progress`: identificou divergências preexistentes entre aceite e
     composição atual sobre ação explícita de medição manual e entrada direta
     Saúde → Calendário. Esses pontos não foram alterados na correção delimitada
     de `3M`/`A` e exigem decisão/escopo próprio antes do fechamento.
    - Reavaliação incremental aprovou isoladamente a janela mensal e sua
       descrição acessível; a subtarefa geral continua pendente pelos bloqueios
       preexistentes acima.

    - Em 2026-09-08, `Leituras por faixa` da FC diária passou de contagem de
      amostras para duração observada agregada por faixa. O cálculo ordena
      leituras válidas, atribui cada intervalo positivo à faixa do ponto inicial,
       ignora nulos, deltas não positivos e o último ponto, e anuncia mesmas
       durações na acessibilidade. `031-06` permanece `in_progress`; revisão
       independente e smoke visual amplo continuam pendentes.
      - Revisor identificou e aprovou correção de lacunas nulas atravessadas;
        duração agora só acumula pares consecutivos com BPM válido. Alteração
        incremental aprovada; `031-06` permanece pendente pelos bloqueios amplos.
      - Em 2026-09-08, Passos passou a usar semana civil, mês atual até hoje e
        janelas `3M`/`A` ancoradas no mês atual, com agregação semanal/mensal,
        labels `D S T Q Q S S`, marcos diários e iniciais mensais. Lacunas ficam
        no gráfico e buckets sem valor ficam fora da lista. Revisão independente
        e smoke visual continuam pendentes; progresso: `5/11`.
      - Revisão independente encontrou texto de acessibilidade desatualizado em
        `3M`/`A`; correção alinhou anúncio ao mês atual até hoje mais dois/onze
        meses anteriores. Checklist segue `[ ]`; progresso: `5/11`.
      - Em 2026-09-08, os cards `Resumo atual` e `Resumo do período` foram
        removidos somente da tela de Passos; histórico, gráficos, listas, filtros
        e estados permanecem. Checklist segue `[ ]`; progresso: `5/11`.
      - Revisão independente aprovou com ressalvas o escopo de Passos. Smoke com
        dados representativos continua pendente por ausência de fixture
        versionada; checklist permanece `[ ]`; progresso: `5/11`.

      - Em 2026-09-08, verificação independente confirmou que a decisão vigente
        não foi revertida: `3M` e `A` incluem o mês civil atual parcial até hoje,
        além de, respectivamente, dois e onze meses anteriores. Registros
        anteriores que mencionam somente meses fechados são históricos e foram
        supersedidos pela correção de janela mensal. `031-06` permanece pendente
        pelos bloqueios amplos já registrados; progresso: `5/11`.

      - Em 2026-09-08, Peso foi analisado na rota correta do app de referência
        (`Saúde → Peso`), confirmando estrutura sanitizada com 77 nós, 15 ações,
        uma rolagem vertical e filtros `3M`/`A`. A implementação de Peso usa
        gráfico de linha, janela civil atual parcial mais dois/onze meses
        anteriores, origem, unidade, acessibilidade e lacunas sem interpolação.
        Correções de fuso local e mensagem de lista vazia foram validadas com
        compile, lint, builds, instalação e smoke sem crash. Peso foi aprovado
        isoladamente com ressalva de cobertura de dados; checklist amplo segue
        `[ ]` pelos bloqueios preexistentes; progresso: `5/11`.

      - Após relato de gráficos quebrados, o eixo X de Peso foi corrigido para
        índices compactos compatíveis com renderer sem rolagem; lacunas seguem
        como pontos nulos e labels mensais preservam seus meses. `Saúde → Peso`
        alternou `A` e `3M` no smoke sanitizado sem crash; checklist de `031-06`
        permanece `[ ]`; progresso: `5/11`.

      - Por decisão posterior, o gráfico de Peso passou de linha para barras e
        passou a reutilizar `ChampionBarChart`, mantendo filtros `3M`/`A`, dados
        reais, unidade, origem, acessibilidade, lacunas e lista. Checklist de
        `031-06` permanece `[ ]`; progresso: `5/11`.

      - As colunas de Peso foram então ajustadas para agregado mensal, como nos
        gráficos existentes: média dos registros válidos em kg, três meses em
        `3M`, doze em `A`, e meses sem dados preservados como lacunas. Checklist
        de `031-06` permanece `[ ]`; progresso: `5/11`.

      - A lista de Peso foi alinhada à lista filtrada de FC: somente meses com
        valor, média mensal em kg e contador de períodos são exibidos. Checklist
        de `031-06` permanece `[ ]`; progresso: `5/11`.

      - Em 2026-09-08, a implementação delimitada de Peso adicionou seleção
        preservada na Activity para `3M`/`A`, filtro local por mês civil atual até
        hoje mais dois/onze meses anteriores, gráfico de linha com kg, origem,
        acessibilidade e lacunas sem interpolação, além de lista filtrada. O
        checklist de `031-06` permanece pendente por revisão independente, smoke
        visual e bloqueios amplos já registrados; progresso: `5/11`.

      - Em 2026-09-08, a revisão independente ampla reprovou o fechamento de
        `031-06`: medição manual não está explícita na UI, D1 não possui CTA e
        feedback visual completo, falta entrada Saúde → Calendário e a origem de
        FC no hub pode ser enganosa. Também foram apontadas listas sem origem por
        item, `WeightHistoryChart.kt` não rastreado e documentação histórica de
        Peso ainda citando linha. Checklist permanece `[ ]`; progresso: `5/11`.

      - Após a revisão, foram confirmadas decisões de produto: medição manual
        não compõe mais a superfície visual de Saúde; D1 usa somente
        pull-to-refresh com ação acessível nativa; Calendário fica restrito ao
        fluxo de Exercícios; e a origem do hub pode exibir relógio/dispositivo
        associado. Spec e aceite de `031-06` foram alinhados; a subtarefa segue
        `in_progress` até nova revisão independente e integração do arquivo
        `WeightHistoryChart.kt` no conjunto da entrega. Checklist permanece
        `[ ]`; progresso: `5/11`.

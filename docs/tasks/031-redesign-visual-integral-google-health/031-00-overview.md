# 031 — Redesign visual integral baseado no Google Health/Fitbit

- **Status geral:** in_progress
- **Spec:** [031-redesign-visual-integral-google-health.md](../../specs/031-redesign-visual-integral-google-health.md)
- **Progresso:** 9/11 subtarefas concluídas

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
- [x] [031-06-redesenhar-saude-e-metricas.md](031-06-redesenhar-saude-e-metricas.md)
- [x] [031-07-redesenhar-exercicios-e-calendario.md](031-07-redesenhar-exercicios-e-calendario.md)
- [x] [031-08-redesenhar-perfil.md](031-08-redesenhar-perfil.md)
- [x] [031-09-redesenhar-dispositivo.md](031-09-redesenhar-dispositivo.md)
- [ ] [031-10-redesenhar-configuracoes.md](031-10-redesenhar-configuracoes.md)
- [ ] [031-11-consolidar-validacao-e-revisao.md](031-11-consolidar-validacao-e-revisao.md)

## Observações

- Ticker `031` escolhido porque auditoria encontrou `029` e `030` reservados em
  trabalhos/branches existentes; nenhum arquivo `031` foi sobrescrito.
- Spec 024 e ADR-023 permanecem como histórico supersedido.
- Evidência sem sanitização não será versionada; regra de privacidade prevalece.
- Bloqueio anterior de `031-07` sobre retry/cancelamento foi resolvido por
  decisão explícita: estados terminais exibem somente Snackbar de sucesso/erro.
- A diretriz explícita posterior de 2026-09-10 supersede o bloqueio anterior:
  estados terminais de importação devem exibir somente uma Snackbar temporária
  de sucesso ou erro, sem retry, cancelamento, resumo ou dados.

- Em 2026-09-10, `031-07` recebeu feedback terminal mínimo: uma única
  Snackbar temporária para sucesso (`Completed`, `Partial`, `Empty`) ou erro
  (`Failed`, `Cancelled`), sem ações ou dados. Lint, builds, instalação Debug e
  smoke de abertura passaram; uma falha concorrente transitória do Gradle foi
  corrigida por rerun sequencial. Subtarefa concluída após validação funcional
  confirmada pelo usuário; progresso `7/11`.
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

      - Em 2026-09-08, nova revisão independente aprovou `031-06` com
        ressalvas, sem bloqueador funcional após as decisões de produto. O
        arquivo `WeightHistoryChart.kt` está rastreado no commit `367ff96` e o
        diff do commit passou. Permanecem ressalvas de evidência física,
        TalkBack e origem não repetida nas listas agregadas. `031-07` ainda
        contém requisito histórico de Calendário via Home/Saúde e precisa ser
        alinhada em seu próprio escopo. Checklist de `031-06` permanece `[ ]`;
        progresso: `5/11`.

      - Em 2026-09-08, o requisito de `031-07` foi alinhado às decisões vigentes:
        Calendário é acessível somente pelo fluxo de Exercícios; Home e Saúde não
        recebem integração direta. Registros anteriores que citam entrada por
        Home/Saúde permanecem como histórico supersedido. `031-06` segue
        `in_progress`, sem alteração no progresso `5/11`.

      - Em 2026-09-08, `031-06` foi concluída após revisão independente aprovada
         com ressalvas e sem bloqueadores funcionais. O item foi marcado `[x]` e
         o progresso recalculado para `6/11`. Permanecem riscos não bloqueantes de
         cobertura física de dados representativos, TalkBack completo e origem
         não repetida em listas agregadas. Nenhuma próxima subtarefa foi iniciada.

      - Em 2026-09-08, a inspeção física autorizada de `com.fitbit.FitbitMobile`
       alcançou Exercícios. Foram confirmados `Semana`, `Mês`, `Esta semana`,
       `Este mês`, `Dias com exercícios`, `Total por semana`, gráfico acessível
       de dias, `Hoje` e ação `Registrar atividade`; a referência mostrou
       denominador visual de quatro dias na semana. A sessão foi somente leitura,
       sem operação sensível, e dump temporário foi apagado e confirmado ausente.
          `031-07` permanece `[ ]`; progresso continua `6/11`.

        - Em 2026-09-09, bloco `Status e origem` foi removido. Header passou a
          exibir `data • hora • origem • status`; demais ações operacionais foram
          preservadas. Vico segue removido; gráfico D usa linha e duração real.
          Validações e instalação passaram; revisão independente aprovou sem
          blockers. `031-07` permanece aberta por solicitação do usuário;
          progresso `6/11`.

      - Em 2026-09-08, o requisito de `031-07` foi replanejado por confirmação
        do usuário: o calendário mensal deve substituir gráfico mensal dentro da
        aba `Mês` de Exercícios, sem página separada como entrada principal.
        Spec 031 e ADR-028 foram alinhadas; `CalendarScreen` permanece somente
        compatível com estado legado/restaurado. Implementação foi ajustada e
        smoke confirmou calendário embutido e resumo semanal; revisão independente
        ainda está pendente. `031-07` permanece `[ ]`; progresso continua `6/11`.

      - Em 2026-09-08, `031-07` recebeu refinamento visual adicional: filtro por
        status foi movido para abaixo do gráfico na aba `Semana`; na aba `Mês`, o
        filtro e a lista individual foram removidos, mantendo somente calendário
        e agregados semanais. `Mês` normaliza status para `Todos`; implementação e
        validação seguem em andamento. `031-07` permanece `[ ]`; progresso
        continua `6/11`.

      - Em 2026-09-08, a lista semanal de `031-07` foi alinhada ao estilo de
        listas de Saúde, com cards de atividade equivalentes. O agregado mensal
        passou a ocultar semanas sem exercícios; a aba `Mês` mantém somente
        calendário e totais. Smoke final confirmou esses estados após reconexão
        do aparelho ADB. Revisão independente ainda pendente; `031-07` permanece
        `[ ]`; progresso continua `6/11`.

      - Em 2026-09-08, `031-07` recebeu ajuste final conforme a lista mensal de
        Frequência cardíaca: cards semanais mostram título/distância, horário e
        duração sem origem/seta; agregados mensais usam linhas simples e omitem
        semanas sem exercícios. `031-07` permanece `[ ]`; progresso continua
        `6/11`, aguardando revisão independente.

      - Em 2026-09-08, a lista semanal foi alinhada à referência observada:
        agrupamento por dia (`Hoje`), distância sem rótulo, duração junto ao
        horário, sem origem e sem seta. Smoke confirmou também que visão `Mês`
        mantém apenas calendário e agregados sem semanas zeradas. `031-07`
        permanece `[ ]`; progresso continua `6/11`, aguardando revisão
        independente.

      - Em 2026-09-08, nova diretriz confirmou que o filtro `Mês` deve manter o
        calendário contextual dentro de Exercícios, junto da lista de cartões
        semanais com período à esquerda, total de atividades e tipos de exercício
        à direita, além dos controles `Dias com exercícios`, `Duração` e
        `Distância`. `031-07` foi alinhada à diretriz; os cards individuais
        retornaram à cor original, altura compacta e bordas distintas por status.
        O checklist permanece `[ ]`, progresso `6/11`, aguardando revisão
        independente.

      - Em 2026-09-08, diretriz complementar restringiu Calendário ao domínio de
        Exercícios: sem indicadores de passos, frequência cardíaca, peso ou Health
         Connect e sem botão `Abrir dia`. Setas de `Mês` alteram calendário,
         resumo e lista; setas de `Semana` alteram gráfico e lista. Setas de
       `Mês` e `Semana` não permitem períodos futuros. `031-07` permanece
         `[ ]`; progresso `6/11`, aguardando revisão independente.

       - Em 2026-09-09, a atividade da referência foi finalmente aberta pelo
         fluxo `Exercícios`, em inspeção somente leitura. Foram confirmados
         título/tipo, data/hora/origem, estatísticas de duração, distância,
         etapas, ritmo médio e frequência cardíaca média, seção de faixas de FC
         e gráfico cardíaco acessível. A diretriz subsequente determinou manter
         as mesmas informações e dados na nossa tela, enriquecendo somente os
         gráficos. `031-07` adicionou gráficos de ritmo por intervalos reais e
         de parciais persistidas, preservando cadência, FC, métricas, estados,
         revisão, calibração e ações existentes. Smoke confirmou o detalhe
         local e os novos gráficos; revisão independente ainda está pendente.
         Checklist permanece `[ ]`; progresso continua `6/11`.

       - Em 2026-09-09, nova diretriz corrigiu o detalhe de Exercícios: ele deve
         copiar literalmente a composição do detalhe Google Health/Fitbit, com
         header simples, tipo, data/hora/origem, cinco métricas sem cards,
         `Faixas de frequência cardíaca`, média e gráfico cardíaco. Gráfico foi
         reduzido para `160 dp`, mantendo leitura completa na viewport; série
         canônica, ritmo, cadência e parciais não são renderizados por enquanto.
         Fluxos operacionais continuam preservados após a área de referência.
         Compile, lint e builds passaram; instalação/smoke final estão bloqueados
          temporariamente por ausência de dispositivo ADB. `031-07` permanece
          `[ ]`; progresso continua `6/11`.

       - Segunda revisão encontrou três pontos adicionais e eles foram corrigidos:
         seção de FC totalmente omitida sem amostras, ritmo de relógio limitado
         a métrica canônica disponível e metadata usando origem real. Validações
         de diff, compile, lint e builds passaram; instalação/smoke físico seguem
         bloqueados por ausência de dispositivo ADB. Nova revisão ainda pendente.
         `031-07` permanece `[ ]`; progresso continua `6/11`.

       - Novo bloqueio em 2026-09-09: após reconexão informada pelo usuário,
         `./gradlew installDebug --no-daemon` falhou com `No connected devices`
         e `adb devices -l` continuou sem listar aparelho. Código e builds estão
         aprovados; smoke físico do detalhe compacto não pode ser confirmado.
         Bloqueio registrado na subtarefa; `031-07` permanece `[ ]` e progresso
         continua `6/11`.

       - Revisão final aprovou código sem blockers após guarda central de
         distância canônica exigir estado `AVAILABLE`. Header, cinco métricas,
         origem real, omissão segura e gráfico compacto de `160 dp` foram
         confirmados. Builds passaram; instalação e smoke físico continuam
         bloqueados por ADB sem dispositivo. `031-07` permanece `[ ]`; progresso
         continua `6/11` até validação física.

       - Após reconexão, `adb devices -l` identificou `SM-S901E`; `installDebug`
         passou. Smoke Home → Exercícios → semana → atividade confirmou header,
         cinco métricas, seção de FC, gráfico acessível compacto de `160 dp`,
         dados persistidos e ausência de gráficos extras. Temporários foram
         removidos e processo permaneceu ativo sem crash. Restam validações de
         fonte ampliada, landscape, temas e TalkBack; `031-07` permanece `[ ]` e
         progresso continua `6/11`.

       - Em 2026-09-09, após comparação por screenshot, alterações fora do
         gráfico foram revertidas. Formatação compacta dos dados foi mantida;
         somente gráfico cardíaco recebeu Canvas compacto de `128 dp`, zonas
         coloridas, linhas pontilhadas, escala inspirada na referência e amostras
         persistidas. Compile, lint, builds e instalação passaram; screenshot
         final após esta última troca ainda está pendente. `031-07` permanece
         `[ ]`; progresso continua `6/11`.

       - Diretriz final confirmou uso do gráfico de FC período `D` do próprio
         Champion Companion. Detalhe reutiliza componente intraday, troca pontos
         por linha e usa duração real da atividade no eixo X; Saúde mantém
         comportamento anterior. Smoke físico confirmou bandas, linha, largura
         do gráfico e `44` amostras reais; validações completas passaram.
         Segundo gráfico não foi criado porque “vico” não identifica referência.
         `031-07` permanece `[ ]`; progresso continua `6/11`.

       - Gráfico Vico foi criado temporariamente para comparação e removido após
         pedido explícito. Estado vigente usa somente componente do período D de
         Frequência cardíaca, com pontos trocados por linha e eixo X pela duração
         real da atividade. Builds, instalação e smoke passaram; revisão final
         permanece pendente. `031-07` permanece `[ ]`; progresso `6/11`.

       - Correção final de 2026-09-09: gráfico do detalhe mantém visual do
         período D, mas eixo X agora usa duração real da atividade, do início ao
         fim, em vez de 24 horas. Saúde preserva janela de 24 horas e pontos;
         detalhe usa linha. Compile, lint, builds e instalação passaram.
         Segundo smoke visual após esta correção ainda pendente. `031-07`
         permanece `[ ]`; progresso continua `6/11`.

       - Diretriz final de 2026-09-09 escolheu gráfico de `Frequência cardíaca`
         do próprio Champion Companion, período `D`. Detalhe agora reutiliza
         componente intraday existente e troca somente pontos por linha; bandas,
         zonas, escala, acessibilidade e dados permanecem os mesmos. Header,
         métricas e metadata não foram alterados neste ajuste. Compile, lint,
         builds, instalação e smoke passaram; temporários foram removidos.
         Segundo gráfico aguarda identificação de “vico”. `031-07` permanece
         `[ ]`; progresso continua `6/11`.

       - Revisão independente identificou cinco riscos no detalhe: tipo
         personalizado no header, conteúdo de intervalos/parciais ainda visível,
         etapas phone sem disponibilidade explícita, métricas canônicas
         implausíveis e seção de FC sem amostras. Correções foram aplicadas,
         incluindo gráfico compacto com `160 dp` e série inteira sem rolagem.
         Compile, lint e builds passaram novamente; instalação e smoke físico
          seguem bloqueados por `adb devices -l` sem dispositivos. A nova revisão
          permanece pendente. `031-07` permanece `[ ]`; progresso continua `6/11`.

        - Comparativo Vico removido em 2026-09-09 por pedido explícito. Permanece
          somente gráfico intraday do período D, com linha e eixo pela duração
         real da atividade. Header, métricas e demais UI preservados. Diff,
         compile, lint, builds e instalação passaram. `031-07` permanece `[ ]`;
         progresso continua `6/11`.

         - Registro histórico de 2026-09-09: bloco `Status e origem` e seus
           helpers foram removidos; status ficou na metadata do header como
           `data • hora • origem • status`. A subtarefa foi posteriormente
           reaberta por refinamentos solicitados no detalhe; `031-07` permanece
           `[ ]` e o progresso correto continua `6/11`.

         - Em 2026-09-10, os formulários de `Revisão e calibração` passaram a ser
           conteúdo direto de modal rolável aberto pelo menu overflow, sem card
           aninhado. `Workflow e sincronização` recebeu o mesmo tratamento, e
           o texto instrucional sob o gráfico foi removido. Callbacks, estados,
           validações e sync explícito foram preservados; compile, lint, builds,
           instalação e `git diff --check` passaram. Revisão independente foi
           solicitada, mas permanece pendente; `031-07` continua `[ ]` e o
           progresso `6/11`.

          - Em 2026-09-10, por solicitação explícita, o detalhe de Exercícios
            manteve o gráfico cardíaco atual e ganhou gráfico cardíaco comparativo
            com amostras persistidas. Ritmo, frequência cardíaca e cadência agora
            exibem informações de resumo abaixo dos gráficos, sem alterar dados,
            contratos ou demais superfícies. Compile, lint e builds passaram;
            instalação/smoke ficaram pendentes por indisponibilidade ADB. Revisão
            independente e validação visual ampla permanecem pendentes. `031-07`
            continua `[ ]`; progresso `6/11`.

          - Após nova diretriz de 2026-09-10, o detalhe manteve gráfico cardíaco
            atual e recebeu gráfico cardíaco de comparação visual, sem alegar
            segunda série. Informações de ritmo, FC e cadência passaram a aparecer
            sob seus gráficos usando métricas persistidas; lacunas e duração real
            foram preservadas. Compile, lint e builds passaram. Instalação/smoke
            foram bloqueados novamente porque `adb devices -l` não encontrou
            dispositivo. `031-07` continua `[ ]`; progresso `6/11`.

          - Em 2026-09-10, o usuário escolheu somente o gráfico novo entre as duas
            versões de frequência cardíaca e pediu redução dos valores. O gráfico
            anterior foi removido; Ritmo, Cadência e Parciais permanecem. Valores
            do resumo e das informações sob gráficos foram reduzidos para
            `titleMedium`, preservando dados e unidades. Compile, lint, builds,
            instalação Debug e abertura somente leitura passaram; revisão
            independente e validação visual ampla seguem pendentes. `031-07`
            continua `[ ]`; progresso `6/11`.

          - Após escolha do gráfico novo, somente essa versão permanece entre os
            gráficos de FC; Ritmo, Cadência e Parciais seguem renderizados. Valores
            do resumo e dos dados abaixo dos gráficos foram reduzidos para
            `titleMedium`; amostras, lacunas e duração efetiva foram preservadas.
            Compile passou; revisão independente e smoke visual final continuam
            pendentes. `031-07` segue `[ ]`; progresso `6/11`.

          - Smoke ADB de 2026-09-10 confirmou `Exercícios`, ação acessível
            `Voltar para Home`, toque e retorno para Home em aparelho Debug.
            Nenhuma operação de dados foi iniciada. Fonte ampliada, landscape,
            temas, TalkBack e revisão final permanecem pendentes. `031-07`
            continua `[ ]`; progresso `6/11`.

          - Em 2026-09-10, `ExercisesHeader` recebeu botão voltar acessível no
            shell principal, e `ChampionNavigation` passou `onBackRequested` para
            `AppDestination.Exercises`. Compile, lint, builds e instalação Debug
            passaram; smoke visual ficou pendente porque o aparelho estava
            bloqueado. `031-07` continua `[ ]`; progresso `6/11`.

          - Em 2026-09-10, Exercícios recebeu menu contextual `Mais opções` com
            Importar treinos, Status Health Connect e PoC técnica. Os três blocos
            saíram da página; loading horizontal acessível permanece visível
            durante importação. Modais preservam ações explícitas e feedback
            secundário. Build, lint, instalação e smoke ADB sanitizado passaram;
            nenhuma operação de dados foi iniciada. `031-07` continua `[ ]`;
            progresso `6/11`.

          - Em 2026-09-10, `Importar treinos` passou a ser ação direta no menu,
            sem modal ou dados; o loading horizontal permanece na página para
            sinalizar execução. Smoke confirmou menu e ações sem tocar importação;
            build, lint e instalação passaram. Revisão independente e operação
            real do loading permanecem pendentes. `031-07` continua `[ ]`;
            progresso `6/11`.

           - Validação posterior da ação direta confirmou compile, lint e builds.
             Nova instalação foi bloqueada por `adb devices -l` sem dispositivo;
             nenhum dado foi alterado. `031-07` continua `[ ]`; progresso `6/11`.

           - Em 2026-09-10, a limpeza delimitada de `ExercisesScreen.kt` removeu UI morta
             de importação sem chamadas (`ExercisesImportDialog`, `ExercisesImportContent`,
             `ImportResultBanner`, `ImportErrorBanner` e `MetadataSection`), além de
             imports/helpers exclusivos. O indicador de progresso, helpers de estado e
             mensagem, diálogo/feedback Health Connect e callbacks foram preservados.
             `git diff --check` e `./gradlew app:compileDebugKotlin --no-daemon`
              passaram. `031-07` continua `[ ]`; revisão independente, smoke visual amplo,
              fonte ampliada, landscape, temas e TalkBack seguem pendentes.

            - Nova execução completa após ação direta passou em compile, lint e builds;
              `installDebug` ficou bloqueado por `adb devices -l` sem dispositivo.
              Nenhuma ação de importação ou alteração de dados foi realizada.

          - Em 2026-09-10, a tela de Frequência cardíaca recebeu menu contextual
            com `Atualizar histórico` e loading horizontal igual ao padrão da
            Home durante importação D1. Pull-to-refresh, ação acessível e
            bloqueios existentes foram preservados. `031-06` permanece concluída
            (`6/11`); revisão independente passou sem bloqueadores. Build/lint
            passaram; instalação e smoke ADB ficaram `N/A` por ausência de
            dispositivo conectado.

          - Após reconexão em 2026-09-10, `installDebug` passou no `SM-S901E`.
            Smoke somente leitura confirmou Saúde → Frequência cardíaca, menu
            `Mais opções`, item `Atualizar histórico`, retorno à tela e processo
            ativo sem `FATAL EXCEPTION`/`AndroidRuntime`. Nenhuma importação D1
            foi iniciada; loading em operação real, TalkBack, fonte ampliada,
            landscape e temas permanecem não exercitados.

          - Em 2026-09-10, validação física de `031-07` passou no `SM-S901E`:
            Exercícios abriu pela Home; menu exibiu Importar treinos, Status
            Health Connect e PoC técnica; `Mês` exibiu setas de calendário sem
            `Abrir dia`; `Semana` exibiu setas e `Dias com exercícios`; retorno
            para Home e processo ativo foram confirmados sem crash. PoC técnica
            foi aberta inadvertidamente ao tocar limite do menu e fechada sem
            iniciar rota/operação ou persistir dados. Importação real/loading,
             fonte ampliada, landscape, temas e TalkBack permanecem pendentes;
             `031-07` continua `[ ]` e progresso `6/11`.

            - Nova validação de `031-07` confirmou mudança de Semana/Mês por hash
              sanitizado, bloqueio de períodos futuros, resumo, lista e detalhe
              com retorno. Fonte `1.3x`, landscape, tema claro/escuro e TalkBack
              foram exercitados; configurações originais foram restauradas.
              Nenhuma operação de dados foi iniciada. Loading/importação real,
              status Health Connect e retry/cancelamento permanecem não
              exercitados; `031-07` continua `[ ]`, progresso `6/11`.

            - Em 2026-09-10, nova diretriz supersedeu a exigência histórica de
              affordances visíveis de retry/cancelamento: `ExercisesScreen` agora
              exibe somente uma Snackbar acessível, temporária e genérica na
              transição para estados terminais da importação. `Completed`,
              `Partial` e `Empty` são sucesso; `Failed` e `Cancelled` são erro.
              Sem modal, resumo, metadata, payload ou ação. Loading horizontal,
              menu e contratos permanecem preservados. `git diff --check` e
              `app:compileDebugKotlin` passaram; revisão independente, smoke de
              importação real e validação ampla continuam pendentes. `031-07`
               permanece `[ ]`, progresso `6/11`.

  - Em 2026-09-10, `031-08` foi concluída após aplicação da fundação visual opt-in
    ao Perfil. Header passou a usar composição do redesign, com retorno contextual
    e acesso a Configurações; cards, espaçamentos, loading e diálogos foram
    alinhados sem alterar edições, calibração, callbacks ou persistência. Lint,
    builds Debug/Release, instalação e smoke ADB sanitizado passaram; fonte
    ampliada, landscape e temas foram exercitados e restaurados. Revisão
    independente aprovou sem bloqueadores. Risco residual: TalkBack não foi
    exercitado explicitamente. Progresso: `8/11`.
   - A inconsistência documental de `031-07` foi resolvida em 2026-09-10: o
     cabeçalho agora declara `completed`, alinhado ao item `[x]` do checklist e às
     evidências de validação funcional já registradas.

  - Em 2026-09-10, `031-09` foi concluída após aplicação da fundação visual
    opt-in à tela de Dispositivo. Header contextual passou a exibir status do
    relógio e retorno, cards foram reorganizados sem bordas coloridas, metadados
    e estados assíncronos foram preservados, e a affordance existente foi
    identificada explicitamente como PoC BLE técnica de debug. Nenhum CTA ou
    `Intent` da PoC MapLibre foi adicionado ou movido. Lint, builds Debug/Release,
    instalação Debug, smoke ADB sanitizado, restauração de configurações e
    revisão independente passaram. Riscos residuais: TalkBack completo,
    operações BLE e screenshot versionado não foram exercitados/retidos.
    Progresso: `9/11`.

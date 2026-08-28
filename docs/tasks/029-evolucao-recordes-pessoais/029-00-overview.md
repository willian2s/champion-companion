# 029 — Evolução esportiva e recordes pessoais

- **Status geral:** `in_progress`
- **Spec:** [029-evolucao-recordes-pessoais.md](../../specs/029-evolucao-recordes-pessoais.md)
- **Progresso:** `8/10` subtarefas concluídas

## Objetivo

Transformar o histórico local já existente em uma leitura clara de evolução esportiva, com volume, consistência, tendências, comparação entre períodos e recordes pessoais factuais.

## Decisões centrais

- evolução é read model derivado; não materializar agregados em Room sem necessidade comprovada;
- primeira versão usa atividades `WATCH_D3_D4`;
- períodos iniciais: 7, 30 e 90 dias;
- filtros por tipo de treino preservam comparabilidade;
- gráficos usam `core/designsystem/chart` da Spec 028;
- recordes iniciais: maior distância, maior duração e maior número de passos em treino elegível;
- frequência cardíaca máxima e maior cadência não são tratados como recordes de performance;
- detecção de novo recorde acontece somente depois de persistência bem-sucedida da atividade em estado/métrica elegível;
- distância usada pelo recorde é a distância final persistida, incluindo distância manual de bicicleta ou calibração de esteira quando aplicável;
- resave sem mudança não repete alerta;
- importação de atividade antiga só gera destaque se ela virar o melhor valor atualmente conhecido;
- conquista pós-save é evento efêmero; recordistas atuais são recalculáveis a partir dos fatos locais.

## Checklist

- [x] `029-01` — auditar métricas, elegibilidade e pontos de persistência
- [x] `029-02` — criar contratos e read model de evolução
- [x] `029-03` — implementar períodos e filtros por esporte
- [x] `029-04` — implementar volume e consistência
- [x] `029-05` — implementar tendências e fórmulas agregadas
- [x] `029-06` — implementar comparação entre períodos
- [x] `029-07` — implementar read model e avaliador de recordes pessoais
- [x] `029-08` — criar tela Evolução e superfície de recordes
- [ ] `029-09` — integrar detecção e destaque de recorde pós-save
- [ ] `029-10` — validar, consolidar e fechar

## Dependências

```text
029-01
  ↓
029-02
  ↓
029-03
  ↓
029-04
  ↓
029-05
  ↓
029-06
  ↓
029-07
  ↓
029-08
  ↓
029-09
  ↓
029-10
```

## Estado atual

`029-01` concluída em `docs/integration/029-01-evolution-records-audit.md`.

`029-02` concluída com contratos neutros e read model local em
`core/model/EvolutionContracts.kt` e `data/LocalEvolutionReadModel.kt`.

`029-03` concluída com seleção/ordenação encapsulada no read model e tratamento
fail-closed para IDs duplicados.

Decisões factuais da auditoria:

- workflows elegíveis: `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED`;
- `PENDING_REVIEW`, `PENDING_CALIBRATION`, `DISCARDED` e `UNKNOWN` ficam fora;
- corrida/caminhada usam distância canônica ativa;
- esteira usa distância calibrada após conclusão;
- bicicleta usa distância manual opcional e não usa passos de performance;
- avaliação pós-save pode ocorrer após `submitReview`, `calibrateImportedActivity`, `applyReferenceDistanceCalibration` e rederivação/reprocessamento explícito quando a atividade final elegível realmente mudar;
- importação/reimportação e transições de sync não geram conquista;
- `AppDestination.Evolution` será subárvore de Exercícios, sem nova aba inferior;
- nenhuma migration Room é necessária neste desenho.

Próxima subtarefa elegível: `029-09`.

## Observações

- `029-02` preserva ausência e dados insuficientes sem convertê-los em zero;
- consulta completa de atividades é suficiente para o escopo atual, mas pode
  exigir projeção temporal limitada se o volume histórico crescer;
- leitura all-history de recordes e fórmula agregada de frequência cardíaca
  permanecem explicitamente nas subtarefas posteriores.
- `029-03` não deixou bloqueio adicional; nenhuma alteração de Room/DAO foi
  necessária.
- `029-04` concluiu agregados de volume, consistência e séries diárias sobre o
  snapshot factual.
- `029-05` concluiu tendências diárias com somas protegidas, BPM ponderado por
  amostras efetivas, cadência baseada em fatos D4 canônicos e pace/velocidade
  restritos a populações comparáveis.
- `029-06` concluiu comparação pura de volume entre snapshots do período atual e
  da janela imediatamente anterior.
- `029-07` concluiu leitura all-history, agregação de recordistas por modalidade e
  evaluator puro idempotente. A leitura all-history falha fechada diante de
  mapeamento inválido, sem alterar caminho tolerante legado.
- `029-08` concluiu tela Evolução integrada a Exercícios, com períodos, filtros,
  resumo, tendências, comparação, recordes e navegação para atividade elegível.
  Estado failed permanece visível quando janela selecionada contém dados locais
  faltantes; janela de 7 dias foi validada fisicamente com dados reconciliados.
  Eixos dos gráficos preservam pontos e labels extremos por padding e rolagem
  horizontal quando densidade exigir. Header mantém tom compartilhado com
  Exercícios.

## Guard rails

- não criar fase macro nova; ticker 029 é spec independente;
- não introduzir GPS, rota, treino phone ou leitura do Health Connect;
- não alterar D4 ou gerar timeline sintética;
- não usar médias de médias quando a fórmula correta exigir ponderação/agregação de fatos;
- não transformar maior BPM/cadência em incentivo de performance;
- não bloquear save por falha de cálculo/apresentação de recorde;
- não criar nem executar testes automatizados sem pedido explícito.

## Verificação

Para subtarefas com código:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Mudanças visuais devem ser validadas fisicamente e registrar evidência sanitizada quando a task correspondente exigir.

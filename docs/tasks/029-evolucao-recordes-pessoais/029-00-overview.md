# 029 — Evolução esportiva e recordes pessoais

- **Status geral:** `in_progress`
- **Spec:** [029-evolucao-recordes-pessoais.md](../../specs/029-evolucao-recordes-pessoais.md)
- **Progresso:** `1/10` subtarefas concluídas

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
- [ ] `029-02` — criar contratos e read model de evolução
- [ ] `029-03` — implementar períodos e filtros por esporte
- [ ] `029-04` — implementar volume e consistência
- [ ] `029-05` — implementar tendências e fórmulas agregadas
- [ ] `029-06` — implementar comparação entre períodos
- [ ] `029-07` — implementar read model e avaliador de recordes pessoais
- [ ] `029-08` — criar tela Evolução e superfície de recordes
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

Próxima subtarefa elegível: `029-02`.

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

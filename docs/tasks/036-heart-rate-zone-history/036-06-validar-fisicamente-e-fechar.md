# 036-06 — Validar fisicamente e fechar

- **Ticker:** `036`
- **Número:** `06`
- **Status inicial:** `pending`
- **Status:** `completed`

## Objetivo e resultado esperado

Validar a distribuição histórica sobre banco e workouts reais existentes,
comprovar invariantes numéricas e ausência de efeitos persistentes/remotos, e
fechar Spec, ADR e overview somente com evidência ou N/A justificado.

## Requisitos cobertos

- Todos os critérios de aceite da Spec 036.
- Room v20, fonte bounded da 035, policy 034, UI contextual e fallback gráfico.
- Preservação de `WorkoutActivity`, workflow, snapshots, derivados, splits e
  Health Connect.

## Escopo incluído

- `git diff --check`.
- `./gradlew lint assembleDebug assembleRelease`.
- Validação física com banco/workouts existentes, sem novo treino ou sample.
- Cálculo independente das zonas individuais, current7/current28 e buckets.
- Verificação de percentuais sobre tempo analisado e coverage ratio separado.
- Verificação de carga zero e workout sem análise quando naturais.
- Mudança temporária/restauração de FC máxima e comparação dos resultados.
- Comparação sanitizada de fatos persistidos e operações Health Connect.
- Reopen/recompose sem persistência analítica.
- Screenshot sanitizada e fechamento documental.

## Escopo excluído

- Criar/fabricar workout, sample, fixture ou estado artificial.
- Migration, tabela, coluna, cache ou snapshot.
- Alterar parser, BLE, derivação, workflow, Health Connect ou design system
  compartilhado.
- Criar testes automatizados novos.
- Documentar BPM individual, activity IDs, MAC, payload BLE ou dados pessoais
  desnecessários.

## Dependências

- `036-02`, `036-03`, `036-04` e `036-05` concluídas.
- Banco e workouts reais já usados nas validações 034/035.
- Acesso a instalação debug/release e evidência sanitizada.

## Arquivos e símbolos prováveis

- `docs/specs/036-heart-rate-zone-history.md`.
- `docs/decisions/adr-036-heart-rate-zone-history.md`.
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`.
- Evidência física em `docs/integration/036-06-physical-validation.md`, se
  criada conforme padrão de fechamento.
- `ChampionDatabase`, `WorkoutActivity`, snapshots e
  `health_sync_operations` somente para inspeção.

## Passos de implementação/validação

1. Confirmar diff somente em arquivos autorizados e Room `user_version = 20`.
2. Registrar que FC máxima atual foi reutilizada sem expor valor desnecessário.
3. Comparar zonas individuais com a Spec 034.
4. Calcular independentemente current7/current28 e verificar seis zonas,
   duração, percentuais e cobertura.
5. Somar buckets por zona e comparar com current28.
6. Verificar carga zero e workout sem análise naturalmente existentes; registrar
   N/A quando ausentes.
7. Alterar FC máxima temporariamente, observar recomposição e restaurar valor.
8. Comparar hashes/assinaturas de activities, workflow, snapshots, derivados,
   splits e `health_sync_operations` antes/depois; inspecionar call paths para
   atribuir qualquer operação Health Connect à inicialização preexistente ou à
   feature, sem prometer imutabilidade absoluta quando o ruído não puder ser
   isolado.
9. Reabrir a página e confirmar que analytics não foram persistidos.
10. Capturar screenshot sanitizada, sem dados pessoais desnecessários.
11. Registrar evidência, N/A, riscos residuais, status e progresso 6/6.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validações físicas obrigatórias:

- Room v20 e nenhuma migration/tabela analítica;
- fonte bounded e policy 034 preservadas;
- fechamentos por zona de 7/28 dias e buckets;
- percentuais com denominador analisado;
- coverage ratio separado;
- parcialidade e ausência distinguíveis;
- recomposição/restauração por FC máxima;
- fatos, workflow, snapshots, derived intervals, splits e Health Connect sem
  mutação causada pela feature;
- screenshot sanitizada.

Não criar testes automatizados novos. N/A exige motivo, evidência disponível e
risco residual; não libera critério numérico obrigatório sem cálculo
independente.

## Definição de pronto

- Todos os critérios exercitáveis têm evidência ou N/A justificado.
- Nenhum cenário foi fabricado.
- Room permanece v20 e não há persistência analítica.
- Distribuição fecha por período e por dia.
- UI e screenshot mostram labels acessíveis e estados factuais.
- Spec, ADR, overview e esta subtarefa têm status/progresso coerentes.

## Riscos e cuidados

- Restaurar FC máxima original antes de encerrar.
- Não confundir ausência natural com zero válido.
- Preservar risco da query sem índice temporal composto sem criar migration.
- Considerar o risco documental 035 de operações `HEART_RATE` criadas na
  inicialização e não atribuí-lo automaticamente à 036.
- Sanitizar toda evidência física.

## Execução e evidências

### Validação realizada

- Ticker `036` confirmado em spec, ADR, overview e seis subtarefas; overview
  mantém uma única seção `## Checklist` com seis itens.
- `git diff --check`, lint, assemble debug/release e install debug passaram.
- Champion C001 real foi validado no `SM-S901E`, sem limpar banco, criar treino,
  sample ou fixture.
- Room confirmou `user_version = 20` e schema `20.json`; nenhuma tabela
  analítica existe.
- Fonte bounded, policy 034, filtro `WATCH_D3_D4`, workflow não descartado,
  leitura única e call path sem DAO no ViewModel foram confirmados por inspeção.
- Cálculo externo confirmou current7 `3/3`, current28 `10/10`, somas de zonas,
  percentuais em 100%, coverage ratios independentes e fechamento de cada zona
  dos buckets em `0,000 s` de erro.
- A UI mostrou seis zonas, `Abaixo de Z1`, cobertura, duração coberta,
  contadores, parcialidade, seleção 7/28 e distribuição diária acessível.
- Mudança temporária da FC máxima alterou a saída; restauração e reabertura
  devolveram os resultados originais.
- `health_sync_operations` permaneceu em 874 linhas com assinatura estável no
  snapshot pós-inicialização, e o call path da 036 não chama Health Connect.
- Screenshot sanitizada registrada em
  `docs/integration/036-06-heart-rate-zone-history.png`.

### Decisões e desvios

- Nenhum requisito funcional foi alterado.
- Carga zero e workout sem análise ficaram `N/A`: nenhum cenário existia
  naturalmente e não foi fabricado dado para preenchê-lo.
- Snapshot físico inicial e snapshot pós-inicialização divergiram em activities,
  snapshots e derived intervals. A causa não foi isolável; o ruído não foi
  atribuído à 036. Snapshot pós-inicialização permaneceu estável durante mudança
  de FC máxima, restauração e reabertura.
- Nenhum código funcional adicional foi necessário para fechamento.

### Arquivos alterados

- `docs/integration/036-06-physical-validation.md`
- `docs/integration/036-06-heart-rate-zone-history.png`
- `docs/specs/036-heart-rate-zone-history.md`
- `docs/decisions/adr-036-heart-rate-zone-history.md`
- `docs/baseline/current-baseline.md`
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`
- `docs/tasks/036-heart-rate-zone-history/036-06-validar-fisicamente-e-fechar.md`

### Comandos executados e resultados

- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- `./gradlew installDebug` — instalado em 1 dispositivo.
- `git diff --check` — passou antes e depois do fechamento documental.
- `adb devices`, dumps sanitizados de UI e screenshots — dispositivo real
  disponível; labels, semantics e estados observados.
- Cálculo independente externo sobre cópia temporária do banco — fechamentos
  numéricos e hashes sanitizados registrados na evidência física.
- Não foram criados nem executados testes automatizados novos, conforme
  `AGENTS.md` e escopo da subtarefa.

### Resultado e riscos residuais

- Todos os critérios exercitáveis têm evidência ou N/A justificado.
- Spec, ADR, overview e esta subtarefa estão coerentes em status e progresso;
  overview está em `6/6`.
- Riscos residuais: ruído de inicialização com causalidade não isolada, ausência
  física de carga zero/workout sem análise e custo da query sem índice temporal
  composto herdado da Spec 035.

### Revisão independente

- Revisão independente final permanece solicitada ao agente `review` após esta
  implementação e fechamento documental.

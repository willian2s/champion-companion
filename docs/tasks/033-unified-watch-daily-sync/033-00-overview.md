# 033 — Overview: sincronização unificada e dados diários granulares

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADRs:** `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`, `docs/decisions/adr-031-d1-granular-steps-health-connect.md`, `docs/decisions/adr-032-sync-entrypoints-and-watch-management.md`
- **Status:** `in_progress`
- **Progresso:** `6/9`
- **Branch de integração:** `feat/033-unified-watch-daily-sync`

## Objetivo

Unificar FEA1, D1 e D3/D4 em um único watch sync iniciado pela Home, promover D1
completo para fatos diários granulares, compor fontes por finalidade/métrica sem
dupla contagem e fazer cutover seguro da projeção Health Connect diária sem
reescrever histórico.

## Sequência

```text
033-01 Auditoria e contratos                         ✓
   |\
   | +--> 033-02 Parser/collector D1 v2             ✓
   | +--> 033-03 Persistência D1 / Room v18         ✓
   |          |
   +----------+
              v
033-04 Composição diária FEA1 + D1 + D4             ✓
              |
       +------+------+
       |             |
       v             v
033-05 Watch sync   033-06 HC cutover/granular
        ✓             ✓
       +------+------+
              v
033-07 Produto/UI e remoção de entrypoints BLE      ~
              |
033-08 Read models, legacy e compatibilidade
              |
033-09 Validação física e fechamento
```

`033-06` foi revalidada fisicamente em 2026-09-14 com STEPS granular D1 real,
workout simultâneo e zero pendências acionáveis após sync. A `033-07` está em
execução: ownership de Home/Health Connect/Configurações já foi migrado, enquanto
limpeza dos affordances legados de Exercícios e Perfil ainda permanece pendente.

## Checklist

- [x] `033-01-auditar-e-congelar-d1-v2-e-cutover.md`
- [x] `033-02-implementar-parser-collector-d1-v2.md`
- [x] `033-03-persistir-fatos-diarios-granulares.md`
- [x] `033-04-compor-dados-diarios-fea1-d1-d4.md`
- [x] `033-05-unificar-watch-sync-na-home.md`
- [x] `033-06-integrar-health-connect-granular-e-cutover.md` — revalidação física granular concluída
- [ ] `033-07-remover-entrypoints-ble-e-calibracao-passo-da-ui.md` — em andamento
- [ ] `033-08-integrar-read-models-legacy-e-compatibilidade.md`
- [ ] `033-09-validar-fisicamente-e-fechar.md`

## Decisões congeladas

- Home pull-to-refresh é o único entrypoint de produto para leitura do relógio;
- o cabeçalho da Home expõe ação explícita `Sincronizar com Health Connect`, reutilizando o mesmo sync global de Configurações;
- gerenciamento normal do Champion C001 fica em `Configurações -> Gerenciar relógio`; CTA contextual de associação pode permanecer na Home quando nenhum dispositivo estiver configurado;
- Saúde não expõe atalho visual de gerenciamento do relógio;
- FEA1 = total diário autoritativo de passos/validação;
- D1 = linha granular diária do relógio e pode conter fatos ocorridos durante workouts;
- D4 = fonte detalhada/canônica do workout;
- D1 v2 preserva passos, distância, calorias, sportValue e raw adicionais mesmo com BPM inválido;
- BPM D1 v2 válido em 30..200;
- a regra antiga "bucket D1 intersecta D4 => suprimir bucket inteiro" foi rejeitada após validação física;
- passos diários usam FEA1 como total/controle e D1 como distribuição; D4 não é somado novamente ao agregado diário;
- para HC pós-cutover, cada D1 em `t` representa o bucket `[t-5min, t)` e gera STEPS somente quando `steps > 0`;
- o cutover granular efetivo tem piso em `2026-09-14`; uma data com operação DailySummary/STEPS existente preserva a família legacy e não recebe D1 granular;
- pós-cutover granular, `WorkoutSteps` fica localmente `BLOCKED/ALREADY_COVERED` e não conta como pendência acionável; `STEPS_CADENCE` continua elegível;
- distância/calorias diárias usam D1 nativo localmente, mas sua projeção HC permanece deferred; distância/calorias do workout continuam no contrato da atividade;
- FC combinada pode preferir D4 durante workout quando houver cobertura D4 válida;
- sync do relógio não executa Health Connect automaticamente;
- step length sai da UI, mas contratos legacy/fallback permanecem;
- medição manual de FC e importação D1 saem da Saúde como ações de produto;
- importação D3/D4 sai de Exercícios como ação de produto;
- `Configurações -> Sincronizar agora` e o botão Sync da Home compartilham o mesmo fluxo global, incluindo atividades Watch `READY_TO_SYNC`, preservando review-before-sync e sem tocar `PENDING_REVIEW`/`PENDING_CALIBRATION`.

## Evidência física da direção temporal D1

Os dumps de 2026-09-13 fecharam a regra temporal necessária para STEPS granular:

```text
workout RUNNING: 08:53:26 -> 09:20:31
D4 steps:         1935

D1:
08:55  73
09:00 211
09:05 407
09:10 480
09:15 423
09:20 341
-------------
soma   1935

09:25   0
```

Todos os gaps consecutivos D1 do dia são de cinco minutos. A soma de `08:55` a
`09:20` iguala exatamente o D4 e encaixa o início do workout dentro do bucket que
termina `08:55`. A evidência de `RECENT_TAIL` também mostra FEA1 avançando depois
da borda do último bucket D1. O contrato adotado é, portanto:

```text
sample D1 em t
=> bucket [t - 5 minutos, t)
```

Não existe residual sintético para `FEA1 - ΣD1`.

## Revalidação física granular de 2026-09-14

Pré-sync físico:

```text
FEA1.steps = 7004
Σ D1.steps = 6997
D1         = 37/37 posições completas
cauda      = +7 passos recentes
```

O workout RUNNING de `07:02:32 -> 07:36:55` trouxe `5133` passos D4. Os buckets
D1 com fim entre `07:05` e `07:35` somaram `5135`, nova confirmação independente
da direção temporal do bucket.

Pós-sync:

```text
WATCH_DAILY_D1_SAMPLE / STEPS = 19 SYNCED
Σ StepsRecord D1              = 6997
DailySummary/STEPS 14/09      = 0
FAILED                        = 0
pendências acionáveis UI      = 0
```

Todos os payloads D1 verificados possuem duração de `300s`. O RUNNING ficou
`SYNCED`; `EXERCISE_SESSION`, DISTANCE, HEART_RATE, SPEED, STEPS_CADENCE e
TOTAL_CALORIES_BURNED foram aceitos. `WorkoutSteps=5133` permaneceu não publicado
com crédito `BLOCKED/ALREADY_COVERED`, sem aparecer como pendência na UI.

Assim, o cutover real observado é:

```text
13/09 -> DailySummary/FEA1 incremental legacy
14/09 -> D1 STEPS granular
```

## Correção física pós-033-05: cauda recente FEA1 × D1

Refreshes consecutivos da Home confirmaram que FEA1, D1 e D3/D4 são executados em
um único fluxo, D1/D4 permanecem idempotentes e o watch sync não escreve Health
Connect automaticamente.

Foi observado:

```text
FEA1.steps = 4323
Σ D1.steps = 4226
D1         = 183 posições completas, sem lacunas
último D1  = 15:10
FEA1       = ~15:12:47
```

Logo `FEA1 > ΣD1` com multipart estruturalmente completo e último bucket recente é
`RECENT_TAIL`, não divergência. Somente novo D1 real materializa a cauda.

## Resultado e correções da 033-06

A implementação entregou Room v19, cutover persistido, `RECENT_TAIL`, proibição de
residual sintético e separação explícita entre watch sync e HC.

Validações físicas encontraram e fecharam:

1. **lacuna de STEPS após cutover prematuro:** FEA1 legacy foi mantido até existir
   substituto granular comprovado;
2. **Settings ignorava READY_TO_SYNC:** o gesto global agora prepara/sincroniza
   atividades revisadas elegíveis;
3. **backlog antigo consumia o primeiro lote:** o gesto drena passes finitos de
   fila enquanto houver progresso;
4. **WorkoutSteps bloqueado aparecia como PENDING:** bloqueio terminal por cobertura
   deixa de contar como trabalho remoto acionável;
5. **D1 granular:** a evidência física promoveu buckets de cinco minutos para
   StepsRecord, com cutover efetivo em 2026-09-14.

A revalidação de 2026-09-14 confirmou o comportamento ponta a ponta em instalação
com histórico real, incluindo zero pendências acionáveis após o sync.

## Execução atual da 033-07

Já implementado:

- ação de relógio no cabeçalho da Home substituída por botão Sync do Health Connect;
- ação usa exatamente `SettingsIntent.SyncNow`, sem segundo pipeline;
- Home mostra progresso foreground e feedback terminal por snackbar;
- `Configurações -> Gerenciar relógio` abre o `DeviceScreen` existente;
- Saúde não mostra mais atalho visual de relógio;
- ADR-032 congela o ownership de navegação/sync.

Ainda pendente na 033-07:

- remover `Importar treinos` e progresso/retry próprios de Exercícios;
- remover comprimento de passo, sugestão por altura e históricos/perfis de calibração por passada do Perfil;
- revisar contratos/state machines legados sem apagar dados;
- executar build/lint/diff e capturar screenshots das mudanças visuais.

## Pendências intencionais após STEPS granular

- **DISTANCE diária D1:** permanece local/fail-closed porque D1 inclui distância de workouts e coexistência com D4 pode duplicar;
- **CALORIES diária D1:** permanece local/fail-closed por sobreposição D1/D4 e semântica `Active` vs `Total` ainda não fechada;
- observar fisicamente o bucket em torno da meia-noite e revalidar firmware futuro;
- partial success forçado continua reservado para 033-09.

Distância e calorias **dentro de workouts** continuam elegíveis pelo contrato já
validado da atividade.

## Evidências físicas sanitizadas

- `docs/integration/033-03-d1-v2-physical-validation.md`;
- `docs/integration/033-05-unified-watch-sync-physical-validation.md`;
- `docs/integration/033-06-health-connect-clean-install-validation.md`;
- ADR-031 registra a promoção da semântica de bucket para STEPS granular;
- ADR-032 registra ownership de sync remoto e gerenciamento do relógio.

## Guard rails

- não criar testes automatizados sem pedido explícito;
- não introduzir GPS/rota/mapa de produção;
- não remover dados históricos de medição manual, D1, step length ou workout;
- não alterar atividades `SYNCED` silenciosamente;
- não criar efeitos Health Connect em migration/watch sync;
- não fabricar fatos diários antigos que nunca foram persistidos;
- não usar header total D4 como fonte de comportamento;
- não somar D1 + D4 ingenuamente nem suprimir D1 apenas por overlap;
- não fabricar bucket residual a partir de FEA1-D1;
- cada task atualiza este overview.

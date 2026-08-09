# 021-04 — Evidência sanitizada de validação

- **Status da evidência:** `completed`
- **Dispositivo:** Galaxy S22, Android 16
- **Aplicativo:** `1.0-dev`
- **Schema observado após upgrade:** `v11`
- **Parser persistido:** `d4-v2` (legado preservado)
- **Parser novo disponível:** `d4-v3`
- **Derivação ativa exercitada:** `watch-d3-d4-v2`

## Upgrade e banco preservado

- `./gradlew installDebug` instalou a versão nova sobre o pacote existente, sem
  `clear data`.
- A abertura do app concluiu a migration local `v10 → v11`.
- `PRAGMA integrity_check` retornou `ok`.
- `PRAGMA user_version` retornou `11`.
- Após o upgrade, permaneceram 4 atividades, 98 intervalos raw, 98 intervalos
  derivados, 98 amostras de frequência cardíaca, 4 payloads diagnósticos e 84
  operações Health Connect.
- Não foi executado SQL corretivo manual.

## Atividade de 32 samples

- A atividade existente foi localizada e reprocessada pela ação explícita do
  app, sem nova leitura BLE.
- O mesmo `activityId` foi preservado; o identificador não é registrado aqui.
- A atividade permaneceu `PENDING_REVIEW`.
- Duração resultante: `29:15`.
- Foram preservados 32 intervalos raw e 32 intervalos derivados, com índices
  `0..31` distintos e ordenados.
- A soma dos passos foi comparada antes/depois e permaneceu igual; o valor não é
  publicado por privacidade.
- A soma das durações efetivas fechou exatamente em `29:15`; primeiro início e
  último fim coincidiram com a janela persistida da atividade.
- Snapshot ativo e distância canônica/materialização permaneceram disponíveis.
- O reprocessamento não criou operação Health Connect nem alterou estados da
  fila.
- Uma segunda atividade local, originalmente com `20:03` e já `SYNCED`, foi
  conferida somente em modo read-only: o envelope diagnóstico preservado contém
  `endSecond = 42`, coerente com `20:45`. Ela não foi reprocessada porque a
  proteção de atividade sincronizada exige reconciliação explícita futura.

## Health Connect

- Após upgrade e reprocessamento: 84 operações, todas `SYNCED`.
- Operações sem `clientRecordId`, sem `clientRecordVersion` ou com erro: zero.
- Comparação sanitizada do conteúdo da fila antes/depois da navegação do
  calendário permaneceu idêntica.
- Não houve leitura ampla do provider; integração continua write-only.
- Não houve revisão/sincronização explícita da atividade durante este gate.

## Calendário offline

- Bluetooth foi desativado durante o cenário e reativado ao final.
- A abertura do mês não exibiu `Dados parciais do mês`.
- O detalhe diário carregou atividades locais; cards sincronizados exibiram
  `Sincronizado` e não `Status indisponível`.
- A navegação mês/dia não alterou a fila local nem executou sync, BLE ou leitura
  remota.

## Durações físicas conhecidas

| Caso observado no app oficial | Resultado esperado |
| --- | --- |
| `15:04 + 24 s` | `15:28` |
| `20:03 + 42 s` | `20:45` |
| `29:01 + 14 s` | `29:15` |

O caso de 32 samples foi exercitado novamente e resultou em `29:15`. O caso
`15:28` foi reprocessado explicitamente e resultou em `15:28`. O caso `20:45`
foi confirmado pelo envelope diagnóstico local em modo read-only; não houve
reprocessamento da atividade já sincronizada.

## Comandos e resultados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- `./gradlew installDebug` — passou; instalação sobre pacote existente no Galaxy
  S22.
- Consultas SQLite read-only sanitizadas — integrity `ok`, schema `v11` e
  contagens preservadas.
- Testes automatizados — não executados, conforme escopo e regras do
  repositório.

## Limitações e N/A

- A tela do app oficial não foi reaberta durante este gate; a confirmação de
  `15:28`/`20:45` usou dados diagnósticos locais preservados e ação explícita
  somente na atividade não sincronizada.
- Cenário negativo de operação órfã/snapshot ausente: `N/A`. Não havia issue
  real segura para observar e não foi fabricada corrupção no banco de produção.
- Não houve readback amplo do Health Connect nem nova sincronização explícita.
- A evidência cobre o Galaxy S22 e firmware exercitado; não garante outros
  firmwares.

## Revisão independente posterior

- Foi identificada uma janela no reprocessamento em que a atualização de duração
  e `endedAt` poderia ser confirmada sem snapshot ativo novo quando a derivação
  retornasse falha sem exceção.
- `reprocessWatchWorkoutInternal` agora lança a exceção controlada já usada pelo
  repositório para todo resultado não materializado após o update. O helper
  `writeTransaction` delega a `ChampionDatabase.withWriteTransaction`; a exceção
  atravessa esse limite e desfaz a atividade e quaisquer gravações parciais.
- Revalidação após a correção: `git diff --check` passou;
  `./gradlew lint assembleDebug assembleRelease` passou em 4m57s;
  `./gradlew installDebug` passou em 11s, instalando no dispositivo conectado.
  A revisão independente final foi concluída com ressalvas.

## Revisão independente final

- Decisão: `aprovada com ressalvas`.
- A janela transacional do reprocessamento foi corrigida: resultado não
  materializado atravessa `withWriteTransaction` como exceção e desfaz alterações
  parciais.
- Nenhuma correção adicional de código foi solicitada.
- Ressalvas: Health Connect write-only, cenário negativo `N/A`, tela oficial não
  reaberta neste gate e cobertura limitada ao Galaxy S22/firmware observado.

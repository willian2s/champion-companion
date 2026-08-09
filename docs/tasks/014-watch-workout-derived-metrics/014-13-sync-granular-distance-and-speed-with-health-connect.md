# 014-13 — Enviar distância granular e `SpeedRecord`

- **Ticker:** `014`
- **Número:** `13`
- **Status:** completed

## Objetivo e resultado esperado

Projetar a série canônica congelada como filhos independentes de Health Connect:
`ExerciseSessionRecord`, múltiplos `DistanceRecord`, `SpeedRecord`,
`HeartRateRecord` e `TotalCaloriesBurnedRecord` elegível.

Resultado: writer explícito, foreground, write-only e idempotente por filho, sem
usar distância original ou calibrada de `WATCH_D3_D4`.

## Dependências

- `014-12` concluída.

## Arquivos permitidos

- `core/model/HeartRateAndSync.kt` e contratos de projeção;
- `core/healthconnect/HealthRecordMapper.kt`;
- `core/healthconnect/HealthConnectSyncCoordinator.kt`;
- `core/healthconnect/HealthConnectGateway.kt`;
- repository/DAO/mappers das projeções;
- permissões/rationale estritamente necessários para velocidade;
- `docs/baseline/health-connect-mapping.md` e overview.

## Arquivos proibidos

- parser, perfil e UI não relacionada;
- migrations/schemas já fechados em `014-12`;
- GPS, rota, `ExerciseRoute` e código phone;
- WorkManager/background.

## Projeção

### Sessão

- um `ExerciseSessionRecord` por atividade elegível.

### Distância

- uma operação/record por intervalo canônico com distância positiva;
- `projectionChildKey = interval-<índice estável>`;
- janela igual ao intervalo efetivo;
- nenhum agregado novo.

### Velocidade

- um `SpeedRecord` por atividade, com amostras derivadas;
- cada amostra usa velocidade média do intervalo;
- timestamp determinístico no ponto médio:

```text
sampledAt = intervalStart + effectiveDuration / 2
```

### BPM e calorias

- BPM usa somente amostras reais;
- calorias usam somente total elegível, sem derivação temporal inventada.

## Gate de legado

Caso exista identidade local conhecida de agregado legado de distância:

- preparar filhos e manifesto;
- manter inserts de distância/velocidade bloqueados;
- não apagar o agregado nesta task;
- liberar somente após checkpoint de reconciliação em `014-14`.

Sessão, BPM e calorias podem manter independência conforme a política fechada em
`014-02/12`, sem causar dupla distância.

## Fora do escopo

- remoção do agregado legado;
- leitura remota ampla;
- delete por janela;
- Steps por atividade, rota ou distância original;
- testes automatizados.

## Passos de implementação

1. Habilitar `SPEED` somente para projeção elegível.
2. Preparar uma operação por filho a partir do payload congelado.
3. Mapear cada intervalo positivo para `DistanceRecord`.
4. Mapear a série para `SpeedRecord` com midpoint.
5. Manter sessão, BPM e calorias como filhos independentes.
6. Fazer o coordinator processar somente operações pendentes/falhas.
7. Garantir retry sem recalcular atividade.
8. Respeitar o gate de agregado legado.
9. Atualizar documentação e overview.

## Critérios de aceite

- Nenhum record usa distância original, calibrada ou estimativa agregada.
- Intervalo zero não cria `DistanceRecord`.
- Cada filho possui estado e retry independentes.
- `SpeedRecord` usa o ponto médio definido.
- Retry idêntico mantém ID, versão e payload.
- Falha de um filho não desfaz os demais.
- Agregado legado bloqueia filhos de distância até `014-14`.
- Sync permanece explícito, foreground e write-only.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Implementação

- O mapper congela payload serializado por filho e recria records somente desse
  payload durante retry.
- `DistanceRecord` usa apenas intervalos canônicos positivos, com janela efetiva;
  intervalos zero não geram filho.
- `SpeedRecord` usa uma amostra por intervalo, com velocidade média e midpoint
  determinístico.
- Sessão, BPM real e calorias elegíveis permanecem filhos independentes.
- `SPEED` foi habilitado no manifesto, gateway, permissões e settings somente
  quando houver projeção elegível.
- O coordinator processa somente operações pendentes/falhas, preserva falha
  parcial e exige todos os filhos esperados para marcar atividade sincronizada.
- Filhos `INSERT` obsoletos são preservados para reconciliação, mas não entram no
  processamento nem podem marcar atividade como sincronizada.
- Payload alterado em reprocessamento incrementa versão monotônica e reinicia
  somente o filho afetado; retry idêntico reutiliza ID, versão e payload.
- Gate de agregado legado consulta identidade local e origem; bloqueia somente
  distância/velocidade de `WATCH_D3_D4` até `014-14`, sem bloquear atividades
  phone.

## Arquivos alterados

- `app/src/main/AndroidManifest.xml`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectGateway.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectPermissionsRationaleActivity.kt`;
- `docs/baseline/health-connect-mapping.md`;
- este arquivo e o overview da task.

## Decisões e desvios

- Não houve migration ou schema: `014-12` permanece fechado e a preparação usa
  payloads já persistidos.
- O gate legado é fail-closed quando a atividade ou a consulta local falha.
- Filhos que deixaram de ser elegíveis permanecem preservados para reconciliação
  explícita em `014-14`; nenhum record remoto é apagado nesta task.
- Não foram criados nem executados testes automatizados, conforme escopo.

## Definição de pronto

Writer granular funciona por filho e permanece bloqueado quando houver agregado
legado não reconciliado; item `014-13` marcado.

## Rollback

Interromper novos inserts e preservar operações/manifests para retomada. Não
recriar agregado automaticamente nem apagar records enviados.

## Evidências

- Manifesto, mapper, coordinator e fila local registram tipos, filhos, estados,
  payloads congelados e checkpoints por operação.
- `WatchWorkoutDerivedMetrics.kt` fornece midpoint por
  `intervalStart + effectiveDuration / 2`; o writer não recalcula a atividade no
  retry.
- Gate legado consulta origem persistida e só bloqueia `WATCH_D3_D4`; atividades
  phone preservam processamento normal.
- `./gradlew lint assembleDebug assembleRelease` — PASS.
- `./gradlew installDebug` — PASS no dispositivo `SM-S901E`.
- `git diff --check` — PASS.
- Testes automatizados não foram executados, conforme instrução.

## Comandos executados e resultados

- `./gradlew lint assembleDebug assembleRelease` — PASS;
- `./gradlew installDebug` — PASS, APK instalado no `SM-S901E`;
- `git diff --check` — PASS;
- `git status --short` — diff local revisado;
- testes automatizados — não executados, conforme escopo.

## Riscos residuais

- Provider Health Connect real não foi exercitado para `SpeedRecord`, midpoint,
  falha parcial ou permissão `SPEED`; validação física fica para `014-15`.
- Agregado legado de distância de relógio continua bloqueando filhos de distância
  e velocidade até reconciliação explícita em `014-14`.
- Filhos obsoletos permanecem persistidos para deleção por ID exato na
  reconciliação; esta task não remove records remotos.

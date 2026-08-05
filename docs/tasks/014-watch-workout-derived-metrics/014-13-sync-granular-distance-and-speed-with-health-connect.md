# 014-13 — Enviar distância granular e `SpeedRecord`

- **Ticker:** `014`
- **Número:** `13`
- **Status:** pending

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

## Evidências

- manifesto sanitizado com tipos, filhos e estados;
- timestamps de midpoint sem valores sensíveis;
- screenshots do provider;
- matriz de falha parcial/retry;
- builds.

## Definição de pronto

Writer granular funciona por filho e permanece bloqueado quando houver agregado
legado não reconciliado; item `014-13` marcado.

## Rollback

Interromper novos inserts e preservar operações/manifests para retomada. Não
recriar agregado automaticamente nem apagar records enviados.

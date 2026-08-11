# 022-02 — Adicionar tipo bicicleta, distância manual e persistência

- **Status:** `pending`
- **Depende de:** `022-01`
- **Desbloqueia:** `022-03`

## Objetivo

Adicionar `WorkoutType.CYCLING`, persistir distância manual opcional em campo
próprio e integrar esse dado ao formulário de revisão sem reutilizar calibração
de esteira.

## Mudanças planejadas

### Domínio

- adicionar `WorkoutType.CYCLING`;
- label de UI `Bicicleta`;
- expor distância manual separadamente em `WorkoutDistanceMetrics` ou contrato
equivalente definido pela auditoria;
- validar somente valor positivo e finito quando presente;
- não criar sugestão automática de bicicleta baseada em passos.

### Persistência

Adicionar campo nullable próprio em `workout_activities`:

```text
manualDistanceMeters REAL NULL
```

Criar migration aditiva:

```text
MIGRATION_11_12
```

Requisitos:

- linhas existentes permanecem `NULL`;
- nenhum backfill por distância original/estimada/calibrada;
- nenhuma derivação durante migration;
- nenhum efeito Health Connect;
- atualizar schema exportado v12;
- atualizar mappers e round-trip Room.

### Revisão

Adicionar campo opcional de distância quando o tipo selecionado for `CYCLING`.

UI:

```text
Distância
[ 16,4 ] km
Opcional
```

A revisão deve:

- aceitar ausência;
- aceitar valor positivo válido;
- permitir limpar;
- armazenar em metros;
- não direcionar bicicleta para `PENDING_CALIBRATION`;
- manter os guards atuais de título, notas, parser/plausibilidade e workflow.

A política congelada em `022-01` deve definir o comportamento ao salvar tipo
não `CYCLING` quando existir valor manual temporário; não deixar esse valor ser
consumido por outro tipo.

## Compatibilidade

- nenhum parser D3/D4 é alterado;
- snapshots derivados não são apagados;
- treadmill continua usando `calibratedDistanceMeters` e seu fluxo próprio;
- atividades antigas não mudam de tipo;
- atividades `SYNCED` continuam sem edição nova por esta spec.

## Validação

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção do schema Room v12;
- upgrade local v11→v12 sem wipe em `022-05`.

## Fora do escopo

- adaptar todos os consumers locais;
- Health Connect;
- GPS/rota;
- velocidade granular;
- testes automatizados novos.

## Critérios de aceite

- [ ] `CYCLING` existe no domínio e persiste corretamente;
- [ ] UI usa rótulo `Bicicleta`;
- [ ] distância manual é opcional e separada das três distâncias existentes;
- [ ] migration v11→v12 é aditiva e sem backfill semântico;
- [ ] bicicleta sem distância pode concluir revisão sem calibração;
- [ ] treadmill mantém comportamento existente;
- [ ] nenhuma atividade antiga é reprocessada;
- [ ] build/lint passam;
- [ ] revisão independente aprova a mudança.

# 014-12 — Modelar e migrar projeções filhas Health Connect

- **Ticker:** `014`
- **Número:** `12`
- **Status:** pending

## Objetivo e resultado esperado

Modelar uma operação persistida por filho Health Connect e executar uma evolução
Room aditiva separada da migration local de métricas.

Resultado: manifesto, payload congelado, identidade natural, ações e checkpoints
estão persistidos antes de qualquer fan-out ou deleção remota.

## Dependências

- `014-10` concluída.
- `014-03` registrou a versão efetiva da migration local.

## Arquivos permitidos

- contratos de sync/projeção em `core/model`;
- `ChampionDatabase.kt`, DAOs, migrations e `DatabaseModule.kt`;
- `DatabaseMappers.kt` e `RoomRepositories.kt` para persistência da fila;
- novo schema exportado;
- `docs/baseline/database-reference.md`;
- `docs/baseline/health-connect-mapping.md`;
- overview.

## Arquivos proibidos

- parser, perfil, UI e writer/gateway remoto;
- Gradle e manifesto;
- alteração do schema exportado da migration local anterior;
- GPS, rota, `ExerciseRoute` e código phone.

## Modelo obrigatório

Cada filho possui sua própria operação:

```text
aggregate + recordType + projectionChildKey
```

Exemplos:

```text
EXERCISE_SESSION / singleton
DISTANCE / interval-000
DISTANCE / interval-001
SPEED / series
HEART_RATE / series
TOTAL_CALORIES_BURNED / singleton
```

Persistir pelo menos:

- `projectionChildKey`;
- ação `INSERT` ou `DELETE`;
- payload congelado suficiente para retry;
- `clientRecordId` e `clientRecordVersion`;
- estado, tentativas e erro seguro;
- conjunto esperado/enviado/obsoleto ou estrutura equivalente;
- checkpoint de reconciliação;
- vínculo com a derivação/projeção que originou o payload.

## Regras

1. Não criar uma operação agregada com subfila interna de vários records.
2. Retry lê payload congelado; não recalcula atividade mutável.
3. ID não contém valor de saúde.
4. Versão é monotônica por `clientRecordId`.
5. Retry idêntico mantém ID, versão e payload.
6. Operações legadas permanecem preservadas e distinguíveis.
7. Migration não executa insert/delete remoto.
8. Migration não deriva atividade nem altera workflow.

## Fora do escopo

- mapear `DistanceRecord` ou `SpeedRecord`;
- inserir/deletar no provider;
- reconciliar agregado legado;
- WorkManager/background;
- testes automatizados.

## Passos de implementação

1. Fechar contratos de manifesto e filho a partir da ADR-018.
2. Definir a identidade natural com `projectionChildKey`.
3. Definir payload congelado e regras de versão.
4. Criar migration aditiva na próxima versão disponível.
5. Preservar operações legadas com defaults seguros.
6. Adicionar DAOs, mappers e transações de preparação.
7. Garantir unicidade de filho por projeção.
8. Exportar somente o novo schema.
9. Atualizar baselines e overview.

## Critérios de aceite

- Cada filho pode falhar e retomar de forma independente.
- Não existe subfila escondida em payload agregado.
- Operação contém payload suficiente para retry sem ler métricas mutáveis.
- IDs e versões respeitam ADR-018.
- Migration abre banco legado sem destructive migration.
- Nenhum efeito remoto ocorre durante upgrade.
- Schemas históricos permanecem imutáveis.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- matriz tipo/filho/identidade/payload;
- diff da migration e schema;
- round-trip de operações preparadas;
- abertura sanitizada de banco legado;
- builds.

## Definição de pronto

Modelo e migration de projeções filhas estão prontos para o writer de `014-13` e
o item `014-12` está marcado.

## Rollback

Antes de release, reverter a migration. Após distribuição, usar migration forward
e preservar operações/projeções já persistidas.

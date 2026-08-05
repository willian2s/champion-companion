# 014-03 — Migrar Room para intervalos e metadados de derivação

- **Ticker:** `014`
- **Número:** `03`
- **Status:** pending

## Objetivo e resultado esperado

Implementar evolução Room aditiva após `v8` para persistir perfil de passo,
fatos raw/medidos, duração efetiva, snapshots derivados e manifesto de projeções
Health Connect. Resultado: banco novo e legado abrem sem perda e sem derivação
ou efeito remoto durante migration.

## Contexto

Schema atual `v8` possui `user_profiles`, `workout_activities`,
`workout_intervals` e `health_sync_operations`, mas não separa metadados exigidos
pela ADR-018.

## Requisitos cobertos

- R2 a R5, R8 e R9;
- migration prevista na spec.

## Dependências

- `014-02` concluída com schema lógico fechado.

## Arquivos permitidos

- `core/database/ChampionDatabase.kt`;
- `core/database/ChampionDaos.kt`;
- `core/database/ChampionMigrations.kt`;
- `core/database/DatabaseModule.kt`;
- `data/DatabaseMappers.kt`;
- `data/RoomRepositories.kt`, somente adapters mínimos de persistência;
- schema exportado da nova versão;
- `docs/baseline/database-reference.md` e overview.

Símbolos prováveis: `UserProfileEntity`, `WorkoutIntervalEntity`, novas entidades
de snapshot/série/projeção, `HealthSyncOperationEntity`, DAOs e `MIGRATION_8_9`.

## Arquivos proibidos

- parser BLE, UI, Health Connect gateway/mapper;
- manifesto, Gradle e schemas históricos `1.json` a `8.json`;
- tabelas/migrations da Fase 013 para remoção ou reconstrução destrutiva.

## Escopo

- colunas/tabelas novas, FKs, índices e constraints;
- defaults nulos/ausentes para legado;
- round-trip de novos contratos;
- registro da migration e schema novo.

## Fora do escopo

- backfill derivado;
- reprocessamento de atividades;
- alteração de workflow;
- escrita ou deleção Health Connect;
- testes automatizados.

## Passos de implementação

1. Traduzir contrato de `014-02` em schema aditivo.
2. Criar migration manual com defaults seguros e sem calcular métricas.
3. Preservar IDs, `recordedAt`, revisão, calibração, imports, intervalos, sessões,
   rotas e operações existentes.
4. Adicionar DAOs e mappers mínimos para leitura/escrita transacional.
5. Garantir unicidade de snapshot ativo e discriminadores de filhos.
6. Exportar somente novo schema e registrar migration no módulo.
7. Atualizar referência de banco e overview.

## Critérios de aceite

- Banco sobe de `v8` para nova versão sem destructive migration.
- Linhas legadas permanecem com derivação ausente, não inventada.
- Schemas históricos não mudam.
- Migration não toca provider, fila remota ou workflow.
- Novas FKs/índices suportam retry e retenção de projeções.

## Validação manual

- Inspecionar SQL da migration e diff do schema exportado.
- Comparar contagens/identidades antes e depois em banco sanitizado disponível.
- Abrir app com banco existente e verificar telas locais sem reprocessar.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- diff de schema novo;
- matriz tabela/coluna/default/índice;
- registro sanitizado da abertura de banco legado e builds.

## Definição de pronto

Migration aditiva registrada, schema novo exportado, dados históricos preservados
e item `014-03` marcado.

## Riscos e cuidados

- Hotspot `RoomRepositories.kt`: limitar alterações a persistência nova.
- Não reutilizar `distanceMeters` legado como canônico.
- Snapshot/projeção em retry não pode ser apagado por cascade indevida.

## Rollback

Antes de release, reverter migration/schema e entidades novas. Após distribuição,
rollback exige migration forward; nunca reduzir versão ou apagar dados.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-03` e registrar versão/schema efetivos.

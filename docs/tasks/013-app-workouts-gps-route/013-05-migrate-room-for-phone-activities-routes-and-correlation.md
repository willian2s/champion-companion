# 013-05 — Migrar Room para atividades phone, rotas e correlação

- **Ticker:** `013`
- **Número:** `05`
- **Status:** pending

## Objetivo e resultado esperado

Aplicar migration explícita `v5 → v6` e persistência transacional para atividade generalizada, sessão completa, rota incremental e correlação. Resultado: histórico D3/D4 preservado, schema `6.json` e APIs Room/data prontas.

## Contexto

v5 possui sessão, atividade D3/D4 e fila, mas sem rota. Mapper de sessão é assimétrico. Joins obrigatórios de import excluem phone. Migration deve evitar índices preventivos.

## Documentos obrigatórios

Spec 013; ADR-003/004/012/014; `013-01/03`; schema 5; docs de Room 3 e contratos aprovados.

## Dependências

`013-04` concluída; contratos de `013-03` congelados.

## Arquivos permitidos

`ChampionDatabase.kt`, `ChampionDaos.kt`, módulo/migration de database; `DatabaseMappers.kt`, `RoomRepositories.kt`, `RepositoryModule.kt`; schema `6.json`; modelos mínimos necessários; task/overview.

## Arquivos e símbolos prováveis

`WorkoutActivityEntity`, `WatchWorkoutImportEntity`, `AppWorkoutSessionEntity`, novos `WorkoutRouteEntity`/`WorkoutRoutePointEntity`, DAOs correspondentes, `AppWorkoutSessionEntity.toDomain()` e `RoomRepositories`.

## Arquivos proibidos

UI, GPS Android, FGS, BLE, Health Connect gateway/provider, testes.

## Requisitos cobertos

Migration v6; tabelas route/points; uma rota por sessão; segmentos; cascata; distância acumulada; generalização; recuperação; correlação e preservação histórica.

## Escopo

- remodelar campos import-only para não serem obrigatórios em atividade comum;
- manter `WatchWorkoutImport` como fonte de CRC/parser/device importado;
- adicionar route e points normalizados;
- sequence única por rota e sessão única;
- corrigir round-trip integral da sessão;
- DAOs de lote pequeno/checkpoint/deleção/candidato de correlação;
- tornar joins de atividade/import opcionais onde necessário;
- ajustar constraint da fila somente se consulta/projeção concreta da spec exigir.

## Fora do escopo

Coletar pontos, FGS, merge de protocolo, records Health Connect.

## Passos de implementação

1. Desenhar SQL v5→v6 e mapear preservação de cada coluna/ID.
2. Criar `workout_routes` e `workout_route_points` com FKs/cascatas e constraints comprovadas.
3. Generalizar `workout_activities` sem fabricar import/device para phone.
4. Adicionar campos mínimos de checkpoint/revisão/associação conforme contratos.
5. Corrigir `AppWorkoutSessionEntity.toDomain()` com restauração validada completa.
6. Implementar DAO/repository lifecycle, ponto/lote, segmentos, descarte físico pré-atividade e finalização atômica.
7. Adaptar queries de lista/calendário/BPM/sync para atividade sem import.
8. Implementar lookup de correlação sem merge ainda.
9. Atualizar versão, migration registration e exportar `6.json` pelo build permitido.
10. Registrar plano de rollback e overview.

## Critérios de aceite

- sem destructive migration;
- v5 histórico mantém IDs, workflows, importações, calibrações e sync;
- rota/pontos têm cascata e ordem determinística;
- uma sessão não recebe duas rotas/atividades;
- sessão round-trip preserva estado/tempos/agregados/vínculo;
- atividades phone aparecem em queries sem import fictício;
- nenhum índice sem query/constraint concreta.

## Validação manual

Instalar versão v5 com histórico real, atualizar para v6, abrir listas/detalhes/calendário e confirmar preservação. Inspecionar schema exportado e foreign keys. Não executar testes.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

SQL/migration, diff 5→6, contagens antes/depois sanitizadas, abertura do banco atualizado, histórico preservado e comandos.

## Screenshots

Capturar lista/detalhe/calendário preservados, sem dados sensíveis, se superfícies mudarem.

## Definição de pronto

Migration v6 aplicada manualmente sem perda; repositories compilam; overview atualizado.

## Riscos e cuidados

Migration é hotspot crítico e downgrade após abrir v6 não é suportado. Não logar conteúdo de pontos. Verificar limite SQLite e transações curtas.

## Rollback

Antes do rollout: reverter APK/schema para v5. Após banco aberto em v6: não fazer downgrade; restaurar ambiente de validação por reinstalação limpa somente em dispositivo de desenvolvimento, preservando evidência de que produção exigiria forward fix.

Não criar, alterar ou executar testes automatizados.

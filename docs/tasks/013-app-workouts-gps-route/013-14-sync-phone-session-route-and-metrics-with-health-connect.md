# 013-14 — Sincronizar rota e métricas phone com Health Connect

- **Ticker:** `013`
- **Número:** `14`
- **Status:** pending

## Objetivo e resultado esperado

Adicionar `ExerciseRoute` à mesma `ExerciseSessionRecord` com identidade canônica, versão monotônica e retry granular. Resultado: sessão pode sincronizar sem permissão de rota e ser enriquecida depois sem duplicação/downgrade.

## Contexto

Fila atual separa sessão/distância/calorias/BPM. Rota é campo da sessão, não record independente. SDK 1.1.0 deve ser validado antes de upgrade. Metadata phone/watch deve preservar proveniência.

## Documentos obrigatórios

Spec 013; ADR-012/014; spec 012; `013-03/04/05/12`; documentação oficial Health Connect da versão usada.

## Dependências

`013-12` concluída. Pode avançar em paralelo com 013-13 se hotspots serializados.

## Arquivos permitidos

`core/healthconnect/**`; `HeartRateAndSync.kt`; fila/repository/mappers/DAOs necessários; manifesto/rationale/Settings permission models; Gradle somente se upgrade comprovado; task/overview.

## Arquivos e símbolos prováveis

`HealthRecordMapper`, `HealthConnectGateway`, `HealthConnectSyncCoordinator`, `HealthSyncOperation`, `HealthSyncRecordType.EXERCISE_SESSION`, nova projeção de route enrichment, `SettingsViewModel` e rationale Activity.

## Arquivos proibidos

GPS capture, FGS, D3/D4 protocol, WorkManager, leitura HC, testes.

## Requisitos cobertos

WRITE_EXERCISE_ROUTE; mesma sessão/ID; versão; permissão ausente/posterior; retry; ordem/atomicidade; proveniência; fully synced.

## Escopo

- capability/permissão de rota separada;
- projeção interna de route enrichment da mesma sessão;
- canonical ID de `EXERCISE_SESSION` sem sufixo route;
- versão persistida monotônica;
- mapper de route points ordenados/segmentados para API;
- upsert completo sempre inclui rota local elegível já existente;
- workflow e falha parcial.

## Fora do escopo

Mapa, leitura remota, delete amplo, servidor, WorkManager, sync automático de atividade.

## Passos de implementação

1. Confirmar classes/limites do SDK 1.1.0; atualizar somente se API necessária faltar.
2. Expor permissão `WRITE_EXERCISE_ROUTE` no gateway/rationale/Settings.
3. Modelar projeção/dependência route sem segundo `ExerciseSessionRecord` identity.
4. Seedar versão sem regredir versões v5 e incrementar transacionalmente em mudança HC-visível.
5. Mapear pontos finais elegíveis, ordenados, dentro da sessão e sem mock.
6. Enviar base primeiro; sem route permission, manter enrichment pendente/falha `PERMISSION_REQUIRED`.
7. Após concessão, upsert mesma ID com versão maior e rota.
8. Garantir que todo upsert posterior inclua rota já elegível.
9. Processar distância/calorias/BPM granularmente; BPM watch com metadata de watch, sessão/rota/distância phone.
10. Marcar atividade SYNCED só quando elegíveis concluem.
11. Tratar sucesso remoto+falha local por retry mesma ID/versão.
12. Atualizar overview.

## Critérios de aceite

- Health Connect contém uma sessão por atividade;
- sem permissão route, sessão/métricas autorizadas sincronizam;
- rota fica pendente local;
- concessão posterior atualiza mesma sessão;
- retry não duplica nem apaga rota;
- falha de route não apaga outros sucessos;
- route nunca vira record independente.

## Validação manual

Provider real: sem permissão route; sync base; conceder; retry; visualizar rota; retry posterior; editar/upsert posterior e confirmar rota preservada; falhas parciais; uma única sessão.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Permissões, IDs canônicos sanitizados/parcialmente redigidos, versões, estados da fila, contagem remota de sessão, rota visível, comandos e screenshots.

## Screenshots

Obrigatórias: Settings/capacidade de rota, falha parcial/status e Health Connect mostrando sessão/rota; ocultar coordenadas pessoais quando evidência sair do ambiente privado.

## Definição de pronto

Upsert route idempotente comprovado fisicamente; overview atualizado.

## Riscos e cuidados

Sem atomicidade Room/provider. Limites de pontos e comportamento do provider exigem evidência física. Nunca fazer write route-less com versão superior após rota synced.

## Rollback

Parar de criar novos enrichments sem apagar records já enviados; manter rota local e operações pendentes. Não remover permissão/mapper se isso causar downgrade remoto; preferir forward fix.

Não criar, alterar ou executar testes automatizados.

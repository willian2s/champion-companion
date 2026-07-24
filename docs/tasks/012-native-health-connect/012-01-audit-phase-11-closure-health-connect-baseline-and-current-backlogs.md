# Task 012-01 - Auditar fechamento da Fase 11, baseline Health Connect e backlogs atuais

## Status

Planejada.

## Objetivo

Reconciliar documentalmente a conclusão da Fase 11, atualizar baseline factual da fase atual e auditar o backlog local real que a Fase 12 herdará antes de qualquer integração funcional com o Health Connect.

## Contexto

A direção da Fase 12 considera a Fase 11 concluída, mas documentação factual ainda está atrasada. O código real já contém backlog local relevante para Health Connect, incluindo operações D1 persistidas e atividades em `READY_TO_SYNC`.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-12-native-health-connect-direction.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Dependências

- nenhuma.

## Entradas

- código real em `core/model`, `core/database`, `core/preferences`, `data`, `feature/home`, `feature/health`, `feature/exercises`, `feature/settings`;
- `AndroidManifest.xml`;
- `app/build.gradle.kts`;
- `gradle/libs.versions.toml`;
- schema exportado `v4`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-01-audit-phase-11-closure-health-connect-baseline-and-current-backlogs.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/decisions/**`
- `docs/roadmap/**`

## Escopo

- reconciliar fechamento documental da Fase 11;
- atualizar baseline factual;
- auditar fila D1 existente;
- auditar medições manuais sem operação;
- auditar resumos diários sem operação;
- auditar atividades `READY_TO_SYNC`;
- auditar peso e altura locais;
- auditar schema `v4`;
- auditar manifesto;
- auditar catálogo de versões;
- auditar `healthSyncEnabled`;
- atualizar overview da Fase 12.

## Fora do escopo

- implementar SDK do Health Connect;
- alterar Kotlin, Compose, Room, Gradle ou Manifest;
- alterar ADR;
- alterar roadmap.

## Passos

1. Confirmar estado real entregue pela Fase 11 no código.
2. Alinhar overview e documentos factuais sem reabrir implementação funcional.
3. Auditar backlog local herdado para passos, manual, D1, atividades, peso e altura.
4. Registrar limitações atuais do schema `v4`, manifesto, catálogo e preferência `healthSyncEnabled`.
5. Atualizar spec e overview da Fase 12 com achados factuais.

## Critérios de aceite

- Fase 11 fica marcada como concluída nos documentos da fase apropriados;
- baseline factual reflete estado real do código;
- backlog herdado fica mapeado por tipo;
- limitações de manifesto, catálogo, schema e preferência ficam explícitas;
- nenhuma implementação funcional de Health Connect é introduzida.

## Validação manual

- conferência textual de referências citadas;
- revisão do diff documental restrito à Fase 11, baseline e Fase 12.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- lista de arquivos auditados;
- backlog local herdado categorizado;
- diff restrito à documentação.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-01` como concluída;
- registrar discrepâncias documentais reconciliadas;
- registrar backlog herdado confirmado.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.

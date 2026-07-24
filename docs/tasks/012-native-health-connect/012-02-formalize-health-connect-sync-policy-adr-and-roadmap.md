# Task 012-02 - Formalizar política de sync Health Connect em ADR e roadmap

## Status

Concluída em 2026-07-24.

## Objetivo

Formalizar a política arquitetural da Fase 12 em ADR nova e alinhar roadmap e mapeamento Health Connect ao escopo write-only, local-first e granular desta fase.

## Contexto

A Fase 12 precisa consolidar decisões duradouras sobre IDs, versões, granularidade, triggers, permissões e falha parcial antes da implementação funcional.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-12-native-health-connect-direction.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-01-audit-phase-11-closure-health-connect-baseline-and-current-backlogs.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`
- `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md`

## Dependências

- `012-01`.

## Entradas

- achados factuais da task `012-01`;
- direção oficial da Fase 12;
- documentação oficial atual do Android Health Connect.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-02-formalize-health-connect-sync-policy-adr-and-roadmap.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/decisions/adr-012-*.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## Escopo

- criar ADR com próximo número disponível;
- atualizar roadmap da Fase 12;
- atualizar mapeamento Health Connect;
- formalizar write-only, local-first, triggers, atividade explícita, IDs, versões, granularidade, falha parcial, permissões por capacidade, ausência de `Steps` por atividade, ausência de rota e ausência de `WorkManager`.

## Fora do escopo

- implementar código;
- alterar schema Room;
- integrar SDK;
- alterar testes.

## Passos

1. Consolidar decisões arquiteturais duradouras da Fase 12.
2. Criar ADR com foco em política de sync.
3. Atualizar roadmap da Fase 12 para refletir fronteira real.
4. Atualizar `health-connect-mapping.md` com política Champion Companion.
5. Atualizar overview e spec da fase.

## Critérios de aceite

- ADR criada com próximo número disponível;
- roadmap atualizado para write-only e sem `Steps` de atividade;
- mapeamento Health Connect alinhado ao app novo;
- decisões duradouras ficam explícitas antes do código.

## Validação manual

- revisão documental cruzada entre spec, ADR, roadmap e mapping.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- ADR criada;
- diff do roadmap;
- diff do mapping Health Connect.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-02` como concluída;
- registrar ADR criada e decisões estabilizadas.

## Execução

### Arquivos atualizados

- `docs/decisions/adr-012-health-connect-write-only-and-granular-sync.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-02-formalize-health-connect-sync-policy-adr-and-roadmap.md`

### Decisões estabilizadas

- integração `write-only` na Fase 12;
- Room permanece fonte operacional e visual primária;
- fila persistida será granular por agregado e tipo de record;
- atividade exige ação explícita para sync;
- `clientRecordId` será determinístico por `applicationId + aggregateType + aggregateId + recordType`;
- `clientRecordVersion` será monotônico para agregados mutáveis;
- atividade não criará `StepsRecord` separado;
- falha parcial será tratada por record complementar, sem desfazer sessão já sincronizada;
- permissões serão somente de escrita e agrupadas por capacidade;
- `WorkManager`, foreground service e leitura remota permanecem fora do escopo da fase.

### Reconciliação documental aplicada

- ADR-012 criada com foco na política duradoura da fase.
- roadmap da Fase 12 alinhado ao escopo write-only e granular.
- `health-connect-mapping.md` atualizado com política planejada do Champion Companion e diferenças deliberadas em relação ao MotionLog.
- spec e overview da Fase 12 atualizados para referenciar a ADR nova e registrar estabilização documental da fronteira da fase.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.

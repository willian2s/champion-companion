# 013-01 — Auditar fronteiras pós-Fase 15 para sessões, localização e correlação

- **Ticker:** `013`
- **Número:** `01`
- **Status:** pending

## Objetivo e resultado esperado

Produzir auditoria documental reproduzível do estado real pós-Fase 15, confirmando contratos, schema v5, limitações de `WorkoutActivity`, sessão, BLE esportivo, localização, foreground service, Health Connect e correlação. Resultado: relatório factual anexado nesta task e insumos fechados para ADR/contratos.

## Contexto

Planejamento encontrou mapper incompleto de sessão, repository parcial, atividade orientada a D3/D4, queries com join obrigatório de import, ausência de rota/FGS e protocolo esportivo live não confirmado. Execução deve revalidar contra branch corrente antes de qualquer mudança estrutural.

## Documentos obrigatórios

`AGENTS.md`; spec 013; baselines `current-baseline`, `feature-inventory`, `gaps-and-open-questions`, `health-connect-mapping`; roadmap; ADR-003/004/008/011/012/013; specs 010/011/012/015; overview 015; protocolos D0/F002, D3/D4, transporte e comandos.

## Dependências

Nenhuma. Primeira subtarefa elegível.

## Arquivos permitidos

- esta task e `013-00-overview.md`;
- documentos de auditoria/baseline listados, somente se correção factual mínima for necessária e explicitamente registrada.

## Arquivos e símbolos prováveis

`AppWorkoutSession`, `WorkoutActivity`, `AppWorkoutSessionRepository`, `ChampionDatabase`, DAOs/mappers de treino, `ForegroundBleOperationGate`, `HealthRecordMapper`, `AppDestination` e `ExercisesViewModel`.

## Arquivos proibidos

- código de produção, Gradle, manifesto, schemas Room;
- `app/src/test/**` e `app/src/androidTest/**`.

## Requisitos cobertos

Estado autoritativo das fases; comportamento atual; conflito do guard; DB v5; protocolo BPM esportivo; fronteira F13/F14; hotspots e riscos de migration/correlação.

## Escopo

- confirmar símbolos e joins reais;
- inventariar permissões/dependências/destinos atuais;
- confirmar ADR sequencial;
- confirmar suporte do SDK atual a `ExerciseRoute` sem promover upgrade;
- registrar divergências entre documentação e código.

## Fora do escopo

Decidir política final, alterar documentação autoritativa, implementar ou migrar.

## Passos de implementação

1. Registrar commit/branch auditados e `git status` inicial.
2. Repassar todos documentos e caminhos da spec.
3. Auditar domínio, entities, DAOs, mappers e repositories.
4. Auditar BLE D0/F002, FEA1, A8/D8, gate e reconexão.
5. Auditar Health Connect, identidade, versões, permissões e mapper de sessão.
6. Auditar manifesto, DataStore, Navigation3 e superfícies.
7. Produzir tabela fato/evidência/lacuna/impacto/task dona.
8. Atualizar somente registro desta task e overview após aceite.

## Critérios de aceite

- relatório cita caminhos e símbolos reais;
- v5 e próxima migration são confirmadas;
- limitações de sessão/atividade/joins são explícitas;
- protocolo live não é inventado;
- guard antigo e dívida ficam destacados;
- nenhuma mudança funcional ocorre.

## Validação manual

Revisão cruzada de caminhos, símbolos, ADRs e documentação. Nenhuma validação física nesta task.

## Comandos

Nenhum comando Gradle necessário. Não executar suites. Comandos de inspeção Git são permitidos.

## Evidências

Registrar commit auditado, tabela de achados, caminhos, decisões ainda abertas e `git status` final.

## Screenshots

Não aplicável; nenhuma mudança visual.

## Definição de pronto

Auditoria revisável registrada; `013-02` possui insumos sem suposições ocultas; item `013-01` marcado no overview somente após aceite.

## Riscos e cuidados

Documentação pode estar atrasada; código é evidência factual, mas não substitui direção do usuário. Não reabrir Fase 12/15.

## Rollback

Reverter somente alterações documentais desta task.

Não criar, alterar ou executar testes automatizados.

# 014-11 — Reprocessar atividades existentes do relógio

- **Ticker:** `014`
- **Número:** `11`
- **Status:** pending

## Objetivo e resultado esperado

Oferecer ação explícita e idempotente para derivar novamente atividades
`WATCH_D3_D4` existentes com contrato atual. Resultado: legado pode receber novo
snapshot sem mutação automática, perda de revisão ou sync implícito.

## Contexto

Migration de `014-03` deixa legado sem derivação. Alteração de perfil não pode
recalcular atividades antigas. Reimportação atual substitui intervalos e preserva
revisão/calibração conforme ADR-016.

## Requisitos cobertos

- R9 e preservação histórica de R1.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- contratos/repository de derivação e reimportação;
- `RoomRepositories.kt` e DAOs necessários;
- `ExercisesViewModel.kt` e superfície de detalhe/lista para ação explícita;
- recursos de texto e screenshot;
- overview.

Símbolos prováveis: `persistImported`, `reimport`, novo comando
`reprocessWatchActivity`, estado de confirmação e snapshot ativo.

## Arquivos proibidos

- parser/protocolo, schema/migration, Health Connect writer;
- Gradle e manifesto;
- atividades phone, GPS, rota e WorkManager.

## Escopo

- elegibilidade e confirmação;
- transação de novo snapshot;
- preservação de revisão, `recordedAt`, título, notas, calibração e workflow;
- comportamento de `SYNCED`/`SYNC_FAILED` sem resync automático.

## Fora do escopo

- reparse sem payload disponível;
- lote silencioso, abertura do app ou scheduler;
- alterar distância original/raw;
- testes automatizados.

## Passos de implementação

1. Listar atividades sem snapshot atual/elegível.
2. Expor ação com resumo do efeito e confirmação explícita.
3. Resolver inputs measured já persistidos; não exigir BLE.
4. Criar snapshot transacional com versão atual.
5. Preservar campos de usuário e ordem temporal da ADR-016.
6. Não preparar/enviar operações Health Connect automaticamente.
7. Para atividade sincronizada, exigir aviso específico e manter sync pendente
   somente após futura ação explícita.
8. Registrar screenshot e atualizar overview.

## Critérios de aceite

- Nenhuma atividade muda sem ação confirmada.
- Mesmos inputs/versão não criam revisão inútil.
- Revisão, `recordedAt`, título, notas e calibração permanecem.
- Atividade sem passo pessoal permanece sem derivação, com motivo claro.
- Reprocessamento não acessa BLE nem Health Connect.
- Mudança visual possui screenshot.

## Validação manual

- Reprocessar atividade pendente, revisada, sincronizada e sem perfil.
- Reiniciar app e conferir snapshot/workflow.
- Editar perfil e confirmar ausência de reprocessamento automático.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots da confirmação/resultado;
- matriz estado anterior → estado posterior;
- registro sanitizado de preservação de campos.

## Definição de pronto

Reprocessamento explícito, idempotente e local funciona; item `014-11` marcado.

## Riscos e cuidados

- Payload raw pode não existir; usar measured persistido ou falhar com segurança.
- Não reabrir revisão nem regredir operação `SYNCED` implicitamente.
- Concorrência com reimportação deve ser serializada/transacional.

## Rollback

Remover/ocultar ação e parar novos snapshots; preservar snapshots já criados e
não alterar estados remotos.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-11` e registrar estados validados.

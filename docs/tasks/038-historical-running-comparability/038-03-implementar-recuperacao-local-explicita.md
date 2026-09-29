# 038-03 — Implementar recuperação local explícita

- **Ticker:** `038`
- **Número:** `03`
- **Status:** `pending`

## Objetivo

Materializar nova derivação local somente para classes aprovadas pelo Gate A e
preflight, preservando lineage, snapshots históricos e contrato remoto.

## Resultado esperado

Uma ação individual, explícita, transacional e idempotente pode criar snapshot
novo reproduzível a partir de fonte autorizada. Falha não deixa mutação parcial.
Classes não aprovadas, legacy sem provenance e estados remotos permanecem sem
mutação.

Em `NO-GO` ou `INCONCLUSIVO`, fechar como não aplicável sem código.

## Requisitos cobertos

- Source/raw distinto de measured, derived snapshot e projection.
- Preservação de `parserVersion`, import identity e facts persistidos.
- Snapshot append-only, derived intervals/splits associados e workflow estável.
- Calibração separada, sem aprendizado ou efeito colateral.
- Zero Health Connect e zero reabertura de `SYNCED`.

## Escopo incluído

- Reusar pipeline atual de derivação e transação quando ela cumprir o Gate.
- Materializar somente interpretação/proveniência aprovada.
- Manter snapshot anterior inativo/histórico e novo snapshot reproduzível.
- Permitir ação individual explícita, sem assumir batch.
- Definir idempotência e comportamento de retry local.

## Escopo excluído

- Edição de enum para converter fallback em nativo.
- Alteração de parser version, bytes, CRC, header, raw/measured ou importação.
- Mutação de `SYNCED`, `SYNCING` ou projection child inseguro.
- Nova migration/coluna para classificação sem decisão independente.
- Chamada, criação, retry, delete ou reset Health Connect.
- Aprender perfil RUNNING, aplicar calibração atual em massa ou recalibrar outra
  atividade.

## Dependências

- `038-01` Gate `GO`/`PARTIAL-GO` e classe autorizada.
- `038-02` preflight aprovado sem escrita.
- ADR-018/019/022/029/038 e transação atual do repository.
- Decisão específica se provenance de recuperação não couber no modelo atual.

## Arquivos e símbolos prováveis

- `RoomRepositories.reprocessWatchWorkoutInternal` e pipeline
  `deriveWatchWorkoutInternal`.
- `WatchWorkoutDerivedMetrics.kt`, `DatabaseMappers.kt`, entidades de snapshot,
  intervalos derivados e splits.
- `RepositoryContracts.kt` para resultado/motivos explícitos.
- `ChampionDaos.kt` para operação atômica e idempotência.

## Passos de implementação

1. Limitar mutação às classes e workflows liberados pelo Gate.
2. Executar preflight dentro do contrato de consistência definido, sem alterar
   source facts.
3. Recalcular série a partir da fonte autorizada; não editar enum/version.
4. Criar snapshot novo e filhos na mesma transação, preservando anterior.
5. Manter workflow e calibration evidence, sem aprendizado implícito.
6. Confirmar que nenhuma operação HC é criada/modificada e que `SYNCED` falha
   fechado.
7. Repetir ação de forma idempotente sem duplicar snapshot ativo ou filhos.
8. Em Gate `NO-GO`, `INCONCLUSIVO` ou classe não aprovada, não implementar.

## Testes e comandos de validação

- Revisão de invariantes transacionais e diff de tabelas antes/depois em banco
  real autorizado numa execução futura.
- `git diff --check`.
- Validação manual física sem wipe e sem reimportação, quando task futura
  autorizar.
- Nenhum teste automatizado criado ou executado nesta execução de planejamento.

## Definição de pronto

- Só classes autorizadas podem mudar.
- Snapshot anterior permanece auditável; novo snapshot é reproduzível e ativo
  somente por política explícita.
- Raw/measured, parser/import identity, workflow, evidence e HC preservados.
- Operação é transacional, idempotente e sem efeito remoto.
- Falha de uma atividade não altera outra.
- NO-GO não gera mecanismo artificial.

## Riscos e cuidados

- Não confundir duração autoritativa recuperada com fonte nativa completa.
- Não usar perfil atual para reescrever distância histórica.
- Não permitir sync posterior automático como consequência da mutação local.
- Não assumir que `SYNC_FAILED` é seguro sem revisar children pendentes.
- Se provenance não couber nas colunas atuais, parar e registrar blocker.

# 038-06 — Validar fisicamente e fechar

- **Ticker:** `038`
- **Número:** `06`
- **Status:** `pending`

## Objetivo

Realizar validação física final somente se houver implementação autorizada, em
release incremental e banco real preservado, ou fechar documentalmente a 038
quando Gate A resultar em `NO-GO`/`INCONCLUSIVO`.

## Resultado esperado

Evidência sanitizada com total de corridas, recuperáveis, recuperadas,
fallbacks, bloqueadas, motivos agregados, 037 antes/depois, Room v20 e
Health Connect preservado. Cenários ausentes ficam `N/A`.

## Requisitos cobertos

- Gate A como decisão real.
- Validação sem wipe, sem reimportação e sem dados artificiais.
- Room v20 sem migration.
- Zero efeitos remotos e preservação do ledger HC.
- Fechamento legítimo em GO parcial, NO-GO ou sem evidência.

## Escopo incluído

- Instalação incremental release somente em execução futura com código aprovado.
- Inspeção read-only antes/depois, sem modificar banco físico.
- Navegação/recomposição para confirmar 037, quando aplicável.
- Registro de Room user_version, contagens e assinaturas sanitizadas.
- Atualização do overview, spec, ADR e evidência final com status correto.

## Escopo excluído

- Wipe, downgrade, criação de corrida/sample/fixture, reimportação ou
  reprocessamento para aumentar cobertura.
- Readback, sync, delete, retry ou reconciliação Health Connect.
- Nova migration, schema, parser, BLE, UI fora do Gate ou expansão de domínio.
- Testes automatizados criados/executados como parte desta execução documental.

## Dependências

- `038-01` com decisão Gate A.
- `038-05` com preservação e cálculo independente.
- Banco físico real/dump autorizado e dispositivo compatível quando a validação
  sair do estado documental.
- Invariantes ADR-038 e contrato 037.

## Arquivos e símbolos prováveis

- `docs/integration/038-06-physical-validation.md` em execução futura.
- `docs/specs/038-historical-running-comparability.md` e
  `docs/tasks/038-historical-running-comparability/038-00-overview.md`.
- `ChampionDatabase`/DAO read-only e read model da 037 para observação.
- Nenhum arquivo de código é necessário em NO-GO.

## Passos de implementação/validação

1. Confirmar decisão GO/PARTIAL-GO/NO-GO/INCONCLUSIVO antes de instalar algo.
2. Se não houver classe autorizada, registrar fechamento documental e N/A; não
   criar feature para preencher tarefas.
3. Se houver classe autorizada, instalar incrementalmente release sem wipe.
4. Capturar contagens agregadas antes/depois de activities, imports, headers,
   intervals, snapshots, derivados, splits, samples, calibração e HC.
5. Confirmar que 037 só ganhou pontos de snapshots válidos e que fallback segue
   fora dos gráficos.
6. Confirmar Room user_version 20 e ausência de migration/tabela/coluna/cache.
7. Confirmar `health_sync_operations`, IDs, versions, frozen payloads e
   reconciliation sem alteração causada pela recuperação.
8. Documentar motivos, limitações e cenários N/A; atualizar progresso somente
   após evidência concluída.

## Testes e comandos de validação

- `git diff --check`.
- Em execução futura com código alterado, builds/lint conforme `AGENTS.md` e
  validação física release; isso não é parte deste planejamento.
- Inspeção read-only e cálculo independente; nenhum teste automatizado criado
  ou executado nesta execução.

## Definição de pronto

- Evidência sanitizada contém totais, classes, motivos e 037 antes/depois.
- Room v20 confirmado, sem nova migration e sem mudança física indevida.
- Health Connect e workflow remoto preservados, sem readback.
- Fallbacks não recuperados continuam explicitamente fallback.
- `NO-GO`/`INCONCLUSIVO` fecha 038 sem código artificial e encaminha decisão
  futura somente se necessário.
- Overview marca somente subtarefas realmente concluídas.

## Riscos e cuidados

- Não confundir ausência de cenário com falha; usar `N/A`.
- Não registrar payload, bytes, IDs pessoais, MAC, CRC individual, BPM
  individual ou timestamp completo.
- Não executar instalação física sem Gate autorizado.
- Não alegar ganho de comparabilidade sem cálculo independente e provenance.

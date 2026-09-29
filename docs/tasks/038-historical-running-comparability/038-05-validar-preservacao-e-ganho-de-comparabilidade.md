# 038-05 — Validar preservação e ganho de comparabilidade

- **Ticker:** `038`
- **Número:** `05`
- **Status:** `pending`

## Objetivo

Provar, por comparação independente antes/depois, que qualquer aumento de
comparabilidade vem somente de fonte autorizada e que nenhum fato local ou
remoto não autorizado foi alterado.

## Resultado esperado

Relatório sanitizado com população 037 antes/depois, métodos/versões,
snapshots, intervals, splits, calibration evidence, workflow e
`health_sync_operations`, incluindo idempotência e motivos das corridas que
permanecem fallback ou bloqueadas.

Em `NO-GO`, confirmar que 037 continua com os 5 pontos observados e 16
fallbacks, sem promoção artificial.

## Requisitos cobertos

- Critérios gerais de aumento de comparabilidade.
- Preservação de source/measured e snapshots históricos.
- Reprodutibilidade da nova série e dos escalares 037.
- Nenhum efeito Health Connect ou reset remoto.
- Separação de recuperável físico e bloqueado por estado.

## Escopo incluído

- Comparar agregados e assinaturas sanitizadas antes/depois.
- Recalcular independentemente distância/tempo e critérios 037.
- Verificar que derived intervals/splits pertencem ao snapshot correto.
- Conferir calibration evidence e workflow sem alteração silenciosa.
- Contar operations HC e confirmar payloads/IDs/versions preservados.

## Escopo excluído

- Criar dados, fixtures, atividades ou samples.
- Ler ou republicar Health Connect.
- Alterar snapshot histórico para obter equivalência.
- Criar testes automatizados como etapa padrão.
- Validar WALKING, TREADMILL, CYCLING, phone ou Specs seguintes.

## Dependências

- Gate A e, se aplicável, preflight/recuperação/UI concluídos.
- Evidência 037 antes da mudança e banco físico preservado.
- Contratos ADR-018, ADR-029, ADR-037 e ADR-038.

## Arquivos e símbolos prováveis

- Evidências `docs/integration/037-06-physical-validation.md` e nova evidência
  sanitizada 038, sem substituir 037.
- `RunningPerformanceHistoryPolicy` e cálculo independente da 037.
- Entidades/DAOs de snapshots, derivados, splits, calibration evidence e HC.
- `HealthProjectionContracts` somente para conferir identidade/versionamento,
  não para escrever.

## Passos de implementação/validação

1. Capturar baseline sanitizado sem IDs e sem payload.
2. Executar ação autorizada somente em atividades elegíveis e explícitas.
3. Recontar população, classes, métodos, versões, workflow e estado remoto.
4. Recalcular 037 diretamente de `canonicalDistanceMeters` e
   `canonicalCoveredDurationNanos` do snapshot ativo.
5. Confirmar que cada ponto adicional tem source/provenance e predicado
   fechados; fallback não conta.
6. Comparar snapshots anteriores, derived intervals, splits, evidências de
   calibração e operations HC.
7. Repetir operação autorizada para verificar idempotência.
8. Registrar N/A para cenários naturais ausentes e fechar sem meta percentual.

## Testes e comandos de validação

- Recalculo independente e inspeção read-only do banco real.
- `git diff --check` para documentação/evidência.
- Nenhuma chamada Health Connect, readback ou teste automatizado criado ou
  executado nesta execução.

## Definição de pronto

- Preservação antes/depois documentada para todas as tabelas relevantes.
- Ganho 037 explicado somente por snapshots nativos novos válidos.
- Nenhuma operação HC nova, reset, delete, retry ou payload congelado reescrito.
- Idempotência e rollback/falha por atividade evidenciados.
- `NO-GO` mantém fallback explícito e 037 sem relaxamento.

## Riscos e cuidados

- Assinaturas devem ser sanitizadas e não revelar IDs ou bytes.
- Não usar contagem maior de pontos como prova suficiente.
- Não interpretar `SYNCED` local como readback do Health Connect.
- Não ocultar diferença entre atividade bloqueada e fonte não recuperável.

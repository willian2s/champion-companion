# 021-01 — Corrigir duração autoritativa D4

- **Ticker:** `021`
- **Número:** `01`
- **Status:** `pending`

## Objetivo

Corrigir a interpretação do fim temporal do layout D4 preferido, reconhecendo o
segundo final preservado no pacote 2 e preparando o contrato `d4-v3` sem ainda
ativar uma versão de parser com timeline parcial.

## Resultado esperado

O parser consegue resolver `startedAt`, `endedAt` e duração oficial com precisão
de segundo no layout preferido, mantendo o layout legado inalterado e sem usar
`recordCount × 60 s` como duração.

A troca efetiva do coordenador para `d4-v3` fica bloqueada até `021-02` entregar a
timeline normalizada completa.

## Contexto factual

O código atual:

- lê início preferido em `packet1[7..13]`;
- lê fim preferido em `packet1[14..19]`;
- completa `second = 0`;
- lê o pacote 2 sem nomear `byte[6]`;
- prioriza `Duration.between(startedAt, endedAt)` quando positiva.

Evidência física sanitizada mostra correspondência exata entre `packet2[6]` e os
segundos faltantes em três atividades observadas: `24`, `42` e `14` segundos.

## Dependências

- Spec 021;
- ADR-019;
- `AGENTS.md`;
- `docs/protocol/workouts-d3-d4.md`;
- Fase 014 concluída e parser `d4-v2` atual.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt` apenas para confirmar o runtime atual, sem ativar `d4-v3` nesta subtarefa;
- `docs/protocol/workouts-d3-d4.md`;
- ADR-018/ADR-019 quando necessário para alinhar supersessão;
- spec/overview/task 021.

## Escopo incluído

- modelar `endSecond` no parse do pacote 2 preferido;
- aceitar `endSecond` somente em `0..59`;
- compor o `endedAt` preferido com o segundo recuperado;
- manter o segundo do layout legado vindo do pacote 1;
- definir suporte de domínio para `d4-v3`, sem torná-lo runtime efetivo antes de
  `021-02`;
- separar claramente duração autoritativa de contagem de samples;
- atualizar protocolo com a nova evidência e seu limite de firmware;
- registrar comportamento para valor inválido sem inferência por `recordCount`.

## Escopo excluído

- normalizar durações dos samples;
- alterar `WorkoutDurationResolver` para a nova política final;
- reprocessar atividades existentes;
- migration Room;
- alterar snapshots, splits ou projeções Health Connect;
- corrigir calendário/fila;
- BLE novo ou nova coleta do relógio;
- testes automatizados.

## Passos de execução

1. Confirmar o layout preferido atual e preservar fallback legado.
2. Introduzir `endSecond` como parte explícita do resultado do pacote 2.
3. Resolver o fim preferido usando hora/minuto do pacote 1 + segundo do pacote 2.
4. Definir comportamento fail-closed para valor fora de `0..59`.
5. Introduzir a constante/compatibilidade de `d4-v3`, mas manter o importador
   runtime em `d4-v2` até a conclusão de `021-02`.
6. Atualizar protocolo/ADR com a fonte temporal correta.
7. Revisar diff para garantir que nenhum cálculo derive duração de `N × 60 s`.

## Critérios de aceite

1. Os três casos físicos conhecidos resolvem `15:28`, `20:45` e `29:15` quando
   seus dados preservados são interpretados pelo novo contrato.
2. O layout legado não sofre alteração de offsets ou semântica.
3. `packet2[6] > 59` não vira horário inventado.
4. `recordCount` não participa do cálculo da duração oficial.
5. Nenhuma atividade existente é alterada automaticamente.
6. Nenhum schema ou operação Health Connect é modificado nesta subtarefa.
7. O runtime não anuncia `d4-v3` antes de `021-02`.
8. Documentação distingue evidência física de garantia universal de firmware.

## Validação manual

Usar somente dados locais já autorizados e sanitizar saídas. Confirmar os três
pares duração antiga/segundo final/duração oficial sem registrar IDs ou payloads
integrais em documentação.

## Comandos permitidos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados.

## Definição de pronto

- duração preferida corrigida e revisão de layout concluída;
- `d4-v3` preparado, mas ainda não ativado parcialmente;
- protocolo e ADR alinhados;
- comandos e evidências registrados;
- overview atualizado;
- revisão independente sem bloqueadores.

## Riscos e rollback

- firmware diferente pode usar `byte[6]` de outra forma; manter range guard e
  classificação de evidência física limitada;
- ativar `d4-v3` cedo criaria versão semanticamente incompleta; rollback é manter
  coordinator em `d4-v2` até `021-02`;
- não alterar dados persistidos nesta subtarefa, tornando rollback local simples.

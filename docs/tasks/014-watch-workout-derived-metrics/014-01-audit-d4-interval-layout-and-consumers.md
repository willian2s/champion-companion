# 014-01 — Auditar D4, consumidores, legado e protocolo

- **Ticker:** `014`
- **Número:** `01`
- **Status:** pending

## Objetivo e resultado esperado

Confirmar, sem alterar comportamento, todos os pontos que produzem, persistem ou
consomem passos, duração, distância, calorias e BPM D4. Auditar também a
recuperabilidade de atividades `d4-v1`, as regras de workflow de esteira e as
afirmações conflitantes do protocolo.

Resultado: inventário factual versionado, matriz `código atual / dispositivo
confirmado / desconhecido / derivado` e fronteiras seguras para `014-02`.

## Dependências

- Nenhuma.

## Arquivos permitidos

- `docs/protocol/workouts-d3-d4.md`;
- `docs/baseline/source-map.md`;
- `docs/baseline/database-reference.md`;
- `docs/baseline/health-connect-mapping.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`;
- novo relatório sanitizado em `docs/integration/014-01-*`, se necessário.

Símbolos somente para leitura:

- `ChampionD4WorkoutParser`;
- `WorkoutInterval`, `WorkoutMetrics` e `WorkoutActivity`;
- `WorkoutActivityEntity` e `WorkoutIntervalEntity`;
- `DatabaseMappers` e `RoomRepositories`;
- consumidores de exercícios, calendário e Health Connect;
- workflow de revisão/calibração de `TREADMILL`;
- fila Health Connect e `operationSequence`.

## Arquivos proibidos

- todo `app/src/**` para escrita;
- Gradle, manifesto, migrations e schemas;
- documentos ou código histórico da Fase 013.

## Escopo

1. Rastrear D4 do transporte até persistência e consumidores.
2. Confirmar offsets, endianess e heurísticas preferido/legado.
3. Registrar conversões e perdas atuais, incluindo o u24 salvo como `steps`.
4. Identificar parser versions efetivamente persistidas e confirmar o contrato
   histórico de `d4-v1`.
5. Confirmar se o valor legado de `WorkoutIntervalEntity.steps` é recuperável
   como `rawMotionValue` sem depender de payload diagnóstico expirável.
6. Mapear índices ausentes, duplicados, primeiro índice e ordenação.
7. Mapear prioridade de distância em resumo, calendário, detalhe e Health Connect.
8. Mapear o bloqueio `PENDING_CALIBRATION` para `TREADMILL` importada do relógio.
9. Mapear a identidade natural e cardinalidade atual das operações Health Connect.
10. Inventariar testes existentes sem criar nem executar.
11. Corrigir o protocolo para distinguir:

```text
comportamento atual do código
semântica confirmada do dispositivo
semântica desconhecida
métrica derivada planejada
```

“Confirmado pelo código” não deve significar “confirmado pelo relógio”.

## Fora do escopo

- corrigir parser, domínio ou persistência;
- atribuir significado aos 16 bits inferiores;
- escolher schema final;
- criar fixture ou teste automatizado;
- reabrir GPS, rota, BLE live ou treino phone.

## Critérios de aceite

- Caminhos e símbolos exatos estão inventariados.
- Exemplo `0x0064002B` permanece separado em raw `6.553.643`, passos `100` e
  componente inferior desconhecido.
- Sequência observada soma `3.737` passos em cálculo externo não persistido.
- O relatório confirma ou rejeita a ponte `d4-v1 steps → rawMotionValue`.
- Lacunas e duplicatas são documentadas como bloqueadores de série V1.
- Workflow atual de esteira e impactos da nova regra estão identificados.
- Cardinalidade atual da fila Health Connect está registrada.
- Distância/calorias temporais permanecem raw.
- Nenhuma mudança funcional ou de schema ocorre.

## Validação

```bash
git diff --check
git status --short
```

Não executar Gradle nem testes.

## Evidências

- matriz de campos e consumidores;
- matriz parser version → persistência conhecida;
- matriz origem/estado → workflow de esteira;
- referências de arquivo/símbolo;
- divergências e perguntas residuais sanitizadas.

## Definição de pronto

Auditoria revisada, protocolo factual alinhado e item `014-01` marcado no
overview. Nenhum arquivo de produção alterado.

## Rollback

Reverter somente documentação desta subtarefa.

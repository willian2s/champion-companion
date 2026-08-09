# 014-01 — Auditar D4, consumidores, legado e protocolo

- **Ticker:** `014`
- **Número:** `01`
- **Status:** completed
- **Concluída em:** 2026-08-05

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

## Escopo executado

1. D4 foi rastreado do transporte até persistência e consumidores.
2. Offsets, endianess e heurísticas preferido/legado foram registrados sem
   promover heurística a semântica do dispositivo.
3. Conversões e perdas atuais foram inventariadas, incluindo o u24 salvo como
   `steps`.
4. A versão de produção `d4-v1` foi confirmada no coordenador de importação e na
   persistência da importação.
5. A recuperação de `WorkoutIntervalEntity.steps` como `rawMotionValue` foi
   confirmada sob guardas de origem, versão e faixa u24.
6. Índices ausentes, duplicados, primeiro índice e ordenação foram mapeados.
7. A prioridade de distância foi mapeada em parser, UI, calendário e Health
   Connect.
8. O bloqueio `PENDING_CALIBRATION` para `TREADMILL` foi confirmado.
9. A identidade e cardinalidade atuais da fila Health Connect foram registradas.
10. Suites existentes foram inventariadas sem criação ou execução.
11. O protocolo foi reescrito para distinguir comportamento do código,
    confirmação física, desconhecido e derivado planejado.

## Fora do escopo preservado

- corrigir parser, domínio ou persistência;
- atribuir significado aos 16 bits inferiores;
- escolher schema final;
- criar fixture ou teste automatizado;
- reabrir GPS, rota, BLE live ou treino phone.

## Resultado factual

### Parser e campo empacotado

O parser `d4-v1` executa atualmente:

```text
bytes[7..9]
  → u24 little-endian
  → WorkoutInterval.steps
  → WorkoutIntervalEntity.steps
```

O exemplo:

```text
0x0064002B
```

foi classificado como:

```text
rawMotionValue = 6.553.643
decodedSteps   = 100
lower16        = 43, significado desconhecido
```

A sequência sanitizada fornecida soma exatamente `3.737` passos após
`rawMotionValue ushr 16`.

### Ponte `d4-v1`

A ponte foi confirmada somente quando:

```text
origin == WATCH_D3_D4
parserVersion == d4-v1
0 <= legacy steps <= 0xFFFFFF
```

A migration poderá copiar:

```text
rawMotionValue = legacy steps
```

sem preencher `decodedSteps`, derivar métricas ou alterar workflow. A
decodificação permanece ação explícita posterior.

### Índices e tempo

O collector registra ausentes e duplicados; o parser mantém os pacotes presentes
e persiste todos os intervalos com 60 segundos. Não existe hoje:

- exigência de índices contínuos `0..N-1`;
- duração efetiva residual;
- motivo tipado de inelegibilidade;
- plausibilidade temporal.

Essas ausências são fronteiras obrigatórias de `014-02`.

### Distância e consumidores

A prioridade atual é, conforme o consumidor:

```text
calibrada → original → estimada
```

O parser também calcula ritmo e velocidade usando a distância original. Nenhum
consumidor funcional atual usa a soma de intervalos canônicos.

Distância e calorias temporais foram classificadas como raw, pois suas unidades e
semânticas no dispositivo não estão comprovadas.

### Esteira

`WorkoutActivity.confirmReview` envia todo tipo confirmado `TREADMILL` para
`PENDING_CALIBRATION`, sem distinguir `WATCH_D3_D4`. A regra nova precisa ser
fechada em `014-02` sem alterar contratos phone históricos.

### Health Connect

A chave de domínio atual contém somente:

```text
aggregate + recordType
```

`operationSequence` existe no schema, mas não representa filhos temporais de
atividade. O mapper cria um único `DistanceRecord` agregado e o coordinator
insere exatamente um record por operação. `SPEED` não existe.

## Evidências

Relatório principal:

```text
docs/integration/014-01-d4-layout-and-consumer-audit.md
```

Documentos alinhados:

```text
docs/protocol/workouts-d3-d4.md
docs/baseline/source-map.md
docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md
docs/roadmap/champion-companion-roadmap.md
```

## Critérios de aceite

| Critério | Resultado |
| --- | --- |
| Caminhos e símbolos exatos inventariados | PASS |
| Raw, passos e componente inferior separados | PASS |
| Sequência sanitizada soma `3.737` | PASS |
| Ponte `d4-v1 steps → rawMotionValue` confirmada | PASS, com guardas explícitas |
| Lacunas e duplicatas documentadas como bloqueadores V1 | PASS |
| Workflow de esteira identificado | PASS |
| Cardinalidade atual da fila registrada | PASS |
| Distância/calorias temporais classificadas como raw | PASS |
| Código, schema, migration e testes inalterados | PASS |

## Validação

Não foram executados Gradle ou testes, conforme o escopo.

O trabalho foi realizado pelo conector GitHub, sem checkout local disponível.
Por isso, os comandos abaixo não foram executados neste ambiente:

```bash
git diff --check
git status --short
```

A validação disponível foi feita pela comparação remota da branch com `main` e
pela inspeção dos arquivos alterados. A ausência de checkout local está
registrada como limitação operacional, não como `PASS` presumido dos comandos.

## Definição de pronto

- auditoria factual concluída;
- relatório sanitizado criado;
- protocolo e source map alinhados;
- critérios da task satisfeitos;
- item `014-01` marcado no overview;
- nenhum arquivo de produção, schema ou migration alterado.

## Fronteiras para 014-02

1. raw, measured, derived e projection separados;
2. ponte `d4-v1` tipada e fail-closed;
3. parser corrigido versionado como `d4-v2`;
4. índices contínuos `0..N-1` para série V1;
5. duração efetiva separada de plausibilidade;
6. distância canônica igual à soma dos intervalos;
7. workflow específico de `TREADMILL` para `WATCH_D3_D4`;
8. BPM por split com sample-and-hold;
9. `SpeedRecord` no ponto médio do intervalo;
10. uma operação por filho Health Connect.

## Rollback

Reverter somente documentação desta subtarefa. Não existe alteração de dados ou
comportamento para desfazer.

# 014-01 — Auditar layout D4 de intervalos e consumidores

- **Ticker:** `014`
- **Número:** `01`
- **Status:** pending

## Objetivo e resultado esperado

Confirmar, sem alterar comportamento, todos os pontos que produzem, persistem ou
consomem passos, duração, distância, calorias e BPM dos intervalos D4. Resultado:
inventário factual versionado, matriz `confirmado/desconhecido/derivado` e
fronteiras seguras para contratos de `014-02`.

## Contexto

Parser atual lê `bytes[7..9]` como u24, fixa 60 segundos e trata distância e
calorias temporais como semânticas. Evidência de campo empacotado exige auditoria
antes de mudar domínio ou schema.

## Requisitos cobertos

- R1, R3 e comportamento atual da spec.
- Preservação da Fase 013 e classificação rigorosa do protocolo.

## Dependências

- Nenhuma.

## Arquivos permitidos

- `docs/protocol/workouts-d3-d4.md`;
- `docs/baseline/source-map.md`;
- `docs/baseline/database-reference.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`;
- novo relatório sanitizado em `docs/integration/014-01-*`, se necessário.

Símbolos somente para leitura: `ChampionD4WorkoutParser`, `WorkoutInterval`,
`WorkoutActivityEntity`, `WorkoutIntervalEntity`, `DatabaseMappers`,
`RoomRepositories`, `ExercisesScreen`, `LocalCalendarRepository` e
`HealthRecordMapper`.

## Arquivos proibidos

- Todo `app/src/**`;
- Gradle, manifesto, migrations e schemas;
- documentos ou código histórico da Fase 013.

## Escopo

- offsets, endianess e layouts preferido/legado;
- sequência de referência e soma `3.737`;
- total versus soma temporal;
- consumidores locais e Health Connect;
- inventário de testes existentes, sem execução.

## Fora do escopo

- corrigir parser;
- atribuir significado aos 16 bits inferiores;
- capturar GPS, rota ou BLE live;
- criar fixture ou teste automatizado.

## Passos de implementação

1. Rastrear pacote D4 desde transporte até persistência.
2. Registrar campos raw, conversões e perdas atuais.
3. Mapear ordem de preferência de distância em todos os consumidores.
4. Classificar cada afirmação como confirmada, desconhecida ou derivada.
5. Documentar divergências que bloqueiem `014-02`, sem transformar hipótese em
   contrato.
6. Atualizar overview após revisão e conclusão real.

## Critérios de aceite

- Inventário identifica caminhos e símbolos exatos.
- `rawMotionValue`, `decodedSteps` e componente inferior desconhecido ficam
  separados.
- Distância/calorias temporais permanecem raw.
- Nenhuma mudança funcional, de schema ou de Fase 013 ocorre.

## Validação manual

- Conferir sequência observada e soma com cálculo externo não persistido.
- Revisar matriz contra código atual e ADR-017.
- Verificar que documentação não registra payload sensível.

## Comandos permitidos

```bash
git diff --check
git status --short
```

Não executar Gradle nem testes.

## Evidências

- Matriz de campos e consumidores;
- referências de arquivo/símbolo;
- divergências e perguntas residuais sanitizadas.

## Definição de pronto

Auditoria revisada, documentação factual alinhada e item `014-01` marcado no
overview. Nenhum arquivo de produção alterado.

## Riscos e cuidados

- Layout depende do índice do pacote; não generalizar offsets.
- Valor observado não prova comportamento em todo firmware.
- Não registrar bytes completos, IDs pessoais ou dados de saúde em logs.

## Rollback

Reverter somente documentação desta subtarefa; nenhum dado ou schema muda.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md` no mesmo trabalho. Marcar `014-01` somente após
aceite e ajustar progresso para `1/14`.

# ADR-020 — Separação entre fases do roadmap e tickers de trabalho

- **Status:** aceita
- **Data:** 2026-08-08

## Contexto

Durante a evolução do Champion Companion, diversos trabalhos executáveis receberam tickers numéricos próximos aos números das fases do roadmap. Em alguns casos houve coincidência intencional, como a Fase 014 e a spec `014-watch-workout-derived-metrics`. Em outros, o ticker identificou apenas uma correção posterior, como a Spec/Task `021` de normalização D4 e integridade do calendário.

Tratar esses números como uma única sequência cria falsa equivalência entre unidade de trabalho e etapa macro do produto. Isso também induz agentes a interpretar uma task corretiva como execução de uma fase histórica com o mesmo número.

Ao mesmo tempo, a direção do produto mudou após a Fase 15: algumas fases futuras antigas ficaram obsoletas por mudanças de escopo, uso pessoal do app e abandono de treinos iniciados pelo celular/GPS.

## Decisão

### 1. Fases e tickers são eixos independentes

- **Fase** representa uma etapa macro do roadmap do produto.
- **Ticker** identifica uma spec/task executável.
- Não existe regra `ticker == fase`.
- Coincidências numéricas não criam dependência semântica.
- Um ticker já utilizado não marca automaticamente uma fase com o mesmo número como executada.

Exemplo normativo:

```text
Spec/Task 021
≠
Fase 21
```

### 2. Fases removidas não são renumeradas

Quando uma fase futura deixa de fazer sentido:

- registrar explicitamente sua remoção;
- preservar o número histórico;
- não deslocar fases posteriores;
- não reciclar o número removido para uma nova fase.

Isso mantém referências históricas estáveis.

### 3. Trabalhos corretivos não reabrem fases concluídas por padrão

Uma correção relacionada a funcionalidade implementada numa fase anterior pode receber novo ticker sem reabrir a fase macro. Reabertura de fase exige decisão explícita.

A Spec/Task `021` permanece trabalho corretivo posterior sobre D4/calendário e não altera o status da Fase 14 nem executa a antiga Fase 21.

### 4. Direção pós-Fase 15

A direção autoritativa é registrada em `docs/roadmap/post-phase-15-direction.md`.

Resumo:

- Fase 16: mantida, revisar antes do planejamento;
- Fase 17: mantida e simplificada, revisar antes do planejamento;
- Fase 18: removida;
- Fase 19: mantida, revisar antes do planejamento;
- Fase 20: mantida e reescrita sem GPS/rota/distribuição pública;
- Fases 21–25: removidas.

### 5. Uso pessoal é premissa de roadmap

O Champion Companion é um aplicativo pessoal e não possui objetivo de distribuição pública.

Portanto, não são gates macro do roadmap:

- beta formal;
- matriz multi-device/multi-fabricante;
- paridade obrigatória com MotionLog;
- lançamento público;
- cobertura ampla de testes como fase isolada.

Testes, validações físicas, builds e comparações continuam podendo ser exigidos por tasks específicas quando necessários.

### 6. GPS e treinos phone não retornam por fases futuras

A direção watch-first definida após a Fase 13 permanece válida. GPS, localização de sessão, rota, mapa, `ExerciseRoute` e novos treinos iniciados pelo celular não fazem parte do roadmap futuro.

Código, migrations, schemas e dados históricos da Fase 13 não são removidos por esta ADR. Qualquer limpeza técnica exige trabalho explícito e separado.

## Consequências

- documentação deixa de inferir fase pelo número da spec/task;
- trabalhos corretivos podem evoluir sem distorcer o roadmap macro;
- histórico de fases permanece estável mesmo quando fases futuras são removidas;
- agentes devem consultar a direção pós-Fase 15 antes de planejar nova fase;
- Fases 16, 17, 19 e 20 precisam de auditoria do estado atual antes de qualquer spec nova;
- antigas Fases 18 e 21–25 permanecem apenas como histórico obsoleto no roadmap completo;
- não há autorização implícita para remover código legado.

## Alternativas rejeitadas

### Renumerar todas as fases restantes

Rejeitado porque quebraria referências históricas e confundiria documentação já concluída.

### Fazer ticker acompanhar obrigatoriamente a fase

Rejeitado porque correções e refinamentos posteriores precisam de identidade própria sem reabrir etapas macro.

### Marcar fases obsoletas como concluídas

Rejeitado porque isso afirmaria execução que não ocorreu. Fase obsoleta deve ser registrada como removida.

### Manter Fases 21–25 apenas por completude do roadmap

Rejeitado porque criaria trabalho artificial sem necessidade para um aplicativo pessoal.

## Relação com documentos

- `docs/roadmap/README.md` — regra de leitura da pasta de roadmap;
- `docs/roadmap/post-phase-15-direction.md` — direção autoritativa após a Fase 15;
- `docs/roadmap/champion-companion-roadmap.md` — histórico completo preservado;
- `docs/baseline/current-baseline.md` — estado factual do produto;
- `AGENTS.md` — regras operacionais para agentes.

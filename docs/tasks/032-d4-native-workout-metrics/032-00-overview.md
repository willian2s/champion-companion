# 032 — Overview: D4 v4, métricas nativas e calibração de distância

- **Spec:** `docs/specs/032-d4-native-workout-metrics.md`
- **ADR:** `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`
- **Status:** `in_progress`
- **Progresso:** `1/8`
- **Branch de integração:** `feat/032-d4-native-workout-metrics`

## Objetivo

Corrigir o parser D3/D4 e promover a distância nativa do Champion C001 para a
arquitetura de treino sem perder a normalização temporal já validada, mantendo
legado reproduzível, fallback por comprimento de passo e Health Connect explícito.

## Sequência e paralelismo

```text
032-01 Congelar evidência e contratos       ✓
   ↓
   ├───────────────┐
   ↓               ↓
032-02 Parser    032-03 Persistência/schema
   │               │
   └───────┬───────┘
           ↓
032-04 Domínio + adapters + reprocessamento
           ↓
   ┌───────┴────────┐
   ↓                ↓
032-05 Distância  032-06 Calibração/aprendizado
   └───────┬────────┘
           ↓
032-07 Integração repository/UI/Health Connect
           ↓
032-08 Validação física e fechamento
```

## Estratégia para trabalhar em paralelo com a Spec 031

A branch `feat/031-google-health` está à frente de `main` e altera principalmente
UI/design. A 032 nasceu de `main` para evitar dependência desnecessária da 031.

### Ownership inicial da 032

Até `032-06`, evitar alterações em:

- `feature/exercises/ExercisesScreen.kt`;
- `data/RoomRepositories.kt`, salvo impossibilidade técnica documentada;
- arquivos de design system ou navegação da 031.

Priorizar:

- `core/bluetooth`;
- `core/model`;
- `core/database`;
- `data/DatabaseMappers.kt` quando isolável;
- documentação/protocolo;
- componentes novos específicos de calibração sem alterar shell visual.

### Momento de integração

`032-07` é o ponto deliberado para:

1. atualizar a branch 032 para a `main` que já contenha a 031, se disponível;
2. resolver conflitos em `RoomRepositories.kt`, `ExercisesScreen.kt` e contratos
   compartilhados conscientemente;
3. preservar o redesign visual e encaixar somente os novos estados/ações.

Não fazer merges cruzados frequentes entre 031 e 032 durante parser/schema.

## Branches/worktrees sugeridos

Depois da `032-01`, parser e persistência estão formalmente independentes e podem
ser executados em paralelo a partir da mesma revisão da branch de integração:

```text
feat/032-d4-native-workout-metrics
├── task/032-02-d4-v4-parser
└── task/032-03-d4-v4-persistence
```

No ambiente local, usar um `git worktree` por branch permite duas sessões/agentes
sem compartilhar working tree:

```bash
git worktree add ../champion-032-parser task/032-02-d4-v4-parser
git worktree add ../champion-032-schema task/032-03-d4-v4-persistence
```

Depois de uma subtarefa revisada, integrar na branch 032; não integrar direto em
`main` enquanto a spec estiver aberta.

## Checklist

- [x] `032-01-auditar-e-congelar-layout-d4-v4.md` — contrato congelado e protocolo atualizado
- [ ] `032-02-implementar-parser-d3-d4-v4.md`
- [ ] `032-03-persistir-fatos-nativos-e-provenance.md`
- [ ] `032-04-integrar-dominio-adapters-e-reprocessamento.md`
- [ ] `032-05-derivar-distancia-nativa-e-metricas.md`
- [ ] `032-06-implementar-calibracao-de-distancia-contextual.md`
- [ ] `032-07-integrar-produto-health-connect-e-spec-031.md`
- [ ] `032-08-validar-fisicamente-e-fechar.md`

## Resultado da 032-01

Contrato autoritativo:

`docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`.

Achados congelados:

- D3 usa posição física de slot; CRC zero não pode comprimir `blockIndex`;
- header fixo D4 é reconstruído em 42 bytes a partir dos pacotes 1–3;
- sample fixo: BPM, `sportValue`, passos u16, calorias raw u16, distância u16 e pausa raw;
- 34 blocos completos fecharam 34/34 em `recordCount`, passos, distância,
  calorias raw, sport count e CRC;
- distância está confirmada em metros para o layout fixo observado;
- calorias usam escala `raw / 1000.0 = kcal`;
- `sportValue` permanece raw sem unidade funcional inventada;
- pausa permanece raw por falta de caso físico com pausa no conjunto auditado;
- timeline `T/N` da Spec 021 permanece autoritativa;
- persistência v4 deve usar campos semanticamente novos sem reutilizar o
  significado legacy de `rawMotionValue/rawDistanceValue/rawCaloriesValue`;
- divergência entre header e série preserva ambos os lados e falha fechada para
  distância nativa canônica.

`docs/protocol/workouts-d3-d4.md` foi reescrito para separar runtime vigente
`d4-v3` do contrato congelado `d4-v4` e remover lacunas já encerradas pela nova
evidência.

### Preflight local antes de 032-02/032-03

A sessão de planejamento usa o conector GitHub e não possui checkout local da
branch. Antes de começar código em um worktree local, executar:

```bash
git checkout feat/032-d4-native-workout-metrics
git pull
git diff --check main...HEAD
```

Não há build necessário para `032-01`, pois nenhum arquivo de runtime foi
alterado.

## Guard rails

- preservar `T/N` da Spec 021;
- não transformar `recordCount` em duração;
- não usar total de passos do header para substituir a série temporal;
- não criar GPS de produção ou integração Adidas/Strava;
- não mudar `CYCLING` implicitamente;
- não converter calibrações antigas de passo para fator de distância;
- não alterar atividades `SYNCED` silenciosamente;
- não criar efeitos Health Connect durante migration/reprocessamento;
- não criar testes automatizados sem pedido explícito;
- cada task atualiza este overview.

## Critério de merge da branch 032

A branch só está pronta para `main` quando:

- parser v4, schema, derivação e provenance estão coerentes juntos;
- runtime não publica `d4-v4` parcial;
- 031 foi integrada ou os conflitos previstos foram resolvidos contra uma base
  comum explícita;
- validação física demonstrou pelo menos um treino com distância nativa;
- `./gradlew lint assembleDebug assembleRelease` e `git diff --check` passam.

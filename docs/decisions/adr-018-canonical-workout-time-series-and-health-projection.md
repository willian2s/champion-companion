# ADR-018 — Série temporal canônica e projeção Health Connect de treinos

- **Status:** aceita como decisão documental da Fase 014
- **Data:** 2026-08-05

## Contexto

ADR-017 definiu direção de produto: Champion C001 controla início e fim, a
distância canônica de `WATCH_D3_D4` deriva de passos válidos e comprimento
pessoal do passo, e sincronização permanece explícita. Estado atual mistura
valores brutos, interpretações medidas e métricas calculadas; intervalos têm
duração fixa; Health Connect recompõe um agregado mutável na hora do envio.

Esta ADR define mecanismo durável para reprodução, temporalidade e idempotência.
Não redefine abandono de treinos phone, fórmula de produto, política write-only
geral nem limites de GPS/rota já registrados na ADR-017 e ADR-012.

## Decisão

### 1. Separação raw, measured e derived

Dados de treino obedecem fluxo unidirecional:

```text
raw de origem
    ↓ normalização versionada
measured
    ↓ derivação determinística
derived snapshot
    ↓ projeção congelada
filhos Health Connect
```

- **Raw:** valor recebido, sem semântica não comprovada.
- **Measured:** fato normalizado com semântica confirmada, incluindo passos
  decodificados, timestamps, BPM e totais elegíveis.
- **Derived:** distância canônica, duração efetiva, cadência, velocidade, ritmo,
  acumulado, splits e motivos tipados de ausência.

Revisão do usuário não sobrescreve raw ou measured. Reimportação pode substituir
revisão measured somente preservando fatos históricos exigidos pela ADR-016 e
sem mudar snapshot usado por operação em retry.

### 2. `stepLengthMeters`

Contratos novos usam `stepLengthMeters` para distância de um passo individual.
Valor pessoal precisa ser positivo, finito, persistido com origem e confirmado
explicitamente. Sugestão não aceita não é perfil. Valor de referência pessoal
não é constante global nem fallback.

### 3. Snapshot por atividade

Derivação de cada atividade congela pelo menos:

- atividade e revisão measured de origem;
- `stepLengthMetersUsed`;
- `stepLengthSource`;
- `derivationMethod = FIXED_PERSONAL_STEP_LENGTH`;
- `derivationVersion`;
- `derivedAt` para auditoria;
- política temporal e resultado/ausência da série.

Mudança posterior de perfil não altera snapshot. `derivedAt` não é versão de
record remoto. Versão do parser e versão da derivação são conceitos distintos.

### 4. Último intervalo parcial

Com duração total `T`, `N` intervalos e quantum `Q = 60 s`:

- `T == NQ`: todos os intervalos usam `Q`;
- `(N−1)Q < T < NQ`: primeiros `N−1` usam `Q`; último usa o restante;
- `T <= (N−1)Q`: série temporal é inelegível, pois último intervalo não teria
  duração positiva; fatos permanecem preservados;
- `T > NQ`: todos usam `Q`; cauda permanece sem intervalo e sem síntese.

Índices ausentes não são preenchidos. Janelas válidas são positivas, ordenadas,
não sobrepostas e contidas na sessão.

### 5. Distância e série canônicas

Somente passos measured e `stepLengthMetersUsed` produzem distância canônica.
Distância original permanece raw/measured diagnóstica. Não há escala para fechar
total, distribuição de residual, suavização ou fallback.

Total canônico pode existir sem série temporal. Série, splits e projeções
granulares exigem intervalos temporal e contabilmente elegíveis. Intervalo com
zero passos mantém distância zero e não produz record positivo.

### 6. Projeções filhas Health Connect

Atividade possui manifesto local congelado de projeções:

```text
Workout projection
├── ExerciseSessionRecord       1
├── DistanceRecord              0..N
├── SpeedRecord                 0..1, com amostras
├── HeartRateRecord             0..1, com amostras reais
└── TotalCaloriesBurnedRecord   0..1
```

Filho é conceito local; Health Connect não fornece chave estrangeira. Falha de
um complemento não desfaz sessão ou outro complemento. Retry usa payload
congelado, não recalcula atividade mutável.

### 7. IDs e versões

- Records únicos mantêm identidade canônica existente quando compatível.
- Filho temporal usa atividade, tipo e índice estável da fonte.
- ID não contém passo pessoal, BPM, distância, timestamp de processamento ou
  outro valor sensível.
- ID agregado legado não é reutilizado como primeiro filho.
- `clientRecordVersion` é monotônico por `clientRecordId`.
- Retry idêntico reutiliza ID, versão e payload.
- Mudança de payload usa versão maior, sem regressão frente a versões legadas.

### 8. Reconciliação e remoção

Projeção nova persiste conjunto esperado, enviado e obsoleto. Remoção usa apenas
IDs próprios conhecidos e exatos. Agregado legado conhecido deve ser removido
antes da série para impedir dupla contagem; checkpoint local permite retomar
janela temporária sem distância. Sem identidade legada conhecida, operação falha
fechada. Não usar leitura remota nem deleção ampla por tempo.

### 9. Reprocessamento explícito

Derivação inicial pode acompanhar nova importação. Atividade existente só recebe
novo snapshot após ação explícita. Mudança de perfil, migration, abertura do app,
reimportação ou scheduler não recalcula silenciosamente. Reprocessar não
ressincroniza; novo envio exige ação explícita e preserva revisão, `recordedAt`,
título, observações, calibração e dados de origem.

## Consequências

- histórico reproduzível;
- diagnóstico preservado sem contaminar métrica canônica;
- splits e Health Connect usam mesma série;
- retry permanece idempotente mesmo após edição local;
- evolução Room aditiva e maior volume local;
- fan-out exige processamento finito e retomável;
- delete-before-insert cria janela remota sem distância.

## Alternativas rejeitadas

- recalcular dinamicamente no mapper ou writer;
- reutilizar distância original sem proveniência;
- sobrescrever intervalos importados com derivados;
- um `DistanceRecord` agregado;
- IDs por valor, UUID de linha Room ou posição mutável;
- reprocessamento/resync automático;
- leitura Health Connect ou delete temporal amplo.

## Relação com decisões anteriores

- **ADR-017:** direção de produto e distância canônica.
- **ADR-012:** Room local-first, Health Connect write-only, falha granular e
  sync explícito.
- **ADR-016:** integridade temporal e `recordedAt` na reimportação.
- **ADR-014:** registro histórico, sem autorização nova de rota phone.

## Limites

Esta ADR não define schema SQL final, fórmula nova de sugestão por altura,
WorkManager, GPS, rota, mapa, BLE live, calorias por intervalo, suavização,
remoção de legado da Fase 013 ou implementação já concluída.

# 033-06 — Integrar Health Connect granular e cutover

- **Status:** pending
- **Depende de:** 033-04 e 033-05
- **Evidência obrigatória:** `docs/integration/033-03-d1-v2-physical-validation.md`

## Objetivo

Projetar dados diários granulares sem coexistir com total diário FEA1 no mesmo
dia, sem presumir que D1 e D4 sejam fontes temporalmente exclusivas, e remover
auto-sync remoto do watch sync.

## Correção de premissa

A validação física mostrou que D1 já pode conter passos, distância e calorias do
workout D4. Portanto, está rejeitada a estratégia genérica:

```text
D1 fora do workout + D4 dentro do workout
```

como regra automática para todos os records.

A projeção Health Connect deve ser definida por métrica e somente publicar
representações cuja não sobreposição possa ser provada localmente.

## Escopo

- persistir data de cutover local;
- preservar datas/operações pré-cutover;
- pós-cutover bloquear novas projeções STEPS de `DailySummary`;
- usar FEA1 como total de referência/validação para passos;
- preparar granularidade D1 conforme contratos de 033-04;
- preservar `WorkoutSteps`/`STEPS_CADENCE` da Spec 027 sem publicar os mesmos
  passos duas vezes;
- tratar DISTANCE D1 e DISTANCE D4 como potencialmente sobrepostos até prova de
  cobertura/substituição;
- HEART_RATE pode preferir D4 em workout quando houver cobertura D4 válida;
- IDs estáveis, versões monotônicas e payload congelado;
- preservar ledgers/credits legacy e não removê-los por suposição;
- remover chamadas automáticas a `syncPending()` de Home/D1;
- manter `Settings -> Sincronizar agora` e sync explícito de workout.

## Passos

Para datas pós-cutover:

```text
FEA1 = total diário esperado
D1   = candidato natural à distribuição granular
D4   = passos/cadência do workout
```

A implementação deve garantir que a soma efetivamente publicada para passos não
represente D1 e D4 duplicados.

Se `WorkoutSteps` continuar sendo publicado, reutilizar ou estender a
contabilidade/guards da Spec 027 conforme necessário. Não desativar crédito ou
bloqueio apenas porque o Daily cumulativo antigo deixou de existir.

Se não houver prova segura de como coexistir D1 granular e `WorkoutSteps`, falhar
fechado para a operação conflitante em vez de publicar ambos.

`STEPS_CADENCE` permanece independente da quantidade diária e pode continuar
usando a série D4 elegível.

## Distância

D1 fornece distância diária nativa e D4 fornece distância canônica do workout.
Não publicar ambos de forma sobreposta sem prova determinística de cobertura.

Uma estratégia de substituição é aceitável somente quando a implementação puder
identificar com segurança a contribuição D1 correspondente ao mesmo fato D4.
Sem essa prova, manter o valor local e bloquear a projeção conflitante em vez de
duplicar distância no provider.

## Frequência cardíaca

D1 continua elegível por ponto BPM válido. Pontos D1 dentro de atividade podem
ser omitidos da projeção combinada somente quando houver cobertura D4 válida para
frequência cardíaca naquele workout. Não descartar D1 apenas pela existência da
janela da atividade.

## Calorias

Somente implementar record Health Connect de calorias se a semântica for fechada
com evidência suficiente. Caso contrário, manter local e documentar `deferred`.

Escalas locais permanecem distintas:

```text
D1 raw / 10.0
D4 raw / 1000.0
```

## Guard rails

- sem backfill granular histórico;
- sem delete remoto amplo;
- sem reabrir `SYNCED` silenciosamente;
- sem leitura Health Connect;
- sem `D1 + D4` ingênuo para métricas compartilhadas;
- sem supressão de bucket D1 apenas por overlap temporal;
- falhar fechado quando não houver prova de representação não sobreposta.

## Validação

Além dos builds, validar fisicamente que a projeção de passos pós-cutover não
ultrapassa o total FEA1 por duplicação de D1/workout e que retry mantém a mesma
identidade/payload.

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

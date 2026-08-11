# 022-03 — Adaptar métricas e UI locais para bicicleta

- **Status:** `pending`
- **Depende de:** `022-02`
- **Desbloqueia:** `022-04`

## Objetivo

Tornar todos os consumers locais type-aware para que `CYCLING` nunca reutilize
métricas derivadas de passos como distância, velocidade, pace, cadência ou
splits, preservando ao mesmo tempo duração, BPM, calorias e dados raw.

## Política funcional

Quando `confirmedType == CYCLING`:

```text
Distância
→ manualDistanceMeters, se presente
→ ausente, caso contrário

Velocidade média
→ manualDistanceMeters / duração oficial, se ambos válidos
→ ausente, caso contrário

Pace
→ ausente

Cadência
→ não usar steps/min como cadência de bicicleta

Splits por km
→ não usar série derivada de passos
```

Snapshots e intervalos derivados existentes permanecem persistidos para
proveniência/diagnóstico, mas deixam de ser elegíveis como métricas de ciclismo.

## Consumers a adaptar

Usar o relatório `022-01` como checklist autoritativo. Incluir pelo menos:

- detalhe da atividade;
- cards/lista de exercícios;
- calendário e detalhe diário;
- agregações/históricos locais que usem distância de treino;
- labels e formatação de tipo;
- qualquer helper de distância final/velocidade/pace/splits;
- estados vazios/ausentes.

## UI de detalhe

Com distância:

```text
Bicicleta
Duração          <valor>
Distância        <km>
Velocidade média <km/h>
FC média         <valor quando disponível>
FC máxima        <valor quando disponível>
Calorias         <valor quando disponível>
```

Sem distância:

```text
Bicicleta
Duração          <valor>
Distância        —
Velocidade média —
FC média         <valor quando disponível>
Calorias         <valor quando disponível>
```

Não exibir como métrica principal de bicicleta:

- pace;
- cadência de passos;
- splits derivados por distância de passos;
- distância estimada por passada.

Passos D4 podem continuar existindo no modelo e diagnóstico, mas não devem ser
promovidos visualmente como deslocamento da bicicleta.

## Regras de cálculo

- usar duração oficial já persistida pela política D4 vigente;
- não persistir velocidade média redundante se puder ser calculada de forma
  determinística no read model;
- não alterar `watch-d3-d4-v2`;
- não recalcular/reescrever snapshots ao trocar o tipo confirmado;
- consumers devem falhar fechados quando distância manual estiver ausente ou
  inválida.

## Validação

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção manual de atividade cycling com e sem distância;
- confirmar que running/walking/treadmill mantêm apresentação vigente.

## Fora do escopo

- Health Connect;
- RPM/power;
- GPS/rota;
- alteração de parser;
- testes automatizados novos.

## Critérios de aceite

- [ ] nenhum consumer local usa distância canônica por passos para `CYCLING`;
- [ ] velocidade média só existe com distância manual válida;
- [ ] pace, cadência de passos e splits de passos não aparecem como métricas de bicicleta;
- [ ] BPM/calorias/duração continuam disponíveis;
- [ ] snapshots raw/derived continuam preservados;
- [ ] calendário/históricos respeitam ausência de distância;
- [ ] outros tipos mantêm comportamento existente;
- [ ] build/lint passam;
- [ ] revisão independente aprova a mudança.

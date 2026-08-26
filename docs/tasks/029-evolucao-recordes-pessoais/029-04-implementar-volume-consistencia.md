# 029-04 — Implementar volume e consistência

- **Ticker:** `029`
- **Número:** `04`
- **Status:** `completed`
- **Depends on:** `029-03`

## Objetivo

Entregar os agregados básicos de treinamento por período e filtro sem inferência fisiológica.

## Escopo

Calcular, conforme elegibilidade definida em `029-01`:

- quantidade de atividades;
- distância total;
- duração total;
- passos em atividades;
- dias ativos;
- série de volume por bucket temporal apropriado.

## Regras

- distância soma somente distâncias finais semanticamente válidas;
- passos de `CYCLING` não entram como volume esportivo de passos;
- duração usa janela oficial/final da atividade;
- múltiplas atividades no mesmo dia contam como um único dia ativo;
- valores ausentes permanecem ausentes quando zero mudaria a semântica.

## Apresentação

Preparar dados para cards de resumo e gráficos Champion da Spec 028, sem Vico direto na feature.

## Critérios de aceite

1. Totais reconciliam com as atividades elegíveis do período.
2. Dias ativos não duplicam por múltiplos treinos no mesmo dia.
3. `CYCLING` não ganha passos de performance derivados do D4.
4. Distâncias incompatíveis/ausentes não são inventadas.
5. Série temporal respeita filtros e períodos.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/EvolutionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionVolumeAggregator.kt`.

### Decisões e desvios

- agregação recebe somente `EvolutionReadSnapshot`, portanto filtros, período e
  população já foram aplicados pelo read model;
- totais preservam ausência por métrica e usam soma protegida para distância,
  duração e passos;
- dias ativos usam a data civil de `startedAt` no timezone da consulta e
  múltiplas atividades no mesmo dia contam uma vez;
- série diária emite pontos de atividade, distância, duração, passos e dias
  ativos; dias sem atividade são `GAP`, métricas indisponíveis são `ABSENT` e
  passos de buckets exclusivamente `CYCLING` são `NOT_APPLICABLE`;
- `toEvolutionSnapshot()` mantém volume integrado ao snapshot completo sem
  incluir tendências, comparação ou recordes desta subtarefa;
- nenhum Vico, Room, migration ou teste foi adicionado.

Não houve desvios dos requisitos. A série foi modelada em granularidade diária,
adequada aos períodos 7/30/90 dias e aos gráficos posteriores.

### Comandos executados e resultados

- `git diff --check` — passou;
- `./gradlew app:compileDebugKotlin app:compileReleaseKotlin` — passou;
- `./gradlew lint assembleDebug assembleRelease` — passou após repetição com
  limite ampliado; primeira execução excedeu o limite de 120 segundos;
- nenhum teste automatizado foi criado ou executado, conforme guard rail.

### Evidências

- totais usam somente atividades do `EvolutionReadSnapshot`, já filtradas por
  período, tipo e elegibilidade;
- dias ativos usam datas civis únicas derivadas de `startedAt` no timezone da
  consulta;
- distância, duração e passos preservam contagens de valores presentes e
  ausência quando nenhum valor válido existe;
- série diária preserva `GAP`, `ABSENT` e `NOT_APPLICABLE` sem fabricar zero;
- `CYCLING` nunca recebe passos de performance;
- lint e builds debug/release concluíram com `BUILD SUCCESSFUL`.

### Riscos residuais

- o histórico completo continua sendo carregado antes da seleção do período;
  projeção temporal limitada permanece uma otimização futura, se necessária;
- ausência de testes automatizados deixa cenários de DST e overflow dependentes
  da validação de build e da inspeção do código, conforme guard rail do projeto;
- integração com UI e gráficos pertence às subtarefas posteriores.

### Próxima etapa

`029-05` permanece pendente e é a próxima subtarefa elegível; `029-06` em
diante continuam bloqueadas pela sequência definida no overview.

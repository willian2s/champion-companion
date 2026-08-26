# 029-03 — Implementar períodos e filtros por esporte

- **Ticker:** `029`
- **Número:** `03`
- **Status:** `completed`
- **Depends on:** `029-02`

## Objetivo

Implementar janelas temporais e filtros que definem a população usada por todas as agregações da Spec 029.

## Escopo

- períodos 7, 30 e 90 dias;
- período anterior equivalente para comparação;
- filtro consolidado e por tipo elegível;
- limites temporais determinísticos no timezone local vigente;
- ordenação estável das atividades;
- exclusão coerente de atividades inelegíveis.

## Regras

- o período atual e o anterior não se sobrepõem;
- filtros são aplicados antes das agregações;
- modalidades com semântica incompatível não são misturadas em métricas específicas;
- atividade antiga entra no período correspondente à data real da atividade, não à data de importação.

## Critérios de aceite

1. 7/30/90 dias retornam populações determinísticas.
2. Comparação usa janela anterior do mesmo tamanho.
3. Filtro por esporte é aplicado antes dos cálculos.
4. Data de importação não desloca atividade antiga para período incorreto.
5. Estados vazios não geram valores sintéticos.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`;

### Decisões e desvios

- a seleção e a ordenação da população foram encapsuladas em API privada do
  read model, mantendo filtro antes da projeção e agregações posteriores;
- a ordenação permanece determinística por `startedAt` ASC e `activityId` ASC;
- IDs repetidos deixaram de ser deduplicados silenciosamente: a invariável do
  snapshot agora faz a leitura falhar fechada como dado local inválido;
- períodos civis, janela semiaberta, período anterior, timezone informado,
  `metrics.startedAt`, filtros por esporte e estados elegíveis já estavam
  cobertos pelos contratos/read model de `029-02` e foram preservados;
- nenhuma entidade, DAO, migration, schema ou teste foi alterado.

### Comandos executados e resultados

- `git diff --check` — passou;
- `./gradlew app:compileDebugKotlin app:compileReleaseKotlin` — passou;
- `./gradlew lint assembleDebug assembleRelease` — a primeira execução excedeu
  o limite de 120 segundos após concluir parte do build; a repetição com limite
  ampliado concluiu com `BUILD SUCCESSFUL`;
- nenhum teste automatizado foi criado ou executado, conforme guard rail.

### Evidências

- população é filtrada por origem `WATCH_D3_D4`, tipo confirmado, parser
  `VALID`/`PARTIAL`, workflow elegível, janela atual e filtro selecionado;
- seleção usa `metrics.startedAt`, sem deslocamento por `importedAt` ou
  `recordedAt`;
- ausência de métricas continua nula e estado vazio não fabrica zeros;
- compilação Kotlin debug/release concluída;
- lint e assemble debug/release concluídos com `BUILD SUCCESSFUL`.

### Riscos residuais

- o read model continua carregando atividades completas antes de filtrar o
  período; projeção temporal limitada só será necessária se o histórico crescer;
- a validação de duplicidade agora reporta `INVALID_LOCAL_DATA` em vez de
  descartar uma cópia, comportamento fail-closed intencional.

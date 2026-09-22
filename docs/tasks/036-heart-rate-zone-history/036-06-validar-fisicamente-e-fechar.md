# 036-06 — Validar fisicamente e fechar

- **Ticker:** `036`
- **Número:** `06`
- **Status inicial:** `pending`

## Objetivo e resultado esperado

Validar a distribuição histórica sobre banco e workouts reais existentes,
comprovar invariantes numéricas e ausência de efeitos persistentes/remotos, e
fechar Spec, ADR e overview somente com evidência ou N/A justificado.

## Requisitos cobertos

- Todos os critérios de aceite da Spec 036.
- Room v20, fonte bounded da 035, policy 034, UI contextual e fallback gráfico.
- Preservação de `WorkoutActivity`, workflow, snapshots, derivados, splits e
  Health Connect.

## Escopo incluído

- `git diff --check`.
- `./gradlew lint assembleDebug assembleRelease`.
- Validação física com banco/workouts existentes, sem novo treino ou sample.
- Cálculo independente das zonas individuais, current7/current28 e buckets.
- Verificação de percentuais sobre tempo analisado e coverage ratio separado.
- Verificação de carga zero e workout sem análise quando naturais.
- Mudança temporária/restauração de FC máxima e comparação dos resultados.
- Comparação sanitizada de fatos persistidos e operações Health Connect.
- Reopen/recompose sem persistência analítica.
- Screenshot sanitizada e fechamento documental.

## Escopo excluído

- Criar/fabricar workout, sample, fixture ou estado artificial.
- Migration, tabela, coluna, cache ou snapshot.
- Alterar parser, BLE, derivação, workflow, Health Connect ou design system
  compartilhado.
- Criar testes automatizados novos.
- Documentar BPM individual, activity IDs, MAC, payload BLE ou dados pessoais
  desnecessários.

## Dependências

- `036-02`, `036-03`, `036-04` e `036-05` concluídas.
- Banco e workouts reais já usados nas validações 034/035.
- Acesso a instalação debug/release e evidência sanitizada.

## Arquivos e símbolos prováveis

- `docs/specs/036-heart-rate-zone-history.md`.
- `docs/decisions/adr-036-heart-rate-zone-history.md`.
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`.
- Evidência física em `docs/integration/036-06-physical-validation.md`, se
  criada conforme padrão de fechamento.
- `ChampionDatabase`, `WorkoutActivity`, snapshots e
  `health_sync_operations` somente para inspeção.

## Passos de implementação/validação

1. Confirmar diff somente em arquivos autorizados e Room `user_version = 20`.
2. Registrar que FC máxima atual foi reutilizada sem expor valor desnecessário.
3. Comparar zonas individuais com a Spec 034.
4. Calcular independentemente current7/current28 e verificar seis zonas,
   duração, percentuais e cobertura.
5. Somar buckets por zona e comparar com current28.
6. Verificar carga zero e workout sem análise naturalmente existentes; registrar
   N/A quando ausentes.
7. Alterar FC máxima temporariamente, observar recomposição e restaurar valor.
8. Comparar hashes/assinaturas de activities, workflow, snapshots, derivados,
   splits e `health_sync_operations` antes/depois; inspecionar call paths para
   atribuir qualquer operação Health Connect à inicialização preexistente ou à
   feature, sem prometer imutabilidade absoluta quando o ruído não puder ser
   isolado.
9. Reabrir a página e confirmar que analytics não foram persistidos.
10. Capturar screenshot sanitizada, sem dados pessoais desnecessários.
11. Registrar evidência, N/A, riscos residuais, status e progresso 6/6.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validações físicas obrigatórias:

- Room v20 e nenhuma migration/tabela analítica;
- fonte bounded e policy 034 preservadas;
- fechamentos por zona de 7/28 dias e buckets;
- percentuais com denominador analisado;
- coverage ratio separado;
- parcialidade e ausência distinguíveis;
- recomposição/restauração por FC máxima;
- fatos, workflow, snapshots, derived intervals, splits e Health Connect sem
  mutação causada pela feature;
- screenshot sanitizada.

Não criar testes automatizados novos. N/A exige motivo, evidência disponível e
risco residual; não libera critério numérico obrigatório sem cálculo
independente.

## Definição de pronto

- Todos os critérios exercitáveis têm evidência ou N/A justificado.
- Nenhum cenário foi fabricado.
- Room permanece v20 e não há persistência analítica.
- Distribuição fecha por período e por dia.
- UI e screenshot mostram labels acessíveis e estados factuais.
- Spec, ADR, overview e esta subtarefa têm status/progresso coerentes.

## Riscos e cuidados

- Restaurar FC máxima original antes de encerrar.
- Não confundir ausência natural com zero válido.
- Preservar risco da query sem índice temporal composto sem criar migration.
- Considerar o risco documental 035 de operações `HEART_RATE` criadas na
  inicialização e não atribuí-lo automaticamente à 036.
- Sanitizar toda evidência física.

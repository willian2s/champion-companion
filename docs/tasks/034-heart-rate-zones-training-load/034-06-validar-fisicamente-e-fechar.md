# 034-06 — Validar fisicamente e fechar

- **Ticker:** `034`
- **Número:** `06`
- **Status:** `pending`

## Objetivo

Provar a entrega em banco real atualizado e workout existente, confirmar
invariantes numéricos e ausência de efeitos Health Connect, registrar screenshot
do detalhe e fechar Spec/ADR/overview sem exigir treino novo.

## Resultado esperado

Evidência física sanitizada mostra migration v19 -> v20, persistência da FC
máxima, cálculo independente de zonas/carga/cobertura, recálculo visual após
alteração da configuração e preservação de workflow e operações remotas.

## Requisitos cobertos

- todos os requisitos R1 a R7;
- todos os critérios de aceite da Spec 034;
- fechamento documental e atualização do overview para `completed` somente após
  os gates realmente concluídos.

## Escopo incluído

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- instalação/execução debug sobre banco real existente;
- confirmação de `user_version=20`;
- comparação antes/depois de perfil, samples, activities, snapshots, workflow e
  `health_sync_operations`;
- configuração/alteração/limpeza de FC máxima;
- cálculo independente das zonas, cobertura e Edwards TRIMP;
- estados sem samples e sem configuração;
- screenshot do detalhe atualizado;
- registro de PASS, N/A justificado ou risco residual; `N/A` de cenário físico
  não libera critério numérico obrigatório.

## Escopo excluído

- exigir importação de treino novo;
- readback Health Connect;
- criar testes automatizados;
- corrigir parser/BLE/Health Connect ou alterar contratos fora da Spec 034;
- esconder cenário não exercitado como aprovado.

## Dependências

- `034-02`, `034-03`, `034-04` e `034-05` concluídas;
- dispositivo Champion C001 e instalação debug disponíveis;
- banco físico com pelo menos um workout existente e amostras, quando possível;
- ferramenta sanitizada para observar schema/operações sem expor dados sensíveis.

## Arquivos e símbolos prováveis

- `docs/integration/034-06-physical-validation.md` ou evidência equivalente,
  se a execução de implementação autorizar sua criação;
- `docs/specs/034-heart-rate-zones-training-load.md`;
- `docs/decisions/adr-034-heart-rate-zones-training-load.md`;
- `docs/tasks/034-heart-rate-zones-training-load/034-00-overview.md`;
- todos os arquivos alterados pelas subtarefas anteriores.

## Passos de implementação

1. Capturar estado inicial sanitizado: `user_version`, perfil, activities,
   samples, snapshots, workflow e operações Health Connect.
2. Instalar build com migration e confirmar `user_version=20` sem wipe.
3. Confirmar dados anteriores intactos e `maxHeartRateBpm=NULL` em perfil antigo.
4. Salvar FC máxima válida, reiniciar e confirmar persistência.
5. Abrir workout existente com samples e comparar resultado da policy com cálculo
   independente em tabela externa/sanitizada.
6. Exercitar fronteiras de razão disponíveis e sample acima da máxima; registrar
   cobertura, soma de buckets, percentuais e carga.
7. Alterar FC máxima e confirmar somente mudança visual derivada.
8. Exercitar sem configuração e sem samples, sem exigir novo treino.
9. Recomparar workflow, snapshots e `health_sync_operations`; confirmar zero
   efeitos da feature.
10. Capturar screenshot do detalhe atualizado.
11. Executar gates técnicos e registrar limitações/N/A honestamente.
12. Atualizar overview e status somente conforme evidência real.

## Testes e comandos de validação

Obrigatórios:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar testes automatizados novos. Não exigir treino novo: usar atividade
existente e samples já persistidos.

## Definição de pronto

- migration preserva banco e `user_version=20` é observado;
- FC máxima persiste após reinício;
- zonas batem com cálculo independente;
- soma de durações fecha cobertura;
- percentuais usam cobertura;
- Edwards TRIMP bate com cálculo independente;
- workout permanece no mesmo workflow;
- fila Health Connect não ganha nem altera operações;
- mudança de FC máxima altera somente análise visual;
- screenshot do detalhe atualizado existe e está sanitizada;
- overview marca apenas subtarefas realmente concluídas e progresso correto.

## Riscos e cuidados

- Não marcar PASS quando fixture não exercitar caso; registrar N/A e risco
  residual, mantendo task bloqueada se critério obrigatório ficar sem prova.
- Não usar readback remoto como evidência de mudança permitida.
- Não incluir BPM individual, payload BLE, MAC, identificadores ou dados pessoais
  desnecessários em evidência/screenshot.
- Não atualizar `SYNCED`, snapshots ou workflow para facilitar demonstração.
- Se build falhar, registrar erro concreto e manter task bloqueada; não simular
  fechamento.

## Registro da execução

- **Status:** `pending` — nenhuma validação física executada nesta fase de
  planejamento.
- **Arquivos alterados:** nenhum arquivo de produto; evidência física será criada
  somente durante implementação/validação.
- **Decisões e desvios:** fechamento exige matriz independente para zonas,
  cobertura e TRIMP; N/A não substitui critério obrigatório.
- **Comandos executados:** não executar assemble/lint nesta fase documental por
  orientação do usuário; comandos obrigatórios permanecem definidos acima para
  o gate de implementação.
- **Resultados e evidências:** requisitos de migration, user_version, perfil,
  workout existente, workflow, fila remota e screenshot estão enumerados.
- **Riscos residuais:** banco físico, dispositivo e fixture com fronteiras de
  zona ainda dependem da execução futura; ausência de prova mantém bloqueio.

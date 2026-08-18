# 024-14 — Refatorar detalhe de exercício

- **Ticker:** `024`
- **Número:** `14`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar detalhe de atividade importada para separar resumo, tendências,
revisão e ações de workflow. Resultado: tela densa mais compreensível sem
alterar derivação, calibração, descarte ou sync.

## Requisitos e critérios cobertos

- R4–R10; critérios 5, 7–13 da spec.

## Escopo incluído

- header/retorno, resumo, métricas, gráfico BPM, série, intervalos e splits;
- tipo, distância manual quando aplicável, título e observações;
- calibração externa, plausibilidade, reprocessamento, descarte, exclusão e sync;
- diálogos e estados loading/not found/failure/ready.

## Escopo excluído

- derivação, snapshots, repository, Health Connect, parser ou schema;
- rota, GPS, mapa ou treino phone;
- mudança nas condições que habilitam ações.

## Dependências

- `024-13` concluída;
- atividade real acessível sem produzir dados artificiais.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`;
- charts locais usados no ramo de detalhe;
- `ExercisesScreen` com `openedActivityId`, detail content e dialogs.

## Passos de implementação

1. Inventariar todos os estados/workflows antes de editar.
2. Priorizar identidade da atividade, status e métricas principais.
3. Separar tendências/gráficos de revisão e ações.
4. Distinguir ações primárias, secundárias, destrutivas e irreversíveis.
5. Preservar alterações não salvas e confirmações de saída/dialogs.
6. Manter snapshots, calibração, exclusão e sync exatamente como hoje.
7. Validar ramos disponíveis; marcar demais `N/A` e registrar evidências.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir detalhe, rolar seções, editar e cancelar sem salvar, dialogs sem
confirmar ação destrutiva, voltar, fonte `1,3x`, landscape e temas.

## Definição de pronto

- resumo, gráficos, revisão e workflow têm seções claras;
- condições, intents, dialogs e guards equivalentes;
- nenhuma ação destrutiva usada para evidência;
- nenhum GPS/rota/mapa introduzido;
- comandos, capturas sanitizadas e overview registrados.

## Riscos e cuidados

- maior hotspot visual/funcional da entrega;
- excluir/reprocessar/sincronizar pode ser irreversível ou remoto;
- screenshots contêm títulos, observações e saúde;
- não reescrever snapshots nem alterar origem dos dados.

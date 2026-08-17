# 024-12 — Refatorar lista de Exercícios

- **Ticker:** `024`
- **Número:** `12`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar lista/importação de Exercícios com ação principal explícita, filtros e
atividades compactas. Resultado: workflow local compreensível sem mudar D3/D4,
deduplicação, cancelamento ou Health Connect.

## Requisitos e critérios cobertos

- R4–R10; critérios 5, 7–13 da spec.

## Escopo incluído

- header, CTA `Buscar treinos no relógio`, progresso e retry;
- pendências, resumo por workflow, filtro e lista de atividades;
- feedback secundário Health Connect;
- cards/status e abertura do detalhe.

## Escopo excluído

- detalhe de atividade;
- coordinators D3/D4, parser, persistência, filtros de domínio ou sync;
- seleção/preparação/treino phone.

## Dependências

- `024-11` concluída.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`;
- ramo de lista de `ExercisesScreen`, header, importação, filtros e activity
  cards;
- `ExercisesViewModel` somente como referência.

## Passos de implementação

1. Isolar no diff somente ramo de lista/importação.
2. Priorizar CTA de importação e feedback operacional.
3. Compactar resumo e filtros mantendo todos os workflows.
4. Estruturar activity row/card com tipo, momento, métricas e status.
5. Preservar cancelamento, retry, deduplicação e confirmação de saída.
6. Garantir que nenhum ponto de entrada phone reapareça.
7. Validar estados reais e registrar evidências/overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: filtros, abertura do detalhe, retorno e importação somente se operação
real for segura; fonte `1,3x`, landscape e temas.

## Definição de pronto

- CTA, progresso, filtros e lista têm hierarquia clara;
- todos os workflows continuam representados;
- operações bloqueantes e confirmação preservadas;
- nenhum fluxo phone/GPS/rota visível;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- arquivo compartilha ramo de detalhe; evitar diff cruzado;
- não iniciar importação apenas para screenshot;
- títulos livres e métricas exigem sanitização;
- Health Connect continua secundário e explícito.

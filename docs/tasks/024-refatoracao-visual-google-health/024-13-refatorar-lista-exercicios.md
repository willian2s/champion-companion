# 024-13 — Refatorar lista de Exercícios

- **Ticker:** `024`
- **Número:** `13`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar lista/importação de Exercícios com ação principal explícita, filtros e
atividades compactas. Resultado: workflow local compreensível sem mudar D3/D4,
deduplicação, cancelamento ou Health Connect.

## Requisitos e critérios cobertos

- R4–R10; critérios 5, 7–13 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

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

- `024-12` concluída.

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

## Registro de execução

- **Status:** completed.
- **Arquivos alterados:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`.
- **Decisões:** header nominal azul-petróleo; CTA, progresso, retry, filtros,
  cards compactos e feedback secundário foram reorganizados sem tocar contratos
  de importação, revisão, deduplicação ou sync. Lista passou a exibir cinco
  atividades por página, com navegação anterior/próxima e reinício de página ao
  trocar filtro; troca de página reposiciona a âncora do título no início da
  lista, somente após ação explícita.
  `CYCLING` usa `DirectionsBike`. Títulos da lista são normalizados sem truncar
  conteúdo.
- **Desvios:** bloco `Pendências e fluxo` removido por solicitação explícita do
  usuário. O título `Todos os treinos importados` foi reduzido para `Todos os
  treinos`.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`;
  `adb devices -l`; `adb shell am start -n
  br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  smoke ADB com rolagem, filtros, paginação, abertura de detalhe e retorno;
  captura sanitizada via `adb exec-out screencap -p` e `magick`.
- **Resultados e evidências:** todos os comandos passaram. Build debug/release,
  lint e instalação concluídos; hierarquia ADB confirmou CTA, importação,
  filtros, paginação `Página 2 de 3`, cards com tipo/momento/métricas/status,
  ícone de bicicleta e âncora `Todos os treinos • 14` no topo após paginação.
  Fonte `1,3x`, portrait escuro,
  landscape claro e restauração para fonte `1,0x`, portrait escuro foram
  exercitados. Evidência em
  `024-13-captura-real-sanitizada-exercicios.png`. Nenhuma importação foi
  iniciada para gerar evidência.
- **Riscos residuais:** estados transitórios de importação, retry e erro não
  foram fabricados; cancelamento, retry e deduplicação continuam cobertos por
  preservação de código, mas não foram disparados fisicamente. Revisão
  independente final aprovada, sem bloqueadores.

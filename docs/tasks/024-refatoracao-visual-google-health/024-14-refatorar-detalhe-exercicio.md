# 024-14 — Refatorar detalhe de exercício

- **Ticker:** `024`
- **Número:** `14`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe de atividade importada para separar resumo, tendências,
revisão e ações de workflow. Resultado: tela densa mais compreensível sem
alterar derivação, calibração, descarte ou sync.

## Requisitos e critérios cobertos

- R4–R10; critérios 5, 7–13 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

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

## Registro de execução

### Status

`completed`; detalhe refatorado e ajuste visual final aplicado após feedback do
usuário sobre retorno, badges e card de status/origem. Revisão independente
encontrou origem histórica genérica e validação física incompleta; ambos foram
corrigidos e revalidados.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `024-14-captura-real-sanitizada-detalhe-exercicio.png`.

### Decisões e desvios

- Header separado do conteúdo, com retorno por `IconButton` de 48 dp, descrição
  acessível e ícone branco sobre azul-petróleo nominal do Champion.
- Badges deixaram de usar `AssistChip` desabilitado. Foram substituídos por
  superfícies não acionáveis com borda, preenchimento tonal e acentos nominais
  `primary`/`secondary`/`tertiary`, agrupadas em `FlowRow`.
- Card `Status e origem` passou a concentrar workflow, tipo, parser, origem e
  derivação canônica. Mensagem, estado e metadados técnicos foram separados sem
  alterar condições, intents, snapshots ou guards.
- Origens phone históricas agora usam rótulo explícito e seguro `sessão local
  legada`, evitando apresentar qualquer origem como importação local genérica.
- Resumo, tendências, detalhamento, revisão/calibração, workflow e ações locais
  permanecem visualmente separados. Ações de descarte e exclusão continuam no
  fim, com confirmação intacta.
- Nenhuma alteração em derivação, calibração, descarte, exclusão, sync,
  repository, Health Connect, parser, schema, GPS, rota ou treino phone.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew app:compileDebugKotlin` | Primeira tentativa falhou por remoção acidental do import `AssistChip` ainda usado na lista; import restaurado e compilação repetida com sucesso |
| `./gradlew app:compileDebugKotlin` | Passou após ajuste final |
| `./gradlew lint assembleDebug assembleRelease` + `./gradlew installDebug` em paralelo | Primeira execução conjunta falhou em `dexBuilderDebug` por corrida nos intermediários; não foi falha de código |
| `./gradlew lint assembleDebug assembleRelease` sequencial | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Smoke ADB detalhe | Passou; abertura, retorno, rolagem, resumo, tendências, intervalos, splits, revisão e badges/status confirmados |
| Smoke ADB edição/saída | Passou; edição temporária, confirmação `Descartar alterações?` e cancelamento sem salvar observados |
| Smoke ADB acessibilidade | Passou; `Voltar para exercícios`, headings e descrição do status/origem expostos pela hierarquia |
| Smoke ADB fonte `1,3x`, portrait | Passou; header, retorno e card `Status e origem` permaneceram visíveis sem colisões observadas |
| Smoke ADB fonte `1,3x`, landscape | Passou; rotação `1`, rolagem revelou card de status, badges e derivação sem erro |
| Smoke ADB tema claro/dynamic padrão | Passou; status, origem e derivação visíveis em landscape |
| Smoke ADB tema escuro/dynamic padrão | Passou; status, origem e derivação visíveis em landscape |
| Restauração ADB | Passou; fonte `1,0x`, portrait, rotação `0`, tema escuro e retorno à lista de Exercícios |
| Captura sanitizada | Passou; status bar, timestamp e dados métricos mascarados em `024-14-captura-real-sanitizada-detalhe-exercicio.png` |
| Revisão independente final | Passou; nenhum bloqueador ou achado importante |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Hierarquia ADB confirmou botão de retorno, card `Status e origem`, quatro
  badges, derivação canônica e headings de resumo.
- Retorno pelo header voltou à lista de Exercícios sem alterar atividade.
- Fonte `1,3x`, landscape e temas claro/escuro com dynamic colors padrão foram
  repetidos após o ajuste final; hierarquia permaneceu navegável e sem clipping
  observado nos estados acessíveis.
- Captura sanitizada registra header, retorno, badges, card de status/origem e
  início do resumo sem expor timestamp ou métricas pessoais.

### Riscos residuais

- Estados transitórios de loading, erro, reprocessamento, calibração, sync e
  exclusão não foram fabricados; permanecem `N/A` nesta validação.
- Conteúdo real de títulos e observações não foi persistido nem usado na captura.

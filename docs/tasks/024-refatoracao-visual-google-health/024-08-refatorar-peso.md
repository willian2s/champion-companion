# 024-08 — Refatorar peso

- **Ticker:** `024`
- **Número:** `08`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe de peso para destacar valor atual, registro manual e histórico.
Resultado: edição simples, feedback claro e ausência segura, preservando dados em
quilogramas e histórico local.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, valor atual, origem/data, editor, mensagens e histórico;
- estados vazio, salvando, sucesso e erro;
- adaptação de teclado, rolagem e ações.

## Escopo excluído

- unidade imperial, meta de peso, cálculo ou schema;
- alteração de `HealthViewModel`, repository ou validação de domínio.

## Dependências

- `024-07` concluída.

## Arquivos e símbolos prováveis

- `feature/health/HealthMetricDetailScreens.kt`;
- `WeightDetailScreen` e Composables locais de editor/histórico.

## Passos de implementação

1. Aplicar padrão de detalhe sem misturar estado com composição.
2. Priorizar valor atual e CTA de registro.
3. Tornar salvar/cancelar e feedback distinguíveis.
4. Preservar quilogramas, timestamps, origem e histórico.
5. Validar vazio e mensagens reais sem criar registro fictício.
6. Conferir teclado, fonte ampliada, landscape e retorno.
7. Registrar evidências e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir/fechar editor, cancelar sem persistir, rolar histórico; evitar
salvar valor apenas para screenshot.

## Definição de pronto

- valor, editor e histórico têm hierarquia clara;
- cancelamento não altera dados;
- unidade e contratos preservados;
- layout funciona com teclado/fonte/landscape;
- capturas sanitizadas, comandos e overview registrados.

## Riscos e cuidados

- peso é dado pessoal sensível;
- não persistir registro artificial;
- teclado pode cobrir ações em landscape;
- arquivo compartilhado exige diff restrito ao ramo de peso.

## Execução registrada

### Status

`completed`; refatoração visual, validação física e evidência sanitizada
concluídas sem alteração de contratos ou persistência.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`;
- `docs/tasks/024-refatoracao-visual-google-health/024-08-captura-real-sanitizada-weight-portrait.png`;
- este arquivo;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`.

### Decisões e desvios

- Peso recebeu header próprio azul-petróleo nominal do Champion, com conteúdo
  branco, título semanticamente marcado e descrição curta.
- Valor atual, registro manual, feedback e histórico passaram a ocupar
  superfícies distintas; valor, unidade, data e origem permanecem visíveis.
- A origem ausente passou a ser comunicada como `Origem: não disponível`.
- Loading e indisponibilidade do histórico não são mais apresentados como ausência
  de peso; ausência só aparece depois de estado disponível sem registros.
- Quando snapshot atual e histórico emitem em momentos diferentes, a tela usa o
  primeiro registro válido disponível como fallback, sem criar dado novo; uma
  lista disponível sem valor válido recebe mensagem segura.
- O editor usa `imePadding`, campo explicitamente rotulado em quilogramas,
  ação de salvar com estado de carregamento e cancelamento secundário.
- Feedback de sucesso, valor inválido e falha usa superfícies semânticas,
  títulos textuais e região viva; callbacks e intents foram preservados.
- `MetricDetailScaffold` recebeu slots opt-in de header e modifier; passos e
  distância mantêm composição padrão sem alteração visual implícita.
- Nenhum valor foi salvo para gerar evidência. Nenhum teste automatizado foi
  criado, alterado ou executado.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro antes e após a revisão |
| `./gradlew app:lintDebug` | Passou; uma execução intermediária falhou por chave extra introduzida durante correção da revisão, removida e rerun passou |
| `./gradlew lint assembleDebug assembleRelease` | Passou no rerun final após fallback do histórico; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou no rerun final; APK instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado e operacional |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Smoke portrait escuro | Passou; header, valor atual, origem/data, CTA e histórico renderizados |
| Smoke editor, teclado e cancelamento | Passou; ações acessíveis após rolagem, cancelamento fechou editor sem persistir novo registro |
| Smoke fonte `1,3x` | Passou; header, valor, CTA e histórico permaneceram acessíveis sem corte observado |
| Smoke landscape | Passou após rotação forçada para validação; rolagem revelou CTA, editor, salvar e cancelar |
| Smoke tema claro | Passou; superfície de peso e ações renderizadas sem corte observado |
| Smoke final após correção | Passou; detalhe abriu, editor/teclado foram exercitados e cancelamento fechou editor sem persistir novo registro |
| Restauração do aparelho | Passou; `font_scale=1.0`, rotação bloqueada em portrait, `ignoreOrientationRequest=false` e tema escuro |

Não foram criados, alterados ou executados testes automatizados.

### Evidências e riscos residuais

- Evidência sanitizada: `024-08-captura-real-sanitizada-weight-portrait.png`.
  Barra de status foi removida e valor, datas e linhas do histórico foram
  desfocados; header, hierarquia, CTA e superfícies permanecem verificáveis.
- Dados locais existentes foram somente exibidos. Salvamento, sucesso, erro,
  loading e vazio real não foram disparados para produzir evidência; esses
  estados foram revisados estruturalmente no código e permanecem `N/A` no smoke.
- Não há risco residual funcional conhecido no escopo. Permanece risco visual
  residual em estados raros não exercitados fisicamente e em tema dinâmico.

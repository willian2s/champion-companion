# 024-10 — Refatorar distância

- **Ticker:** `024`
- **Número:** `10`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe de distância para destacar valor diário, caráter estimado e
tendência de 7/30/90 dias. Resultado: origem e ausência claras, sem criar
fallback ou ampliar coleta.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, resumo, unidade, origem estimada, períodos, gráfico e estado vazio;
- acessibilidade e adaptação.

## Escopo excluído

- GPS, rota, mapas, distância do relógio como fallback;
- mudança de derivação, step length, repository ou dados.

## Dependências

- `024-09` concluída.

## Arquivos e símbolos prováveis

- `feature/health/HealthMetricDetailScreens.kt`;
- `DistanceHistoryShell.kt`;
- `DistanceHistoryChart.kt`;
- `DistanceDetailScreen` e ViewModel somente como referência.

## Passos de implementação

1. Priorizar valor, unidade e identificação de estimativa local.
2. Padronizar seletor de período conforme Passos quando abstração for real.
3. Refinar gráfico preservando lacunas e ausência.
4. Não sugerir precisão, GPS ou fonte remota inexistente.
5. Validar estados disponíveis, períodos e retorno.
6. Revisar diff contra guard rails de localização.
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

Smoke: alternar períodos, rolar, validar ausência/estimativa e back.

## Definição de pronto

- estimativa e origem ficam explícitas;
- períodos e histórico são legíveis;
- nenhuma localização, rota ou fallback novo;
- adaptação e semântica validadas;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- copy não pode sugerir medição GPS;
- zero e indisponível são estados distintos;
- dados pessoais devem ser sanitizados;
- reutilizar padrão de Passos só após equivalência comprovada.

## Registro de execução

- **Status:** completed.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryChart.kt` e
  `024-10-captura-real-sanitizada-distance-portrait.png`.
- **Decisões:** detalhe recebeu header azul-petróleo nominal com conteúdo branco,
  heading semântico, valor diário em `km`, origem e caráter de estimativa local
  explícitos. Seletor 7D/30D/90D recebeu acento nominal primário. Gráfico recebeu
  grade discreta, acento primário, descrição resumida acessível e marca compacta
  para lacunas; zero continua barra preenchida e ausência continua distinta.
  ViewModel, intents, repository, derivação, persistência e dados não foram
  alterados.
- **Desvios:** nenhum requisito funcional alterado. Estados loading, erro e vazio
  não foram fabricados para evidência.
- **Comandos executados:** `git diff --check`; `./gradlew app:lintDebug`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`;
  `adb devices -l`; abertura via `adb shell am start -n
  br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  dumps de hierarquia sanitizados via `uiautomator`; smoke com seleção de 7D,
  30D e 90D, rolagem e retorno; configuração temporária de fonte `1,3x`, tema
  claro e landscape; restauração para fonte `1,0x`, tema escuro, portrait,
  rotação e densidade físicas originais.
- **Resultados e evidências:** `git diff --check`, lint, builds debug/release e
  instalação passaram no Galaxy S22 `SM-S901E`.
  Header, valor diário, unidade, origem, períodos, gráfico/lista, rolagem e
  retorno foram observados. Fonte `1,3x`, tema claro e landscape abriram sem
  corte da ação essencial; tema escuro portrait foi capturado. Evidência
  sanitizada: `024-10-captura-real-sanitizada-distance-portrait.png`.
- **Riscos residuais:** loading, erro e vazio ficaram `N/A` por não fabricar
  estados; nenhum bloqueio físico permanece.
- **Revisão independente:** primeira revisão bloqueou contraste do header escuro
  e evidência física. Header foi ajustado para azul-petróleo nominal fixo com
  conteúdo branco. Reavaliação final aprovou o escopo estático sem bloqueadores;
  validação física posterior foi concluída sem achados bloqueadores.

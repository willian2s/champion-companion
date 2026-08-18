# 024-10 — Refatorar distância

- **Ticker:** `024`
- **Número:** `10`
- **Status:** pending

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

# 024-07 — Refatorar peso

- **Ticker:** `024`
- **Número:** `07`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar detalhe de peso para destacar valor atual, registro manual e histórico.
Resultado: edição simples, feedback claro e ausência segura, preservando dados em
quilogramas e histórico local.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- header, valor atual, origem/data, editor, mensagens e histórico;
- estados vazio, salvando, sucesso e erro;
- adaptação de teclado, rolagem e ações.

## Escopo excluído

- unidade imperial, meta de peso, cálculo ou schema;
- alteração de `HealthViewModel`, repository ou validação de domínio.

## Dependências

- `024-06` concluída.

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

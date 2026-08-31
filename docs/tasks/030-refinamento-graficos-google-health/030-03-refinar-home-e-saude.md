# 030-03 — Refinar Home e Saúde

- **Ticker:** `030`
- **Número:** `03`
- **Status:** `pending`
- **Depends on:** `030-02`

## Objetivo e resultado esperado

Aplicar linguagem visual integral ao overview Home, hub Saúde e detalhes de
frequência cardíaca, peso, passos e distância, tornando resumo, ações, estados,
listas e gráficos claros sem alterar semântica.

## Requisitos e critérios cobertos

- R1, R2, R4–R6; critérios 1–8 da Spec 030.

## Escopo incluído

- `HomeScreen`: resumo diário, métricas secundárias, meta, pendências, refresh e
  feedback de sincronização;
- `HealthScreen`: cabeçalho, cards de entrada, valor, unidade, origem e estado;
- `HealthMetricDetailScreens`: resumo atual, medição manual, D1, edição,
  mensagens, listas, paginação e navegação;
- `StepsHistoryShell`, `DistanceHistoryShell` e histórico de BPM;
- períodos, resumo, unidade, gaps, vazio, marker, scroll e acessibilidade.

## Escopo excluído

- agregação, cálculo, coleta, persistência, sync e permissões;
- novos períodos, métricas, queries ou dados sintéticos;
- Calendário, Exercícios, Evolução, Dispositivo, Perfil e Configurações.

## Dependências

- `030-02` deve estar concluída.

## Arquivos e símbolos prováveis

- `feature/home/*`;
- `feature/health/*`;
- `core/designsystem/*` e `core/designsystem/chart/*`.

## Passos de implementação

1. Mapear cada estado e ação atual para composição compartilhada.
2. Alinhar resumo dominante, cards compactos, labels, unidades e origem.
3. Aplicar períodos reais: passos/distância 7/30/90 e BPM TODAY/7/30.
4. Preservar ausência, loading, falha, paginação, refresh e ações explícitas.
5. Validar navegação, fonte ampliada, rolagem, portrait, landscape e temas.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar, alterar ou executar testes automatizados. Registrar screenshot
sanitizado ou `N/A`.

## Definição de pronto

- Home e Saúde usam composição coerente com referência autorizada;
- períodos e dados reais permanecem intactos;
- ações manuais e estados operacionais continuam observáveis;
- smoke, acessibilidade, screenshot e riscos residuais estão registrados.

## Riscos e cuidados

- não esconder feedback de Health Connect ou origem local;
- não fabricar valor para completar cards ou séries;
- evitar alteração de comportamento em refresh, medição e D1;
- não expor dados pessoais em evidências.

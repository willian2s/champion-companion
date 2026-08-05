# 014-10 — Integrar métricas estimadas, gráficos e splits

- **Ticker:** `014`
- **Número:** `10`
- **Status:** pending

## Objetivo e resultado esperado

Substituir consumidores visuais de atividades `WATCH_D3_D4` pela distância e
série canônicas, exibindo proveniência, gráficos e splits com estados ausentes.
Resultado: detalhe, resumo e calendário não usam distância original como
fallback.

## Contexto

`ExercisesScreen`, calendário e mappers atuais priorizam distância calibrada ou
original. UI já possui gráfico BPM e cartões de intervalos.

## Requisitos cobertos

- R6 e R7;
- acessibilidade/proveniência da métrica estimada.

## Dependências

- `014-09` concluída.

## Arquivos permitidos

- `feature/exercises/ExercisesScreen.kt` e `ExercisesViewModel.kt`;
- read models/mappers de exercícios e calendário;
- componentes visuais existentes necessários;
- recursos de texto;
- documentação/screenshot e overview.

Símbolos prováveis: detalhe de atividade, cartões de métricas, gráfico temporal,
lista de intervalos/splits e `CalendarWorkoutSummaryRecord`.

## Arquivos proibidos

- parser, perfil, schema/migration e Health Connect writer;
- Gradle e manifesto;
- entradas ou telas phone descontinuadas.

## Escopo

- resumo, ritmo, velocidade, acumulado, gráficos, splits e BPM/km;
- rótulo de estimativa/proveniência;
- estados sem passo pessoal ou série inelegível;
- acessibilidade e screenshot.

## Fora do escopo

- mapa/rota/GPS;
- edição de fórmula;
- reprocessamento de legado;
- sincronização Health Connect;
- testes automatizados.

## Passos de implementação

1. Adaptar read models para métricas canônicas.
2. Remover fallback visual da distância original em `WATCH_D3_D4`.
3. Exibir ausência orientando configuração/reprocessamento quando aplicável.
4. Integrar série/acumulado no gráfico sem suavização.
5. Exibir splits e BPM com unidades/formatos locais.
6. Manter distância original somente em diagnóstico claramente identificado.
7. Revisar acessibilidade, estados vazios e layouts.
8. Registrar screenshots e atualizar overview.

## Critérios de aceite

- Todas as superfícies listadas usam mesma distância canônica.
- Distância original não aparece como valor principal/fallback.
- Ausência não vira zero, salvo intervalo real com zero passos.
- Splits/gráficos correspondem ao read model de `014-09`.
- Conteúdo possui descrições acessíveis e screenshot.

## Validação manual

- Abrir atividade derivada, sem perfil, com série inelegível e com zero passos.
- Conferir resumo, calendário, detalhe, gráfico e splits.
- Reiniciar app e confirmar estabilidade visual/persistência.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots antes/depois e estados principais;
- matriz superfície → fonte canônica;
- builds e roteiro manual.

## Definição de pronto

Consumidores visuais alinhados, screenshots registradas e item `014-10` marcado.

## Riscos e cuidados

- Calendário compartilha dados com outras origens; aplicar regra somente a
  `WATCH_D3_D4`.
- Formatação não deve arredondar cálculo persistido.
- Não misturar refatoração visual não relacionada.

## Rollback

Ocultar gráficos/splits novos e retornar ausência para métrica canônica; nunca
restaurar original como fallback funcional.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-10` e referenciar screenshots.

# 031-07 — Redesenhar Exercícios e calendário

- **Ticker:** `031`
- **Número:** `07`
- **Status:** pending

## Objetivo

Aplicar layout da referência ao fluxo de Exercícios e transformar Calendário em
calendário contextual de exercícios, mantendo importação e revisão locais.

## Requisitos e aceite

- Lista, filtros, CTA, progresso, retry, cancelamento e estados preservados.
- Detalhe mantém revisão, calibração, descarte, gráficos e sync explícito.
- Calendário é acessível somente pela tela de Exercícios; Home e Saúde não
  recebem integração direta com Calendário.
- Detalhe diário e retorno contextual continuam funcionando.
- Nenhuma ação BLE/sync é disparada somente para renderizar tela.

## Escopo

Inclui composição visual e posicionamento contextual do calendário. Exclui novo
workflow, treino phone, GPS, rota, mapa, localização e alteração de parser.

## Dependências

- `031-04`, `031-05`, `031-06`.
- Specs 010, 011, 014, 015, 021, 027, 028.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`, shells e detalhes.
- `ExercisesViewModel.kt`, contratos e import coordinators.
- `feature/calendar/CalendarScreen.kt`, `CalendarDayScreen.kt` e contratos.

## Passos

1. Mapear blocos de exercícios/calendário da referência.
2. Integrar entrada do calendário ao contexto de Exercícios sem duplicar read
   model ou ViewModel.
3. Preservar rotas/detalhes existentes e adaptar retorno.
4. Validar lista, filtros, calendário, detalhe e ações não destrutivas.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado sem iniciar importação ou sync para gerar evidência. Cobrir
rolagem, filtros, calendário, retorno, fonte ampliada, landscape e temas.

## Definição de pronto

Exercícios contém calendário contextual, rotas não ficam órfãs e todos os
workflows existentes mantêm comportamento; evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Duplicação entre Calendário e Exercícios pode causar divergência temporal. Reusar
contratos e manter uma única fonte de read model.

# 024-12 — Refatorar detalhe diário

- **Ticker:** `024`
- **Número:** `12`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar detalhe diário para apresentar panorama local do dia antes de fontes,
problemas e fila. Resultado: passos, distância, BPM, atividades e peso agrupados
com navegação contextual preservada.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- header, data, retorno, resumo e seções métricas;
- atividades, peso, problemas por fonte e fila local informativa;
- atalhos para detalhes de Saúde e Exercícios;
- loading, erro, data inválida e ausência.

## Escopo excluído

- read model, sync, repositories ou regra temporal;
- ação nova sobre dados; detalhe permanece somente leitura.

## Dependências

- `024-11` concluída.

## Arquivos e símbolos prováveis

- `feature/calendar/CalendarDayScreen.kt`;
- `CalendarDayScreen`, `DayHeader` e cards/seções locais.

## Passos de implementação

1. Priorizar data e panorama do dia.
2. Agrupar métricas relacionadas sem ocultar ausência ou origem.
3. Manter atividades e atalhos acionáveis com labels contextuais.
4. Rebaixar fila e problemas técnicos para seção secundária legível.
5. Preservar retorno ao calendário e tratamento de data inválida.
6. Validar dias completos, vazios e estados disponíveis.
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

Smoke: Calendário → dia → detalhes relacionados → voltar; rolagem, fonte
`1,3x`, landscape e temas representativos.

## Definição de pronto

- panorama diário antecede detalhes técnicos;
- atalhos e retorno preservados;
- nenhuma sincronização iniciada;
- estados vazio/erro/data inválida seguros;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- conteúdo longo pode pressionar landscape;
- fila informativa não pode parecer CTA;
- captura pode reunir várias métricas pessoais;
- não alterar candidate temporal nem navegação cruzada.

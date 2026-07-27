# 015-07 — Implementar grade mensal, marcadores e acessibilidade

- **Status:** pending
- **Objetivo:** substituir placeholder por calendário mensal local e acessível.
- **Resultado esperado:** grade funcional com navegação, seleção, Hoje,
  indicadores e legenda.

## Documentos obrigatórios

Spec 015, 015-06, `CalendarScreen.kt`, design system, convenções de localidade,
Navigation3 e baseline visual.

## Dependências e entradas

- 015-06 concluída.

## Arquivos permitidos

`feature/calendar` e componentes próprios de calendário; documentação e
screenshots de validação.

## Arquivos proibidos

Biblioteca externa sem justificativa, DAOs, repositories, migrations, Health
Connect, BLE, workflows, testes e roadmap/ADR.

## Escopo

Renderizar mês, cabeçalho, anterior/próximo, Hoje, dias da semana conforme
localidade, célula selecionada, célula de hoje, indicadores, legenda, empty,
loading, erro, semântica de acessibilidade e acionamento de `SelectDate`/
`OpenDate`.

## Fora do escopo

Detalhe diário, gráficos, sync, edição de dados e operações externas.

## Passos

1. Implementar grade sem dependência externa.
2. Mapear presença do snapshot para células.
3. Aplicar seleção/Hoje e navegação.
4. Adicionar semantics/content descriptions.
5. Validar dias vazios e limites de mês.

## Critérios de aceite

- Mês anterior/próximo e Hoje funcionam.
- Localidade é respeitada.
- Indicadores não inventam dados.
- Células vazias continuam navegáveis conforme política definida.
- Leitor de tela anuncia data, seleção, hoje e presença.
- Primeiro dia da semana segue localidade atual.
- Dias fora do mês não abrem outro mês sem atualizar seleção corretamente.
- Datas futuras podem abrir estado vazio sem criar valores.
- Título, mês, ano, legenda e prioridade visual de indicadores estão presentes.
- Células anunciam número, hoje, seleção, passos, BPM, atividades e pendência
  ou erro em descrição acessível.

## Validação manual

Galaxy S22: navegar meses, selecionar datas, usar leitor de tela e conferir
orientação/escala. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar screenshots, localidade usada, estados vazio/loading/erro e comandos.
Atualizar overview após aceite.

## Validação final

Confirmar ausência de biblioteca externa não aprovada e ausência de operações
BLE/Health Connect. Não criar, alterar ou executar testes automatizados.

## Contexto

O calendário atual é `PlaceholderScreen`; esta task implementa somente a grade
mensal sobre snapshots já fornecidos.

## Arquivos alterados

Inicialmente nenhum; registrar componentes de calendário alterados.

## Decisões e desvios

Registrar política de localidade, biblioteca própria e qualquer desvio aprovado.

## Screenshot

Exigido. Registrar mês com dados, mês vazio e navegação acessível.

## Riscos residuais

Diferenças de localidade, tamanho de célula e leitor de tela podem exigir ajuste
visual sem mudar contrato.

## Atualização do overview

Marcar somente o item 015-07 após aceite de 015-06.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.

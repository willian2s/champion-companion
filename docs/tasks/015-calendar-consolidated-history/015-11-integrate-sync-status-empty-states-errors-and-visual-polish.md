# 015-11 — Integrar sync, estados, erros e acabamento visual

- **Status:** pending
- **Objetivo:** consolidar experiência final de calendário e históricos.
- **Resultado esperado:** estados coerentes, acessibilidade, privacidade,
  mensagens seguras e acabamento visual sem alterar fronteiras funcionais.

## Documentos obrigatórios

Spec 015, tasks 015-06 a 015-10, ADRs de privacidade/local-first, design system,
Health Connect spec e baseline visual.

## Dependências e entradas

- 015-08, 015-09 e 015-10 concluídas.

## Arquivos permitidos

Arquivos de UI/ViewModel temporal já pertencentes à Fase 15, recursos visuais,
documentação e screenshots.

## Arquivos proibidos

Novo sync, BLE, Health Connect, permissões, Room/schema, WorkManager,
foreground service, alteração de workflow, testes e roadmap/ADR.

## Escopo

Integrar `Sem operações`, `Pendente`, `Sincronizando`, `Sincronizado`, `Falha` e
`Parcial`; estados empty/loading/error/retry; mensagens sem dados sensíveis;
acessibilidade; consistência de unidades; espaçamento, contraste e hierarquia;
logs seguros e ausência de operação automática.

## Fora do escopo

Corrigir provider, sincronizar fila, mudar dados, criar novos gráficos ou alterar
features fora do calendário/históricos.

## Passos

1. Revisar estados de todas as superfícies.
2. Integrar status local ao detalhe.
3. Padronizar empty/loading/error.
4. Revisar semantics, contraste e foco.
5. Revisar logs e privacidade.
6. Capturar screenshots finais.

## Critérios de aceite

- Status vem somente da fila local.
- Falha de sync não apaga dado local.
- Ausência, zero e erro permanecem distintos.
- Acessibilidade cobre controles, indicadores e listas.
- Nenhum log expõe métricas, IDs ou payloads.
- Sem operações produz `Sem operações`; sucesso combinado (`syncedCount > 0`)
  com qualquer pendência, sincronização ou falha produz `Parcial`.
- Sem sucesso combinado, falha produz `Falha`; sem falha, sincronizando produz
  `Sincronizando`; sem falha/sincronização, pendente produz `Pendente`; sucesso
  só aparece quando tudo está concluído.
- Aggregate ausente, operação órfã e degradação parcial não são mascarados.
- Acessibilidade e mensagens cobrem estados vazios, loading, erro e retry.

## Validação manual

Galaxy S22 offline, com cada estado local reproduzível; conferir leitor de tela,
contraste, screenshots e ausência de prompts/operações externas. Screenshot
exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar screenshots, estados revisados, logs permitidos e riscos. Atualizar
overview após aceite.

## Validação final

Confirmar acabamento sem refactor oportunista, sem provider e sem alteração de
escopo. Não criar, alterar ou executar testes automatizados.

## Contexto

As frentes visuais precisam de integração final sem transformar status local em
operação de sync nem mascarar ausência como sucesso.

## Arquivos alterados

Inicialmente nenhum; registrar arquivos de UI, estado e recursos alterados.

## Decisões e desvios

Registrar prioridade de estados, mensagens e qualquer desvio aprovado.

## Screenshot

Exigido. Registrar estados normal, vazio, erro e fila local quando disponíveis.

## Riscos residuais

Estado parcial de fontes pode ser confundido com vazio; mensagens devem manter a
origem da degradação sem expor dados sensíveis.

## Atualização do overview

Marcar somente o item 015-11 após aceite de 015-08, 015-09 e 015-10.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.

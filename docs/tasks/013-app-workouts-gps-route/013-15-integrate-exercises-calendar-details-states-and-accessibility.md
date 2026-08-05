# 013-15 — Integrar superfícies, estados e acessibilidade

- **Ticker:** `013`
- **Número:** `15`
- **Status:** cancelled

## Objetivo e resultado esperado

Consolidar atividades phone/correlacionadas em Exercícios, detalhe, Calendário, dia, histórico BPM, feedback HC e Settings, com estados acessíveis e sem duplicação. Resultado: experiência completa e coerente, sem mapa.

## Contexto

Fase 15 usa read model local e algumas queries exigem import. Fluxos 013-13/14 definem correlação e sync. UI deve distinguir CTA phone de import watch.

## Documentos obrigatórios

Spec 013; spec/overview 015; Spec 017 para o refinamento visual independente;
ADR-009/013/014; `013-07/08/12/13/14`; design system/a11y existente.

## Dependências

`013-13` e `013-14` concluídas.

## Arquivos permitidos

`feature/exercises/**`, `feature/calendar/**`, `feature/health/**`, `feature/settings/**`; read models/repositories/queries mínimos; navigation/resources; task/overview.

## Arquivos e símbolos prováveis

`ExercisesScreen/ViewModel/FeatureContract`, detalhe de atividade, `LocalCalendarRepository`, `CalendarDaySummary`, histórico de frequência cardíaca, `SettingsScreen/ViewModel` e `ChampionNavigation`.

## Arquivos proibidos

Novo schema/migration, GPS filter/service core, BLE protocol, mapa/API key/export, testes.

## Requisitos cobertos

Todas superfícies; CTA separada; detalhe de rota; histórico BPM; feedback sync; Settings; a11y/estados; screenshots.

## Escopo

- `Iniciar treino` separado de `Buscar treinos no relógio`;
- lista/card/detalhe para origens phone/watch/correlacionada;
- resumo route: presença, pontos, segmentos, precisão média, distância, sync;
- calendário/dia uma vez por atividade;
- HR history inclui amostras finais válidas da atividade phone sem duplicar real-time/import;
- Settings mostra route permission/capability;
- estados de permission/GPS/watch/recovery/persistência/HC/correlação;
- confirmação finalizar/descartar, fonte aumentada/landscape/TalkBack.

## Fora do escopo

Mapa, tiles, arquivo, servidor e nova regra de negócio de captura/sync.

## Passos de implementação

1. Remover joins/assunções visuais import-only restantes.
2. Adicionar CTA phone sem mover CTA watch.
3. Integrar sessão ativa/preparação/recovery ao back stack.
4. Exibir origem/prioridade e resumo de rota sem coordenadas.
5. Adaptar Calendar read model/day/detail para phone/correlated uma vez.
6. Consolidar BPM com identidade de atividade final e dedup.
7. Integrar route sync status e permissão em detalhes/Settings.
8. Implementar todos estados/erros seguros e mensagens sem detalhes técnicos.
9. Ajustar semantics/live regions/botões/contraste/font scale/landscape nas
   superfícies de integração funcional, sem absorver o alinhamento geral de
   margens e cabeçalhos definido pela Spec 017.
10. Capturar screenshots e atualizar overview.

## Critérios de aceite

- CTAs são distintas;
- atividade correlacionada aparece uma vez em todas superfícies;
- detalhe informa rota sem revelar coordenadas/mapa;
- livre/esteira não mostram erro GPS;
- Calendar/HR history aceitam phone;
- route partial sync é compreensível;
- a11y não depende só de cor e não anuncia excessivamente.

## Validação manual

Percorrer seleção→preparação→ativa→revisão→detalhe; filtros/listas/calendário/dia/HR/Settings; todos estados críticos; fonte 1,3x+, landscape e TalkBack representativo.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Roteiro por superfície/estado, ausência de duplicatas, comandos e catálogo de screenshots.

## Screenshots

Obrigatórias para cada superfície alterada e estados principais, incluindo fonte aumentada e landscape; sem coordenadas/BPM pessoal identificável fora de ambiente privado.

## Definição de pronto

Integração visual completa pronta para gate físico; overview atualizado.

## Riscos e cuidados

Hotspots UI grandes. Não misturar refatorações. Live metrics não devem causar recomposição/announcement excessivos. O pacote visual de margens e cabeçalhos das superfícies existentes pertence à Spec 017; não duplicar sua implementação nesta subtarefa.

## Rollback

Ocultar entradas phone e manter atividades persistidas acessíveis por forward fix; não apagar dados nem reverter v6.

Não criar, alterar ou executar testes automatizados.

## Encerramento da Fase 013 — cancelada e não executada

- **Status atual:** `cancelled`.
- **Motivo:** `product_reorientation`; a integração de superfícies para novos
  treinos iniciados pelo celular não continua como objetivo ativo.
- **Execução:** esta subtarefa não foi executada nem concluída. Não há alegação
  de CTA phone final, detalhe de rota, integração de Calendário, histórico BPM,
  Settings, Health Connect ou acessibilidade específicos da Fase 013.
- **Histórico e riscos:** escopo, dependências, critérios, screenshots exigidos
  e riscos de UI permanecem preservados; ausência de implementação não é
  aceite.
- **Destino:** a experiência de Exercícios, Calendário e revisão para treinos
  iniciados pelo relógio pode ser redefinida conceitualmente na Fase 014, sem
  reabrir ou ampliar superfícies phone nesta fase.

# Task 008-08B - Implementar hub de Saúde e shells das telas de métricas

## Status

Planejada.

## Dependencia

`008-08B` depende de `008-08A`.

Nao pode comecar antes de:

- ADR criada;
- roadmap atualizado;
- rotas definidas;
- spec alinhada.

## Objetivo

Implementar hub de Saúde e estrutura inicial das telas de métricas sem antecipar funcionalidades futuras.

## Contexto

Depois de `008-08`, medição manual já existe, último BPM persistido já está integrado na Home e lifecycle já foi ajustado. Extensão aprovada move ownership visual da medição manual para tela específica de frequência cardíaca e transforma Saúde em hub de métricas com navegação para detalhes. Implementação deve preservar tudo que `008-08` já entregou e reutilizar apenas dados persistidos existentes.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-08A-formalize-health-metrics-navigation-and-roadmap.md`
- ADR criada por `008-08A`
- roadmap atualizado por `008-08A`

## Dependencias

- `008-08A`

## Entradas

- navegacao atual com `AppDestination` e back stack manual;
- `HealthScreen` e `HealthViewModel` atuais;
- `HomeViewModel` e `DailySummaryRepository` atuais;
- `HeartRateRepository` atual;
- `PersonalMetricsRepository` atual.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/**`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-08B-implement-health-hub-and-metric-detail-shells.md`

## Arquivos Proibidos

- roadmap
- ADRs
- `008-08` concluida
- D1 parser/fluxo
- Health Connect
- novos fluxos BLE
- Room schema, salvo necessidade inevitavel comprovada

## Escopo

- transformar `HealthScreen` em hub de metricas;
- criar cards para frequencia cardiaca, peso, passos e distancia;
- usar apenas dados persistidos existentes;
- criar rotas especificas;
- mover ou compor fluxo manual de BPM na tela de frequencia cardiaca;
- preservar funcionalidade ja entregue;
- criar card de valor atual em cada tela;
- criar secoes de grafico e historico;
- usar estados vazios quando dados ainda nao existirem;
- nao criar dados simulados;
- manter ViewModels e repositories como fonte;
- adicionar screenshots conforme `AGENTS.md`;
- executar validacao minima.

## Tela De Frequencia Cardiaca Na `008-08B`

Deve incluir funcionalmente:

- ultimo BPM;
- horario;
- acao de medir;
- medicao de 60 segundos;
- cancelamento;
- conclusao;
- erro;
- retry;
- medicoes manuais recentes ja suportadas.

Deve reservar visualmente:

- grafico futuro;
- historico completo futuro;
- acao futura de buscar dados do relogio.

A acao futura nao deve ser apresentada como funcional.

## Telas De Peso, Passos E Distancia

Devem incluir:

- valor atual disponivel;
- unidade;
- timestamp ou data;
- origem ou indicacao de estimativa;
- estado sem dados;
- secao de grafico vazia;
- secao de historico vazia.

Nao devem incluir:

- novas formas de persistencia;
- novas sincronizacoes;
- edicao de peso;
- D1;
- Health Connect;
- calendario;
- graficos funcionais;
- historico completo.

## Fora Do Escopo

- implementar D1;
- buscar BPM historico do relogio;
- implementar graficos reais;
- implementar historico completo;
- implementar filtros;
- implementar calendario;
- implementar edicao de peso;
- implementar Health Connect;
- alterar schema Room sem necessidade inevitavel;
- criar novos fluxos BLE;
- refazer medicao manual;
- alterar regra de resultado final do BPM.

## Passos

1. Transformar rota raiz de Saúde em hub com quatro cards navegaveis.
2. Definir e implementar rotas especificas conforme contratos aprovados em `008-08A`.
3. Mover ou compor UI atual de medicao manual dentro da tela de frequencia cardiaca sem duplicar coordinator/ViewModel desnecessariamente.
4. Ler peso de fonte persistida existente via repository/estado apropriado.
5. Ler passos e distancia de `DailySummary` atual sem criar nova sync.
6. Criar secoes vazias de grafico e historico com copy segura para funcionalidades futuras.
7. Atualizar overview/spec com resultado real e adicionar screenshots no PR.
8. Executar validacao minima e smoke manual reduzido.

## Criterios De Aceite

- `HealthScreen` vira hub de metricas.
- quatro cards aparecem com estados reais ou vazios seguros.
- cada card navega para propria tela.
- medicao manual continua funcional na tela de frequencia cardiaca.
- nenhuma tela inicia BLE ao abrir.
- nao existem dados simulados.
- D1 continua fora da Fase 08.
- Health Connect continua fora da Fase 08.

## Testes E Validacao

Validacao minima obrigatoria:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Validacao manual minima:

1. abrir Saúde;
2. visualizar os quatro cards;
3. confirmar valores disponíveis;
4. abrir frequência cardíaca;
5. executar medição manual;
6. concluir e salvar;
7. voltar para Saúde;
8. confirmar atualização do card;
9. abrir Peso;
10. confirmar valor atual ou estado vazio;
11. abrir Passos;
12. confirmar passos atuais;
13. abrir Distância;
14. confirmar valor ou estado indisponível;
15. confirmar navegação de retorno;
16. confirmar estados vazios de gráfico e histórico;
17. confirmar ausência de dados simulados;
18. confirmar que nenhuma tela inicia BLE ao abrir;
19. confirmar que D1 não foi implementado;
20. confirmar que Health Connect não foi implementado.

## Evidencia Manual

- screenshots da tela Saúde e de cada tela de métrica;
- resultado dos comandos obrigatórios;
- resumo do smoke manual reduzido.

## Atualizacao Do Overview

- Marcar `008-08B` como concluida quando hub e telas shell estiverem implementados.
- Atualizar riscos residuais antes de `008-09`.

## Validacao Final

- Confirmar que medicao manual nao foi reimplementada do zero e que D1 continua ausente.

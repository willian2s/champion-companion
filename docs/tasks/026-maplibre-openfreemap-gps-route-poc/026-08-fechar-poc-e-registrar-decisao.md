# 026-08 — Fechar PoC e registrar decisão

- **Ticker:** `026`
- **Número:** `08`
- **Status:** completed

## Objetivo

Consolidar evidências da Spec 026, classificar o gate como `go`, `go_with_adjustments` ou `no_go` e registrar claramente o que a PoC autoriza ou não autoriza depois do encerramento.

## Resultado esperado

A Spec 026 termina com estado factual, riscos residuais documentados e próxima decisão explícita, sem promover automaticamente a PoC a feature de produção.

## Requisitos e critérios cobertos

- Spec 026: critério 35 e Definition of Done completa;
- consolidação do gate físico;
- atualização de overview/spec/ADR conforme resultado;
- preservação dos guards da Fase 013.

## Dependências

- `026-01` a `026-07` concluídas ou bloqueios explicitamente registrados;
- evidência física sanitizada disponível;
- builds finais executados quando código da PoC existir.

## Classificação

### `go`

Aplicar somente quando:

- MapLibre Compose funcionou de forma estável;
- OpenFreeMap atendeu ao mapa-base real da PoC;
- atribuição permaneceu correta;
- rota cresceu corretamente;
- interação foi fluida o suficiente;
- GPS/filtro produziram trajetória visualmente coerente;
- nenhum bloqueio arquitetural relevante foi encontrado.

### `go_with_adjustments`

Aplicar quando:

- conceito funcionou;
- há problemas corrigíveis de câmera, style, frequência de atualização, filtro, integração MapLibre ou provedor;
- nenhum achado inviabiliza estruturalmente a feature futura.

### `no_go`

Aplicar quando:

- MapLibre Compose é incompatível com o comportamento necessário;
- rota dinâmica apresenta problema não aceitável;
- OpenFreeMap é inadequado por disponibilidade, termos ou comportamento real;
- a arquitetura necessária violaria fronteiras relevantes do projeto.

## Escopo incluído

- atualizar status da spec;
- atualizar overview e checklist;
- registrar resultado por task;
- atualizar ADR experimental com resultado do gate quando necessário;
- atualizar baseline somente se a PoC concluída mudar informação factual duradoura;
- registrar riscos residuais;
- indicar próximo trabalho recomendado sem criá-lo automaticamente.

## Escopo excluído

- implementar feature de produção;
- liberar `PhoneWorkoutPolicy`;
- integrar foreground service;
- persistir rota;
- adicionar mapa no detalhe real da atividade;
- Health Connect `ExerciseRoute`;
- trocar automaticamente de provider;
- remover legado histórico da Fase 013.

## Questões obrigatórias de fechamento

Responder com evidência:

1. MapLibre Compose foi estável no aparelho real?
2. OpenFreeMap carregou de forma adequada durante o teste?
3. A atribuição permaneceu correta?
4. A linha cresceu sem jank relevante?
5. A câmera permitiu follow e interação manual?
6. O filtro produziu uma rota visualmente coerente?
7. A perda de rede foi tratada sem corromper GPS?
8. O reset impediu contaminação entre ciclos?
9. Houve exposição de localização em logs/evidências?
10. Há algum motivo técnico para não seguir com MapLibre + OpenFreeMap numa futura feature?

## Se resultado for `go` ou `go_with_adjustments`

Registrar que uma nova spec poderá avaliar:

```text
sessão phone real
    ↓
foreground service
    ↓
GPS persistente
    ↓
rota persistida
    ↓
mapa ao vivo
    ↓
atividade final
    ↓
detalhe com mapa
```

Essa futura spec deve decidir novamente:

- reativação/substituição de `PhoneWorkoutPolicy`;
- recuperação do legado da Fase 013;
- Room/migration;
- background/tela apagada;
- estratégia de provider em produção;
- uso público do OpenFreeMap vs outro provider vs self-host;
- cache/offline;
- privacidade;
- Health Connect;
- BLE/Champion C001.

## Se resultado for `no_go`

Registrar:

- motivo técnico;
- se problema pertence ao MapLibre, OpenFreeMap, GPS/filtro ou integração;
- quais artefatos debug podem ser removidos depois;
- se vale investigar alternativa em nova spec.

Não reabrir Mapbox automaticamente, especialmente se continuar incompatível com a premissa de zero forma de pagamento.

## Validação final

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Executar Gradle somente se houver código/Gradle da PoC no estado final.

## Definition of Done

- resultado `go`, `go_with_adjustments` ou `no_go` registrado;
- spec e overview refletem estado real;
- checklist não marca task bloqueada como concluída sem evidência;
- ADR/baseline são alinhados quando necessário;
- riscos residuais estão documentados;
- Fase 013 continua fechada;
- `PhoneWorkoutPolicy` não foi liberada pela conclusão da PoC;
- próxima feature, se recomendada, exige spec própria;
- documentação permite compreender o resultado sem depender desta conversa.

## Riscos e cuidados

- não confundir sucesso técnico da PoC com autorização de produto;
- não declarar OpenFreeMap permanentemente gratuito ou disponível; registrar apenas condição observada e decisão baseada nela;
- não incluir rota/localização pessoal em relatório final;
- não transformar `go_with_adjustments` em `go` para simplificar fechamento.

## Execução — 2026-08-21

- **Status:** `completed`.
- **Arquivos documentais atualizados:** esta subtarefa, `026-00-overview.md`,
  `docs/specs/026-maplibre-openfreemap-gps-route-poc.md`,
  `docs/decisions/adr-024-maplibre-openfreemap-gps-poc.md` e
  `docs/baseline/current-baseline.md`.
- **Escopo preservado:** nenhum arquivo de código, teste, dependência,
  manifesto ou configuração de build foi alterado nesta subtarefa.

## Decisão final

Classificação: **`go_with_adjustments`**.

O conceito foi validado no aparelho real, sem bloqueio estrutural para o
experimento: MapLibre Compose renderizou, OpenFreeMap/Positron carregou com
atribuição, GPS real alimentou rotas visuais, e câmera, parada, enquadramento,
reset e segundo ciclo foram exercitados. A classificação não autoriza feature de
produção.

## Respostas às questões obrigatórias

1. **MapLibre Compose foi estável no aparelho real?** Sim. Renderizou sem crash
   ou ANR observado durante as atividades reais.
2. **OpenFreeMap carregou adequadamente?** Sim para o cenário observado, com
   Positron e atribuição visível. Falha real em cold-cache permaneceu
   inconclusiva porque o mapa continuou pronto offline.
3. **A atribuição permaneceu correta?** Sim. Permaneceu visível no mapa.
4. **A linha cresceu sem jank relevante?** Parcialmente evidenciado. As rotas
   cresceram visualmente, mas não houve medição final explícita de jank após as
   correções; esse ponto permanece risco residual.
5. **A câmera permitiu follow e interação manual?** Sim. Pan, zoom, follow e
   Recentralizar foram confirmados; gesto manual suspende follow até recentralizar.
6. **O filtro produziu rota visualmente coerente?** Parcialmente. GPS real
   produziu rotas funcionais, mas houve variância do fused e uma rejeição sem
   categoria, mantidas como riscos de observabilidade.
7. **A perda de rede foi tratada sem corromper GPS?** Parcialmente comprovado.
   O mapa permaneceu pronto por provável cache e o estado GPS ficou separado;
   falha real do mapa em cold-cache não foi isolada.
8. **O reset impediu contaminação entre ciclos?** Sim. Reset e segundo ciclo
   foram confirmados sem reutilização de rota ou métricas.
9. **Houve exposição de localização em logs/evidências?** Não foi observada.
   Auditoria sanitizada não encontrou coordenadas, GeoJSON, rota ou bounds.
10. **Há motivo técnico para não seguir com MapLibre + OpenFreeMap?** Não há
    bloqueio estrutural para investigação futura. Provider, cache/offline,
    disponibilidade, termos e privacidade exigem nova avaliação.

## Decisões e desvios

- Manter MapLibre Compose + OpenFreeMap como stack experimental da Spec 026.
- Recomendar revalidação de provider, cache/cold-cache e observabilidade antes de
  qualquer uso posterior; não trocar provider automaticamente nesta subtarefa.
- Aceitar uma rejeição sem categoria na atividade 3 como resíduo de
  observabilidade, sem alterar filtro ou suavização.
- Registrar que a falha real do mapa em cold-cache permaneceu inconclusiva: o
  mapa continuou `Pronto` offline, provavelmente por cache remanescente.
- Manter a variação do fused entre ambientes como risco técnico residual.
- Preservar Fase 013 `closed` / `partially_delivered` /
  `product_reorientation`, `PhoneWorkoutPolicy`, ausência de persistência,
  Health Connect, BLE e `WorkoutRecordingService`.

## Resultados e evidências

- No `SM-S901E`, atividades reais registraram aproximadamente 538 m e 557 m,
  com 122 pontos aceitos em cada uma; as rotas cresceram visualmente.
- Pan, zoom, follow, `Recentralizar`, fit bounds após parada, reset e segundo
  ciclo sem reinício foram confirmados.
- Permissão negada, localização aproximada, localização desligada/GPS perdido,
  stop precoce em modo mock e isolamento entre falha de mapa e GPS foram
  exercitados. O stop precoce mock não é evidência de GPS real.
- Atribuição cartográfica permaneceu visível. Não foram observados dados
  sensíveis em Logcat ou nas evidências sanitizadas.

## Comandos e validação

- `git diff --check` — executado nesta subtarefa após as alterações; passou sem
  erros.
- `./gradlew lint assembleDebug assembleRelease` — não executado nesta
  subtarefa documental; não houve alteração de código ou Gradle. A evidência de
  execução anterior permanece registrada em `026-07`.
- Testes automatizados — não criados nem executados, conforme política do
  projeto.

## Riscos residuais e bloqueios

- Falha real do mapa em cold-cache não foi isolada; disponibilidade, termos e
  ausência de SLA do OpenFreeMap continuam dependências externas.
- A fonte fused pode variar conforme aparelho, Google Play Services, rede,
  Wi-Fi, cache, sensores e ambiente físico.
- Observabilidade ainda precisa categorizar toda rejeição sem expor localização.
- Não há registro final sanitizado com medição explícita de jank após as
  correções de responsividade.
- Qualquer efetivação produtiva continua bloqueada até nova spec e nova decisão
  explícita sobre provider, cache/offline, privacidade, persistência,
  `PhoneWorkoutPolicy` e demais fronteiras de produto.

## Próxima decisão recomendada

Se houver interesse em efetivação, criar spec própria para avaliar, nesta ordem,
sessão phone, foreground service, GPS persistente, rota persistida, mapa ao vivo,
atividade final e detalhe com mapa. A nova spec deve decidir novamente
`PhoneWorkoutPolicy`, legado da Fase 013, Room/migration, background, provider,
cache/offline, privacidade, Health Connect e BLE/Champion C001. Nenhuma dessas
etapas é criada ou autorizada por este fechamento.

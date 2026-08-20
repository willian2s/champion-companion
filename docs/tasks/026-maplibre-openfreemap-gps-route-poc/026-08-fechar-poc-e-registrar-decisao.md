# 026-08 — Fechar PoC e registrar decisão

- **Ticker:** `026`
- **Número:** `08`
- **Status:** pending

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

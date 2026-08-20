# ADR-024 — MapLibre + OpenFreeMap para PoC GPS debug-only

- **Status:** aceita somente para a PoC da Spec 026
- **Data:** 2026-08-20

## Contexto

O Champion Companion encerrou a Fase 013 como `closed`, com resultado
`partially_delivered` e motivo `product_reorientation`. Treinos iniciados pelo
celular, GPS, localização de sessão, rotas e mapas continuam fora da direção
de produto. `PhoneWorkoutPolicy` permanece vigente, e a infraestrutura histórica
da fase deve ser preservada sem expansão oportunista.

A Spec `026-maplibre-openfreemap-gps-route-poc.md` introduz uma exceção técnica
isolada para avaliar, em aparelho real, mapa-base, posição GPS e rota crescente.
Essa avaliação precisa de renderizador Compose e de um provedor de mapa-base,
mas não precisa reativar a sessão phone, persistir coordenadas ou criar uma
decisão de produção.

A alternativa inicialmente considerada foi Mapbox. Ela foi abandonada antes da
implementação porque exigia configuração de billing/forma de pagamento
incompatível com a premissa desta PoC, que não deve introduzir cadastro,
credencial ou pagamento.

## Decisão

Para a PoC da Spec 026, adotar:

```text
MapLibre Compose 0.13.0
        +
OpenFreeMap public instance
        +
OpenFreeMap Positron
```

1. MapLibre Compose será o renderizador experimental da tela Compose.
2. OpenFreeMap será usado somente como fonte pública do mapa-base, inicialmente
   pelo estilo `https://tiles.openfreemap.org/styles/positron`.
3. A geometria da rota virá exclusivamente do GPS recebido pelo Champion
   Companion. A rota não será enviada explicitamente ao OpenFreeMap e ficará
   somente em memória.
4. A atribuição cartográfica exigida pelo mapa deve permanecer visível. Não
   remover controles ou ornamentos necessários para atribuição.
5. A PoC será `debug-only`, limitada ao source set `app/src/debug`, sem destino
   permanente na navegação, CTA em superfícies de produto, `WorkoutRecordingService`,
   alteração de `PhoneWorkoutPolicy`, Room, Health Connect, BLE ou persistência.
6. Nenhuma conta, API key, token, credencial ou forma de pagamento será
   introduzida. O uso gratuito observado não é tratado como condição permanente.
7. OpenFreeMap não oferece SLA para esta decisão. Termos, privacidade,
   disponibilidade e política de uso devem ser revalidados na execução da
   `026-02` e antes de qualquer uso posterior.
8. Esta decisão autoriza somente a PoC. Mesmo resultado `go` ou
   `go_with_adjustments` não autoriza feature de produção; eventual efetivação
   exige nova spec e nova decisão sobre provider, SLA, privacidade, cache,
   offline, persistência e reativação ou não do treino phone.

## Escopo e fronteiras

Esta ADR constitui exceção explícita aos guards gerais somente para a Spec 026.
Não altera o encerramento histórico da Fase 013 e não transforma a PoC em
backlog de produto. A variante release não deve expor entrypoint da PoC.

Logs, mensagens e evidências não podem conter latitude, longitude, altitude,
rota, GeoJSON, bounds ou identificadores pessoais. A comunicação de rede
necessária para style, tiles, fontes e infraestrutura associada do mapa-base é
aceita somente no experimento e deve ser reavaliada antes de promoção.

## Consequências

### Positivas

- permite validar MapLibre Compose e OpenFreeMap sem conta, billing ou segredo;
- mantém a Fase 013 encerrada e os guards de produto preservados;
- separa experimento visual de persistência, Health Connect, BLE e atividade real;
- mantém aberta a possibilidade de trocar provider após evidência física.

### Negativas e riscos

- OpenFreeMap público pode sofrer indisponibilidade, limites ou mudança de
  termos sem SLA;
- o experimento depende de rede e de nova confirmação das condições do provider;
- a escolha não resolve requisitos de produção, como cache, offline, retenção,
  privacidade, escala ou suporte;
- atribuição e privacidade exigem revisão durante a validação física.

## Alternativas consideradas

- **Mapbox:** rejeitada antes da implementação por exigir configuração de
  billing/forma de pagamento incompatível com a premissa sem cadastro e sem
  pagamento da PoC.
- **Outro provedor comercial com token:** rejeitado por introduzir credenciais,
  cadastro ou billing antes de existir necessidade comprovada.
- **Self-hosting de tiles:** rejeitado por ampliar o escopo e não ser necessário
  para validar a hipótese técnica desta PoC.
- **Não usar mapa-base:** rejeitado porque não permitiria validar o objetivo
  visual do experimento.

## Relação com outras decisões

- ADR-004 continua autoritativa para privacidade operacional e logs fora da
  exceção explícita desta PoC.
- ADR-014 permanece histórica e não é reativada por esta decisão.
- A Spec 026 e seu overview definem ordem, critérios, validação física e
  resultado final `go`, `go_with_adjustments` ou `no_go`.

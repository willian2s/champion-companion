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

A revisão de escopo de 2026-08-20 registrou exigência explícita de teste externo:
ADB sozinho não é suficiente; o acesso manual precisa estar disponível por um
botão técnico na página de Exercícios do build debug.

A alternativa inicialmente considerada foi Mapbox. Ela foi abandonada antes da
implementação porque exigia configuração de billing/forma de pagamento
incompatível com a premissa desta PoC, que não deve introduzir cadastro,
credencial ou pagamento.

O plano inicial de coleta usava registros manuais de
`LocationManager.GPS_PROVIDER`. Durante a `026-07`, uma tentativa intermediária
registrou `GPS_PROVIDER` e `NETWORK_PROVIDER` em paralelo, mas essa abordagem foi
substituída por `FusedLocationProviderClient` para permitir que a API combine
GPS, rede móvel, Wi-Fi, cache de localização e sensores disponíveis. O objetivo
experimental continua sendo validar GPS nativo do aparelho; fused é a API de
coleta da variante debug, não uma decisão de fonte produtiva.

## Decisão

Para a PoC da Spec 026, adotar:

```text
MapLibre Compose 0.14.0
        +
OpenFreeMap public instance
        +
OpenFreeMap Positron
```

1. MapLibre Compose `0.14.0` substitui a versão planejada `0.13.0` após
   revalidação da release estável e da coordenada oficial no Maven Central em
   2026-08-20. Será o renderizador experimental da tela Compose.
2. OpenFreeMap será usado somente como fonte pública do mapa-base, inicialmente
   pelo estilo `https://tiles.openfreemap.org/styles/positron`.
3. A geometria da rota virá exclusivamente do GPS recebido pelo Champion
   Companion. A rota não será enviada explicitamente ao OpenFreeMap e ficará
   somente em memória.
4. A atribuição cartográfica exigida pelo mapa deve permanecer visível. Não
   remover controles ou ornamentos necessários para atribuição.
5. A PoC será `debug-only`, limitada ao source set `app/src/debug`, sem destino
   permanente na navegação. Para teste externo, a página de Exercícios poderá
   exibir somente no build debug uma affordance técnica que dispara `Intent`
   explícito para a Activity da PoC. Esse CTA não inicia treino phone nem é
   feature de produto. Não haverá CTA equivalente em Home, Saúde, Dispositivo ou
   Configurações.
6. Botão, `Intent` executável e entrypoint da PoC não podem ser expostos na
   variante release. ADB pode continuar disponível como fallback diagnóstico,
   mas não é o caminho exclusivo de abertura.
7. A PoC não usará `WorkoutRecordingService`, não alterará `PhoneWorkoutPolicy`
   e não terá Room, Health Connect, BLE ou persistência.
8. Nenhuma conta, API key, token, credencial ou forma de pagamento será
   introduzida. O uso gratuito observado não é tratado como condição permanente.
9. OpenFreeMap não oferece SLA para esta decisão. Termos, privacidade,
   disponibilidade e política de uso devem ser revalidados na execução da
   `026-02` e antes de qualquer uso posterior.
10. Esta decisão autoriza somente a PoC. Mesmo resultado `go` ou
    `go_with_adjustments` não autoriza feature de produção; eventual efetivação
    exige nova spec e nova decisão sobre provider, SLA, privacidade, cache,
    offline, persistência e reativação ou não do treino phone.
11. A fonte de coleta da PoC será `FusedLocationProviderClient`, configurada
    com `Priority.PRIORITY_HIGH_ACCURACY`, intervalo de 1.000 ms, intervalo
    mínimo de 1.000 ms e distância mínima de 0 m. `lastLocation` recente e
    válido pode aquecer readiness e posição corrente, mas não entra
    automaticamente na rota antes de `Iniciar`.
12. Fused é a API efetiva para combinar GPS, rede móvel, Wi-Fi, cache de
    localização e sensores disponíveis. GPS nativo permanece objetivo de
    validação; a decisão não exige nem autoriza registros manuais concorrentes
    de `GPS_PROVIDER` e `NETWORK_PROVIDER`.
13. `com.google.android.gms:play-services-location:21.3.0` fica somente em
    `debugImplementation`. A variante release não resolve a dependência fused,
    não expõe o entrypoint da PoC e não recebe promoção produtiva por esta ADR.
    A integração não introduz segredo, token, API key ou credencial.

### Revisão de escopo e desvio de acesso

O plano inicial previa abertura somente por ADB. Por exigência explícita do
usuário, o contrato foi revisado para tornar o CTA técnico da página de
Exercícios o caminho manual primário para teste externo no debug. A mudança cabe
coerentemente em `026-03`, pois altera apenas o contrato de acesso ao entrypoint
debug e não cria nova superfície de navegação ou nova subtarefa.

Esta revisão foi implementada em `026-03`: o debug injeta callback com `Intent`
explícito e o release injeta `null`, sem referência à Activity ou ao MapLibre.
Builds, instalação debug e abertura da Activity foram validados; a validação
física de GPS/rota continua fora desta subtarefa.

## Contraindicações

- não registrar manualmente `GPS_PROVIDER` e `NETWORK_PROVIDER` como fontes
  concorrentes da PoC;
- não colocar `play-services-location` no grafo release ou em dependência
  produtiva;
- não adicionar API key, token, segredo, credencial, conta ou billing para a
  coleta fused ou para o mapa;
- não alterar `PhoneWorkoutPolicy`, não usar `WorkoutRecordingService` e não
  converter fused em fluxo de treino iniciado pelo celular;
- não solicitar localização em background, persistir rota ou enviar fixes a
  OpenFreeMap, Health Connect ou serviço próprio.

## Escopo e fronteiras

Esta ADR constitui exceção explícita aos guards gerais somente para a Spec 026.
Não altera o encerramento histórico da Fase 013 e não transforma a PoC em
backlog de produto. O CTA técnico em Exercícios é exceção de acesso manual
somente no debug; não é CTA de treino. A variante release não deve expor botão,
`Intent` executável ou entrypoint da PoC.

Logs, mensagens e evidências não podem conter latitude, longitude, altitude,
rota, GeoJSON, bounds ou identificadores pessoais. A comunicação de rede
necessária para style, tiles, fontes e infraestrutura associada do mapa-base é
aceita somente no experimento e deve ser reavaliada antes de promoção.

## Consequências

### Positivas

- permite validar MapLibre Compose e OpenFreeMap sem conta, billing ou segredo;
- mantém a Fase 013 encerrada e os guards de produto preservados;
- separa experimento visual de persistência, Health Connect, BLE e atividade real;
- permite aquecimento e coleta combinada por GPS, rede móvel, Wi-Fi, cache e
  sensores sem orquestrar manualmente múltiplos providers;
- mantém `play-services-location` isolado em debug, sem contaminar o artefato
  release;
- mantém aberta a possibilidade de trocar provider após evidência física.

### Negativas e riscos

- OpenFreeMap público pode sofrer indisponibilidade, limites ou mudança de
  termos sem SLA;
- o experimento depende de rede e de nova confirmação das condições do provider;
- a API fused depende de comportamento e disponibilidade de Google Play Services
  no aparelho debug e pode produzir fixes apoiados por fontes distintas de GPS;
- `lastLocation` pode estar ausente ou perder atualidade; ele serve somente para
  aquecer readiness/posição e não substitui fixes válidos após o início;
- a combinação de fontes pode aumentar consumo de bateria, rede e variação entre
  fabricantes, sem constituir requisito ou arquitetura de produção;
- a escolha não resolve requisitos de produção, como cache, offline, retenção,
  privacidade, escala ou suporte;
- a affordance em Exercícios pode ser confundida com fluxo de treino se não for
  identificada como PoC técnica, e isolamento incorreto de source set, recursos,
  `Intent` ou manifest pode vazar o acesso para release;
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

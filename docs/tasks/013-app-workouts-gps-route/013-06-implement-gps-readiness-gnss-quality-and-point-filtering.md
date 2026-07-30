# 013-06 — Implementar prontidão GPS, qualidade GNSS e filtro de pontos

- **Ticker:** `013`
- **Número:** `06`
- **Status:** completed

## Objetivo e resultado esperado

Criar fronteira de localização precisa, classificação de readiness e filtro determinístico de pontos, sem UI ativa nem persistência de rota em produção. Resultado: stream tipado pronto para preparação/captura.

## Contexto

Projeto não possui runtime de localização. Prontidão deve usar fix/idade/precisão como fonte primária e GNSS apenas como complemento. Mock e pontos ruins são proibidos.

## Documentos obrigatórios

Spec 013; ADR-014; `013-03/04/05`; documentação Android `LocationManager`, `Location`, `GnssStatus` e permissões da API alvo.

## Dependências

`013-05` concluída.

## Arquivos permitidos

Novos `core/location/**` ou caminho real aprovado; contratos em `core/model`; bindings Hilt; logging seguro; task/overview.

## Arquivos e símbolos prováveis

Novos `WorkoutLocationSource`, `GpsReadinessEvaluator`, `GnssQualitySnapshot`, `RoutePointFilter`, `RoutePointRejection` e `WorkoutRouteAccumulator`.

## Arquivos proibidos

UI final, FGS, repositories Room salvo adaptação mínima já aprovada, BLE, Health Connect e testes.

## Requisitos cobertos

Localização precisa; estados GPS; thresholds; GnssStatus opcional; mock; filtro; segmentos; categorias seguras de logging.

## Escopo

- adapter Android de permissão/serviços/fix;
- high accuracy durante preparação/sessão;
- readiness forte/bom/fraco/inutilizável;
- texto semântico com precisão/satélites;
- filtro puro com motivos categorizados;
- cálculo geodésico entre pontos aceitos e regras de gap/segmento.

## Fora do escopo

Persistir aquecimento, tela, countdown, serviço, mapa ou enviar HC.

## Passos de implementação

1. Implementar fonte com lifecycle explícito e cancelamento.
2. Detectar permissão negada/aproximada, provider desligado e fix mock.
3. Classificar fix recente `<=10 s`; forte `<=10 m`; bom `<=20 m`; fraco `<=50 m`; demais indisponível.
4. Complementar satélites/C/N0 quando API fornecer, sem bloquear readiness pela ausência.
5. Implementar filtro de ordem, idade, precisão, coordenadas, velocidade humana, duplicata, mock e ruído.
6. Emitir accepted/rejected com categoria, nunca coordenada em logs.
7. Definir gap prolongado ajustável e criação de segmento após retomada/perda/recuperação.
8. Garantir cleanup e rejeição de callbacks por geração antiga.
9. Atualizar overview.

## Critérios de aceite

- iniciar fica conceitualmente elegível só em bom/forte;
- precisão não é chamada de força de rádio;
- ausência de GNSS detalhado não impede avaliação por fix;
- ponto rejeitado não entra em distância;
- pausa/lacuna não conecta segmentos;
- logs expõem só contagens/categorias/faixa.

## Validação manual

No Galaxy S22, observar estados com permissão negada/aproximada, localização desligada, indoor/outdoor, fix envelhecido e mock quando reproduzível. Comparar texto com precisão real sem registrar coordenadas.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Matriz estado→entrada, logs sanitizados, captura de estados sem coordenadas e saídas Gradle.

## Screenshots

Somente se houver superfície diagnóstica temporária aprovada; ocultar coordenadas e dados pessoais.

## Definição de pronto

Fonte/readiness/filtro compilam, têm cleanup e desbloqueiam preparação; overview atualizado.

## Riscos e cuidados

APIs/valores variam por aparelho. Thresholds são direção inicial ajustável após validação física. Não armazenar ponto de aquecimento.

## Rollback

Remover binding/runtime e manter contratos inertes; nenhuma migration nova.

Não criar, alterar ou executar testes automatizados.

## Registro de execução — implementação concluída

### Causa do escalonamento e escopo

- Escalonamento para `build-sol` confirmado pela combinação de callbacks Android
  concorrentes, descarte de gerações antigas, cleanup idempotente e necessidade
  de produzir lotes compatíveis com as invariantes temporais da Room v6.
- Diff inicial estava limpo e não continha tentativa anterior desta subtarefa.
  Dependências `013-03`, `013-04` e `013-05` estavam concluídas.
- Escopo permaneceu em `core/location`, refinamento mínimo de readiness em
  `core/model` e allowlist de logging. UI, FGS, Room/repositories, BLE, Health
  Connect, manifesto, Gradle e testes permaneceram inalterados.

### Implementação e decisões

- `AndroidWorkoutLocationSource` usa `LocationManager.GPS_PROVIDER`, lifecycle
  explícito `start/stop/close`, factory Hilt sem singleton mutável de sessão,
  cleanup idempotente e identidade de runtime para descartar callbacks antigos,
  inclusive quando a mesma geração é reiniciada.
- Stream tipado carrega geração em eventos de permissão, serviço, fix efêmero,
  GNSS opcional e readiness. FINE/COARSE distinguem precisa, aproximada e
  negada; provider ausente/desligado e mock produzem estados seguros. Nenhuma
  permissão é solicitada e nenhum repository é chamado.
- Readiness mantém fix recente de até 10 segundos como fonte primária, com
  faixas forte `<=10 m`, boa `<=20 m`, fraca `<=50 m` e inutilizável para fix
  ausente, antigo, mock ou acima do limite. GNSS/C/N0 apenas complementa.
- Filtro puro aplica ordem determinística: geração, contexto de rota,
  coordenadas, medições lossless em epoch-millis, ordem/futuro, idade, precisão,
  mock, velocidade humana, duplicata e ruído. Distância usa Haversine somente
  entre pontos aceitos do mesmo segmento.
- Acumulador imutável mantém sequência, contagens, métricas, segmentos e
  rejeições; pausa, recuperação GPS/processo e gap configurável iniciam segmento
  sem linha reta. Lotes possuem no máximo 32 pontos e produzem diretamente
  `AppendWorkoutRoutePointsInput`, respeitando `endedAt`, cursor e revisão.
- Logging usa somente geração, categorias, estados/faixas de qualidade e
  contagens. Coordenadas, precisão numérica, instantes, velocidade, bearing,
  IDs, sequência e dados pessoais/saúde não entram na allowlist.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/GpsReadinessEvaluator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/LocationModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/RoutePointFilter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/WorkoutLocationLogger.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/WorkoutLocationSource.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/WorkoutRouteAccumulator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/location/android/AndroidWorkoutLocationSource.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/StructuredLogger.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutRoute.kt`
- esta task e `013-00-overview.md`.

### Comandos e evidências

- `git diff --check`: passou antes e depois da implementação.
- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL` em 26 segundos.
- `./gradlew lint assembleDebug assembleRelease`: execução final com `BUILD
  SUCCESSFUL` em 1 minuto e 30 segundos; lint, Hilt/KSP e builds debug/release
  concluíram.
- Nenhum teste automatizado foi criado, alterado ou executado. O guard antigo da
  Fase 03 permaneceu inalterado e não foi executado.

### Riscos residuais e estado

- Variações reais de provider/OEM, permissão aproximada, localização desligada,
  qualidade indoor/outdoor e mock ainda exigem validação manual no Galaxy S22.
- Captura persistente, integração ao FGS e consumo pela preparação pertencem a
  `013-07`, `013-08` e `013-09`; nenhum ponto é persistido nesta subtarefa.
- Status: `completed`. Próxima subtarefa elegível: `013-07`; não iniciada.

## Registro de revisão independente e correções

- Primeira revisão independente encontrou dois bloqueios médios: salto
  incompatível podia ser aceito ao abrir segmento após lacuna, e idade do fix
  dependia de relógio civil.
- Correção mínima aplicada em `RoutePointFilter`: velocidade implícita continua
  sendo validada contra o ponto anterior mesmo na troca de segmento; distância
  entre segmentos permanece zero e não entra nas métricas.
- `GpsReadinessInput.fixAge` passou a aceitar idade monotônica opcional. A fonte
  Android calcula idade com `Location.elapsedRealtimeNanos` e
  `SystemClock.elapsedRealtimeNanos`; `Location.time` permanece somente como
  timestamp civil do ponto.
- Revisão independente final: aprovada, sem bloqueadores. Ressalvas: validação
  física de permissões, provider/OEM, indoor/outdoor e mock ainda pendente no
  Galaxy S22; fallback civil de `fixAge` permanece somente para consumidores
  Android-free legados.

### Validação final

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` em 2 min
  39 s; lint, KSP, compilação debug/release e assemble concluídos.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

# 026-06 — Implementar câmera, stop, rota final e reset

- **Ticker:** `026`
- **Número:** `06`
- **Status:** completed

## Objetivo

Completar o ciclo interativo da PoC com follow de câmera, suspensão por gesto, recentralização, parada estável, enquadramento da rota concluída e reset sem contaminação entre ciclos.

## Resultado esperado

O usuário consegue acompanhar a posição, explorar o mapa manualmente, recentralizar, parar a captura e visualizar toda a rota antes de limpar e iniciar novo ciclo.

## Requisitos e critérios cobertos

- Spec 026: critérios 20 a 26;
- câmera utilizável durante tracking;
- stop idempotente;
- fit bounds após parada;
- reset completo de estado efêmero.

## Escopo incluído

- câmera inicial no primeiro ponto aceito;
- follow enquanto `followUser == true`;
- pan/zoom manual suspendendo follow;
- ação `Recentralizar`;
- stop da inclusão de novos pontos;
- fit bounds final;
- comportamento seguro para zero/um ponto;
- reset completo;
- segundo ciclo limpo.

## Escopo excluído

- bearing-up;
- rotação automática;
- navegação turn-by-turn;
- animações sofisticadas;
- auto-pause;
- persistência entre recreações/process death.

## Dependências

- `026-05` concluída;
- estado da rota disponível;
- API de câmera MapLibre Compose confirmada na versão fixada.

## Follow durante tracking

No primeiro ponto aceito:

```text
center = ponto
zoom ≈ 16
followUser = true
```

Durante tracking:

- mover câmera de forma moderada, sem animação agressiva por fix;
- não redefinir zoom escolhido pelo usuário a cada ponto;
- gesto manual deve suspender follow;
- `Recentralizar` restaura follow e centraliza no ponto corrente.

## Stop

`Parar` deve:

- tornar o estado `Stopped`;
- impedir novos pontos de entrarem na rota atual;
- manter linha e métricas em memória;
- desativar follow;
- continuar permitindo pan/zoom;
- não apagar imediatamente a rota.

Chamadas repetidas de stop não devem duplicar efeitos ou causar crash.

## Enquadramento final

Com dois ou mais pontos:

- calcular bounds da rota;
- aplicar padding de UI;
- enquadrar toda a geometria.

Com um ponto:

- manter centro no ponto e zoom urbano razoável.

Com zero pontos:

- preservar câmera atual.

## Reset

`Limpar` deve:

- remover rota;
- zerar distância;
- zerar contadores;
- limpar ponto final da sessão anterior;
- destruir routeId/generation anteriores;
- limpar erro de ciclo;
- manter estado atual de permissão/readiness quando seguro;
- retornar a `Ready`, `SearchingGps` ou estado equivalente.

## Segundo ciclo

Após reset:

- primeiro accepted point deve começar sequência nova;
- nenhum contador ou ponto do ciclo anterior pode reaparecer;
- callback tardio de runtime anterior deve ser descartado.

## Passos de implementação

1. Configurar câmera inicial.
2. Implementar follow sem resetar gestos indevidamente.
3. Detectar interação manual e suspender follow.
4. Implementar recentralização.
5. Tornar stop idempotente.
6. Implementar bounds final.
7. Implementar casos de zero e um ponto.
8. Implementar reset completo.
9. Exercitar dois ciclos na mesma instância do app.
10. Registrar comportamento e desvios no overview.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke físico deve cobrir pan, zoom, recentralização, stop, fit bounds e segundo ciclo.

## Definition of Done

- câmera segue enquanto habilitada;
- gesto manual não é imediatamente desfeito;
- recentralização funciona;
- stop não recebe novos pontos;
- rota final permanece visível;
- bounds final funciona;
- zero/um ponto são seguros;
- reset limpa identidade e métricas do ciclo;
- segundo ciclo não contém estado anterior;
- builds/lint passam.

## Riscos e cuidados

- callback de gesto da biblioteca pode variar por versão; usar API pública da release fixada;
- não transformar recentralização em loop que brigue com gestos;
- bounds calculado contém coordenadas e nunca deve ser logado;
- evitar animações longas que prejudiquem percepção do tracking.

## Execução

- **Status final:** completed.
- **Arquivos alterados:** `app/src/debug/java/br/com/mwconsulting/championcompanion/feature/gpspoc/GpsMapPocCoordinator.kt`, `GpsMapPocScreen.kt` e `GpsMapPocUiState.kt`.
- **Decisões e desvios:** `CameraState` permanece na tela debug e usa a mesma posição aceita pelo coordinator. O primeiro ponto centraliza em zoom urbano aproximado de 16; follow preserva zoom escolhido pelo usuário. `CameraMoveReason.GESTURE` suspende follow, e `Recentralizar` usa nonce de comando para restaurar acompanhamento durante captura e centralizar o ponto corrente. Follow, recentralização e fit final são comandos serializados em um único efeito de câmera, evitando animações concorrentes obsoletas. Stop encerra o runtime GPS, invalida a geração anterior, preserva rota/métricas e é idempotente. Parada enquadra bounds com padding para dois ou mais pontos, centraliza um ponto e preserva câmera com zero pontos. Reset zera estado efêmero, inclusive nonce de recentralização, e mantém readiness/permissão por meio do runtime recriado.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`; `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.feature.gpspoc.GpsMapPocActivity`; `adb shell dumpsys activity activities`.
- **Resultados e evidências:** todos os comandos concluíram com sucesso. O APK debug foi instalado no dispositivo `SM-S901E` com Android API 36, e a Activity técnica abriu em estado `resumed`. Nenhum teste automatizado foi criado, conforme política do projeto. Revisão independente confirmou isolamento debug-only, ausência de persistência/logs de coordenadas e coerência estrutural de follow, stop, bounds e reset.
- **Riscos residuais:** comportamento físico de pan/zoom, recentralização, fit bounds com percurso real, stop repetido e dois ciclos ainda precisa ser observado no smoke sanitizado da `026-07`; build e abertura da Activity não substituem esse gate. A validação não deve versionar rota ou localização pessoal.

# Task 002-09 — Validar Design System

## Status

Concluída em 2026-07-20.

## Dependências

- `002-01` a `002-08` concluídas.

## Objetivo

Consolidar evidências, validar requisitos e encerrar checklist de `002-00`.

## Pode modificar

- Este arquivo.
- `002-00-overview.md` exclusivamente para checklist/status final.

## Passos

1. Rodar testes unitários e instrumentados relevantes, incluindo seleção/fallback, contraste, estados, semântica, fonte ampliada e alvo de toque.
2. Rodar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`.
3. Em Galaxy S22 conectado, validar tema do sistema, Material You ligado/desligado quando disponível, splash, launcher, navegação, contraste e TalkBack básico.
4. Inspecionar fontes: nenhum Material 2, `Color.White`, `Color.Black`, hex em features, nem artefato/tokens/componentes MotionLog.
5. Conferir contratos públicos, limites de escopo, previews e recursos Android contra matriz de rastreabilidade da spec.
6. Registrar comandos, resultados, limitações reais e evidência manual; atualizar somente status próprio e checklist de `002-00`.

## Aceite

- Todos requisitos da spec têm evidência ou bloqueio explícito e reproduzível.
- Build completo passa; validação em dispositivo registrada.
- Nenhuma alteração em documentos autoritativos, MotionLog, commit ou push.

## Evidências

### Validação automatizada

```text
./gradlew lint testDebugUnitTest assembleDebug assembleRelease --console=plain
BUILD SUCCESSFUL in 1s
110 actionable tasks: 1 executed, 109 up-to-date
```

- `testDebugUnitTest`: 18 testes, sem falhas ou erros. Cobertura inclui seleção
  claro/escuro, Material You, fallback, contraste mínimo de `4.5:1`, spacing,
  tokens de estado e escala do gráfico.
- `connectedDebugAndroidTest`: 11 testes, sem falhas ou erros, em 10,85 s no
  Galaxy S22. Cobertura Compose inclui semântica dos estados, feedback e cards,
  além do alvo de toque mínimo de 48dp do botão primário. Fonte ampliada possui
  evidência em previews, não em asserção instrumentada dedicada.
- `lint`: sem erros. Avisos não bloqueantes pré-existentes: versões disponíveis
  de SDK/Gradle/dependências e `android:label` redundante no manifesto.
- Inspeção de fontes: nenhum import Material 2, `Color.White` ou `Color.Black`.
  Não há hex em `feature/`; valores literais permanecem restritos às paletas e
  vetores Android. Única ocorrência de `MotionLog` é comentário que registra a
  exclusão da identidade anterior.
- Contratos públicos da spec conferidos em `core.designsystem`: tema, tokens,
  estados, ações, cards, gráfico, feedback e banners existem sem dependência de
  BLE, Health Connect, persistência, notificações ativas ou sincronização.
- Previews claro, escuro e fonte ampliada existem para tema, estados, ações,
  exibição e feedback. Recursos launcher, splash e notificação são vetores.

### Galaxy S22

- Dispositivo conectado: `SM-S901E`, Android 16, tema escuro do sistema ativo
  (`ui_night_mode=2`). APK debug instalado e iniciado por ADB.
- Navegação inicial exibiu conteúdo, descrições semânticas e cinco destinos:
  Início, Saúde, Calendário, Exercícios e Mais. Contraste do shell escuro foi
  inspecionado em tela.
- Launcher e splash usam a marca vetorial centralizada criada em `002-08`;
  `processDebugResources` e o APK instalado validaram ligação de recursos.
- Material You ligado/desligado e fallback são cobertos deterministicamente por
  `ChampionThemeTest`. Não há preferência de tema no produto nesta fase, por
  requisito de escopo.

### Limitação registrada

- TalkBack não foi ativado no aparelho compartilhado, pois mudaria uma
  configuração global de acessibilidade. Como evidência reproduzível alternativa,
  os 11 testes instrumentados verificaram descrições semânticas, estados e alvo
  de toque no dispositivo. Uma passagem auditiva manual permanece recomendada
  antes de distribuição externa.

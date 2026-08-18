# 024-06 — Refatorar Home

- **Ticker:** `024`
- **Número:** `06`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar Home como visão diária prioritária, com progresso, métricas e
pendências legíveis. Resultado: resumo útil em primeira dobra, sem reduzir
clareza de refresh, relógio ou atividades pendentes.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- top bar, status do relógio, progresso de passos, BPM, distância, calorias;
- refresh, feedback Health Connect e banners de revisão/calibração;
- estados loading, vazio, erro, permissões e Bluetooth;
- atalhos para Dispositivo, Perfil, BPM e Exercícios.

## Escopo excluído

- política de estimativas, FEA1, coordinator, ViewModels ou novos dados;
- refresh automático, nova ação BLE ou mudança de meta.

## Dependências

- `024-05` aceita.

## Arquivos e símbolos prováveis

- `feature/home/HomeScreen.kt`;
- `HomeScreen`, `HomeTopBar`, `HomeSurface`, `HomeSummaryContent`,
  `HomeRefreshStatus` e mapeamentos visuais locais;
- componentes do design system.

## Passos de implementação

1. Aplicar hierarquia do piloto à visão diária.
2. Destacar progresso/meta e último dado relevante antes de metadados.
3. Remover cores locais tocadas em favor de tokens semânticos adequados.
4. Preservar pull-to-refresh, ação acessível, banners e callbacks.
5. Exercitar estados reais sem inventar métricas.
6. Validar ciclo de vida e saída da tela sem mudança funcional.
7. Registrar capturas sanitizadas, resultados e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: refresh seguro, atalhos, retorno, rolagem, portrait, fonte `1,3x`,
landscape e temas representativos.

## Definição de pronto

- primeira dobra prioriza resumo diário e estado do relógio;
- refresh e pendências continuam explícitos;
- estados sem dados permanecem seguros;
- callbacks e lifecycle equivalentes;
- comandos e capturas registrados; overview atualizado.

## Riscos e cuidados

- não disparar BLE apenas para screenshot;
- banners não podem ocultar resumo ou ação essencial;
- semântica do pull-to-refresh deve permanecer;
- dados pessoais precisam ser ocultados nas evidências.

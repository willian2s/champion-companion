# 031-09 — Redesenhar Dispositivo

- **Ticker:** `031`
- **Número:** `09`
- **Status:** pending

## Objetivo

Aplicar composição da referência à gestão do Champion C001 sem alterar conexão,
permissões, diagnóstico ou isolamento da PoC.

## Requisitos e aceite

- Associação, scan, conexão, disconnect, retry, esquecer e substituir preservados.
- Permissões, erros, diálogos e diagnósticos permanecem seguros.
- Não adicionar nem mover CTA/Intent da PoC MapLibre para Dispositivo; a única
  affordance manual da PoC MapLibre continua na página Exercícios, debug-only,
  fora de release. Diagnóstico BLE existente permanece apenas se já fizer parte
  do contrato da tela, sem ser confundido com a PoC de mapa.
- Header e status de dispositivo seguem entrada da Home.

## Escopo

Inclui layout, hierarquia, cards, ações e estados visuais. Exclui protocolo BLE,
GPS, rota, mapa, persistência e alteração de `PhoneWorkoutPolicy`.

## Dependências

- `031-04` a `031-08`, além da fundação visual.
- Specs 006, 026 e ADR-024.

## Arquivos e símbolos prováveis

- `feature/device/DeviceScreen.kt`.
- `DeviceManagementViewModel.kt`, contratos e estados.
- Providers/Activity da PoC debug, sem alteração funcional.

## Passos

1. Mapear estados visuais para composição aprovada.
2. Reorganizar cards sem perder eventos one-shot ou launchers.
3. Manter diagnóstico sanitizado e CTA por variante.
4. Validar operações não destrutivas e cancelamentos.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de abertura, rolagem, permissões disponíveis, retorno,
fonte ampliada, landscape e temas. Não iniciar scan/conexão apenas para captura.

## Definição de pronto

Dispositivo visualmente aprovado, estados assíncronos preservados, nenhum CTA da
PoC MapLibre exposto na tela ou release, estado inicial/restaurado do aparelho e
revisão independente aprovados.

## Riscos

Compactação pode esconder erro ou ação de recuperação. Manter estados textuais e
alvos acessíveis.

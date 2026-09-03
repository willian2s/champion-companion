# 031-08 — Redesenhar Perfil

- **Ticker:** `031`
- **Número:** `08`
- **Status:** pending

## Objetivo

Aplicar composição da referência ao Perfil, preservando edição de métricas
pessoais, calibração e navegação para atividade relacionada.

## Requisitos e aceite

- Altura, meta, comprimento de passo e estados de calibração permanecem.
- Diálogos, validação, cancelamento, remoção e correção vinculada preservados.
- Dados ausentes são omitidos ou exibidos como ausência segura.
- Header e ação de retorno seguem shell aprovado.

## Escopo

Inclui layout, cards, agrupamentos, ícones, textos e estados visuais. Exclui novo
perfil, nova métrica, persistência ou mudança de algoritmo.

## Dependências

- `031-04`, `031-05`, `031-06` e `031-07`, além da fundação visual.
- Spec 023 e contratos de Perfil.

## Arquivos e símbolos prováveis

- `feature/profile/ProfileScreen.kt`.
- `ProfileViewModel.kt`, `ProfileFeatureContract.kt`.

## Passos

1. Mapear seções da referência para dados de Perfil existentes.
2. Reordenar composição e manter editores/modais.
3. Preservar precisão, provenance e abertura de atividade.
4. Validar cancelamento e retorno sem persistência indevida.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de leitura, diálogos, retorno, fonte ampliada, landscape e
temas. Não criar ou executar testes automatizados.

## Definição de pronto

Perfil aprovado visualmente, sem regressão em edições/calibração, com evidência
sanitizada, estado inicial/restaurado do aparelho e revisão independente
aprovados.

## Riscos

Layout compacto pode cortar precisão de campos ou ocultar ação destrutiva.
Preservar clareza, validação e confirmação.

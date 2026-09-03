# 031-05 — Redesenhar Home

- **Ticker:** `031`
- **Número:** `05`
- **Status:** pending

## Objetivo

Aplicar composição da referência à Home, priorizando resumos e mantendo todas as
ações e estados atuais.

## Requisitos e aceite

- Resumo diário, passos/meta, BPM, distância, calorias e pendências aparecem
  somente quando dados existem.
- Refresh explícito, status do relógio e estados seguros permanecem.
- Header fornece acesso a Dispositivo e Perfil.
- Resumos encaminham a Saúde, Exercícios e calendário conforme matriz do shell.
- Registrar nomes dos componentes compartilhados usados também em `031-06`;
  promoção exige uso real nas duas superfícies, nunca apenas preview.

## Escopo

Inclui hierarquia, cards, listas, textos, ícones, espaçamento e estados visuais
de Home. Exclui novas métricas, coleta, sincronização e mudança de cálculo.

## Dependências

- `031-04` e fundação visual.
- `HomeUiState`, `HomeViewModel` e contratos existentes.

## Arquivos e símbolos prováveis

- `feature/home/HomeScreen.kt`.
- `HomeFeatureContract.kt`, `HomeViewModel.kt`.

## Passos

1. Mapear conteúdo atual para blocos da referência.
2. Reordenar composição sem alterar callbacks ou lifecycle.
3. Omitir blocos sem equivalente e preservar ausência segura.
4. Validar atalhos e refresh sem disparar operação artificial.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de estados observáveis, rolagem, atalhos, fonte ampliada,
landscape e temas. Não criar ou executar testes automatizados.

## Definição de pronto

Home visualmente aprovada contra baseline, com dados/estados/callbacks
equivalentes, evidência sanitizada, estado inicial/restaurado do aparelho e
revisão independente aprovados.

## Riscos

Redução de conteúdo pode esconder pendências. Conferir todos os estados do
contrato antes de omitir qualquer seção.

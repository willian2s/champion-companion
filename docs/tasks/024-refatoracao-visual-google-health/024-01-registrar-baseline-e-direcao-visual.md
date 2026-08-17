# 024-01 — Registrar baseline e direção visual

- **Ticker:** `024`
- **Número:** `01`
- **Status:** completed

## Objetivo e resultado esperado

Inspecionar pelo ADB o Google Health/Fitbit atual e o Champion Companion,
registrando baseline sanitizado, matriz de guia visual e ADR da estratégia
incremental. Resultado: guia verificável de estilo e design sem exposição de
dados pessoais e decisão duradoura registrada antes de código de produção.

## Requisitos e critérios cobertos

- R1, R7, R8 e R9.
- Confirmação de `com.fitbit.FitbitMobile` como referência.
- Critérios 1, 2, 10 e 12 da spec.

## Escopo incluído

- confirmar dispositivo e pacotes pelo ADB;
- observar navegação, headers, cards, listas, gráficos, seções e estados do
  Google Health sem registrar valores;
- capturar baseline equivalente das treze superfícies atuais do Champion;
- criar matriz visual e capturas sanitizadas na pasta da task;
- criar ADR sobre foundation aditiva, shell preservado e rollout tela por tela;
- registrar versão/data e limitações da referência.

## Escopo excluído

- alteração de código de produção;
- login, mudança de conta, exportação ou exclusão;
- leitura de banco, Health Connect, Logcat amplo ou dados pessoais;
- definição pixel a pixel ou cópia de ativos.

## Dependências

- spec 024;
- aparelho desbloqueado e autorização ADB operacional;
- Google Health instalado;
- permissão de edição em `docs/decisions/`;
- variante debug do Champion instalada ou instalável.

## Arquivos e símbolos prováveis

- nova evidência `docs/tasks/024-refatoracao-visual-google-health/evidencia-baseline-visual-024-01.md`;
- novo `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`,
  se `023` continuar disponível;
- imagens sanitizadas na pasta da task;
- nenhum arquivo Kotlin.

## Passos de implementação

1. Confirmar `adb devices -l` e pacotes `com.fitbit.FitbitMobile` e
   `br.com.mwconsulting.championcompanion.dev`.
2. Registrar versão dos apps sem coletar dados de conta.
3. Navegar pelo Google Health e observar somente estrutura, labels genéricos,
   hierarquia, bounds e padrões.
4. Navegar pelas superfícies atuais do Champion e registrar composição,
   densidade, ações e estados disponíveis.
5. Redigir matriz: padrão de estilo/design observado, aplicação proposta,
   adaptação Champion e elemento proibido de copiar.
6. Registrar ADR com rollout aditivo e navegação preservada.
7. Sanitizar capturas antes de persistir; descartar dumps temporários sensíveis.
8. Registrar estados não alcançados como `N/A` e bloqueios concretos.

## Testes e comandos de validação

Não executar testes automatizados.

```bash
adb devices -l
adb shell pm list packages com.fitbit.FitbitMobile
adb shell pm list packages br.com.mwconsulting.championcompanion.dev
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
git diff --check
```

Uso de `uiautomator` ou screenshot deve ser temporário e sanitizado antes de
entrar no repositório.

## Definição de pronto

- pacote e versão da referência registrados;
- matriz cobre shell, resumo, métrica, lista, gráfico, feedback e privacidade;
- treze superfícies Champion possuem baseline;
- ADR criada no próximo número disponível;
- nenhuma evidência contém dado pessoal;
- limitações e `N/A` estão explícitos;
- overview atualizado conforme AGENTS.md.

## Riscos e cuidados

- políticas da ferramenta podem bloquear ADB ou `docs/decisions/`;
- UI Google varia por conta, região, premium e estado de dados;
- não executar ações destrutivas ou sincronizações;
- capturas pessoais não podem ser persistidas sem sanitização.

## Execução registrada

### Arquivos alterados

- `evidencia-baseline-visual-024-01.md`
- `024-01-captura-sanitizada-referencia.svg`
- `024-01-captura-sanitizada-champion.svg`
- `captura-sanitizada-superficies-024-01.svg`
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`
- `024-00-overview.md`
- `024-01-registrar-baseline-e-direcao-visual.md`

### Decisões e desvios

- Confirmado `com.fitbit.FitbitMobile` como referência; Google Fit e Health
  Connect ficaram fora da referência visual.
- ADR-023 registra foundation aditiva, shell preservado, hub Saúde piloto e
  rollout tela por tela.
- Screenshots e dumps integrais permaneceram temporários. Evidência versionada
  usa reconstruções vetoriais sem métricas ou identificadores.
- Detalhe diário ficou `N/A` no smoke físico porque o CTA não respondeu; sua
  composição foi registrada por código e nenhuma ação adicional foi fabricada.
- Estados raros, landscape e fonte `1,3x` ficaram `N/A` nesta subtarefa e serão
  validados nas subtarefas de código.

### Comandos executados e resultados

| Comando | Resultado |
|---|---|
| `adb devices -l` | Passou; Galaxy S22 `SM-S901E` autorizado |
| `adb shell pm list packages com.fitbit.FitbitMobile` | Passou; pacote presente |
| `adb shell pm list packages br.com.mwconsulting.championcompanion.dev` | Passou; pacote presente |
| `adb shell dumpsys package com.fitbit.FitbitMobile` filtrando versão | Passou; versão registrada no baseline |
| `adb shell dumpsys package br.com.mwconsulting.championcompanion.dev` filtrando versão | Passou; `1.0-dev` |
| `adb shell monkey -p com.fitbit.FitbitMobile 1` | Passou; referência aberta pelo launcher |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Champion aberto |
| `uiautomator dump` temporário | Passou; labels genéricos/bounds usados; dump removido |
| `git diff --check` | Executado após documentação; sem erro |

Não foram criados, alterados ou executados testes automatizados. Gradle não foi
executado porque subtarefa não altera código, dependências ou build.

### Evidências

- Baseline sanitizado: `evidencia-baseline-visual-024-01.md`.
- Capturas vetoriais sanitizadas: `024-01-captura-sanitizada-referencia.svg` e
  `024-01-captura-sanitizada-champion.svg`.
- Captura compósita das treze superfícies:
  `captura-sanitizada-superficies-024-01.svg`.
- Decisão duradoura: `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`.
- Nenhum dado pessoal foi persistido.

### Riscos residuais

- Google Health pode variar por versão, região, conta e estado de dados.
- Smoke físico não cobriu detalhe diário pelo CTA, landscape, fonte `1,3x`,
  estados raros e temas alternativos.
- Capturas não são screenshots integrais; representam somente estrutura
  sanitizada para auditoria sem exposição de dados.
- Dumps do aparelho foram removidos com `adb shell rm` após leitura. PNGs
  temporários locais foram removidos do diretório externo autorizado após a
  inspeção; nenhum arquivo bruto permanece.

## Reexecução solicitada — 2026-08-17

### Status

`completed`; rerun físico concluído sem alteração de escopo.

### Arquivos alterados nesta reexecução

- `evidencia-baseline-visual-024-01.md`;
- `024-01-registrar-baseline-e-direcao-visual.md`;
- `024-00-overview.md`.

As capturas SVG e o ADR não precisaram de alteração: a estrutura observada e a
decisão registrada permaneceram válidas.

### Decisões e desvios

- A referência continuou sendo `com.fitbit.FitbitMobile`; Google Fit e Health
  Connect não foram abertos nem tratados como referência visual.
- O rerun repetiu somente inspeção estrutural e navegação de abertura; não houve
  login, troca de conta, importação, sincronização, exclusão ou medição real.
- Contagens de hierarquia foram registradas sem labels, textos ou valores para
  preservar privacidade.
- Nenhum desvio novo foi identificado.

### Comandos executados e resultados

| Comando | Resultado |
|---|---|
| `adb devices -l` | Passou; Galaxy S22 `SM-S901E` autorizado |
| `adb shell pm list packages com.fitbit.FitbitMobile` | Passou; pacote presente |
| `adb shell pm list packages br.com.mwconsulting.championcompanion.dev` | Passou; pacote presente |
| `adb shell dumpsys package ...` | Passou; Fitbit `5.05.health-mobile-1104094190-958410393`, Champion `1.0-dev` |
| `adb shell cmd package resolve-activity ...` | Passou; Fitbit `com.fitbit.HealthBrandedAlias`, Champion `MainActivity` |
| `adb shell monkey -p com.fitbit.FitbitMobile 1` | Passou; referência aberta |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Champion aberto |
| `uiautomator dump` temporário sanitizado | Passou; Fitbit 67 nós/9 clicáveis/0 roláveis; Champion 77 nós/8 clicáveis/1 rolável; dump removido |
| `adb shell wm size`, `wm density`, `font_scale`, `user_rotation`, `cmd uimode night` | Passou; `1080x2340`, density `480`, fonte `1.0`, portrait, tema escuro |
| `git diff --check` | Passou; sem erro |

Não foram criados, alterados ou executados testes automatizados. Gradle não foi
executado porque a subtarefa continua limitada a documentação e evidência
visual, sem código, dependência ou build.

### Evidências e riscos residuais

- Evidência detalhada atualizada em `evidencia-baseline-visual-024-01.md`.
- Capturas vetoriais sanitizadas e ADR-023 permanecem válidas.
- Riscos residuais permanecem: variação por versão/região/conta, ausência de
  estados raros, landscape, fonte `1,3x`, temas alternativos e smoke do detalhe
  diário pelo CTA.

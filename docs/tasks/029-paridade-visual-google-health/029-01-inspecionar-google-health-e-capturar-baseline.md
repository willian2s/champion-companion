# 029-01 — Inspecionar Google Health e capturar baseline

- **Ticker:** `029`
- **Número:** `01`
- **Status:** `pending`

## Objetivo e resultado esperado

Inspecionar o aplicativo instalado de referência e o Champion Companion com o
aparelho ADB conectado. Resultado: matriz verificável de telas, cards, detalhes,
filtros, períodos, estados e evidências visuais sanitizadas, sem alteração de
código ou dados do usuário.

## Requisitos e critérios de aceite cobertos

- R1, R2, R3 e R8 da Spec 029.
- Critérios 1, 2, 3 e 10.
- Confirmar `com.fitbit.FitbitMobile`, versão, Activity e configuração física.
- Abrir todos os cards alcançáveis antes de registrar `N/A`.
- Percorrer todos os filtros, incluindo frequência cardíaca diária, períodos
  sem dados, abas Semana/Mês, calendário mensal e os sete filtros de workflow.

## Escopo incluído

- confirmar dispositivo, pacotes, versão e Activity via ADB;
- observar hierarquia, bounds, composição, densidade, ordem, labels genéricos,
  gestos e estados sem registrar valores pessoais;
- abrir telas e cards já existentes, sem login ou troca de conta;
- capturar screenshots temporários para comparação e sanitizá-los antes de
  qualquer persistência;
- registrar matriz por superfície, estado, filtro e cobertura.

## Escopo excluído

- alteração de produção, domínio ou persistência;
- login, troca de conta, importação, sync, medição, exclusão ou reset;
- leitura de banco, Health Connect readback, payload BLE ou logcat amplo;
- screenshot bruto no repositório;
- uso de sites, Google Fit ou Health Connect como referência visual.

## Dependências

- aparelho desbloqueado e ADB autorizado;
- `com.fitbit.FitbitMobile` instalado;
- variante debug do Champion instalada;
- Spec 029 e acesso para criar evidência em sua pasta.

## Arquivos e símbolos prováveis

- `docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`;
- screenshots sanitizados nomeados por superfície dentro da pasta da task;
- matriz de cobertura no mesmo documento;
- `MainActivity`, `ChampionNavigation`, `HomeScreen`, `HealthScreen`,
  `CalendarScreen` e `ExercisesScreen` como pontos de navegação.

## Passos de implementação

1. Executar `adb devices -l` e confirmar Galaxy S22 autorizado.
2. Identificar pacote, versão, `versionCode`, Activity, resolução, density,
   font scale, rotação, modo noturno e configuração regional observável.
3. Abrir a referência e registrar somente estrutura, contagens, bounds e
   comportamento visual genérico.
4. Percorrer todos os cards e detalhes alcançáveis; registrar cada resultado ou
   `N/A` com motivo.
5. Passar por todos os filtros: períodos de frequência cardíaca, Semana/Mês,
   calendário de atividades e filtros de workflow.
6. Repetir a cobertura estrutural no Champion para formar comparação por tela.
7. Remover valores, textos pessoais, títulos, BPM, peso, horários, IDs e
   metadados; revisar pixels e arquivos antes de versionar.
8. Registrar limitações concretas, inclusive estados não alcançados.

## Testes e comandos de validação

```bash
adb devices -l
adb shell pm list packages com.fitbit.FitbitMobile
adb shell pm list packages br.com.mwconsulting.championcompanion.dev
adb shell cmd package resolve-activity --brief com.fitbit.FitbitMobile
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
git diff --check
```

Não executar testes automatizados. Dumps e screenshots de referência devem ser
temporários e removidos após sanitização.

## Definição de pronto

- referência instalada e configuração física registradas;
- matriz cobre todas as superfícies listadas na Spec 029;
- cada card e filtro foi aberto ou possui `N/A` justificado;
- caso cardíaco sem dados e caso exercícios Semana/Mês estão documentados;
- evidências versionadas não contêm dados pessoais nem screenshot bruto;
- nenhum código, dado, integração ou configuração de produção foi alterado.

## Riscos e cuidados

- ADB bloqueado impede aceite físico; não substituir por suposição.
- UI pode depender de versão, região, conta e conteúdo atual.
- Não disparar ações que alterem dados para fabricar estados.
- Revisar pixels, EXIF e nomes de arquivos antes de persistir evidências.
- A inspeção deve respeitar a privacidade mesmo com autorização do usuário para
  dados não sanitizados.

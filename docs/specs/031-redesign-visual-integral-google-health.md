# 031 — Redesign visual integral baseado no Google Health/Fitbit

- **Ticker:** `031`
- **Status:** pending
- **Tipo:** redesign visual e reorganização de navegação
- **ADR:** [ADR-028](../decisions/adr-028-redesign-visual-integral-google-health.md)
- **Supersede:** [Spec 024](024-refatoracao-visual-google-health.md), quanto à direção visual

## Contexto e objetivo

O Champion Companion possui features funcionais para dados coletados do Champion
C001: resumo diário, importação de treinos do relógio, frequência cardíaca,
passos, distância, peso, perfil, calibração, exercícios, calendário e Health
Connect write-only. A Spec 024 concluiu uma adaptação visual inspirada no Google
Health/Fitbit, mas manteve diferenças deliberadas de disposição e shell.

O novo objetivo é reconstruir a apresentação do app com fidelidade visual máxima
à versão observada do Google Health/Fitbit (`com.fitbit.FitbitMobile`), sem copiar
logo, fonte proprietária ou recursos de produto que o Champion Companion não
possui. O conteúdo exibido continuará limitado aos dados e fluxos já existentes.

## Decisões confirmadas pelo usuário

- Referência: `com.fitbit.FitbitMobile`.
- Barra inferior: somente `Home` e `Saúde`.
- Home: resumos principais; header com acesso a Dispositivo e Perfil.
- Calendário: calendário de exercícios restrito ao fluxo de Exercícios, acessível
  pela tela de Exercícios; Home e Saúde não ganham integração direta.
- Escopo: todas as superfícies atuais, incluindo Configurações.
- Fidelidade: disposição, gráficos, filtros, ícones, textos relevantes,
  espaçamentos e hierarquia visual devem seguir a referência observada.
- Métricas sem equivalente: seção omitida; métrica próxima existente pode ser
  usada sem alterar sua semântica.
- Features, estados, callbacks, BLE, importação, persistência e Health Connect:
  preservados.
- ADR-023 e Spec 024: supersedidas, com histórico preservado.

## Evidência da referência

Fontes públicas atuais confirmam a associação do app ao Google Health/Fitbit e ao
pacote `com.fitbit.FitbitMobile`. A página pública de Google Health e a página da
Google Play podem orientar catálogo e versão, mas não substituem inspeção da UI
instalada.

A inspeção física deve registrar versão, data, estrutura, labels genéricos,
bounds e, no repositório, somente evidência sanitizada. A autorização temporária
descrita abaixo permite uma inspeção local excepcional, mas não muda a regra de
que screenshots, dumps ou dados não sanitizados nunca são versionados. Imagens
públicas que contenham marca ou ativos proprietários ficam fora dos recursos do
app e servem apenas como referência efêmera/documental permitida.

### Exceção operacional temporária da Spec/Task 031

O usuário autorizou, exclusivamente durante a execução da Spec/Task 031, o uso
efêmero de screenshots ou outros dados de referência não sanitizados quando isso
for indispensável para construir o layout. A autorização vale somente para
inspeção em aparelho autorizado, com finalidade visual ou estrutural, e não
constitui autorização geral para coleta, tratamento ou divulgação de dados
pessoais. Esta revisão documental não capturou nem leu dados pessoais reais ou
screenshots brutos; o XML estrutural temporário da auditoria foi usado apenas
para agregados e removido.

Limites obrigatórios da exceção:

- **Escopo de uso:** analisar composição, hierarquia, geometria, navegação e
  estados visuais da referência. O material não pode alimentar feature, dado de
  produto, fixture, teste, dataset, treinamento, analytics ou decisão funcional.
- **Local e transporte:** material não sanitizado pode permanecer somente no
  armazenamento temporário local do aparelho autorizado durante a sessão. É
  proibido copiá-lo para o repositório, máquina de desenvolvimento, anexos,
  pull request, contexto do agente, cloud, rede, backup, device transfer ou
  Logcat.
- **Ações no aparelho:** inspeção deve ser somente leitura. Não iniciar
  importação, sincronização, medição, exclusão de dados, edição, login, troca de
  conta, concessão de permissão, Health Connect ou operação BLE para obter
  evidência. A exclusão de temporários criados para inspeção é obrigatória;
  configurações temporárias de visualização devem ser restauradas.
- **Propriedade intelectual:** não extrair, importar, recortar ou versionar
  logo, fonte, imagem, ícone, ilustração ou outro asset proprietário. Notas e
  reconstruções devem usar recursos próprios ou nativos. Captura sanitizada
  versionável deve ser totalmente desidentificada e sem pixels proprietários;
  reconstrução estrutural própria é preferível.
- **Retenção e descarte:** criar somente temporários necessários, sem retenção
  além da sessão. Apagar cada arquivo bruto ao fim da sessão, conferir que não
  restou cópia local e não usar lixeira, cache, backup ou sincronização como
  retenção. Evidência derivada só pode ser versionada depois de sanitizada.
- **Auditoria:** registrar somente metadados sanitizados da sessão — data,
  autorização, aparelho identificado por alias não sensível, versão observada,
  finalidade, escopo, ações não destrutivas e confirmação do descarte. Nunca
  registrar conteúdo bruto, conta, métrica, identificador pessoal, identificador
  de conta, serial ou título livre.
- **Falha e rollback:** se houver desvio de escopo, transporte indevido ou
  dúvida sobre limpeza, interromper a inspeção, não usar o material, apagar os
  temporários conhecidos, restaurar o aparelho e registrar o incidente de forma
  sanitizada. A subtask fica bloqueada; decisões ou alterações derivadas da
  sessão irregular devem ser revertidas para o último baseline público ou
  sanitizado aprovado. Se o descarte não puder ser confirmado, a Spec não pode
  ser encerrada como concluída.
- **Validade:** a autorização expira no encerramento da Spec 031 ou se for
  revogada e não se estende a outra spec, task ou aparelho.

Esta exceção não altera `AGENTS.md`, ADR-004, os guards de logs e backup ou as
proibições de dados sensíveis no repositório. Se alguma regra hierarquicamente
superior impedir uma sessão, a exceção não se aplica e o bloqueio deve ser
registrado em `Observações`, sem contorno.

Se pacote, aparelho ou estado de conta não estiverem disponíveis, registrar
`N/A` e limitar a decisão ao material público verificável; não declarar paridade
não observada.

## Comportamento atual encontrado

- `ChampionNavigation` mantém back stack manual e cinco entradas na barra/menu,
  com destinos Home, Saúde, Calendário e Exercícios no topo e Dispositivo,
  Perfil e Configurações em `Mais`.
- `AppDestination` possui destinos estáveis para Home, Saúde, quatro detalhes de
  métricas, Calendário, Exercícios, Dispositivo, Perfil e Configurações.
- Home exibe resumo diário, passos/meta, pendências, último BPM, distância,
  calorias, status do relógio, refresh e atalhos.
- Saúde é hub de frequência cardíaca, peso, passos e distância, com detalhes,
  períodos e históricos locais.
- Exercícios reúne importação D3/D4, filtros, lista, detalhe, revisão,
  calibração, descarte e sincronização explícita.
- Calendário possui grade mensal e detalhe diário com navegação cruzada.
- Perfil gerencia altura, meta, comprimento de passo, perfis aprendidos e
  calibrações.
- Dispositivo gerencia permissões, scan, associação, conexão e diagnóstico/PoC
  BLE debug-only já existente.
- Configurações gerencia Health Connect write-only, permissões e fila local.
- A PoC GPS + MapLibre permanece exclusivamente no fluxo de Exercícios, no
  source set debug, conforme ADR-024; nenhum CTA dela pertence a Dispositivo.
- Gráficos já usam `core/designsystem/chart` e preservam lacunas, unidades,
  séries reais e acessibilidade.

## Requisitos

### R1 — Referência visual

Usar a UI observada de `com.fitbit.FitbitMobile` como referência de composição,
hierarquia, densidade, navegação, cards, listas, filtros, ícones, gráficos,
espaçamento, textos relevantes, estados e ritmo visual. Não importar assets,
fontes ou dependências proprietárias.

### R2 — Shell de duas áreas

- Barra inferior mostra somente `Home` e `Saúde`.
- Seleção, back stack, restauração, animações e retorno contextual permanecem
  funcionais.
- Calendário não aparece como área de topo; entra pelo fluxo de Exercícios.
- Dispositivo e Perfil ficam acessíveis pelo header da Home.
- Exercícios, calendário e Configurações mantêm entradas contextuais claras;
  nenhuma rota funcional pode ficar órfã. Calendário permanece restrito ao
  fluxo de Exercícios; Saúde e demais superfícies não ganham integração direta
  com Calendário.

Matriz mínima de entradas e retorno:

| Origem | Destino | Retorno |
|---|---|---|
| Home/header `Dispositivo` | Dispositivo | Home |
| Home/header `Perfil` | Perfil | Home |
| Perfil/ação `Configurações` | Configurações | Perfil |
| Home/resumo `Atividades` | Exercícios | Home |
| Saúde/resumo `Atividades` | Exercícios | Saúde |
| Saúde/card de métrica | detalhe correspondente | Saúde |
| Exercícios/ação `Calendário` | Calendário | Exercícios |
| Exercícios/item | detalhe de atividade | Exercícios |
| Calendário/dia | detalhe diário | Calendário |
| Calendário/voltar | origem que abriu Calendário | Exercícios |
| detalhe diário/atividade | detalhe de atividade | detalhe diário |
| detalhe de atividade/voltar | Exercícios | origem contextual |
| detalhe de métrica/voltar | Saúde | Saúde |

### R3 — Mapeamento de conteúdo

- Usar somente dados já persistidos ou estados reais das features.
- Métrica equivalente próxima pode ocupar posição visual correspondente quando
  unidade, origem e significado permanecerem explícitos.
- Se não houver equivalente confiável, ocultar seção inteira.
- Não inventar valores, séries, metas, insights, estados ou fontes.

### R4 — Home

Reorganizar Home para espelhar a estrutura observada de resumo diário, priorizar
resumos úteis e manter refresh, estados de dispositivo, pendências, BPM, passos,
distância, calorias e atalhos contextuais conforme dados disponíveis.

### R5 — Saúde e detalhes

Reorganizar hub e detalhes de frequência cardíaca, peso, passos e distância para
seguir a referência em cards, cabeçalhos, filtros, tendências, listas e ações,
origem, unidade, período, lacunas e ausência segura.

### R6 — Exercícios e calendário

Reorganizar lista, filtros, importação, progresso, detalhe, revisão, calibração,
descarte e sincronização; incorporar calendário ao fluxo de Exercícios e
preservar detalhe diário e navegação contextual existentes.

### R7 — Perfil, Dispositivo e Configurações

Aplicar a mesma composição visual às telas de Perfil, Dispositivo e Configurações,
preservando edições, diálogos, permissões, conexão, diagnósticos, fila Health
Connect e isolamento debug-only da PoC.

### R8 — Fundação visual

Evoluir tokens e componentes Compose somente quando necessário. Componentes devem
ser stateless, baseados em slots e sem acesso a ViewModel, DAO, repository, BLE
ou Health Connect. Não alterar consumidores não migrados por efeito global.

### R9 — Acessibilidade e estados

Preservar heading, content descriptions, state descriptions, live regions,
ordem de foco, contraste, alvos de pelo menos 48 dp, fonte ampliada, landscape,
temas claro/escuro e estados transitórios seguros.

### R10 — Integridade funcional

Não alterar Room, schema, DataStore, BLE, Health Connect, permissões, manifesto,
serviços, fórmulas, granularidade, contratos ou políticas phone. Não adicionar
GPS, localização, rota, mapa ou treino phone.

### R11 — Validação visual e privacidade

Cada bloco de código deve passar por `git diff --check`,
`./gradlew lint assembleDebug assembleRelease`, `./gradlew installDebug` e smoke
ADB. Registrar somente evidências versionadas sanitizadas, com configurações do
aparelho restauradas. Qualquer material não sanitizado usado sob a exceção deve
permanecer no aparelho autorizado, ser descartado ao fim da sessão e ter essa
limpeza auditada. Não criar ou executar testes automatizados sem pedido
explícito.

### R12 — Rollout e revisão

Subtarefas são independentes e sequenciais. Cada uma deve registrar arquivos,
decisões, desvios, comandos, evidências e riscos; não avançar automaticamente
após concluir uma subtarefa; exigir revisão independente.

## Critérios de aceite

1. Referência é documentada como `com.fitbit.FitbitMobile`, com versão/data e
   evidências permitidas sem dados pessoais; toda sessão excepcional possui
   escopo e descarte confirmados.
2. Barra inferior possui somente Home e Saúde, com seleção e restauração corretas.
3. Dispositivo e Perfil são acessíveis pelo header da Home.
4. Exercícios é acessível pelos contextos previstos de Home e Saúde; Calendário
   é acessível somente pelo fluxo de Exercícios, sem rota funcional órfã.
5. As 13 superfícies — Home, hub Saúde, detalhes de frequência cardíaca, peso,
   passos e distância, lista de Exercícios, detalhe de atividade, Calendário,
   detalhe diário, Perfil, Dispositivo e Configurações — seguem a composição
   visual aprovada da referência.
6. Gráficos e filtros preservam séries reais, lacunas, unidades, períodos e
   acessibilidade; formato visual segue a referência sem inventar dados.
7. Seções sem métrica equivalente são omitidas, e métricas próximas mantêm
   semântica e origem explícitas.
8. Importação do relógio, BPM, passos, peso, distância, calibração, revisão,
   descarte e Health Connect continuam funcionando com os mesmos contratos.
9. Nenhum logo, fonte proprietária, asset proprietário, backend, login, IA,
   premium ou feature Google/Fitbit é incorporado.
10. Nenhuma alteração reativa ou expande phone workout, GPS, localização, rota,
    mapa ou PoC fora de seu isolamento debug-only.
11. Fonte ampliada, landscape, temas, foco, contraste e estados seguros não
    ocultam ações essenciais.
12. Cada subtarefa registra validação, evidência versionada sanitizada, riscos
    residuais e revisão independente antes da próxima; quando a exceção for
    usada, registra também auditoria de escopo e descarte.

## Abordagem escolhida

Baseline público/estrutural e físico sanitizado, com inspeção efêmera local
excepcional quando indispensável → supersessão documental → fundação visual →
shell de duas áreas → Home → Saúde e métricas → Exercícios e calendário → Perfil
→ Dispositivo → Configurações → consolidação. A referência define apresentação;
contratos locais definem comportamento e verdade dos dados.

## Alternativas descartadas

- Manter apenas inspiração visual da Spec 024.
- Fazer redesign apenas por mudança de paleta.
- Preservar cinco itens na barra inferior.
- Apagar rotas de Exercícios, Calendário, Perfil, Dispositivo ou Configurações.
- Migrar para `NavController` sem necessidade funcional.
- Criar métricas/insights fictícios para preencher telas.
- Versionar dumps, screenshots ou dados não sanitizados.
- Reativar fluxo de treino phone para obter paridade visual.

## Arquivos, módulos e contratos afetados

Módulo `:app`. Arquivos principais:

- `core/designsystem/ChampionTokens.kt`, `ChampionTheme.kt`,
  `ChampionColorScheme.kt` e componentes compartilhados;
- `core/navigation/AppDestination.kt` e `ChampionNavigation.kt`;
- `feature/home/HomeScreen.kt` e contratos/ViewModel relacionados;
- `feature/health/HealthScreen.kt`, `HealthMetricDetailScreens.kt`, shells,
  gráficos e contratos/ViewModels;
- `feature/exercises/ExercisesScreen.kt`, shells, detalhes e contratos;
- `feature/calendar/CalendarScreen.kt`, `CalendarDayScreen.kt` e contratos;
- `feature/profile/ProfileScreen.kt` e contratos/ViewModel;
- `feature/device/DeviceScreen.kt` e contratos/ViewModel;
- `feature/settings/SettingsScreen.kt` e contratos/ViewModel;
- documentação da Spec 024, ADR-023, ADR-028 e evidências da Spec 031.

Contratos preservados: repositories, modelos, Room v13, DataStore, BLE,
Health Connect, `PhoneWorkoutPolicy`, intents, estados, callbacks, IDs e
persistência. Navegação muda somente sua apresentação e posição contextual,
conforme ADR-028.

## Riscos e mitigação

- **Referência indisponível:** usar fontes públicas e marcar observações físicas
  como `N/A`, sem declarar sucesso não observado.
- **Evidência sensível:** material não sanitizado só pode existir no aparelho
  autorizado durante a sessão excepcional; sanitização é obrigatória antes de
  versionar e o descarte precisa ser confirmado.
- **Regressão de shell:** preservar destinos e saver; smoke de cada rota.
- **Rota órfã:** matriz de entradas e retorno em `031-04` e `031-11`.
- **Perda de semântica:** revisar mapeamento de unidade, origem, período e
  estados antes de alterar composição.
- **Escopo proprietário:** excluir logo, fonte, assets, IA, premium e dados do
  Google/Fitbit; usar recursos nativos/próprios.
- **PoC debug-only:** manter source set, Intent e isolamento de release intactos.
- **Abstração prematura:** promover componentes somente após dois usos reais.

## Estratégia de validação

Sem testes automatizados novos ou execução de testes automatizados por padrão.
Para cada subtarefa com código:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke deve cobrir abertura, rolagem, retorno, Home/Saúde, rotas contextuais,
ações não destrutivas, portrait, landscape, fonte ampliada e temas. Ações de
importação, medição, sync ou exclusão não devem ser iniciadas somente para gerar
evidência. Evidências versionadas devem ser sanitizadas; qualquer temporário
bruto usado na exceção deve ser apagado ao fim da sessão e auditado.

## Ordem das subtarefas

```text
031-01 → 031-02 → 031-03 → 031-04 → 031-05 → 031-06
       → 031-07 → 031-08 → 031-09 → 031-10 → 031-11
```

## Premissas explícitas

- `com.fitbit.FitbitMobile` é referência visual, não fonte de dados.
- Usuário forneceu autorização para superseder ADR-023/Spec 024.
- Recursos proprietários não serão incorporados ao app.
- Dados locais e contratos existentes são suficientes para preencher somente
  partes equivalentes da referência.
- Ausência de equivalente resulta em omissão, nunca em dado sintético.
- Calendário continua funcional, mas passa a ser contextual ao fluxo de
  Exercícios e não item da barra inferior.
- Configurações continua acessível fora da barra inferior.
- ADB/referência física pode estar indisponível; nesse caso registrar `N/A`.
- Evidência não sanitizada não será versionada por regra de privacidade. A
  exceção temporária permite somente uso local no aparelho autorizado, com
  descarte ao fim de cada sessão e confirmação final em `031-11`.

## Rastreabilidade

| Subtarefa | Requisitos | Superfície |
|---|---|---|
| `031-01` | R1, R11 | Auditoria e baseline da referência |
| `031-02` | R10, R12 | Supersessão documental e ADR |
| `031-03` | R8, R9 | Fundação visual |
| `031-04` | R2, R9, R10 | Shell e navegação |
| `031-05` | R3, R4, R9, R10 | Home |
| `031-06` | R3, R5, R9, R10 | Saúde, métricas, gráficos e filtros |
| `031-07` | R2, R3, R6, R9, R10 | Exercícios e calendário |
| `031-08` | R3, R7, R9, R10 | Perfil |
| `031-09` | R3, R7, R9, R10 | Dispositivo |
| `031-10` | R3, R7, R9, R10 | Configurações |
| `031-11` | R1–R12 | Consolidação e revisão final |

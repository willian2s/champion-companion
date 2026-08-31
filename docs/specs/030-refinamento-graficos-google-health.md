# 030 — Refinamento visual integral inspirado no Google Health

- **Ticker:** `030`
- **Status:** `in_progress`
- **Tipo:** melhoria visual transversal

## Contexto e objetivo

As Specs 024 e 028 alinharam superfícies ao Google Health/Fitbit e
centralizaram gráficos em Vico. O plano original desta spec ficou estreito ao
tratar principalmente gráficos, embora a autorização do usuário cubra a
experiência visual completa.

Objetivo: revisar todas as superfícies produtivas atuais do Champion Companion
com referência direta à composição observada no Google Health/Fitbit, incluindo
overview, navegação visual, cabeçalhos, cards, listas, estados, controles,
gráficos, cores, espaçamento, hierarquia, acessibilidade e comportamento de
apresentação. Dados, contratos, fluxos e funcionalidades continuam próprios do
Champion Companion.

Nesta spec, “tudo do Google Health” significa aplicar a linguagem visual de
referência a todas as superfícies existentes do Champion. Não significa portar
funcionalidades, dados, destinos ou integrações que não existem no app.

## Fontes de referência

- https://healthapp.google/;
- https://healthapp.google/your-health-connected;
- https://play.google.com/store/apps/details?id=com.fitbit.FitbitMobile;
- inspeção estrutural do app instalado `com.fitbit.FitbitMobile`, quando
  disponível, limitada a labels, hierarquia, bounds e padrões sem dados pessoais;
- Material 3 e componentes existentes em `core/designsystem`.

As páginas públicas foram consultadas em 2026-08-31. Google Health/Fitbit é a
referência visual; Google Fit e Health Connect não são referências visuais. A
implementação usa código próprio: não copiar código-fonte ou APK, dados pessoais,
credenciais, arquivos ou ativos extraídos do app de referência.

## Escopo visual

A revisão cobre composição, hierarquia, geometria, espaçamento, navegação
visual, controles já existentes, cards, listas, estados de loading/vazio/erro,
mensagens, diálogos, gráficos, interação, temas, orientação e fonte ampliada.
Cada alteração deve ser opt-in e preservar callbacks, destinos e contratos
existentes.

## Requisitos

### R1 — Replicação visual integral autorizada

- Aplicar referência Google Health/Fitbit a todas as superfícies produtivas
  listadas em R4, não somente aos gráficos.
- Reproduzir fielmente, quando compatível com o Champion, layout, geometria,
  espaçamento, navegação visual, cards, listas, controles, estados, gráficos,
  cores, hierarquia e comportamento de apresentação.
- Priorizar leitura principal por superfície, reduzindo redundância sem remover
  informação operacional necessária.
- Diferenças causadas por dados, contratos, limites de período ou fluxos do
  Champion devem ser registradas, sem fabricar dados ou funcionalidades.

### R2 — Períodos de pesquisa existentes

- Adotar `D`, `S`, `M`, `3M` e `A` como vocabulário visual, com descrições
  acessíveis `Dia`, `Semana`, `Mês`, `3 meses` e `Ano`.
- Passos, distância e Evolução usam somente `S`/7 dias, `M`/30 dias e
  `3M`/90 dias. `D` e `A` ficam ocultos ou desabilitados, sem callback ou query.
- Frequência cardíaca usa `D`/`TODAY` intradia, `S`/7 dias e `M`/30 dias.
  `3M` e `A` ficam ocultos ou desabilitados.
- Detalhe de treino não possui seletor: BPM, distância canônica e cadência D4
  usam janela, intervalos e amostras reais da atividade.
- Outras superfícies preservam seus controles atuais. Nenhum período novo,
  read model ou consulta é criado para aparência.

### R3 — Infraestrutura e linguagem compartilhadas

- Manter `core/designsystem/chart` como boundary única de renderer, tema,
  marker, tooltip e interação de séries.
- Features não importam Vico nem desenham séries com Canvas.
- Primitivos visuais compartilhados devem ser stateless, baseados em dados e
  slots, com adoção gradual para não alterar fluxos não migrados.
- Shell, cards, estados, controles e diálogos podem compartilhar componentes,
  sem acoplar design system a domínio, DAO, BLE ou Health Connect.

### R4 — Superfícies produtivas cobertas

Revisar visualmente todas as superfícies atuais:

- shell transversal: navegação inferior, menu Mais, cabeçalhos, transições,
  cards, botões, chips, banners, diálogos e estados globais;
- Home: overview diário, resumo principal, métricas secundárias, pendências,
  refresh e feedback de sincronização;
- Saúde: hub, detalhes de frequência cardíaca, peso, passos e distância,
  medição manual, importação D1, históricos, listas e estados operacionais;
- Calendário: mês, seleção, indicadores, legenda, detalhe diário e navegação
  contextual;
- Exercícios: importação watch-first, progresso, filtros, lista, detalhe,
  revisão, calibração, intervalos, ações, sincronização e os três gráficos;
- Evolução: período, filtro, resumo, tendências, comparação, recordes e
  navegação para atividade;
- Dispositivo: pré-requisitos, associação, busca, conexão, manutenção,
  diagnóstico e exportação sanitizada;
- Perfil: altura, meta, comprimento de passo, calibração, perfis aprendidos,
  histórico, edição, correção e confirmações;
- Configurações: disponibilidade, permissões, toggle, ações foreground, fila e
  estados do Health Connect.

A PoC de GPS/mapa permanece somente debug e fora da revisão produtiva.

### R5 — Preservação semântica

- Não alterar Room, DataStore, BLE, Health Connect, domínio, sincronização,
  granularidade, derivação ou períodos persistidos.
- Gaps permanecem ausência; não suavizar, interpolar, reamostrar ou converter
  ausência em zero.
- BPM usa amostras reais, distância usa série canônica e cadência usa
  intervalos D4 elegíveis.
- Estados de workflow, permissões, fila, falha e revisão permanecem distintos.
- Não reativar treino iniciado pelo celular, GPS, rota ou localização.

### R6 — Acessibilidade e adaptação

- Resumos acessíveis informam métrica, estado, janela quando aplicável, unidade,
  quantidade e lacunas.
- Fonte ampliada, portrait, landscape, claro/escuro e conteúdo longo não podem
  ocultar navegação, ação principal, valor, controle ou leitura da superfície.
- Tap/drag, marker e tooltip não podem impedir rolagem vertical sem necessidade.
- Cor não pode ser único sinal de estado, comparação, disponibilidade ou erro.

## Critérios de aceite

1. Todas as superfícies produtivas de R4 usam composição visual coerente,
   incluindo as que não possuem gráfico.
2. Shell, navegação visual, cabeçalhos, cards, listas, estados, controles,
   diálogos e gráficos seguem linguagem comum sem quebrar callbacks ou destinos.
3. Seletor e labels seguem `D`/`S`/`M`/`3M`/`A` onde aplicável, com mapa de R2,
   descrição acessível, opções sem suporte sem ação e nenhuma query nova.
4. Resumos, unidades, janelas, origem, estado e série são compreensíveis sem
   redundância visual.
5. Gaps, dados reais, granularidade e estados de workflow permanecem intactos.
6. Marker, tooltip, eixos, grid, barras, linhas e estados vazios seguem tema
   comum; nenhuma feature importa Vico ou cria novo Canvas de série.
7. Nenhuma alteração funcional ocorre em Room, DataStore, BLE, Health Connect,
   domínio, sincronização ou navegação.
8. Validação visual sanitizada cobre superfícies migradas, períodos disponíveis,
   fonte ampliada, landscape e temas, registrando `N/A` quando não exercitada.
9. Nenhum teste automatizado é criado, alterado ou executado.

## Comportamento atual encontrado

- Home, Saúde, Calendário, Exercícios, Evolução, Dispositivo, Perfil e
  Configurações são superfícies produtivas com padrões locais de composição.
- Saúde, Exercícios e Evolução possuem gráficos compartilhados, mas ainda têm
  alturas, cabeçalhos, textos, estados e limites de rolagem definidos localmente.
- Navegação, cards, diálogos, listas, filtros e feedback de operações não estavam
  incluídos explicitamente no plano original.
- `HealthChart` legado permanece em previews/testes, sem consumidor produtivo
  confirmado; só pode ser removido com comprovação de uso.

## Abordagem escolhida

Executar rollout em seis subtarefas: benchmark visual integral; fundação visual
e shell; Home e Saúde; Calendário e Exercícios; Evolução, Dispositivo, Perfil e
Configurações; validação integrada e fechamento documental. Cada boundary mantém
rollback isolado e nenhuma subtarefa altera contratos funcionais.

Dependências:

```text
030-01 → 030-02 → 030-03 → 030-04 → 030-05 → 030-06
```

## Arquivos, módulos e contratos afetados

- `core/designsystem/*` e `core/designsystem/chart/*`;
- `core/navigation/*` somente para apresentação, sem alterar destinos ou back
  stack;
- `feature/home/*`, `feature/health/*`, `feature/calendar/*`;
- `feature/exercises/*`, `feature/evolution/*`, `feature/device/*`;
- `feature/profile/*`, `feature/settings/*`;
- contratos de apresentação somente quando necessários para labels ou estados;
- documentação e evidências sanitizadas da task.

Nenhum contrato persistente, migration, manifesto ou dependência nova é
esperado. ADR só será criada se surgir decisão arquitetural duradoura não coberta
por ADR-023 e ADR-027.

## Riscos e mitigação

- **Escopo amplo:** uma boundary por subtarefa, adoção opt-in e sem refactor
  funcional lateral.
- **Paridade funcional indevida:** replicar composição, não criar features,
  fontes, destinos ou dados do Google Health.
- **Período sem suporte:** ocultar/desabilitar e registrar `N/A`; nunca simular.
- **Regressão de estado:** validar gaps, loading, erro, permissões, fila e
  workflow antes/depois.
- **Shell global:** preservar back stack manual, callbacks e diálogos de saída.
- **Dados pessoais em evidência:** recortar/mascarar valores e não persistir
  dumps ou screenshots crus.
- **Referência mutável:** registrar data, fonte, pacote e limitações observadas.

## Estratégia de validação

Não criar, alterar ou executar testes automatizados. Para subtarefas com código:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Executar smoke ADB das superfícies alteradas, fluxos e períodos reais,
rolagem, retorno, portrait, landscape, fonte `1,3x` e temas. Registrar
evidência sanitizada e restaurar configurações do aparelho.

## Ordem das subtarefas

```text
030-01 → 030-02 → 030-03 → 030-04 → 030-05 → 030-06
```

## Premissas explícitas

- O usuário autoriza replicação visual fiel do Google Health/Fitbit em todas as
  superfícies existentes do Champion, sem copiar implementação ou ativos.
- “Tudo” nesta spec é cobertura visual integral do produto atual, não paridade
  funcional com áreas inexistentes no Champion.
- Dados, contratos, fluxos, permissões, sincronização e integrações permanecem
  próprios do Champion Companion.
- A direção pós-Fase 15 permanece vigente; não criar fase macro nova.
- ADB continua obrigatório para aceite físico quando uma subtarefa alterar código.

## Fora do escopo

- novas telas, destinos, métricas, fontes, queries ou funcionalidades do Google
  Health que não existam no Champion;
- Coach, Gemini, IA, insights médicos, sono, nutrição, login, backend, analytics
  ou recursos premium;
- leitura do Health Connect ou sync automático;
- GPS, localização, rota, mapa ou treino phone;
- novas métricas ou pipeline de sensores;
- testes automatizados;
- cópia de código-fonte/APK, dados pessoais, credenciais, arquivos ou ativos do
  app de referência.

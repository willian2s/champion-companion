# ADR-028 — Paridade visual com referência instalada e evidência sanitizada

- **Status:** aceita
- **Data:** 2026-09-01

## Contexto

A Spec 029 precisa transformar o baseline visual da task `029-01` em contrato
verificável antes de qualquer alteração de UI. O aplicativo observado como
referência é exclusivamente `com.fitbit.FitbitMobile`, instalado no Galaxy S22
`SM-S901E`, com versão `5.06.1.health-mobile-1104101009-966217170`,
`versionCode` `1104101009`, resolução `1080x2340`, densidade física `480`,
override `450`, escala de fonte `1.0`, portrait, tema escuro e região `pt-BR`.

O baseline está registrado em
`docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`.
Ele contém superfícies cobertas, estados vazios, equivalentes ausentes e
limitações observadas. A referência possui organização `Semana`/`Mês` e filtros
de exercícios que ainda não existem no APK do Champion observado; esses casos
não podem ser tratados como cobertura presumida.

ADR-023 já orienta evolução visual incremental, reversível e inspirada no
Google Health. ADR-027 define boundary único para gráficos interativos. Esta
decisão detalha fonte normativa, fidelidade, identidade, evidência e ordem de
rollout, sem reescrever o histórico da Spec 024.

## Decisão

### 1. Fonte visual normativa

`com.fitbit.FitbitMobile` instalado é a única fonte normativa para aparência,
hierarquia e comportamento visual. Google Fit, Health Connect e sites não
participam do aceite visual. A versão, pacote, Activity, aparelho, resolução,
densidade, escala de fonte, orientação, tema, região e data devem acompanhar
cada novo baseline.

O baseline deve ser repetido somente em estados alcançáveis sem ações
destrutivas. Cada card, detalhe, período e filtro relevante precisa ser aberto
ou receber `N/A` com motivo concreto. Conteúdo dependente de conta ou região
não vira requisito literal: compara-se estrutura observável, não dados da
conta.

### 2. Fidelidade e identidade

“Pixel a pixel” é objetivo de fidelidade visual, não autorização para copiar
marca, fonte, textos, assets, dados ou screenshots proprietários. A comparação
deve buscar equivalência observável de:

1. hierarquia e ordem das superfícies;
2. densidade, espaçamento, proporções e agrupamento;
3. estados, affordances, navegação de retorno e filtros;
4. comportamento em portrait/landscape, tema e fonte ampliada;
5. semântica e ações de acessibilidade.

O resultado continua sendo Champion: identidade, dynamic color, tema, tokens,
componentes, textos, ícones e ações próprias permanecem sob controle do
aplicativo. Não criar rotas, dados ou contratos apenas para imitar a referência.

### 3. Contrato de comparação

Cada superfície deve ser comparada na mesma configuração física e no mesmo
estado de apresentação. A revisão ocorre nesta ordem: estrutura, geometria,
densidade, estados/interação, acessibilidade e, por último, pixels. Diferenças
causadas por conteúdo pessoal, renderização do sistema, dynamic color ou
identidade Champion são registradas como contexto, não corrigidas com cópia.

Uma superfície só pode ser declarada `coberto` quando foi aberta e possui
leitura de código ou evidência sanitizada correspondente. `vazio` significa
estado sem dados realmente aberto. `N/A` exige motivo concreto: estado não
alcançado, superfície inexistente no aplicativo comparado ou dependência não
disponível. `N/A` não equivale a aprovado e deve permanecer pendente para
validação quando a dependência for resolvida.

### 4. Evidência sanitizada

O repositório versiona somente matriz, reconstrução estrutural e screenshots
sanitizados quando necessários. Dumps ADB, screenshots brutos, arquivos
intermediários e metadados de imagem são temporários e devem ser removidos após
a revisão local. Não enviar evidência bruta para assets de produção, issues,
cloud storage ou outro destino externo.

Identificadores determinísticos usam o formato
`029-<superficie>-<aplicativo>-<estado>-<configuracao>`, com minúsculas,
separador `-` e sem nomes de conta. Antes de versionar, remover ou substituir
por placeholders: conta, foto, nome, títulos de atividade, horários, BPM,
peso, distâncias, contagens exatas, IDs, tokens, localização, EXIF e qualquer
metadado pessoal. Esse processo é a redaction/sanitização obrigatória. A
retenção fica limitada ao documento sanitizado da task e ao ADR; cópias brutas
devem ser apagadas ao terminar a comparação.

### 5. Preservação de contratos

O rollout não altera `AppDestination`, back stack manual Navigation 3,
`ChampionNavigation`, domínio, Room, DataStore, permissões, BLE, Health
Connect write-only, snapshots históricos, políticas de treino phone ou backup.
Frequência cardíaca mantém fontes reais, lacunas e períodos existentes; não há
série sintética. Os sete filtros de workflow permanecem funcionais e com a
mesma semântica. Gráficos usam exclusivamente o boundary compartilhado de
`core/designsystem/chart`, conforme ADR-027 e Spec 028; features não importam
Vico diretamente e não há renderer paralelo.

## Matriz de aceite por superfície

| Superfície | Baseline 029-01 | Critério verificável | Dependência / ordem |
|---|---|---|---|
| Hoje / Home | referência e Champion cobertos | Header, cards, histórico, ordem, densidade, rolagem e retorno equivalentes sem alterar destinos | tokens/shell → 1 |
| Saúde | referência e Champion cobertos | Hub e agrupamento de frequência cardíaca, peso, passos e distância equivalentes | tokens/shell → 2 |
| Frequência cardíaca | referência e Champion cobertos; vazio observado | `TODAY`, `LAST_7_DAYS` e `LAST_30_DAYS` usam dados locais reais, preservam gaps e vazio, com interação acessível | Spec 028-10 → gráficos → 3 |
| Peso | referência e Champion cobertos | Períodos, gráfico, resumo, lista, estados e retorno equivalentes; sem alterar fonte de dados | Spec 028-10 → gráficos → 3 |
| Passos | referência e Champion cobertos | Diário/semanal/mensal, gráfico, explicação, rolagem e retorno equivalentes | Spec 028-10 → gráficos → 3 |
| Distância | referência e Champion cobertos | Diário/semanal/mensal, gráfico, estado, rolagem e retorno equivalentes; preservar semântica canônica | Spec 028-10 → gráficos → 3 |
| Calendário mensal | referência e Champion cobertos | Mês, navegação anterior/próximo, marcadores, legenda, dia com dados/vazio e retorno | estado de apresentação → 4 |
| Detalhe diário | referência `N/A`; Champion coberto | Dados e vazio permanecem somente leitura, com ausência explícita quando aplicável | calendário → 4 |
| Exercícios / Semana | referência coberta; Champion `N/A` observado | Implementar somente quando equivalente for alcançável; aba e estados não podem ser declarados cobertos antes de evidência | calendário/exercícios → 5 |
| Exercícios / Mês | referência coberta; Champion `N/A` observado | Mês contém calendário de atividades; sem nova persistência ou migração | calendário/exercícios → 5 |
| Filtros de exercícios da referência | referência coberta; Champion `N/A` observado | Dias com exercícios, duração e distância só recebem equivalente se semântica for confirmada; não copiar nomenclatura sem contrato | exercícios → 5 |
| Detalhe de exercício | referência e Champion cobertos | Card, origem, tipo, status, reprocessamento e retorno equivalentes sem reescrever snapshot | exercícios → 5 |
| Filtros de workflow | referência `N/A`; Champion coberto | `ALL`, `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED`, `SYNC_FAILED` e `DISCARDED` continuam selecionáveis, inclusive vazios | exercícios → 5 |
| Dispositivo | referência e Champion cobertos | Hierarquia e estados equivalentes; ações não executadas não são declaradas como sucesso | superfícies restantes → 6 |
| Perfil | referência e Champion cobertos | Menu/formulário, campos e retorno equivalentes sem alterar dados durante inspeção | superfícies restantes → 6 |
| Configurações | referência e Champion cobertos | Data, unidades, tema, notificações, permissões e Health Connect preservados | superfícies restantes → 6 |

O aceite físico ainda exige portrait e landscape, temas claro/escuro, fonte
ampliada, rolagem, marker, tooltip, toque, retorno, cada período e cada filtro.
ADB indisponível bloqueia esse aceite; não há conversão para aceite presumido.

## Ordem de rollout

1. Congelar e manter matriz sanitizada de `029-01`.
2. Fechar `Spec 028-10` antes de qualquer mudança de renderer.
3. Evoluir tokens e componentes opt-in.
4. Alinhar shell, Home e Saúde.
5. Alinhar métricas e gráficos através do boundary de ADR-027.
6. Alinhar calendário e exercícios como estado de apresentação.
7. Alinhar dispositivo, perfil e configurações, uma superfície por vez.
8. Executar smoke físico e fechar somente estados alcançados; registrar demais
   estados como `N/A` com motivo e dependência.

## Consequências

A equipe possui fonte visual única, critérios auditáveis, matriz de rollout e
evidência reutilizável sem dados pessoais. A fidelidade pode ser avaliada sem
confundir aparência de referência com cópia de identidade ou conteúdo.

O aceite fica condicionado ao aparelho e à versão observados. Estados ausentes,
ADB indisponível e dependência `028-10` continuam riscos explícitos. Qualquer
necessidade de novo domínio, persistência, integração ou quebra de contrato
deve bloquear a implementação visual e originar spec/decisão própria.

## Relação com decisões e especificações

- Estende e não reescreve [ADR-023](adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md).
- Preserva o boundary de [ADR-027](adr-027-unified-interactive-charts.md).
- Detalha limites de execução de [Spec 024](../specs/024-refatoracao-visual-google-health.md).
- Depende da ordem e do renderer previstos em [Spec 028](../specs/028-unified-interactive-charts.md).
- Executa critérios visuais de [Spec 029](../specs/029-paridade-visual-google-health.md).

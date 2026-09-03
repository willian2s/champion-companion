# ADR-028 — Redesign visual integral baseado no Google Health/Fitbit

- **Status:** aceita
- **Data:** 2026-09-03
- **Supersede:** ADR-023
- **Spec:** [031-redesign-visual-integral-google-health.md](../specs/031-redesign-visual-integral-google-health.md)

## Contexto

A ADR-023 autorizou somente inspiração visual no Google Health/Fitbit e preservou
o shell com cinco entradas. O usuário agora autorizou redesign visual integral
baseado no pacote `com.fitbit.FitbitMobile`, mantendo a marca, as fontes e as
features próprias do Champion Companion.

O pedido também altera a arquitetura de apresentação: a barra inferior terá
somente `Home` e `Saúde`; Dispositivo e Perfil ficarão acessíveis pelo header da
Home; Exercícios concentrará seu calendário; demais rotas continuarão acessíveis
por contextos da própria aplicação.

## Decisão

1. Usar a versão observada do Google Health/Fitbit como referência visual
   principal para disposição, hierarquia, densidade, cards, gráficos, filtros,
   ícones, textos relevantes e navegação.
2. Recriar a linguagem visual no Compose com componentes e recursos próprios do
   projeto. Não adicionar logo, fonte, arquivo de imagem, conta, backend ou
   dependência proprietária do Google/Fitbit.
3. Manter exatamente as features, dados e contratos existentes: importação de
   treinos do relógio, frequência cardíaca, passos, distância, peso, perfil,
   calibração, Health Connect write-only, dispositivo e estados de erro/loading.
4. Quando referência mostrar métrica sem equivalente local, omitir seção. Quando
   existir métrica próxima já coletada, usar somente seu valor e sua semântica
   real; nunca criar valor sintético.
5. Reduzir barra inferior a `Home` e `Saúde`. Preservar Navigation 3, back stack
   manual, destinos serializáveis, restauração, callbacks e confirmação de saída.
6. Manter `AppDestination.Calendar` como destino compatível, mas retirar sua
   posição de topo: calendário será acessado pelo fluxo de Exercícios e pelos
   resumos contextuais de Home/Saúde.
7. Manter Dispositivo e Perfil acessíveis pelas ações nomeadas do header da Home.
   Configurações será acessível pela ação `Configurações` do Perfil, retornando
   ao Perfil, sem desaparecer nem ganhar item na barra.
8. Executar migração por superfície, com validação e revisão independente entre
   blocos. Nenhuma tela não migrada pode mudar implicitamente por alteração de
   tema global.

## Guard rails preservados

- Não alterar Room, schema, DataStore, BLE, Health Connect, permissões,
  manifesto, serviços ou contratos de domínio.
- Não reativar treino phone, GPS, localização, rota, mapa ou `ExerciseRoute`.
- Dispositivo pode manter somente diagnóstico/PoC BLE debug-only já existente;
  a PoC GPS + MapLibre, com Intent e CTA técnico, permanece exclusivamente em
  Exercícios/debug, conforme ADR-024. Uma não autoriza ou substitui a outra.
- Não adicionar Coach, IA, premium, sono, nutrição, ciclo, prontuário, login,
  analytics, rede nova ou métricas não existentes.
- Não registrar dados pessoais, payload BLE, dumps completos ou screenshots não
  sanitizados no repositório.
- Evidências de referência devem usar material público sem dados pessoais,
  inspeção efêmera sanitizada ou reconstruções estruturais. A solicitação de
  versionar evidências não sanitizadas é incompatível com ADR-004 e AGENTS.md.
- Acessibilidade, contraste, alvos acionáveis de 48 dp, fonte ampliada,
  landscape, temas e estados seguros permanecem obrigatórios.

## Consequências

### Positivas

- Interface terá organização visual coerente com referência escolhida.
- Barra inferior ficará reduzida às duas áreas principais solicitadas.
- Features locais permanecerão preservadas enquanto a apresentação muda.
- Rotas secundárias continuarão disponíveis por contexto, sem apagar contratos.

### Negativas e riscos

- Cópia visual integral aumenta risco de confusão de identidade e de trade dress;
  recursos proprietários continuam fora do repositório.
- Mudança de shell pode produzir regressões de back stack e acessibilidade.
- Referência varia por versão, região, conta, dados e testes A/B.
- Ocultação de métricas sem equivalente pode reduzir densidade visual.
- Evidência física de referência pode ficar limitada por indisponibilidade do
  pacote ou ausência de aparelho autorizado.

## Alternativas

- Manter inspiração parcial da ADR-023: rejeitada pelo novo objetivo explícito.
- Preservar cinco entradas na barra: rejeitada pelo pedido de dois itens.
- Apagar destinos secundários: rejeitada porque features continuam existentes.
- Criar dados fictícios para preencher o layout: rejeitada por integridade das
  métricas.
- Armazenar evidência não sanitizada: rejeitada por privacidade operacional.

## Relação com decisões existentes

- Supersede ADR-023 somente quanto à direção visual e organização de navegação.
- ADR-005 continua regendo Navigation 3 e back stack manual.
- ADR-009 continua regendo Saúde como hub e detalhes de métricas.
- ADR-004 continua regendo logs, evidências, backup e dados sensíveis.
- Specs funcionais de BLE, importação, métricas e Health Connect permanecem
  autoritativas para comportamento.

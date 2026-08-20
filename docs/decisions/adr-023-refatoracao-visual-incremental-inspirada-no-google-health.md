# ADR-023 — Refatoração visual incremental inspirada no Google Health

- **Status:** aceita
- **Data:** 2026-08-17

## Contexto

O Champion Companion possui Material 3, dynamic color, paleta fallback própria,
navegação Navigation 3 com back stack manual e treze superfícies funcionais.
Existe, porém, densidade desigual, repetição de cards, cabeçalhos locais e
mistura de resumo com metadados técnicos.

O Google Health atual, distribuído no pacote Android `com.fitbit.FitbitMobile`,
foi inspecionado somente como referência de hierarquia, agrupamento, densidade,
cards, listas, ações e navegação calma. Google Fit e Health Connect não são
referências visuais.

## Decisão

Adotar refatoração visual incremental, aditiva e reversível:

1. Registrar baseline físico sanitizado antes de código.
2. Evoluir fundação visual com componentes stateless, opt-in e baseados em slots.
3. Preservar shell, destinos, back stack manual, saver, animações, guards,
   callbacks e seleção de subárvores.
4. Migrar primeiro shell e hub Saúde como piloto; somente avançar após aceite
   visual registrado.
5. Migrar demais superfícies uma por vez, mantendo componentes antigos durante
   rollout e permitindo rollback pela subtarefa correspondente.
6. Promover abstrações compartilhadas somente após uso real em pelo menos duas
   superfícies, salvo invariantes do shell.
7. Preservar identidade Champion: nome, conteúdo, ícones Material, tipografia do
   sistema, dynamic color e paleta fallback azul-petróleo.
8. Tratar variedade cromática no app inteiro como direção final, mas adotar cores
   opt-in por superfície, usando roles e containers Material 3 sem alterar tema
   raiz enquanto houver telas não migradas.

## Guard rails

- Não alterar domínio, Room, DataStore, BLE, Health Connect, permissões,
  manifesto, dependências ou contratos funcionais.
- Não copiar logo, fonte, quatro cores, ilustrações, textos, layouts,
  funcionalidades, Coach, IA ou premium Google/Fitbit.
- Não reativar treino phone, GPS, localização, rota, mapa ou `ExerciseRoute`.
- Não persistir screenshot, dump, métrica, conta, título livre, identificador ou
  payload BLE sem sanitização.
- Ações de importação, medição, sincronização, exclusão e mudança de conta não
  podem ser disparadas somente para produzir evidência visual.

## Consequências

### Positivas

- Cada tela pode ser revisada, aceita e revertida isoladamente.
- Telas não migradas não sofrem redesign implícito por mudança global de tema.
- A decisão mantém navegação e fronteiras funcionais existentes.
- Baselines e capturas sanitizadas tornam comparação visual auditável.

### Negativas

- Inconsistência temporária entre telas antigas e migradas é esperada.
- Componentes antigos permanecem até haver evidência de ausência de uso.
- Validação física precisa repetir portrait, landscape, fonte ampliada, temas e
  estados relevantes em cada subtarefa de código.
- A referência é mutável por versão, região, conta e estado de dados.

## Alternativas rejeitadas

- **Big-bang visual:** dificulta revisão, isolamento de regressões e rollback.
- **Troca de navegação por `NavController`:** não resolve requisito visual e
  viola a decisão registrada em ADR-005.
- **Mudança global de tokens:** redesenha consumidores não migrados
  implicitamente.
- **Cópia integral do Google Health/Fitbit:** viola identidade, privacidade e
  escopo funcional do Champion.
- **Catálogo completo antes do piloto:** cria abstração sem uso comprovado.

## Emenda — estratégia cromática transversal

- **Data:** 2026-08-18

Após revisão do piloto Saúde, foi identificada necessidade de maior variedade
cromática no app inteiro. A decisão não autoriza mutação global de
`ChampionColorScheme` ou `ChampionTheme`: isso redesenharia implicitamente
superfícies ainda não migradas e contrariaria R2.

A direção cromática será definida em `024-04` e aplicada por superfície a partir
do piloto `024-05`, com uso intencional de `primary`, `secondary`, `tertiary` e
respectivos containers. Dynamic color, fallback azul-petróleo, contraste e
sinais textuais/icônicos permanecem obrigatórios. Nenhuma cor, marca, fonte,
ativo ou mapeamento proprietário do Google Health/Fitbit será copiado.

## Emenda — resultado visual do piloto Saúde

- **Data:** 2026-08-18

O hub Saúde aceito aplica azul-petróleo no header e acentos `primary`, `secondary`
e `tertiary` em bordas, valores e setas dos cards. `Métricas locais` permanece
branca no escuro e usa `onBackground` no claro para preservar contraste. A
superfície pode usar fallback cromático nominal do Champion quando dynamic color
for acromático; tema raiz e superfícies não migradas permanecem inalterados.

Resumo e ação precedem metadados técnicos. O hub preserva origem, período,
unidade e estado necessários, mas não duplica card de Health Connect; status da
integração permanece em Configurações. Quatro destinos e callbacks continuam
preservados.

## Emenda — regra cromática comum das superfícies pendentes

- **Data:** 2026-08-18

Após solicitação de alinhamento visual da Home ao piloto Saúde, a adoção da
linguagem cromática passa a ser obrigatória em cada superfície migrada da Spec
024: header, quando presente, usa azul-petróleo nominal do Champion com conteúdo
branco; cards, indicadores e progresso usam acentos nominais `primary`,
`secondary` e `tertiary` da paleta clara ou escura conforme o tema. Bases neutras
continuam Material 3. Essa regra é opt-in por superfície e não altera dynamic
color do tema raiz nem consumidores não migrados.

O fallback nominal deixa de ser apenas condicional a esquema acromático nas
superfícies migradas; a exigência vale para preservar coerência com o piloto,
contraste e identidade do Champion. Sinais textuais e semânticos continuam
obrigatórios, e cor não é sinal único.

## Relação com outras decisões

- ADR-005 continua autoritativa para Navigation 3 e back stack manual.
- ADR-009 continua autoritativa para Saúde como hub e rotas específicas.
- ADR-004 continua autoritativa para privacidade operacional.
- A Spec 024 e o overview `024-00` definem ordem, critérios e evidências.

## Emenda — consolidação final

- **Data:** 2026-08-20

O fechamento da Spec 024 confirmou que primitives e componentes compartilhados
com consumidores reais permanecem estáveis. Foram removidos somente
`LocalChampionSpacing` e `LocalChampionElevation`, composition locals sem uso
comprovado. Componentes sem consumidor de superfície, mas cobertos por
previews/testes existentes, foram preservados para não alterar contratos fora do
escopo da subtarefa. A validação física final reutiliza evidências sanitizadas
individuais e smoke integrado, sem alterar tema raiz, navegação ou contratos
funcionais.

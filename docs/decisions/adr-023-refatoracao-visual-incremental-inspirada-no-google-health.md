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

## Relação com outras decisões

- ADR-005 continua autoritativa para Navigation 3 e back stack manual.
- ADR-009 continua autoritativa para Saúde como hub e rotas específicas.
- ADR-004 continua autoritativa para privacidade operacional.
- A Spec 024 e o overview `024-00` definem ordem, critérios e evidências.

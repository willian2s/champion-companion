# 014-07 — Derivar distância canônica a partir de passos

- **Ticker:** `014`
- **Número:** `07`
- **Status:** pending

## Objetivo e resultado esperado

Criar snapshot de derivação V1 por atividade `WATCH_D3_D4` e calcular distância
canônica total e temporal somente por passos válidos × `stepLengthMeters`.
Resultado: distância reproduzível, ausente quando inelegível e independente da
distância do relógio.

## Contexto

Parser grava distância original e consumidores a priorizam. Após `014-04..06`,
passos decodificados, duração e perfil elegível estarão disponíveis.

## Requisitos cobertos

- R1, R2, R5, R6 e R9.

## Dependências

- `014-05` e `014-06` concluídas.

## Arquivos permitidos

- contratos/serviço de derivação em `core/model`;
- `RepositoryContracts.kt`;
- mappers e `RoomRepositories.kt` para persistência transacional do snapshot;
- consumers mínimos necessários para expor distância canônica sem UI final;
- overview.

Símbolos prováveis: `WorkoutDerivationSnapshot`, `WorkoutDerivationPolicy`,
`deriveCanonicalDistance`, repository de derivação e entidades de `014-03`.

## Arquivos proibidos

- parser/protocolo já fechado;
- UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- infraestrutura phone/rota.

## Escopo

- snapshot imutável;
- total por passos totais válidos;
- intervalo por passos decodificados;
- consistência soma temporal versus total;
- ausência tipada.

## Fora do escopo

- cadência, velocidade, ritmo e splits;
- reprocessamento em lote de legado;
- calorias derivadas;
- testes automatizados.

## Passos de implementação

1. Resolver perfil elegível no instante da derivação.
2. Congelar valor, origem, método, versão e `derivedAt`.
3. Calcular total canônico sem consultar distância original.
4. Calcular deltas temporais com mesma passada.
5. Marcar série inelegível quando duração, índices ou contabilização falharem.
6. Persistir snapshot e série atomicamente.
7. Não substituir snapshot existente sem ação explícita.
8. Atualizar overview.

## Critérios de aceite

- Fórmula é exatamente passos × `stepLengthMeters`.
- Perfil ausente/inválido mantém distância ausente.
- Distância original não participa do cálculo/fallback.
- Mudança de perfil não altera snapshot antigo.
- Divergência temporal não recebe residual; total pode permanecer disponível.
- Zero passos produz zero local e não cria distância positiva.

## Validação manual

- Calcular atividade de referência com `0,8818 m` somente como valor pessoal
  informado e comparar resultado esperado.
- Editar perfil depois e confirmar snapshot imutável.
- Simular ausência de perfil e conferir UI/read model sem valor inventado.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- cálculo manual sanitizado;
- snapshot antes/depois de edição do perfil;
- builds e round-trip Room.

## Definição de pronto

Distância canônica e snapshot persistem atomicamente, ausência é segura e item
`014-07` está marcado.

## Riscos e cuidados

- Evitar overflow/NaN e arredondamento prematuro.
- Não promover calibração de esteira ou original para canônica.
- Snapshot usado por retry futuro deve permanecer retido.

## Rollback

Parar criação/consumo de snapshots e retornar ausência canônica; preservar dados
derivados para diagnóstico até migration forward.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-07` e registrar versão inicial da
derivação.

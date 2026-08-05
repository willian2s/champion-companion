# 014-12 — Sincronizar distância granular e velocidade com Health Connect

- **Ticker:** `014`
- **Número:** `12`
- **Status:** pending

## Objetivo e resultado esperado

Projetar série canônica congelada como múltiplos `DistanceRecord` e
`SpeedRecord`, preservando sessão, BPM real, calorias elegíveis, falha parcial e
retry idempotente. O writer prepara operações, mas bloqueia qualquer insert de
filho enquanto houver agregado legado próprio sem reconciliação concluída em
`014-13`; assim agregado e filhos nunca coexistem.

## Contexto

Writer atual cria um `DistanceRecord` agregado e fila assume um complemento por
tipo. `SPEED` não existe. `operationSequence` já oferece base parcial para
cardinalidade múltipla.

## Requisitos cobertos

- R8 e consumidores canônicos de R6.

## Dependências

- `014-10` concluída.

## Arquivos permitidos

- `core/model/HeartRateAndSync.kt` e contratos de projeção;
- `core/healthconnect/HealthRecordMapper.kt`;
- `core/healthconnect/HealthConnectSyncCoordinator.kt` e gateway;
- repository/DAO/mappers de operações e projeções;
- permissões/rationale estritamente necessários para `SpeedRecord`;
- `docs/baseline/health-connect-mapping.md` e overview.

Símbolos prováveis: `HealthSyncRecordType.SPEED`, mapper de série,
`prepareActivityOperations`, ID de filho, manifesto congelado e payload de retry.

## Arquivos proibidos

- parser, perfil, UI não relacionada;
- Gradle/manifesto, salvo exigência real do SDK revisada e explicitamente
  registrada antes da edição;
- migrations/schemas após `014-03`;
- `ExerciseRoute`, GPS e código phone.

## Escopo

- `ExerciseSessionRecord` único;
- distância positiva por intervalo;
- velocidade com amostras derivadas;
- BPM real e calorias totais elegíveis;
- IDs/versões determinísticos e payload congelado;
- retry foreground por ação explícita.

## Fora do escopo

- reconciliação do agregado legado, tratada em `014-13`;
- leitura Health Connect;
- WorkManager/background;
- Steps por atividade, rota ou distância original;
- testes automatizados.

## Passos de implementação

1. Habilitar `SPEED` somente para atividade com projeção elegível.
2. Congelar manifesto antes de enfileirar.
3. Criar filho `DISTANCE` por índice estável e delta positivo.
4. Criar `SpeedRecord` com janelas/amostras da série derivada.
5. Manter sessão, BPM real e calorias independentes.
6. Gerar IDs sem dados de saúde e versões monotônicas acima do legado.
7. Fazer retry ler payload congelado e retomar somente filhos pendentes/falhos.
8. Detectar agregado legado conhecido e manter projeção em estado pendente, sem
   inserir ou apagar remoto; `014-13` executará a reconciliação antes do envio.
9. Remover filhos próprios obsoletos por ID exato quando projeção mudar.
10. Atualizar mapeamento Health Connect e overview.

## Critérios de aceite

- Nenhum `DistanceRecord` usa original/calibrada para `WATCH_D3_D4`.
- Intervalo zero não cria `DistanceRecord` positivo.
- Distância produz N filhos estáveis e velocidade usa série derivada.
- Agregado legado bloqueia insert granular até reconciliação exata.
- Retry idêntico mantém ID, versão e payload.
- Falha de um filho não desfaz sessão/outros records.
- Sync continua explícito, foreground e write-only.

## Validação manual

- Sincronizar atividade elegível em provider real.
- Repetir ação/retry e confirmar ausência de duplicatas.
- Revogar permissão de distância/velocidade e conferir falha granular.
- Confirmar sessão, BPM e calorias independentes.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- manifesto sanitizado com tipos/índices/estados, sem valores de saúde;
- screenshots do provider e estados locais;
- retry/falha parcial e builds.

## Definição de pronto

Projeção granular nova funciona sem agregado novo, com retry idempotente e item
`014-12` marcado.

## Riscos e cuidados

- Limites do provider podem exigir lotes finitos; não truncar nem agregar.
- `SpeedRecord` pode ter contrato de amostras distinto de `DistanceRecord`;
  validar SDK real.
- Não usar delete temporal amplo.

## Rollback

Interromper novas projeções granulares e manter operações/manifests para retomada.
Não recriar agregado automaticamente nem apagar records já enviados.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-12` e registrar IDs/versões e gate
manual executado.

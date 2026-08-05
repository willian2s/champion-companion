# 014-14 — Reconciliar agregado e filhos legados no Health Connect

- **Ticker:** `014`
- **Número:** `14`
- **Status:** pending

## Objetivo e resultado esperado

Remover agregado legado próprio conhecido, liberar os filhos granulares
preparados em `014-13`, retomar falhas e remover filhos próprios obsoletos sem
leitura ampla ou delete temporal.

Resultado: uma única projeção de distância por atividade, sem dupla contagem e
com checkpoints retomáveis.

## Dependências

- `014-13` concluída.

## Arquivos permitidos

- coordinator/gateway Health Connect para delete por ID exato;
- repository/DAO/mappers de projeção e operações;
- contratos de ação, checkpoint e tombstone;
- `docs/baseline/health-connect-mapping.md` e overview.

## Arquivos proibidos

- parser, derivação, perfil e UI não necessária;
- Gradle, manifesto, migrations e schemas;
- leitura de records de terceiros, delete por janela, rota e Fase 013.

## Escopo

- identidade agregada legada conhecida;
- delete-before-insert com checkpoint;
- falhas entre etapas;
- liberação dos filhos preparados;
- remoção de filhos próprios obsoletos;
- atividade sem série substituta.

## Regras

1. Resolver identidade legada somente por dados locais determinísticos.
2. Persistir intenção/checkpoint antes do efeito remoto.
3. Remover agregado conhecido por `clientRecordId` exato.
4. Persistir sucesso da remoção antes de liberar filhos de distância/velocidade.
5. Retomar inserts com os IDs, versões e payloads já congelados.
6. Comparar manifesto anterior/novo e remover filhos próprios obsoletos.
7. Sem ID conhecido, falhar fechado.
8. Não remover agregado sem série substituta elegível.
9. Não apagar por tempo, tipo amplo ou origem presumida.

## Fora do escopo

- descobrir IDs por leitura remota;
- apagar records de outros apps;
- substituir agregado quando a série for inelegível ou implausível bloqueada;
- background/WorkManager;
- testes automatizados.

## Critérios de aceite

- Agregado e filhos não coexistem após conclusão.
- Falha após delete retoma inserts sem novo ID, versão ou payload.
- Falha de marcação local após sucesso remoto permanece idempotente.
- ID desconhecido não dispara delete nem insert concorrente.
- Atividade sem série não perde agregado automaticamente.
- Filhos obsoletos próprios são removidos somente por identidade exata.
- Debug e release não cruzam identidades por `applicationId`.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- estados/checkpoints sanitizados;
- provider antes/depois;
- matriz de falhas e retries;
- verificação de ausência de dupla distância;
- builds.

## Definição de pronto

Reconciliação exata e retomável validada, sem dupla contagem, com item `014-14`
marcado.

## Rollback

Parar o reconciliador e preservar checkpoints. Não recriar agregado nem apagar
filhos automaticamente; retomada exige fluxo explícito.

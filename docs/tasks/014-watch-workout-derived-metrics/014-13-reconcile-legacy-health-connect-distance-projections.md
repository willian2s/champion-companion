# 014-13 — Reconciliar projeções legadas de distância no Health Connect

- **Ticker:** `014`
- **Número:** `13`
- **Status:** pending

## Objetivo e resultado esperado

Remover agregado legado próprio conhecido, liberar a série granular preparada em
`014-12`, retomar falhas e remover filhos obsoletos sem leitura remota ou delete
amplo.
Resultado: uma única projeção de distância por atividade, sem dupla contagem.

## Contexto

Versão atual pode ter enviado ID agregado
`<applicationId>:WORKOUT_ACTIVITY:<id>:DISTANCE`. Nova série usa IDs filhos
distintos; coexistência é proibida.

## Requisitos cobertos

- reconciliação, remoção, versões e retry de R8.

## Dependências

- `014-12` concluída.

## Arquivos permitidos

- coordinator/gateway Health Connect para delete exato;
- repository/DAO/mappers de projeção e operações;
- contratos de ação/checkpoint/tombstone;
- `docs/baseline/health-connect-mapping.md` e overview.

Símbolos prováveis: identidade agregada legada, operação `DELETE`, checkpoint de
reconciliação, conjunto esperado/enviado/obsoleto e retry coordinator.

## Arquivos proibidos

- parser, derivação, perfil e UI não necessária;
- Gradle, manifesto, migrations e schemas;
- leitura de records de terceiros, delete por janela, rota/Fase 013.

## Escopo

- identidade legada conhecida;
- delete-before-insert com checkpoint;
- falhas entre etapas;
- filhos obsoletos próprios;
- atividade sem série substituta.

## Fora do escopo

- descobrir IDs por leitura remota;
- apagar records de outros apps;
- substituir agregado quando série é inelegível;
- background/WorkManager;
- testes automatizados.

## Passos de implementação

1. Resolver identidade legada somente por dados locais determinísticos.
2. Persistir intenção/checkpoint antes do efeito remoto.
3. Remover agregado conhecido por ID exato.
4. Persistir sucesso da remoção antes de enviar filhos.
5. Retomar inserção granular pelos IDs congelados somente após checkpoint de
   remoção confirmado.
6. Comparar manifesto anterior/novo e remover filhos próprios obsoletos.
7. Falhar fechado quando ID legado for desconhecido.
8. Não remover agregado sem substituição elegível, salvo ação específica futura.
9. Atualizar documentação e overview.

## Critérios de aceite

- Agregado e filhos não coexistem após conclusão.
- Falha após delete retoma inserção sem novo ID/versão.
- Falha de marcação local após sucesso remoto permanece idempotente.
- ID desconhecido não dispara delete por tempo nem inserção concorrente.
- Atividade sem série não perde agregado automaticamente.

## Validação manual

- Exercitar legado conhecido em provider real.
- Interromper fluxo após delete e retomar.
- Alterar projeção explicitamente e conferir remoção de filho obsoleto.
- Confirmar ausência de impacto em records externos.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- estados/checkpoints sanitizados;
- consulta visual no provider antes/depois;
- matriz de falhas e retries;
- builds.

## Definição de pronto

Reconciliação exata e retomável validada, sem dupla contagem, com item `014-13`
marcado.

## Riscos e cuidados

- Delete-first cria janela temporária sem distância.
- IDs por variante usam `applicationId`; não cruzar debug/release.
- Não declarar ausência de record externo sem evidência do provider.

## Rollback

Parar reconciliador e preservar checkpoints. Não recriar agregado nem apagar
filhos automaticamente; retomada exige fluxo explícito.

## Atualização obrigatória do overview

Atualizar `014-00-overview.md`, marcar `014-13` e registrar cenários de falha
validados.

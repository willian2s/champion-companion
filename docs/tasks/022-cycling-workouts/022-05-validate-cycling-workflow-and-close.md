# 022-05 — Validar fluxo de bicicleta e fechar a spec

- **Status:** `pending`
- **Depende de:** `022-04`
- **Desbloqueia:** fechamento da Spec 022

## Objetivo

Validar a implementação completa sobre banco existente e dispositivo real,
confirmando os cenários com e sem distância manual sem introduzir GPS, wipe ou
projeções sintéticas.

## Pré-condições

- `022-01..04` concluídas;
- banco local existente preservado;
- app atualizado sobre instalação atual;
- nenhuma limpeza de dados;
- nenhuma corrupção artificial de banco;
- Health Connect disponível para os cenários de escrita exercitados.

## Gate A — Upgrade do banco

Instalar a nova versão sobre o app existente e confirmar:

- migration `v11 → v12` executada;
- `PRAGMA integrity_check = ok`;
- `PRAGMA user_version = 12`;
- atividades, imports, intervals, samples, snapshots e operações existentes
  preservados;
- nenhum reprocessamento automático;
- nenhuma operação Health Connect criada apenas pelo upgrade.

## Gate B — Bicicleta sem distância

Usar atividade D3/D4 elegível e revisar como `Bicicleta` sem informar distância.

Confirmar:

- tipo persistido `CYCLING`;
- workflow pode chegar a `READY_TO_SYNC` sem `PENDING_CALIBRATION`;
- duração/BPM/calorias continuam disponíveis quando existirem;
- distância fica ausente;
- velocidade média fica ausente;
- pace/cadência de passos/splits de passos não são exibidos como métricas de
  bicicleta;
- passos/raw continuam preservados no banco;
- sync não cria operação `DISTANCE` nem `SPEED` para a atividade;
- sessão usa biking genérico no Health Connect.

## Gate C — Bicicleta com distância

Revisar uma atividade elegível como `Bicicleta` e informar distância conhecida.

Confirmar:

- valor persistido em metros;
- UI exibe distância em km;
- velocidade média local corresponde a distância / duração oficial;
- não existe pace;
- sync cria exatamente um filho `ManualDistance`;
- não cria `DistanceInterval`;
- não cria `SpeedSeries`;
- `DistanceRecord` cobre a sessão completa;
- metadata/provenance da distância é manual/local;
- BPM/calorias mantêm provenance do relógio.

## Gate D — Regressão dos tipos existentes

Conferir pelo menos uma atividade existente de running/walking/treadmill que já
esteja no banco, sem alterar dados remotos.

Confirmar:

- labels continuam corretos;
- política atual de distância permanece;
- treadmill mantém calibração própria;
- snapshots existentes continuam legíveis;
- calendário não perde atividades nem cria issue artificial.

## Gate E — Efeitos colaterais

Confirmar:

- abrir calendário/detalhe não cria nem altera operação Health Connect;
- não há acesso GPS/localização/rota;
- nenhum treino phone foi reativado;
- nenhuma atividade `SYNCED` foi alterada silenciosamente;
- nenhuma distância manual foi aplicada a tipo diferente de `CYCLING`.

## Comandos

Registrar resultados de:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Testes automatizados não são executados como etapa padrão.

## Evidência

Criar relatório sanitizado em:

```text
docs/integration/022-05-cycling-validation-and-close.md
```

Não publicar IDs de atividade, BPM individuais, payload BLE, distância pessoal
real ou outros dados de saúde desnecessários.

## Fechamento documental

Após gates aplicáveis:

- atualizar status de `022-01..05`;
- atualizar `022-00-overview.md`;
- atualizar `docs/specs/022-cycling-workouts.md`;
- registrar ressalvas/N/A factuais;
- solicitar revisão independente final.

## Critérios de aceite

- [ ] upgrade v11→v12 passou sem wipe;
- [ ] cycling sem distância funciona e sincroniza sem DISTANCE/SPEED;
- [ ] cycling com distância usa um único `ManualDistance`;
- [ ] velocidade média local fecha com distância/duração;
- [ ] nenhuma métrica baseada em passos é promovida como ciclismo;
- [ ] provenance Health Connect está correta;
- [ ] tipos existentes não regrediram nos cenários observados;
- [ ] GPS/rota/phone workout continuam fora do fluxo;
- [ ] build completo e instalação passam;
- [ ] evidência sanitizada foi registrada;
- [ ] revisão independente final aprova o fechamento ou registra ressalvas.

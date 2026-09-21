# 034-06 — Validação física final e fechamento

- **Data:** 2026-09-21
- **Dispositivo:** Champion C001 real, validado pelo app no `SM-S901E`
- **Branch:** `feat/034-heart-rate-zones-training-load`
- **Privacidade:** evidência sanitizada; sem payload BLE, MAC, IDs pessoais ou BPM individual
- **Status:** `completed` — todos os critérios numéricos foram exercitados;
  cenário sem samples e snapshot pré-upgrade direto ficam registrados como
  limitações residuais

## Validação técnica

```text
git diff --check                         -> passou
./gradlew lint assembleDebug assembleRelease -> BUILD SUCCESSFUL
./gradlew installDebug                   -> instalado em 1 dispositivo
```

Nenhum teste automatizado novo foi criado ou executado. A instalação debug foi
feita sobre dados existentes, sem limpar armazenamento.

## Banco físico e preservação

O banco existente abriu após instalação da versão com migration e mostrou:

```text
Room user_version:             20
user_profiles:                 1
workout_activities:            8
workout_heart_rate_samples:    165
workout_derivation_snapshots:  11
health_sync_operations:        779
workflow:                      2 PENDING_REVIEW, 6 SYNCED
```

`user_profiles.maxHeartRateBpm` existia como coluna `INTEGER` nullable e perfis
antigos foram lidos sem wipe. O dump direto de `user_version=19` antes da
instalação não foi capturado nesta rodada; a versão anterior do app e a cadeia
documental eram Room v19. Portanto, preservação pós-upgrade foi observada, mas a
leitura física pré-upgrade permanece limitação de evidência.

O dump foi obtido após `adb shell am force-stop`, copiando temporariamente
`champion-companion.db`, `-wal` e `-shm` via `adb exec-out run-as`. Consultas
imprimiram somente versão, contagens, estados agregados e assinaturas; arquivos
temporários foram descartados.

## FC máxima e read model

O Perfil aceitou valor inteiro válido, exibiu supporting text sobre zonas e carga,
e persistiu a alteração após reinício:

```text
alterações físicas: 191 -> 190 -> 180 BPM
após reinício final: 180 BPM
```

A limpeza foi exercitada. Com samples existentes e FC máxima ausente, o detalhe
mostrou `Configuração necessária` e preservou somente cobertura `98,4%`, sem
zonas/carga zero sintéticas. O valor `180` foi restaurado ao final.

Workout existente com 32 samples na janela canônica permaneceu no mesmo detalhe
e workflow. A alteração `180 -> 179` e restauração para `180` não alteraram a
fila: assinatura SHA-256 de `health_sync_operations` permaneceu
`68c09ab295624a28382646fad0291c31d56f78acfa2c3d90a94890eafb86f998`, com 779
linhas, antes e depois.

## Cálculo independente

Atividade existente com duração de `1873,0 s` e 32 samples válidos foi calculada
fora do app usando a janela `[startedAt, startedAt + duration)` e sample-and-hold:

```text
FC máxima configurada:          180
FC máxima observada:            183
samples na janela:               32
tempo coberto:                  1843,735 s
razão de cobertura:                0,984375
soma dos buckets:               1843,735 s
Edwards TRIMP independente:      149,743 pontos
amostra acima da máxima:              sim
```

Buckets independentes:

```text
BELOW_ZONE_1:     0,000 s /  0,00%
Z1:               0,000 s /  0,00%
Z2:               0,000 s /  0,00%
Z3:              58,531 s /  3,17%
Z4:             117,063 s /  6,35%
Z5:            1668,141 s / 90,48%
```

A UI mostrou `Z5 27:48 90%`, `Z4 01:57 6%`, `Z3 00:58 3%`, carga `149,7`
pontos e cobertura `98,4%`, compatíveis após arredondamento de apresentação.
O aviso de sample acima da FC máxima apareceu e manteve a amostra em `Z5`.

Para exercitar fronteiras disponíveis sem alterar samples, a FC máxima foi
temporariamente configurada para `200`. A fixture contém uma janela exatamente
em `0,80` e outra em `0,90`; a UI exibiu a mudança de buckets e a conta externa
confirmou:

```text
configuração:                   200
hits exatos:                    razão 0,80 = 1; razão 0,90 = 1
Z2:                              58,531 s /  3,17%
Z3:                              58,531 s /  3,17%
Z4:                             497,517 s / 26,98%
Z5:                            1229,156 s / 66,67%
carga independente:             140,475 pontos
UI:                              Z5 20:29, Z4 08:17, carga 140,5
```

Cobertura e soma permaneceram `1843,735 s` e `0,984375`. A configuração final
foi restaurada para `180` e persistiu após novo reinício.

O segundo workout existente, com duração de `2594,0 s`, cobriu fronteiras
adicionais sem criação de fixture:

```text
max=210: hits exatos 0,50 = 3; 0,60 = 1; abaixo de Z1 presente
         cobertura 0,988636; buckets abaixo/Z1/Z2/Z3/Z4/Z5 =
         147,387/294,773/176,864/117,909/1827,590/0,000 s
         Edwards TRIMP 138,543; UI carga 138,5 e cobertura 98,9%
max=180: hits exatos 0,70 = 1; 0,90 = 1
         cobertura 0,988636; buckets abaixo/Z1/Z2/Z3/Z4/Z5 =
         0,000/383,205/58,955/117,909/58,955/1945,499 s
         Edwards TRIMP 180,303; UI carga 180,3 e cobertura 98,9%
```

Com `max=210`, as razões `0,50`, `0,60` e o bucket abaixo de Z1 foram
observados; com `max=180`, `0,70` e `0,90`; com `max=200`, `0,80` e `0,90`.
Com `max=180` no primeiro workout, sample acima da máxima também foi observado.

## Screenshot

Screenshot sanitizada do detalhe atualizado:

- [`034-06-heart-rate-analysis.png`](034-06-heart-rate-analysis.png)

Imagem contém somente seção de zonas, carga, cobertura e texto de cobertura;
gráfico temporal e valores individuais foram excluídos do recorte.

## Cenários N/A e limitações residuais

1. Todas as 8 activities existentes tinham samples; cenário físico sem samples
   ficou `N/A`. Não foi criado treino novo nem fixture artificial. O estado é
   coberto pelo supporting text e pelo contrato de ausência, mas não recebeu
   exercício físico com activity sem samples.
2. O `user_version=19` pré-instalação não foi lido diretamente antes do upgrade.
   A instalação ocorreu sobre app/banco existentes conhecidos como Room v19,
   abriu em v20 sem wipe observável e preservou os agregados; permanece somente
   limitação de captura do snapshot pré-upgrade.

Nenhum dado foi alterado para fabricar cenário. Todas as fronteiras numéricas,
cobertura, carga e acima da máxima receberam cálculo independente e comparação
visual; portanto, limitações N/A não bloqueiam fechamento numérico.

## Conclusão

Migration pós-instalação, persistência, todas as fronteiras numéricas, cobertura,
flag acima da máxima, carga independente, preservação de workflow e ausência de
alteração da fila Health Connect foram observados. Fechamento documental final
concluído com N/A explícito para activity sem samples e risco residual de não
ter capturado snapshot físico direto antes da migration.

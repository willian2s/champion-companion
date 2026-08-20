# 026-07 — Validar fisicamente e executar casos negativos

- **Ticker:** `026`
- **Número:** `07`
- **Status:** pending

## Objetivo

Executar o gate físico da PoC em aparelho Android real, incluindo percurso externo, interação com mapa e cenários negativos mínimos, com evidência sanitizada.

## Resultado esperado

Existe evidência suficiente para classificar tecnicamente a PoC sem depender de emulador, dados simulados ou screenshots contendo localização pessoal.

## Requisitos e critérios cobertos

- Spec 026: critérios 18 a 35;
- percurso real;
- responsividade do mapa;
- qualidade visual da rota;
- isolamento entre ciclos;
- cenários negativos de rede e GPS;
- privacidade da evidência.

## Pré-condições

```text
aparelho Android real conectado por ADB
build debug instalado
rede disponível para cenário principal
localização habilitada
permissão precisa disponível
ambiente externo adequado para GPS
```

O cenário de rede indisponível deve ser exercitado separadamente.

## Cenário principal

1. Instalar build debug.
2. Abrir Activity da PoC por ADB.
3. Confirmar mapa OpenFreeMap carregado.
4. Confirmar atribuição visível.
5. Conceder localização precisa quando solicitada.
6. Aguardar GPS `READY` com qualidade elegível.
7. Iniciar tracking.
8. Percorrer trajeto externo de aproximadamente 500 m ou mais.
9. Observar posição progredindo.
10. Confirmar rota crescendo somente com pontos aceitos.
11. Fazer pan/zoom manual.
12. Confirmar que follow não briga com gesto.
13. Acionar `Recentralizar`.
14. Continuar tracking.
15. Parar.
16. Confirmar que novos fixes não alteram a rota final.
17. Confirmar fit bounds da rota.
18. Limpar.
19. Iniciar segundo ciclo sem reiniciar o app.
20. Confirmar ausência de estado do primeiro ciclo.

## Observações técnicas mínimas

Registrar de forma sanitizada:

- aparelho/modelo geral e versão Android;
- versão MapLibre Compose usada;
- style OpenFreeMap usado;
- resultado de carregamento do mapa-base;
- categoria de qualidade GPS observada;
- duração aproximada do teste;
- distância agregada aproximada;
- contagem de fixes/aceitos/rejeitados, se útil;
- percepção de fluidez/jank;
- comportamento de pan/zoom/follow;
- resultado do stop/reset;
- categorias de erro observadas.

Nunca registrar coordenadas, endereço, screenshot de rota pessoal ou GeoJSON.

## Casos negativos mínimos

### Rede indisponível

- abrir PoC sem conectividade;
- verificar que falha do mapa-base não vira erro de GPS;
- app não deve crashar;
- comportamento ao restaurar rede deve ser observado sem inventar requisito de retry automático se a biblioteca não o fornecer.

### Permissão negada

- negar localização;
- `Iniciar` permanece indisponível;
- retry explícito continua possível.

### Localização aproximada

- conceder apenas coarse quando o Android permitir;
- tracking não inicia;
- UI informa necessidade de localização precisa.

### GPS desligado

- desabilitar localização;
- estado próprio é apresentado;
- nenhum tracking inicia.

### GPS procurando

- abrir em condição sem fix elegível;
- UI permanece estável em searching/weak.

### Perda temporária de qualidade

- durante tracking, observar comportamento quando readiness deixa de ser ideal;
- não fabricar segmento ou ponto inválido;
- qualquer decisão efetiva deve ser registrada para futura spec.

### Stop precoce

- parar com zero ou um ponto aceito;
- não crashar;
- câmera permanece em estado válido.

### Dois ciclos

- executar start/stop/reset/start sem reiniciar app;
- não reutilizar rota, contadores ou geração anteriores.

## Evidência visual

Screenshot versionado deve usar estado neutro ou mapa sem localização pessoal identificável.

Não versionar:

- rota real;
- região residencial;
- coordenadas;
- barra de status com informação sensível;
- logs contendo localização.

## Validação de logs

Inspecionar Logcat procurando exposição indevida de:

- latitude/longitude;
- altitude;
- GeoJSON;
- route/bounds serializados;
- URLs contendo localização.

Falha de privacidade bloqueia o gate.

## Comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Comandos ADB podem ser usados para abrir Activity e smoke, preservando evidência sanitizada.

## Definition of Done

- cenário principal executado em aparelho real;
- percurso real >= aproximadamente 500 m concluído ou limitação física explicitamente registrada sem declarar sucesso;
- mapa, rota e câmera avaliados;
- casos negativos mínimos exercitados;
- dois ciclos consecutivos exercitados;
- Logcat não expõe localização;
- screenshot/evidência não expõe local pessoal;
- achados estão registrados para `026-08`;
- nenhuma classificação final é inventada antes da análise dos resultados.

## Riscos e cuidados

- indisponibilidade do OpenFreeMap durante teste deve ser distinguida de defeito do MapLibre ou GPS;
- trajeto pessoal é dado sensível; evidência textual deve permanecer agregada;
- qualidade GPS varia com ambiente; registrar contexto sem afrouxar filtro durante o gate apenas para obter aprovação.

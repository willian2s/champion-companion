# 013-16 — Executar validação física manual de GPS, relógio e Health Connect

- **Ticker:** `013`
- **Número:** `16`
- **Status:** pending

## Objetivo e resultado esperado

Executar gate manual completo no Galaxy S22 e registrar evidências reais sem fabricar cobertura. Resultado: cenários aprovados, falhas/riscos residuais explícitos e nenhuma coordenada no Logcat.

## Contexto

Única task dedicada de validação. Não é task de testes. Thresholds GPS, FGS OEM, protocolo watch e upsert route dependem de hardware/provider real.

## Documentos obrigatórios

Spec 013; todas tasks 013-01 a 013-15; ADR-014; protocolos; roteiro abaixo.

## Dependências

`013-15` concluída.

## Arquivos permitidos

Esta task, overview, documentação/protocolo apenas para evidência real e correções factuais; screenshots em caminho de evidência já usado pelo projeto.

## Arquivos e símbolos prováveis

Nenhum símbolo de produção. Evidências referenciam superfícies, serviço, fila Health Connect e categorias de logging entregues nas subtarefas anteriores.

## Arquivos proibidos

Código de produção, Gradle, manifesto, schema e testes. Defeito exige retornar à task dona, não corrigir silenciosamente aqui.

## Requisitos cobertos

Todos cenários manuais da solicitação; comandos permitidos; privacidade Logcat; screenshots; aceite de riscos.

## Escopo

Executar e registrar:

- countdown 0/3/5/10 e cancelamento;
- localização negada/aproximada/desativada;
- procurando/fraco/bom/forte e degradação no countdown;
- caminhada/corrida externas;
- pausa/retomada sem gap, perda/recuperação GPS;
- tela apagada/background/notificação/process death/recovery;
- descarte/exclusão de rota;
- livre e esteira sem GPS, calibração;
- sem relógio, conectado sem esporte, esporte com BPM, disconnect;
- watch-only, phone-only, <=60s, único, ambíguo, CRC repetido e sem duplicata;
- calendário consolidado;
- HC sem route permission, base sem rota, concessão, retry, única sessão, rota visível, retry sem apagar;
- Logcat sem coordenadas/rota/BPM/payload/IDs pessoais.

## Fora do escopo

Testes automatizados, benchmark formal, outros aparelhos, mapas/export.

## Passos de implementação

1. Registrar aparelho, Android, app variant/version e provider sem identificadores pessoais.
2. Executar builds e instalar.
3. Preparar checklist narrativo de todos cenários, sem usar checklist adicional no overview.
4. Executar cenários GPS em área segura e não publicar coordenadas.
5. Executar relógio/protocolo e anotar somente fatos sanitizados.
6. Executar correlação/importação/reimportação.
7. Executar Health Connect e confirmar identidade única/versionamento por comportamento.
8. Revisar Logcat por categorias proibidas sem copiar payload sensível para docs.
9. Capturar screenshots exigidas.
10. Classificar aprovado/falho/não reproduzido e encaminhar defeitos à task dona.
11. Atualizar overview somente após aceite explícito.

## Critérios de aceite

- todos cenários têm resultado honesto;
- cenários bloqueadores passam ou retornam à implementação;
- uma única sessão HC e route retry são comprovados;
- descarte remove rota;
- FGS/recovery funcionam;
- Logcat não expõe proibidos;
- riscos não reproduzidos são explicitamente aceitos, nunca marcados como aprovados.

## Validação manual

Esta seção é a própria execução do roteiro mínimo integral da spec no Galaxy S22.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Saídas de build/install, matriz completa cenário/resultado/evidência, screenshots, versão do provider, categorias Logcat e aceite do usuário.

## Screenshots

Obrigatórias para todas mudanças visuais representativas e estados exigidos. Redigir/recortar coordenadas e valores pessoais antes de publicação.

## Definição de pronto

Gate manual aceito, defeitos bloqueadores resolvidos nas tasks donas e overview atualizado.

## Riscos e cuidados

Condições GNSS/HC podem não ser reproduzíveis. Não fabricar. Rotas revelam rotina; evidência pública nunca inclui mapa/coordenadas.

## Rollback

Se risco crítico surgir, desabilitar entrada phone/route sync por mudança na task dona e repetir cenários afetados. Sem downgrade v6.

Não criar, alterar ou executar testes automatizados.

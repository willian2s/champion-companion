Quero executar a Fase 0 do projeto Champion Companion.

## Contexto do workspace

Existem dois repositórios abertos no mesmo workspace:

- `motionlog-app`: aplicativo React Native/Expo atual e funcional;
- `champion-companion`: novo projeto do aplicativo Android nativo.

A baseline funcional do MotionLog é a branch:

```text
feat/015-watch-workouts-d3-d4
```

O Champion Companion será um aplicativo Android nativo, em Kotlin e Jetpack Compose, dedicado exclusivamente ao relógio Champion C001.

## Documentos autoritativos existentes

Os seguintes documentos já existem no repositório `champion-companion`:

```text
docs/roadmap/champion-companion-roadmap.md
docs/decisions/adr-001-native-rewrite.md
```

Esses documentos representam decisões já aprovadas.

### Regras para o roadmap

O arquivo:

```text
docs/roadmap/champion-companion-roadmap.md
```

é o roadmap oficial do projeto.

Use-o para compreender:

- objetivo do produto;
- escopo;
- fases;
- prioridades;
- restrições;
- critérios de aceite;
- Definition of Done.

Não recrie, resuma, reorganize ou altere o roadmap nesta tarefa.

### Regras para a ADR

O arquivo:

```text
docs/decisions/adr-001-native-rewrite.md
```

contém a decisão arquitetural aprovada de:

- criar um novo aplicativo;
- utilizar Android nativo;
- utilizar Kotlin e Jetpack Compose;
- manter o projeto em um repositório separado;
- utilizar o MotionLog somente como baseline funcional;
- não migrar dados locais do MotionLog;
- suportar exclusivamente o Champion C001.

Não altere:

- a decisão;
- o status;
- o escopo;
- as alternativas escolhidas;
- as consequências registradas.

Não transforme a ADR em roadmap, especificação funcional ou documentação de protocolo.

Nesta tarefa, apenas leia o roadmap e a ADR para garantir que os novos artefatos estejam alinhados com as decisões existentes.

## Verificação inicial

Antes de analisar o código:

1. Confirme que o diretório `motionlog-app` está na branch:

```text
feat/015-watch-workouts-d3-d4
```

2. Registre o commit atual com:

```bash
git -C motionlog-app rev-parse HEAD
```

3. Verifique o estado dos dois repositórios:

```bash
git -C motionlog-app status --short
git -C champion-companion status --short
```

4. Caso o `motionlog-app` não esteja na branch esperada, interrompa a análise e informe o problema.

5. Não troque automaticamente de branch e não descarte alterações existentes.

## Regra de segurança

O repositório `motionlog-app` deve ser tratado como estritamente somente leitura.

Não:

- altere arquivos;
- formate arquivos;
- mova arquivos;
- exclua arquivos;
- gere arquivos dentro dele;
- instale dependências;
- execute migrations;
- faça commits;
- crie tags;
- aplique correções;
- adicione logs temporários.

Todos os arquivos produzidos nesta tarefa devem ser criados exclusivamente dentro do repositório:

```text
champion-companion
```

Não implemente ainda:

- código Kotlin;
- projeto Android;
- interface;
- banco Room;
- BLE nativo;
- Health Connect nativo;
- testes Kotlin.

Esta tarefa é somente de análise, documentação e preparação do handoff.

## Objetivo

Analisar o estado funcional do MotionLog e criar, no Champion Companion, os artefatos necessários para orientar sua reimplementação em Kotlin.

A Fase 0 deve transferir conhecimento técnico e funcional, não copiar mecanicamente o código existente.

O resultado deve permitir que as fases posteriores saibam:

- o que precisa ser reproduzido;
- onde o comportamento está implementado atualmente;
- quais contratos os parsers devem atender;
- como o protocolo BLE funciona;
- como os dados são armazenados;
- como os dados são enviados ao Health Connect;
- quais pontos ainda precisam ser confirmados no dispositivo físico.

## Limitação das amostras reais

O MotionLog funciona somente com o Champion C001 físico.

Não tente capturar antecipadamente todas as variações de pacotes BLE.

As fixtures reais serão adicionadas progressivamente durante as fases correspondentes:

- handshake e BPM durante a implementação BLE;
- D1 durante a implementação do histórico de frequência cardíaca;
- D3 e D4 durante a implementação dos treinos do relógio.

Nesta fase:

- documente o que já pode ser comprovado pelo código;
- identifique onde os payloads são montados e interpretados;
- prepare a estrutura para futuras fixtures;
- registre claramente as lacunas que exigem validação física.

Não bloqueie a conclusão da Fase 0 pela ausência de amostras reais completas.

## Escopo da análise

Analise no `motionlog-app`:

### Estrutura geral

1. Estrutura de diretórios.
2. Arquitetura atual.
3. Telas.
4. Navegação.
5. Hooks, services, repositories e stores.
6. Modelos de domínio.
7. Persistência local.
8. Serviços em segundo plano.

### Bluetooth Low Energy

9. Permissões BLE.
10. Scan.
11. Filtros de dispositivo.
12. Conexão.
13. Descoberta de services e characteristics.
14. Notifications e indications.
15. Fila ou sequenciamento de operações.
16. Timeouts.
17. Retries.
18. Desconexão.
19. Reconexão.
20. UUIDs e characteristics utilizados.

### Protocolo Champion C001

21. Handshake A1.
22. D0.
23. D1.
24. D3.
25. D4.
26. D8.
27. A8.
28. F002.
29. FEA1.

Para cada comando, identifique:

- finalidade;
- characteristic;
- payload enviado;
- resposta esperada;
- condição de conclusão;
- timeout;
- retry;
- offsets;
- endianess;
- escalas;
- valores inválidos;
- arquivos responsáveis.

### Passos e saúde

30. Leitura de passos.
31. Consolidação diária.
32. Estimativa de distância.
33. Estimativa de calorias.
34. Medição manual de BPM.
35. Stream de BPM.
36. Valor final da medição.
37. Histórico automático D1.
38. Validação dos registros de BPM.
39. Deduplicação dos registros.

### Treinos do relógio

40. Consulta D3.
41. Interpretação de blocos e CRCs.
42. Solicitação D4.
43. Montagem dos blocos.
44. Parsing dos treinos.
45. Identificação de:

- data;
- horário;
- duração;
- passos;
- distância;
- distância por intervalo;
- calorias;
- BPM;
- BPM por intervalo;
- BPM médio;
- BPM mínimo;
- BPM máximo;
- tipo de atividade.

46. Tratamento de dados incompletos.
47. Deduplicação dos treinos.
48. Armazenamento de payloads brutos.
49. Uso atual de D8 e A8.

### Atividades e revisão

50. Estados das atividades.
51. Atividades pendentes.
52. Sugestão automática de tipo.
53. Edição do tipo.
54. Edição de distância.
55. Revisão.
56. Descarte.
57. Sincronização manual.

### Esteira

58. Criação da atividade de esteira.
59. Estimativa de distância.
60. Calibração.
61. Distância informada pelo usuário.
62. Fator de calibração.
63. Recalculo de:

- distância;
- ritmo;
- velocidade;
- métricas derivadas.

64. Valor enviado ao Health Connect.

### Exercícios iniciados pelo aplicativo

65. Caminhada.
66. Corrida.
67. Treino livre.
68. Esteira.
69. Sessão ativa.
70. Pausa.
71. Retomada.
72. Recuperação após encerramento.
73. GPS.
74. BPM durante exercício.
75. Persistência da sessão.
76. Finalização.

### Health Connect

77. Permissões.
78. Tipos de registros enviados.
79. Campos preenchidos.
80. Unidades.
81. Timestamps.
82. IDs estáveis.
83. Regras de insert e update.
84. Deduplicação.
85. Tratamento de erros.
86. Sincronização de passos.
87. Sincronização de BPM.
88. Sincronização de exercícios.
89. Sincronização de peso e altura.
90. Ausência de rotas.

Deixe explícito que o comportamento esperado no Champion Companion será:

- todos os registros válidos de BPM obtidos por D1 devem ser enviados ao Health Connect;
- não haverá seleção manual registro por registro;
- atividades D3/D4 devem ser revisadas antes do envio;
- atividades de esteira devem usar a distância calibrada;
- nenhuma rota deve ser armazenada ou enviada.

### Persistência

91. Banco SQLite atual.
92. Tabelas.
93. Colunas.
94. Relacionamentos.
95. Índices.
96. Estados.
97. Regras de deduplicação.
98. Dados temporários.
99. Dados que não devem existir no novo modelo.

O banco atual será somente uma referência conceitual.

Não crie:

- plano de migração;
- compatibilidade de schema;
- importador do banco antigo;
- transferência de dados entre aplicativos.

### Backup

100. Backup e restauração atuais, somente como referência funcional.

O backup futuro do Champion Companion será independente e não precisará ser compatível com o MotionLog.

## Artefatos a criar

Crie dentro do `champion-companion`:

```text
docs/
  baseline/
    current-baseline.md
    feature-inventory.md
    feature-parity-checklist.md
    source-map.md
    database-reference.md
    health-connect-mapping.md
    gaps-and-open-questions.md

  protocol/
    champion-c001.md
    ble-transport.md
    handshake-a1.md
    heart-rate-d0-f002.md
    history-d1.md
    workouts-d3-d4.md
    diagnostics-d8-a8.md

    sample-packets/
      README.md
```

Não crie ou substitua:

```text
docs/roadmap/champion-companion-roadmap.md
docs/decisions/adr-001-native-rewrite.md
```

Caso algum arquivo de destino já exista, preserve informações válidas e complemente o conteúdo sem apagar decisões anteriores.

## Conteúdo esperado

### `docs/baseline/current-baseline.md`

Inclua:

- repositório de origem;
- branch;
- commit;
- data da análise;
- comandos utilizados;
- estado do working tree;
- regra de somente leitura;
- funcionalidades confirmadas;
- limitações conhecidas;
- explicação sobre fixtures progressivas;
- referência ao roadmap;
- referência à ADR-001.

### `docs/baseline/feature-inventory.md`

Para cada funcionalidade, registre:

- nome;
- descrição;
- comportamento atual;
- telas relacionadas;
- arquivos principais;
- dependências;
- persistência envolvida;
- integração envolvida;
- estado funcional;
- observações para a implementação nativa.

Classifique o estado como:

```text
Funcional
Parcial
Experimental
Diagnóstico
Não confirmado
```

### `docs/baseline/feature-parity-checklist.md`

Crie uma checklist para validar a futura paridade.

Inclua pelo menos:

#### Dispositivo

- scan;
- seleção do Champion C001;
- conexão;
- descoberta;
- notifications;
- handshake;
- desconexão;
- reconexão.

#### Dados diários

- passos;
- distância estimada;
- calorias estimadas;
- resumo diário.

#### Frequência cardíaca

- BPM manual;
- F002;
- valor final;
- D1;
- validação;
- deduplicação;
- envio de todos os registros válidos ao Health Connect;
- retry de falhas parciais.

#### Treinos

- D3;
- blocos;
- CRCs;
- D4;
- remontagem;
- parser;
- distância;
- intervalos;
- calorias;
- BPM;
- deduplicação;
- atividade pendente.

#### Revisão

- sugestão de tipo;
- edição;
- descarte;
- calibração de esteira;
- recalculo;
- envio manual.

#### Exercícios

- caminhada;
- corrida;
- treino livre;
- esteira;
- pausa;
- retomada;
- recuperação;
- segundo plano.

#### Health Connect

- passos;
- BPM;
- exercícios;
- peso;
- altura;
- IDs estáveis;
- prevenção de duplicação;
- ausência de rota.

#### Dados

- banco próprio;
- backup próprio;
- restauração;
- política de limpeza;
- sem migração do MotionLog.

### `docs/baseline/source-map.md`

Crie um mapa entre o código atual e os destinos conceituais no Champion Companion.

Use uma tabela ou estrutura com:

```text
Funcionalidade
Origem no motionlog-app
Responsabilidade atual
Destino conceitual no champion-companion
Documentos relacionados
Status da análise
Dúvidas
```

Destinos conceituais possíveis:

```text
core/bluetooth
core/database
core/healthconnect
core/location
core/notifications
feature/home
feature/heart
feature/exercises
feature/activitydetail
feature/device
feature/profile
feature/diagnostics
```

Não invente nomes definitivos de classes Kotlin nesta fase.

### `docs/baseline/database-reference.md`

Documente:

- tabelas;
- colunas;
- tipos;
- relacionamentos;
- índices;
- estados;
- regras de deduplicação;
- transações importantes;
- dados temporários;
- dados necessários no novo modelo;
- dados desnecessários no novo modelo.

Deixe explícito que:

- o Champion Companion terá banco próprio;
- o banco começará vazio;
- não haverá migração;
- coordenadas e rotas não serão preservadas;
- o documento serve apenas como referência funcional.

### `docs/baseline/health-connect-mapping.md`

Documente para cada tipo:

- registro do Health Connect;
- origem no MotionLog;
- campos;
- unidades;
- timestamps;
- IDs;
- versões;
- permissões;
- insert;
- update;
- retry;
- prevenção de duplicação;
- tratamento de erro.

Regras obrigatórias:

- todos os BPM válidos obtidos por D1 devem ser enviados;
- falhas parciais permanecem pendentes;
- atividades D3/D4 exigem revisão;
- esteira usa distância calibrada;
- não usar `ExerciseRoute`;
- não armazenar ou enviar coordenadas.

### `docs/baseline/gaps-and-open-questions.md`

Centralize todas as lacunas encontradas.

Para cada lacuna, informe:

- assunto;
- impacto;
- evidência encontrada;
- motivo da dúvida;
- fase em que precisa ser validada;
- necessidade ou não de dispositivo físico;
- sugestão de como validar.

Não espalhe dúvidas apenas em comentários genéricos.

### Documentos de protocolo

Para cada comando ou fluxo, documente:

- finalidade;
- fluxo completo;
- characteristic;
- payload enviado;
- payload recebido;
- offsets;
- endianess;
- escalas;
- checksum ou CRC;
- condição de conclusão;
- timeout;
- retry;
- cancelamento;
- tratamento de erro;
- objeto produzido;
- arquivos de origem;
- lacunas.

Classifique cada informação como:

```text
Confirmado pelo código
Confirmado no uso funcional atual
Inferido a partir do código
Ainda não confirmado
```

Nunca apresente inferências como fatos confirmados.

### `docs/protocol/sample-packets/README.md`

Explique que as fixtures serão adicionadas progressivamente.

Defina o padrão:

```text
<fluxo>-<cenario>.hex
<fluxo>-<cenario>-expected.json
<fluxo>-<cenario>-notes.md
```

Exemplos:

```text
handshake-a1-success.hex
handshake-a1-success-notes.md

d1-valid.hex
d1-valid-expected.json

d3-index.hex
d3-index-expected.json

d4-running-workout.hex
d4-running-workout-expected.json
```

Inclua regras para:

- manter payload bruto;
- usar hexadecimal consistente;
- registrar resultado observado;
- registrar firmware quando conhecido;
- remover dados sensíveis;
- informar origem da captura;
- informar app usado na captura;
- transformar fixtures em testes Kotlin posteriormente.

## Como documentar os parsers

Não faça apenas um inventário dos arquivos TypeScript.

Para cada parser, produza um contrato com:

```text
Entrada
Validações
Transformações
Offsets
Endianess
Escalas
Campos opcionais
Valores inválidos
Saída
Erros possíveis
```

Exemplo conceitual:

```text
Payload D4
    ↓
validar cabeçalho
    ↓
interpretar data e duração
    ↓
interpretar passos e distância
    ↓
interpretar calorias
    ↓
interpretar amostras de BPM
    ↓
produzir atividade normalizada
```

Não traduza mecanicamente TypeScript para Kotlin.

## Regras importantes

- Leia o roadmap antes de iniciar a análise.
- Leia a ADR-001 antes de iniciar a análise.
- Não altere o roadmap.
- Não altere a ADR-001.
- Não altere o MotionLog.
- Não implemente código Android.
- Não copie dependências Expo.
- Não copie componentes React Native.
- Não crie banco Room.
- Não crie plano de migração.
- Não crie transferência de dados.
- Não exija todas as amostras físicas nesta fase.
- Não invente offsets, UUIDs ou regras.
- Não apresente inferências como confirmações.
- Registre caminhos exatos dos arquivos consultados.
- Preserve os nomes D0, D1, D3, D4, D8, A1, A8, F002 e FEA1.

## Validação final

Ao concluir:

1. Liste todos os arquivos criados ou modificados no `champion-companion`.
2. Informe a branch e o commit da baseline.
3. Resuma as funcionalidades identificadas.
4. Resuma o protocolo identificado.
5. Liste as lacunas que exigem dispositivo físico.
6. Liste as lacunas que podem ser resolvidas apenas pelo código.
7. Informe quais parsers foram totalmente documentados.
8. Informe quais parsers permanecem parciais.
9. Confirme que o roadmap não foi alterado.
10. Confirme que a ADR-001 não foi alterada.
11. Confirme que nenhum arquivo do `motionlog-app` foi alterado.
12. Mostre o resultado de:

```bash
git -C motionlog-app status --short
git -C champion-companion status --short
```

13. Não faça commit.
14. Não faça push.

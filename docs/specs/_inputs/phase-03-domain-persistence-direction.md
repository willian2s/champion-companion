# Direção de domínio e persistência da Fase 03

## Contexto

A Fase 03 estabelece a fundação de domínio e persistência do Champion Companion.

O Champion Companion é um novo aplicativo Android nativo, independente do MotionLog, desenvolvido em Kotlin e Jetpack Compose e dedicado exclusivamente ao relógio Champion C001.

Os documentos produzidos pela Fase 00 descrevem o comportamento atual do MotionLog e servem como referência funcional. Eles não representam um schema que deva ser copiado mecanicamente.

## Objetivo

Criar uma fundação de domínio e persistência capaz de representar de forma consistente:

- dispositivo Champion C001;
- dados diários;
- frequência cardíaca;
- atividades importadas do relógio;
- revisão de atividades;
- calibração de esteira;
- exercícios iniciados pelo aplicativo;
- estados de sincronização;
- perfil e configurações;
- dados diagnósticos necessários para parsing e reprocessamento.

A implementação deve separar claramente regras do domínio, persistência local e integrações externas.

## Decisões obrigatórias

O planejamento e a implementação da Fase 03 devem respeitar que:

- o Champion Companion possui banco próprio;
- o banco começa vazio;
- não existe migração de dados do MotionLog;
- não existe importação do banco do MotionLog;
- não existe acesso direto ou indireto ao banco do MotionLog;
- o schema do MotionLog serve somente como referência funcional;
- coordenadas e rotas não devem ser armazenadas;
- dados não devem depender diretamente de APIs do BLE ou do Health Connect;
- modelos de domínio não devem ser cópias das entidades Room;
- estados do produto devem ser representados explicitamente;
- inferências da Fase 00 não devem ser tratadas como fatos confirmados.

## Limites arquiteturais

A Fase 03 deve estabelecer separação conceitual entre:

```text
Protocol/BLE
    ↓
Modelos de entrada e resultados de parsing
    ↓
Domínio
    ↓
Repositories
    ↓
Persistência local
```

Integrações futuras, como BLE e Health Connect, não devem expor diretamente suas classes para o domínio.

O domínio deve ser independente de:

- `BluetoothGatt`;
- characteristics BLE;
- payloads binários;
- classes do Room;
- registros do Health Connect;
- componentes Compose;
- Android `Context`.

## Domínio e persistência

A implementação pode usar modelos diferentes para:

- representação do domínio;
- representação persistida;
- representação de importação;
- representação do Health Connect;
- representação da interface.

Mapeamentos entre essas camadas devem ser explícitos.

Não utilizar uma única classe como:

- entidade Room;
- modelo de domínio;
- modelo de UI;
- payload BLE;
- registro do Health Connect.

## Room

A persistência local deverá utilizar Room.

A Fase 03 deve estabelecer:

- banco inicial do Champion Companion;
- entidades;
- DAOs;
- relacionamentos;
- índices;
- constraints;
- transações;
- converters;
- mapeadores;
- repositories;
- estratégia de IDs;
- estratégia de deduplicação;
- testes de persistência;
- estrutura para migrations futuras do próprio Champion Companion.

Não existe migration do banco do MotionLog.

A primeira versão do banco deve ser tratada como versão inicial do Champion Companion.

Migrations futuras deverão ocorrer somente entre versões do banco do próprio Champion Companion.

## Dispositivo

O modelo do dispositivo deve representar somente dados persistentes relevantes, como:

- identificador interno;
- nome conhecido;
- endereço ou identificador BLE permitido pela plataforma;
- modelo Champion C001;
- firmware, quando conhecido;
- data da primeira associação;
- data da última conexão bem-sucedida;
- metadados necessários para importação.

Estados efêmeros não devem ser tratados como estado persistente definitivo.

Exemplos de estados efêmeros:

- conectando;
- desconectando;
- descobrindo services;
- aguardando resposta;
- executando handshake;
- reconectando.

Esses estados pertencem ao fluxo de execução BLE das fases posteriores.

## Dados diários

O domínio deve permitir representar:

- data local;
- passos;
- distância estimada;
- calorias estimadas;
- origem;
- instante da coleta;
- instante da última atualização;
- estado de validade;
- identificador estável;
- deduplicação por data, origem e dispositivo quando aplicável.

O modelo deve permitir atualização segura do mesmo resumo diário sem criar duplicações.

## Frequência cardíaca

O domínio deve diferenciar as origens dos registros de frequência cardíaca, incluindo:

- medição manual;
- stream de medição;
- histórico D1;
- exercício iniciado pelo aplicativo;
- atividade importada do relógio.

Cada registro histórico deve poder representar:

- identificador estável;
- timestamp;
- BPM;
- origem;
- validade;
- dispositivo de origem;
- instante de importação;
- estado de sincronização;
- quantidade de tentativas;
- última tentativa;
- último erro;
- versão ou origem do parser, quando relevante.

## Sincronização de BPM

Todos os registros válidos obtidos pelo histórico D1 e ainda não sincronizados deverão ser enviados futuramente ao Health Connect.

Não haverá seleção manual registro por registro.

O modelo deve permitir:

- consultar todos os registros válidos e pendentes;
- marcar individualmente os registros sincronizados;
- preservar como pendentes os registros que falharem;
- executar novamente apenas os registros ainda pendentes;
- impedir duplicação;
- registrar erros parciais.

Esta fase modela e persiste os estados necessários, mas não implementa a integração com o Health Connect.

## Atividades do relógio

As atividades importadas por D3/D4 devem ser persistidas inicialmente como pendentes de revisão.

O modelo deve permitir representar, quando disponível:

- identificador interno;
- identificador estável de importação;
- data e hora de início;
- duração;
- passos;
- calorias;
- distância original;
- distância estimada;
- distância calibrada;
- ritmo;
- velocidade;
- BPM médio;
- BPM mínimo;
- BPM máximo;
- amostras ou intervalos de BPM;
- distância por intervalo;
- tipo sugerido;
- tipo confirmado;
- origem;
- dispositivo;
- estado de revisão;
- estado de sincronização;
- instante de importação;
- instante de atualização;
- versão do parser;
- referência ao payload diagnóstico.

A ausência de um campo no protocolo não deve ser substituída por um valor inventado.

Campos desconhecidos ou indisponíveis devem permanecer explicitamente ausentes.

## Tipos de atividade

O domínio deve representar pelo menos:

- caminhada;
- corrida;
- treino livre;
- esteira;
- tipo desconhecido ou não confirmado.

O tipo sugerido não deve substituir automaticamente o tipo confirmado pelo usuário.

A regra inicial conhecida de sugestão é:

```text
menos de 1000 passos
→ treino livre

1000 passos ou mais
→ corrida
```

Essa regra é apenas uma sugestão e deverá continuar editável.

## Estados de atividade

Evitar combinações de booleanos independentes como:

```text
isPending
isReviewed
isCalibrated
isSynced
hasError
isDiscarded
```

Essas combinações podem produzir estados inválidos ou contraditórios.

Planejar estados explícitos e transições válidas para representar situações como:

- pendente de revisão;
- pendente de calibração;
- pronta para sincronização;
- sincronização pendente;
- sincronizando;
- sincronizada;
- falha de sincronização;
- descartada.

Os nomes definitivos podem ser ajustados durante a spec, desde que as invariantes permaneçam claras.

Uma atividade descartada não deve ser enviada ao Health Connect.

Uma atividade D3/D4 não deve ser enviada sem revisão.

Uma atividade de esteira que exija calibração não deve ser considerada pronta enquanto a calibração necessária não estiver concluída.

## Revisão de atividades

A persistência deve permitir:

- manter o valor original importado;
- armazenar alterações feitas pelo usuário;
- editar tipo;
- editar distância;
- calibrar esteira;
- descartar;
- marcar como pronta;
- registrar sincronização;
- preservar informações necessárias para auditoria e diagnóstico.

A revisão não deve destruir silenciosamente os valores originais fornecidos ou estimados.

## Calibração de esteira

A calibração deve preservar:

- distância original ou estimada;
- distância real informada pelo usuário;
- distância calibrada;
- fator de calibração;
- data da calibração;
- origem da calibração;
- ritmo recalculado;
- velocidade recalculada;
- métricas derivadas recalculadas.

A distância calibrada será a distância enviada futuramente ao Health Connect.

A distância original não deve ser substituída ou perdida.

O modelo deve permitir distinguir claramente:

```text
distância original
distância estimada
distância informada pelo usuário
distância calibrada
```

Não duplicar dados quando uma propriedade puder ser derivada de forma segura. Entretanto, valores necessários para preservar o histórico da calibração podem ser armazenados.

## Exercícios iniciados pelo aplicativo

Planejar o domínio e a persistência necessários para sessões de:

- caminhada;
- corrida;
- treino livre;
- esteira.

Uma sessão poderá representar:

- criada;
- ativa;
- pausada;
- retomada;
- finalizada;
- interrompida;
- aguardando recuperação;
- descartada.

O modelo deve permitir:

- início;
- pausa;
- retomada;
- finalização;
- recuperação após encerramento inesperado;
- duração ativa;
- duração pausada;
- passos;
- distância;
- calorias;
- frequência cardíaca;
- métricas agregadas;
- sincronização futura.

## GPS e privacidade de localização

O GPS poderá ser utilizado futuramente apenas em memória para calcular:

- distância;
- ritmo;
- velocidade;
- métricas derivadas.

Não persistir:

- latitude;
- longitude;
- altitude por ponto;
- trilha GPS;
- polylines;
- segmentos de rota;
- `ExerciseRoute`;
- payloads que permitam reconstruir a rota.

A regra de não armazenar rota deve ser refletida nas entidades, modelos, repositories e testes.

## Perfil e configurações

Planejar somente dados previstos no roadmap e necessários ao produto, incluindo quando aplicável:

- peso;
- altura;
- preferências de cálculo;
- preferência de tema;
- preferência por cores dinâmicas;
- preferências de sincronização;
- configurações do dispositivo;
- configurações de diagnóstico.

Não ampliar o perfil com:

- conta remota;
- autenticação;
- rede social;
- metas não previstas;
- dados clínicos não necessários;
- funcionalidades de outros wearables.

## Material You e preferência de tema

A Fase 02 definiu Material You como padrão em aparelhos compatíveis e a paleta própria do Champion Companion como fallback.

Caso a preferência visual seja persistida, o modelo deve permitir representar:

- seguir o sistema;
- tema claro;
- tema escuro;
- uso de cores dinâmicas quando suportadas.

A implementação dessa preferência deve respeitar o que já foi definido na Fase 02.

Não redefinir o design system nesta fase.

## IDs estáveis

Planejar identificadores estáveis para:

- dispositivos;
- resumos diários;
- registros de BPM;
- atividades do relógio;
- sessões do aplicativo;
- calibrações;
- tentativas de sincronização;
- payloads diagnósticos.

IDs autoincrementais locais podem ser usados como chaves técnicas, mas não devem ser a única estratégia de deduplicação.

A deduplicação deve considerar os dados estáveis disponíveis em cada origem.

Exemplos conceituais:

- dispositivo;
- origem;
- timestamp;
- tipo de registro;
- dados relevantes do payload;
- identificador fornecido pelo relógio;
- hash controlado;
- bloco e posição do protocolo.

Não definir algoritmos definitivos sem evidência suficiente.

## Deduplicação

A persistência deve impedir ou tratar duplicações de:

- resumos diários;
- registros D1;
- medições manuais;
- atividades D3/D4;
- sessões recuperadas;
- sincronizações repetidas.

Para cada tipo, a spec deverá documentar:

- chave natural disponível;
- chave técnica;
- estratégia de deduplicação;
- risco de colisão;
- comportamento em conflito;
- possibilidade de atualização;
- necessidade de transação.

## Tempo e datas

O domínio deve diferenciar:

- instante absoluto;
- data local;
- horário local;
- timezone;
- instante de coleta;
- instante informado pelo relógio;
- instante de importação;
- instante de criação;
- instante de atualização;
- início e fim de uma sessão.

Não armazenar apenas strings formatadas para exibição.

Planejar o uso de tipos apropriados, como:

- `Instant`;
- `LocalDate`;
- `LocalDateTime`;
- timezone ou offset, quando necessário.

A spec deve considerar:

- mudança de fuso;
- horário de verão;
- dados do relógio sem timezone explícito;
- agrupamento por dia local;
- sincronização com timestamps absolutos.

## Repositories

Repositories devem representar operações e regras do domínio, não apenas espelhar métodos de DAO.

Planejar contratos para necessidades como:

- observar dispositivo associado;
- consultar resumo diário;
- armazenar dados importados;
- consultar BPM pendente;
- consultar atividades pendentes de revisão;
- atualizar revisão;
- registrar calibração;
- consultar atividades prontas para sincronização;
- registrar sucesso ou falha de sincronização;
- recuperar sessão interrompida;
- limpar dados diagnósticos expirados.

Não criar um repository genérico único para todas as entidades.

## Transações e integridade

Planejar transações para operações compostas, como:

- importar payload e registros derivados;
- inserir atividade e intervalos;
- aplicar revisão e recalcular métricas;
- registrar calibração;
- finalizar sessão;
- marcar sincronizações parciais;
- armazenar erro sem perder o dado original;
- reprocessar payload.

A persistência deve evitar estados parcialmente gravados.

## Dados diagnósticos

Payloads brutos do Champion C001 podem ser persistidos quando necessários para:

- diagnóstico;
- auditoria técnica;
- reprodução de falhas;
- evolução dos parsers;
- reprocessamento.

O modelo diagnóstico deve considerar:

- comando ou fluxo;
- payload bruto;
- formato de armazenamento;
- data da captura;
- dispositivo;
- firmware, quando conhecido;
- versão do parser;
- resultado do parsing;
- erro;
- vínculo com o registro produzido;
- origem da captura.

Evitar transformar o banco em armazenamento ilimitado de logs.

Planejar política de retenção, limpeza ou limite de volume.

## Parsing e reprocessamento

Quando payloads diagnósticos forem preservados, o modelo deve permitir distinguir:

- payload recebido;
- versão do parser;
- parsing concluído;
- parsing parcial;
- parsing inválido;
- erro técnico;
- registros produzidos;
- possibilidade de reprocessamento.

Reprocessar um payload não deve criar registros duplicados.

## Dados inválidos e incompletos

Não persistir valores inventados para preencher campos ausentes.

O modelo deve distinguir:

- valor ausente;
- valor inválido;
- valor não suportado;
- valor estimado;
- valor informado pelo usuário;
- valor calculado;
- valor confirmado pelo protocolo.

A spec deve definir quando:

- rejeitar;
- persistir como diagnóstico;
- persistir parcialmente;
- marcar como inválido;
- permitir reprocessamento.

## Testes

Planejar testes para:

- entidades e constraints;
- DAOs;
- índices únicos;
- transações;
- mapeadores;
- repositories;
- estados e transições;
- deduplicação;
- datas e timezones;
- calibração;
- preservação de valores originais;
- sincronizações parciais;
- reprocessamento;
- ausência de coordenadas e rotas.

Os testes devem usar dados determinísticos.

Fixtures futuras do protocolo poderão ser incorporadas aos testes quando estiverem disponíveis.

## Segurança e privacidade

Dados de saúde, atividades, perfil e payloads devem permanecer locais, salvo integrações explicitamente previstas no roadmap.

A persistência deve evitar:

- logs contendo dados pessoais sem necessidade;
- armazenamento de coordenadas;
- exposição acidental em backups;
- retenção ilimitada de payloads;
- dados sensíveis em nomes de arquivos;
- valores secretos no banco.

## Backup

O banco e os dados do Champion Companion são independentes do MotionLog.

Não implementar compatibilidade com backups do MotionLog.

A Fase 03 deve apenas preparar um modelo consistente para o futuro backup próprio do Champion Companion.

Regras de backup e restauração serão tratadas em fase específica.

## Fora do escopo

A Fase 03 não deve:

- migrar dados do MotionLog;
- importar banco do MotionLog;
- acessar arquivos privados do MotionLog;
- implementar BLE;
- implementar handshake;
- implementar D0, D1, D3 ou D4;
- implementar Health Connect;
- implementar sincronização real;
- implementar GPS;
- implementar exercícios completos;
- implementar foreground services;
- implementar backup ou restauração;
- armazenar rotas ou coordenadas;
- redefinir o tema da Fase 02;
- criar suporte a relógios diferentes do Champion C001.

## Critérios de aceite

A Fase 03 será considerada concluída quando:

- existir separação clara entre domínio e persistência;
- existir banco Room inicial próprio;
- existirem entidades e DAOs necessários ao escopo da fase;
- existirem repositories com contratos orientados ao domínio;
- existirem mapeadores explícitos;
- os estados principais forem representados sem combinações contraditórias;
- BPM D1 puder ser persistido com estado individual de sincronização;
- atividades D3/D4 entrarem como pendentes de revisão;
- a distância original da esteira for preservada;
- a calibração puder ser registrada sem destruir valores anteriores;
- IDs e deduplicação estiverem documentados e implementados;
- datas e timezones forem tratados explicitamente;
- payloads diagnósticos possuírem retenção controlada;
- reprocessamento não gerar duplicações;
- não existir armazenamento de rotas ou coordenadas;
- existirem testes para persistência, transações e invariantes;
- não existir migração ou dependência do banco do MotionLog.

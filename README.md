# Entrega 0: Projeto de Sistemas Distribuídos

O intuito desse projeto é aplicar conteúdos ensinados na disciplina de Sistemas Distribuídos.

### Resumo do Projeto:

A ideia inicial deste projeto é desenvolver um sistema que simule leilões utilizando o modelo cliente-servidor.  

O **servidor** será responsável por iniciar um processo de leilão, definir o lance inicial, quanto tempo os participantes terão para dar os seus lances e aceitar conexões de até quinze clientes.

O **cliente** deverá solicitar conexão com o servidor e, uma vez estabelecida a conexão, deverá escolher um *nickname* e enviar seus lances quando solicitado pelo servidor.  

### Tecnologias Utilizadas:

* Será utilizada a linguagem Java ou Javascript com NodeJS (ainda não definido).

### Testes a serem feitos para evidenciar o funcionamento:

* **Testes de concorrência:** Após o servidor entrar em funcionamento e um processo de leilão for iniciado, múltiplos clientes devem ser capazes de se conectarem ao servidor, todos eles devem ser capazes de enviar e receber dados e a ordem de envio dos lances deve ser respeitada. 

* **Testes de funcionalidade:** Um cliente não pode ser capaz de enviar um lance cujo valor é menor que o lance corrente. Ele não deve ser capaz de escolher um *nickname* já escolhido por outro cliente. Deve ser permitido que ele envie apenas números como lance. Outras condições devem surgir no momento da implementação.
 
* **Testes de funcionalidade:** Se um componente falhar e voltar a executar, ele não pode levar o sistema a nenhum estado inesperado. Por exemplo, um cliente não ser o dono de um lance que não realizou de fato.


# Entrega 1: Leilão Online usando socket TCP

## Introdução
- O projeto consiste em um leilão online onde os usuários podem criar autenticar, criar e conectar em leilões, fazer ofertas etc.
- O projeto utiliza Node.js em sua estrutura. Para baixar a versão mais recente, acesse https://nodejs.org/en/.
- Para usuários linux, utilizar algum gerenciador de de pacotes, exemplo apt-get:
```
sudo apt-get install nodejs
```

## Instalação
- Instalar o NodeJS
- Clonar o repositório
- Fazer a instalação de algumas libs usadas no projeto: 
```
npm install
```

## Explicação
O servidor consegue atender a múltiplos clientes conectados. O cliente, ao se conectar ao servidor, consegue se autenticar ou fazer seu cadastro. 
Feito o login, ele tem acesso aos leilões correntes e os que já terminaram. Ele pode então escolher um leilão para se conectar. 

Quando o número de participantes mínimo é atingido, o leilão começa e os participantes podem enviar seus lances e tem acesso em seu console sobre os lances enviados pelos outros. Passado o tempo pré-estabelecido pelo leilão, o último lance enviado (o lance atual deve ser sempre maior que o anterior, garantido pelo servidor) é o vencedor.


## Estrutura

### Ideias Gerais

- Client-server: encontram-se os códigos principais do projeto, o do cliente e o do servidor.

- Databases: são onde ficam os arquivos que salvam informações relacionais e temporárias. A ideia foi criar arquivos 'json' que ficam como singleton na memória e só leem o arquivo em caso de update.

- O arquivo actions.js é utilizado para definir as ações que ocorrem entre o servidor e o cliente em cada etapa do leilão.

- O arquivo messages.js armazena as mensagens printadas nos consoles do servidor e do cliente em diferentes momentos.

### Armazenamento de dados 

- No diretório cache o arquivo sessions.js armazena informações sobre as sessões dos clientes conectados ao servidor. 

- No diretório relational são armazenadas as informações dos leilões correntes e finalizados no arquivo auctions.js. 

- No diretório util ficam algumas funções para auxiliar no restante da aplicação, como geração de hash e uuid.

- Em bets.js são armazenadas as informações dos lances enviados pelos participantes. 

- Em users.js são armazenadas as informações dos usuários cadastrados.

Se o servidor cair por qualquer motivo, ele consegue continuar cada leilão exatamente do mesmo momento onde ele se encontrava através da leitura
dos arquivos bets.js e auctions.js.

### Singleton de acesso aos dados

- No diretório model/repository estão os arquivos de repositório. 

- O DatabaseRepository.js é um arquivo que serve como base para os outros. Ele contém métodos básicos de leitura e escrita nos arquivos de 
persistência e cache. 

- Os demais arquivos estendem de DatabaseRepository.js e também possuem internamente classes Singleton. Sua função, como o próprio nome diz,
é manter um singleton em memória dos objetos lidos dos arquivos de persistência, para que não seja necessário ler ou instanciar classes constantemente para obter informações do arquivos.

### Camada de serviço

- O diretório service armazena a camada de serviço da aplicação. Ela é responsável por conter as principais regras de acesso e de negócio da aplicação aos bancos de dados. 
- Essa camada cria toda a lógica de autenticação, proteção de acesso, gerenciamento de usuários, sessões e dos leilões ativos.

### Camada de controle
- Os arquivos client.js e server.js são arquivos que gerenciam os sockets e a comunicação entre eles. Elas são responsáveis por comunicar entre as frentes mandando uma 'ação', o 'tipo da ação' e os dados necessários para a aplicação, por exemplo, o token de sessão.


# Entrega 2: Leilão Online usando gRPC e Java

## Introdução
- O projeto consiste em um leilão online onde os usuários podem basicamente criar um leilão, listar os leilões ativos e dar lances nestes leilões.
- O projeto utiliza gRPC com Java 8.
- Foi utilizado o Maven como gerenciador de dependências.

## Executar o projeto
- Para executar o projeto, é necessário instalar o Java 8, disponível em: 
```
https://www.oracle.com/technetwork/pt/java/javase/downloads/jdk8-downloads-2133151.html
```
- Feito isso, é necessário clonar o repositório e buildar com o Maven utilizando o seguinte comando:
```
mvn clean install
```
- No projeto temos o diretório do servidor, chamado 



# Autores

* **Rodrigo Pereira** - [Github](https://github.com/rodrigorpo)
* **Thiago Pereira** - [Github](https://github.com/thiagopolv)

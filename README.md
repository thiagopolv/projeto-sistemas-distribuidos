# Projeto de Sistemas Distribuídos

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
 

# BidRoom - Leilão Multiplayer em Tempo Real

## Objetivo

Desenvolver uma aplicação distribuída utilizando TCP Sockets em Java, onde múltiplos clientes podem participar simultaneamente de uma sala de leilão.

O sistema simula uma economia virtual. Cada participante possui moedas fictícias, pode cadastrar itens próprios para leilão e disputar lances em tempo real. O servidor é responsável por controlar toda a lógica do sistema, garantindo consistência dos dados, sincronização entre clientes e comunicação através de sockets TCP.

O projeto tem como foco demonstrar conceitos de Sistemas Distribuídos, como comunicação cliente-servidor, concorrência, sincronização de estado, protocolo de aplicação e gerenciamento de múltiplas conexões.

## Objetivos Técnicos

- Implementar comunicação utilizando ServerSocket e Socket.
- Desenvolver um protocolo próprio de comunicação.
- Sincronizar informações entre diversos clientes.
- Controlar concorrência durante os lances.
- Centralizar todas as decisões no servidor.
- Manter o estado do sistema consistente.

## Tecnologias

- Backend
- Java 21 (ou a versão exigida pela disciplina)
- ServerSocket
- Socket
- Threads
- Frontend
- Java Swing

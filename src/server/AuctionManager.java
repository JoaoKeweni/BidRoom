package server;

import java.util.LinkedList;
import java.util.Queue;
import models.Auction;
import models.Item;
import models.User;

public class AuctionManager {
    
    private Queue<Item> itemPool = new LinkedList<>();
    private Auction currentAuction;
    private MainServer server;

    public AuctionManager(MainServer server) {
        this.server = server;
        // Popula a fila de itens que serão leiloados pelo servidor
        itemPool.add(new Item("1", "Placa de Vídeo RTX 4090", 5000.0));
        itemPool.add(new Item("2", "Notebook Gamer", 3500.0));
        itemPool.add(new Item("3", "Cadeira Ergonômica", 800.0));
    }

    // Método disparado pelo administrador no servidor
    public void startNextAuction() {
        if (currentAuction != null && currentAuction.isOpen()) {
            System.out.println("Já existe um leilão em andamento!");
            return;
        }

        Item nextItem = itemPool.poll();
        if (nextItem == null) {
            System.out.println("Não há mais itens para leiloar.");
            server.broadcast("INFO|O leilão acabou! Todos os itens foram vendidos.");
            return;
        }

        // Instancia o leilão em memória
        currentAuction = new Auction(nextItem);
        System.out.println("Iniciando leilão para: " + nextItem.getName());
        
        server.broadcast("AUCTION_START|" + nextItem.getName() + "|" + nextItem.getStartPrice());
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    // FASE 7: Exclusão Mútua (O Coração do Projeto) ⭐
    // A palavra-chave 'synchronized' é um Monitor (Lock) do Java. Ela transforma
    // esse método numa Seção Crítica. Se a Maria e o Pedro enviarem um lance no mesmo
    // milissegundo, a Thread de um deles terá que aguardar do lado de fora até a 
    // Thread do outro terminar de executar esse bloco de código.
    public synchronized boolean processBid(User user, double bidAmount, ClientHandler handler) {
        // Validação 1: Tem leilão aberto?
        if (currentAuction == null || !currentAuction.isOpen()) {
            handler.sendMessage("ERROR|Não há nenhum leilão aberto no momento.");
            return false;
        }

        // Validação 2: É maior que o lance atual?
        if (bidAmount <= currentAuction.getHighestBid()) {
            handler.sendMessage("ERROR|O lance deve ser estritamente maior que o valor atual (" + currentAuction.getHighestBid() + ").");
            return false;
        }

        // Validação 3: Tem dinheiro na carteira?
        if (user.getBalance() < bidAmount) {
            handler.sendMessage("ERROR|Saldo insuficiente. Você tem apenas " + user.getBalance() + " moedas.");
            return false;
        }

        // Se passar por todos os IFs, o estado é atualizado com segurança!
        currentAuction.setHighestBid(bidAmount);
        currentAuction.setLeader(user);

        System.out.println("NOVO LIDER: " + user.getName() + " ofereceu " + bidAmount + " pelo item " + currentAuction.getItem().getName());
        
        // Faz broadcast pra galera informando que o preço subiu
        server.broadcast("NEW_BID|" + user.getName() + "|" + bidAmount);
        return true;
    }
}

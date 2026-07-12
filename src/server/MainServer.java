package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer {

    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private UserManager userManager = new UserManager();
    
    // Fase 6 e 7: Gerenciador de Leilões
    private AuctionManager auctionManager;

    public static void main(String[] args) {
        new MainServer().startServer();
    }

    public void startServer() {
        // Inicializa o gerenciador passando a referência do servidor para ele poder fazer broadcast
        this.auctionManager = new AuctionManager(this);
        
        // Thread dedicada para ler comandos do administrador no terminal do servidor
        startAdminConsole();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Servidor escutando na porta 5000...");
            System.out.println(">>> Digite 'iniciar' a qualquer momento para abrir o 1º leilão <<<");

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this, userManager);
                clients.add(clientHandler);
                
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    private void startAdminConsole() {
        Thread adminThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String cmd = scanner.nextLine();
                if (cmd.equalsIgnoreCase("iniciar")) {
                    auctionManager.startNextAuction();
                }
            }
        });
        adminThread.start();
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("Total de clientes conectados: " + clients.size());
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
}
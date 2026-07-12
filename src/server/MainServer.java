package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainServer {

    // Fase 3: Lista thread-safe para manter estado compartilhado dos clientes
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // Criamos uma instância do servidor em vez de fazer tudo estático
        new MainServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Servidor escutando na porta 5000...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + socket.getInetAddress());

                // Fase 3: Passamos a referência do servidor (this) para o ClientHandler
                ClientHandler clientHandler = new ClientHandler(socket, this);
                
                // Adicionamos na lista de clientes conectados
                clients.add(clientHandler);
                System.out.println("Total de clientes agora: " + clients.size());

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    // Método para ser chamado pelo ClientHandler quando um cliente desconectar
    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        System.out.println("Cliente removido. Total de clientes conectados: " + clients.size());
    }

    // Fase 4: O método broadcast envia mensagem para todos na lista
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}
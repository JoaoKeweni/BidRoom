package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private MainServer server;
    private PrintWriter out;
    private String clientName = "Anônimo"; // Fase 4: Nome do usuário

    public ClientHandler(Socket socket, MainServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            String mensagem;
            while ((mensagem = in.readLine()) != null) {
                // Fase 4: Interpreta usando o Protocolo
                String[] comando = Protocol.parseCommand(mensagem);
                String acao = comando[0];

                switch (acao) {
                    case "LOGIN":
                        if (comando.length > 1) {
                            this.clientName = comando[1];
                            System.out.println("Usuário logou como: " + clientName);
                            sendMessage("LOGIN_OK|Bem-vindo, " + clientName + "!");
                        } else {
                            sendMessage("ERROR|O comando LOGIN exige um nome. Ex: LOGIN|Joao");
                        }
                        break;
                    
                    case "CHAT":
                        if (comando.length > 1) {
                            String textoChat = comando[1];
                            System.out.println(clientName + " disse: " + textoChat);
                            // Faz o broadcast para todos! (CHAT|Nome|Mensagem)
                            server.broadcast("CHAT|" + clientName + "|" + textoChat);
                        }
                        break;
                        
                    default:
                        sendMessage("ERROR|Comando desconhecido: " + acao);
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                System.out.println("Cliente desconectou: " + socket.getInetAddress() + " (" + clientName + ")");
                server.removeClient(this);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

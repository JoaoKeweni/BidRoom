package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import models.User; // Fase 5

public class ClientHandler implements Runnable {

    private Socket socket;
    private MainServer server;
    private UserManager userManager; // Fase 5
    private PrintWriter out;
    private User user; // Fase 5: Agora temos um Objeto User em vez de só uma String
    private String tempAddress; // Guarda o IP caso o usuário não tenha feito login ainda

    // Construtor: agora recebe o UserManager
    public ClientHandler(Socket socket, MainServer server, UserManager userManager) {
        this.socket = socket;
        this.server = server;
        this.userManager = userManager;
        this.tempAddress = socket.getInetAddress().toString();
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    // Método auxiliar para facilitar pegarmos o nome em logs
    private String getDisplayName() {
        return (user != null) ? user.getName() : "Anônimo(" + tempAddress + ")";
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            String mensagem;
            while ((mensagem = in.readLine()) != null) {
                String[] comando = Protocol.parseCommand(mensagem);
                String acao = comando[0];

                switch (acao) {
                    case "LOGIN":
                        if (comando.length > 1) {
                            String nomeInformado = comando[1];
                            
                            // Fase 5: O UserManager faz o trabalho pesado de criar ou recuperar o saldo
                            this.user = userManager.login(nomeInformado);
                            
                            System.out.println("Usuário logou: " + user.getName() + " com saldo " + user.getBalance());
                            sendMessage("LOGIN_OK|Bem-vindo, " + user.getName() + "! Saldo atual: " + user.getBalance());
                        } else {
                            sendMessage("ERROR|O comando LOGIN exige um nome. Ex: LOGIN|Joao");
                        }
                        break;
                    
                    case "CHAT":
                        if (comando.length > 1) {
                            String textoChat = comando[1];
                            System.out.println(getDisplayName() + " disse: " + textoChat);
                            // Faz o broadcast para todos! (CHAT|Nome|Mensagem)
                            server.broadcast("CHAT|" + getDisplayName() + "|" + textoChat);
                        }
                        break;

                    case "BID":
                        if (this.user == null) {
                            sendMessage("ERROR|Você precisa fazer o LOGIN antes de dar lances.");
                            break;
                        }
                        if (comando.length > 1) {
                            try {
                                double valor = Double.parseDouble(comando[1]);
                                // Fase 7: Repassa a decisão para o AuctionManager
                                server.getAuctionManager().processBid(this.user, valor, this);
                            } catch (NumberFormatException e) {
                                sendMessage("ERROR|Valor de lance inválido. Use formato double, ex: BID|1500.0");
                            }
                        } else {
                            sendMessage("ERROR|O comando BID exige um valor. Ex: BID|1500.0");
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
                System.out.println("Cliente desconectou: " + getDisplayName());
                server.removeClient(this);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

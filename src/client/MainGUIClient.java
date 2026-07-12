package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class MainGUIClient extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Componentes da UI
    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel lblItemName;
    private JLabel lblHighestBid;
    private JLabel lblTimeLeft;
    private JTextField bidField;
    private JButton btnBid;
    
    public MainGUIClient() {
        // Configurações Básicas da Janela
        setTitle("BidRoom - Leilão Distribuído");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            // Tenta usar o visual nativo do Windows/SO
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // --- PAINEL SUPERIOR (LEILÃO) ---
        JPanel auctionPanel = new JPanel(new GridLayout(4, 1));
        auctionPanel.setBorder(BorderFactory.createTitledBorder("Mesa do Leiloeiro"));
        auctionPanel.setBackground(new Color(240, 248, 255)); // Azul clarinho
        
        lblItemName = new JLabel("Aguardando leilão começar...", SwingConstants.CENTER);
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblItemName.setForeground(new Color(25, 25, 112)); // Midnight Blue
        
        lblHighestBid = new JLabel("Maior Lance: R$ 0.00", SwingConstants.CENTER);
        lblHighestBid.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        lblTimeLeft = new JLabel("Tempo: --", SwingConstants.CENTER);
        lblTimeLeft.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTimeLeft.setForeground(new Color(178, 34, 34)); // Firebrick Red
        
        // Área de dar lance
        JPanel bidPanel = new JPanel(new FlowLayout());
        bidPanel.setOpaque(false);
        bidField = new JTextField(10);
        btnBid = new JButton("Dar Lance!");
        btnBid.setBackground(new Color(46, 139, 87));
        btnBid.setForeground(Color.WHITE);
        btnBid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        bidPanel.add(new JLabel("Seu valor (R$):"));
        bidPanel.add(bidField);
        bidPanel.add(btnBid);
        
        auctionPanel.add(lblItemName);
        auctionPanel.add(lblHighestBid);
        auctionPanel.add(lblTimeLeft);
        auctionPanel.add(bidPanel);

        // --- CENTRO (CHAT E LOGS) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(220, 220, 220));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat & Eventos do Servidor"));

        // --- PAINEL INFERIOR (ENVIAR MENSAGENS) ---
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton btnSend = new JButton("Enviar Chat");
        
        chatInputPanel.add(inputField, BorderLayout.CENTER);
        chatInputPanel.add(btnSend, BorderLayout.EAST);

        // Adiciona tudo na Janela Principal
        add(auctionPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(chatInputPanel, BorderLayout.SOUTH);

        // --- EVENTOS DE CLIQUE ---
        btnBid.addActionListener(e -> enviarLance());
        btnSend.addActionListener(e -> enviarChat());
        // Enviar chat ao apertar Enter no campo de texto
        inputField.addActionListener(e -> enviarChat()); 
    }

    private void conectar() {
        String nome = JOptionPane.showInputDialog(this, "Bem-vindo ao BidRoom!\nDigite seu nome de usuário:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.trim().isEmpty()) {
            System.exit(0);
        }

        try {
            // Conecta ao servidor TCP raiz que construímos
            socket = new Socket("127.0.0.1", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + nome);

            // Thread secundária dedicada a ESCUTAR o servidor em background
            new Thread(() -> {
                try {
                    String mensagem;
                    while ((mensagem = in.readLine()) != null) {
                        processarMensagemServidor(mensagem);
                    }
                } catch (IOException e) {
                    appendChat("[Erro] Conexão perdida: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor na porta 5000.\nGaranta que o MainServer está rodando!", "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    // O CÉREBRO DA INTERFACE: Traduz o protocolo para a tela
    private void processarMensagemServidor(String mensagem) {
        // SwingUtilities garante que as mudanças na tela ocorram na Thread correta de UI
        SwingUtilities.invokeLater(() -> {
            String[] partes = mensagem.split("\\|", -1);
            String acao = partes[0];

            switch (acao) {
                case "AUCTION_START":
                    lblItemName.setText("Em leilão: " + partes[1]);
                    lblHighestBid.setText("Maior Lance Inicial: R$ " + partes[2]);
                    appendChat("🔔 O LEILÃO COMEÇOU! Item: " + partes[1]);
                    break;
                case "TIME":
                    lblTimeLeft.setText("Tempo restante: " + partes[1] + "s");
                    // Pisca em vermelho quando faltar pouco tempo (efeito visual)
                    if (Integer.parseInt(partes[1]) <= 5) {
                        lblTimeLeft.setForeground(Color.RED);
                    } else {
                        lblTimeLeft.setForeground(new Color(178, 34, 34));
                    }
                    break;
                case "NEW_BID":
                    lblHighestBid.setText("Líder: " + partes[1] + " | Valor: R$ " + partes[2]);
                    appendChat("💰 " + partes[1] + " assumiu a liderança com R$ " + partes[2]);
                    break;
                case "AUCTION_END":
                    lblTimeLeft.setText("TEMPO ENCERRADO");
                    appendChat("🛑 [FIM] " + partes[1]);
                    break;
                case "CHAT":
                    appendChat("🗣️ [" + partes[1] + "]: " + partes[2]);
                    break;
                case "LOGIN_OK":
                case "INFO":
                case "ERROR":
                    // Junta o resto da mensagem para ignorar outros pipes caso existam
                    String texto = mensagem.substring(acao.length() + 1);
                    appendChat("[" + acao + "] " + texto);
                    break;
                default:
                    appendChat("[Desconhecido] " + mensagem);
                    break;
            }
        });
    }

    // Manda um comando estruturado de lance pro servidor
    private void enviarLance() {
        String valor = bidField.getText().trim();
        if (!valor.isEmpty()) {
            out.println("BID|" + valor);
            bidField.setText("");
        }
    }

    // Manda texto livre, mas checa se é o comando de PERFIL pra inspecionar a grana
    private void enviarChat() {
        String texto = inputField.getText().trim();
        if (!texto.isEmpty()) {
            if (texto.equalsIgnoreCase("PERFIL")) {
                out.println("PERFIL");
            } else {
                out.println("CHAT|" + texto);
            }
            inputField.setText("");
        }
    }

    private void appendChat(String texto) {
        chatArea.append(texto + "\n");
        // Rola o chat pra baixo automaticamente
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainGUIClient client = new MainGUIClient();
            client.setVisible(true);
            client.conectar();
        });
    }
}

package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import javax.imageio.ImageIO;

public class MainGUIClient extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Componentes da UI
    private JTextArea chatArea;
    private JTextField inputField;
    
    // Área de Infos
    private JLabel lblItemName;
    private JLabel lblHighestBid;
    private JLabel lblTimeLeft;
    private JLabel lblImage; // FASE 13: Container da Imagem!
    
    private JTextField bidField;
    private JButton btnBid;
    
    // Painéis Mutantes
    private JPanel bottomArea;
    private JPanel bidPanel;
    private JPanel adminPanel;
    private JTextField txtItemName;
    private JTextField txtItemPrice;
    private JTextField txtItemUrl; // FASE 13: URL da Imagem
    
    public MainGUIClient() {
        setTitle("BidRoom - Leilão Distribuído");
        setSize(800, 600); // Aumentei um pouco pra caber a imagem confortavelmente
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // --- PAINEL SUPERIOR (LEILÃO) ---
        JPanel auctionPanel = new JPanel(new BorderLayout());
        auctionPanel.setBorder(BorderFactory.createTitledBorder("Mesa do Leiloeiro"));
        auctionPanel.setBackground(new Color(240, 248, 255));
        
        // Área das Etiquetas (Esquerda)
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setOpaque(false);
        lblItemName = new JLabel("Aguardando leilão começar...", SwingConstants.CENTER);
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblItemName.setForeground(new Color(25, 25, 112));
        
        lblHighestBid = new JLabel("Maior Lance: R$ 0.00", SwingConstants.CENTER);
        lblHighestBid.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        lblTimeLeft = new JLabel("Tempo: --", SwingConstants.CENTER);
        lblTimeLeft.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTimeLeft.setForeground(new Color(178, 34, 34));
        
        infoPanel.add(lblItemName);
        infoPanel.add(lblHighestBid);
        infoPanel.add(lblTimeLeft);
        
        // Área da Imagem (Direita)
        lblImage = new JLabel("Nenhuma Foto", SwingConstants.CENTER);
        lblImage.setPreferredSize(new Dimension(200, 200));
        lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        // Junta os dois na Mesa
        auctionPanel.add(infoPanel, BorderLayout.CENTER);
        auctionPanel.add(lblImage, BorderLayout.EAST);
        
        // --- ÁREA DE CONTROLES INFERIOR ---
        bottomArea = new JPanel(new BorderLayout());
        bottomArea.setOpaque(false);
        
        // Área de dar lance (Comprador)
        bidPanel = new JPanel(new FlowLayout());
        bidPanel.setOpaque(false);
        bidField = new JTextField(10);
        btnBid = new JButton("Dar Lance!");
        btnBid.setBackground(new Color(46, 139, 87));
        btnBid.setForeground(Color.WHITE);
        btnBid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        bidPanel.add(new JLabel("Seu valor (R$):"));
        bidPanel.add(bidField);
        bidPanel.add(btnBid);
        
        // FASE 12/13: Área do Leiloeiro com 2 linhas e Campo de URL
        adminPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        adminPanel.setOpaque(false);
        
        JPanel adminRow1 = new JPanel(new FlowLayout());
        adminRow1.setOpaque(false);
        txtItemName = new JTextField(10);
        txtItemPrice = new JTextField(5);
        txtItemUrl = new JTextField(12); // FASE 13
        JButton btnAddItem = new JButton("Cadastrar");
        adminRow1.add(new JLabel("Nome:"));
        adminRow1.add(txtItemName);
        adminRow1.add(new JLabel("R$:"));
        adminRow1.add(txtItemPrice);
        adminRow1.add(new JLabel("URL Foto:"));
        adminRow1.add(txtItemUrl);
        adminRow1.add(btnAddItem);

        JPanel adminRow2 = new JPanel(new FlowLayout());
        adminRow2.setOpaque(false);
        JButton btnStartAuction = new JButton("▶ Iniciar Próximo");
        JButton btnAddTime = new JButton("⏳ +5s");
        JButton btnForceEnd = new JButton("🛑 Encerrar Leilão");
        adminRow2.add(btnStartAuction);
        adminRow2.add(btnAddTime);
        adminRow2.add(btnForceEnd);
        
        adminPanel.add(adminRow1);
        adminPanel.add(adminRow2);
        
        // Por padrão, comprador
        bottomArea.add(bidPanel, BorderLayout.CENTER);
        auctionPanel.add(bottomArea, BorderLayout.SOUTH);

        // --- CENTRO (CHAT E LOGS) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(220, 220, 220));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat & Eventos do Servidor"));

        // --- PAINEL INFERIOR (ENVIAR) ---
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton btnSend = new JButton("Enviar Chat");
        
        chatInputPanel.add(inputField, BorderLayout.CENTER);
        chatInputPanel.add(btnSend, BorderLayout.EAST);

        add(auctionPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(chatInputPanel, BorderLayout.SOUTH);

        // --- EVENTOS ---
        btnBid.addActionListener(e -> enviarLance());
        btnSend.addActionListener(e -> enviarChat());
        inputField.addActionListener(e -> enviarChat()); 
        
        btnAddItem.addActionListener(e -> {
            String name = txtItemName.getText().trim();
            String price = txtItemPrice.getText().trim();
            String url = txtItemUrl.getText().trim();
            
            if(!name.isEmpty() && !price.isEmpty() && !url.isEmpty()) {
                // ADD_ITEM|Nome|Preco|URL
                out.println("ADD_ITEM|" + name + "|" + price + "|" + url);
                txtItemName.setText("");
                txtItemPrice.setText("");
                txtItemUrl.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Preencha Nome, Preço e URL!");
            }
        });
        
        btnStartAuction.addActionListener(e -> out.println("START_AUCTION"));
        btnAddTime.addActionListener(e -> out.println("ADD_TIME|5"));
        btnForceEnd.addActionListener(e -> out.println("FORCE_END_AUCTION"));
    }

    private void conectar() {
        String nome = JOptionPane.showInputDialog(this, "Bem-vindo ao BidRoom!\nDigite seu nome de usuário:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.trim().isEmpty()) {
            System.exit(0);
        }
        
        if (nome.equals("admin1207")) {
            bottomArea.remove(bidPanel);
            bottomArea.add(adminPanel, BorderLayout.CENTER);
            bottomArea.revalidate();
            bottomArea.repaint();
            setTitle("BidRoom - PAINEL DO LEILOEIRO");
            appendChat("[SISTEMA] Autenticado como Leiloeiro Supremo!");
        }

        try {
            socket = new Socket("127.0.0.1", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + nome);

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
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor.");
            System.exit(0);
        }
    }

    private void carregarImagemDaWeb(String imageUrl) {
        // Zera a imagem antiga
        lblImage.setIcon(null);
        lblImage.setText("Carregando foto...");
        
        // FASE 13: Thread Paralela! Se a internet for lenta, a tela NÃO trava.
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);
                if (image != null) {
                    // Redimensiona pra caber no quadrado de 200x200
                    Image scaled = image.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);
                    
                    // Coloca na tela via SwingUtilities
                    SwingUtilities.invokeLater(() -> {
                        lblImage.setText(null);
                        lblImage.setIcon(icon);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> lblImage.setText("Imagem Inválida"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> lblImage.setText("Falha ao carregar"));
            }
        }).start();
    }

    private void processarMensagemServidor(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            String[] partes = mensagem.split("\\|", -1);
            String acao = partes[0];

            switch (acao) {
                case "AUCTION_START":
                    lblItemName.setText("Em leilão: " + partes[1]);
                    lblHighestBid.setText("Maior Lance Inicial: R$ " + partes[2]);
                    appendChat("🔔 O LEILÃO COMEÇOU! Item: " + partes[1]);
                    
                    // FASE 13: Recebemos a URL pelo protocolo e mandamos baixar!
                    if (partes.length >= 4) {
                        carregarImagemDaWeb(partes[3]);
                    }
                    break;
                case "TIME":
                    lblTimeLeft.setText("Tempo restante: " + partes[1] + "s");
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
                    // Apaga a imagem para o próximo item
                    lblImage.setIcon(null);
                    lblImage.setText("Aguardando próximo...");
                    break;
                case "CHAT":
                    appendChat("🗣️ [" + partes[1] + "]: " + partes[2]);
                    break;
                case "LOGIN_OK":
                case "INFO":
                case "ERROR":
                    String texto = mensagem.substring(acao.length() + 1);
                    appendChat("[" + acao + "] " + texto);
                    break;
                default:
                    appendChat("[Desconhecido] " + mensagem);
                    break;
            }
        });
    }

    private void enviarLance() {
        String valor = bidField.getText().trim();
        if (!valor.isEmpty()) {
            out.println("BID|" + valor);
            bidField.setText("");
        }
    }

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

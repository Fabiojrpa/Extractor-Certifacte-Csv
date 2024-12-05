import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.security.Provider;
import java.security.Security;
import javax.security.auth.x500.X500Principal;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

public class CertificateExtractorApp {

    public static void main(String[] args) {
        // Criar a janela principal
        JFrame frame = new JFrame("Extrator de Certificados A1");
        frame.setSize(500, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Layout da interface
        JPanel panel = new JPanel(new GridLayout(4, 1));
        frame.add(panel);

        // Campo de entrada para o caminho do diretório
        JLabel label = new JLabel("Informe o caminho do diretório para salvar o arquivo CSV:");
        JTextField directoryPathField = new JTextField("C:\\Users\\SeuUsuario\\Documents");

        // Botão para gerar o arquivo
        JButton generateButton = new JButton("Gerar CSV");

        // Botão para encerrar manualmente
        JButton exitButton = new JButton("Sair");

        // Adicionar componentes ao painel
        panel.add(label);
        panel.add(directoryPathField);
        panel.add(generateButton);
        panel.add(exitButton);

        // Timer para monitorar inatividade (2 minutos = 120.000 ms)
        Timer inactivityTimer = new Timer(120000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Encerra completamente o programa sem exibir mensagens de confirmação
                System.exit(0);
            }
        });
        // Iniciar o timer
        inactivityTimer.start();

        // Ação ao clicar no botão "Gerar CSV"
        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inactivityTimer.stop(); // Parar o timer ao clicar no botão
                String directoryPath = directoryPathField.getText();
                if (directoryPath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Por favor, informe o caminho do diretório.", "Erro", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Adicionar o nome fixo do arquivo ao caminho fornecido
                    String filePath = directoryPath + File.separator + "certificados.csv";
                    try {
                        generateCSV(filePath);
                        JOptionPane.showMessageDialog(frame, "Arquivo CSV gerado com sucesso em:\n" + filePath, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        frame.dispose(); // Fechar a janela após o sucesso
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Erro ao gerar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                }
            }
        });

        // Ação ao clicar no botão "Sair"
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inactivityTimer.stop(); // Parar o timer ao clicar no botão
                frame.dispose(); // Fechar a janela manualmente
            }
        });

        // Exibir a janela
        frame.setVisible(true);
    }

    // Método para gerar o arquivo CSV
    private static void generateCSV(String filePath) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {
            // Cabeçalho das colunas
            writer.write("Razao Social,CNPJ,Data de Validade\n");

            // Carregar o repositório de certificados Pessoais do Windows
            Provider provider = Security.getProvider("SunMSCAPI");
            KeyStore keyStore = KeyStore.getInstance("Windows-MY", provider);
            keyStore.load(null, null);

            // Listar certificados no repositório
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                // Obter o certificado
                Certificate certificate = keyStore.getCertificate(alias);

                if (certificate instanceof X509Certificate) {
                    X509Certificate x509Certificate = (X509Certificate) certificate;

                    // Extrair os dados do certificado
                    String[] dadosCertificado = extractCertData(x509Certificate);
                    String razaoSocial = dadosCertificado[0];
                    String cnpj = dadosCertificado[1];
                    String validade = dadosCertificado[2];

                    // Escrever os dados no arquivo CSV
                    writer.write(razaoSocial + "," + cnpj + "," + validade + "\n");
                }
            }
        }
    }

    // Método para extrair a razão social, CNPJ e a data de validade
    private static String[] extractCertData(X509Certificate certificate) {
        // Obter o DN do sujeito
        X500Principal subject = certificate.getSubjectX500Principal();
        String subjectDN = subject.getName();

        // Extrair razão social, CNPJ e validade
        String razaoSocial = extractCN(subjectDN);
        String cnpj = extractCNPJ(subjectDN);
        String validade = extractValidity(certificate);

        return new String[]{razaoSocial, cnpj, validade};
    }

    // Extrair o CN (Razão Social)
    private static String extractCN(String subjectDN) {
        String[] parts = subjectDN.split(",");
        for (String part : parts) {
            if (part.startsWith("CN=")) {
                return part.substring(3).split(":")[0];
            }
        }
        return "Não encontrado";
    }

    // Extrair o CNPJ
    // Extrair o CNPJ com um ponto no início
    private static String extractCNPJ(String subjectDN) {
        String[] parts = subjectDN.split(",");
        for (String part : parts) {
            if (part.contains(":")) {
                String[] subParts = part.split(":");
                if (subParts.length > 1) {
                    // Adiciona o ponto no início do CNPJ e retorna
                    return "." + subParts[1];
                }
            }
        }
        return "Não encontrado";
    }

    // Extrair a validade
    private static String extractValidity(X509Certificate certificate) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        return sdf.format(certificate.getNotAfter());
    }
}

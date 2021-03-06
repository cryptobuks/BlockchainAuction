package Client;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.json.*;

/**
 *
 * Client Class
 * <br>
 * Cliente no leilão
 * 
 */
public class Client {
    
    //Variveis relativas ao cliente e auxiliares
    private static String type = "";
    private static String messageType = "";
    private static String sendMsg = "";
    private static JSONObject sendObj=null;
    private static DatagramSocket clientSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receivebuffer = new byte[30000];
    private static String serverData;
    private static JSONObject rec;  
    private static boolean bid = false;
    private static boolean simetricKeyGen = false;
    
    //Variveis relativas ao Manager
    private static SecretKey secretKeyManager = null;
    private static PublicKey managerKey = null;
    private static Certificate managerCert = null;
    
    //Variveis relativas ao Repository
    private static SecretKey secretKeyRepository = null;
    private static PublicKey repositoryKey = null;
    private static Certificate repositoryCert = null;


    
    public static void main(String[] args) throws SocketException, IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, GeneralSecurityException {
        
        //Security
        //Inicia configurações relativas ao CC.
        SecurityClient.init();
        
        //Fica á espera de pacotes
        boolean exit = false;
        BufferedReader br =new BufferedReader(new InputStreamReader(System.in));
        InetAddress IP = InetAddress.getByName("127.0.0.1");
        clientSocket = new DatagramSocket();
        byte[] sendbuffer;
        
        //Inicia troca de certificados com o servidor
        initCommunicationManager(clientSocket,managerKey);
        initCommunicationRepository(clientSocket,repositoryKey);
        int clientID = 0;
        
        //Convert to json, cuidado com a maneria como se constroi a mensagem
        sendMsg = "{ \"Type\":\"clientID\"}";
        
        rec = messageManager(clientSocket,sendMsg);
                        
        //Devolve ID do cliente
        for(int i=1; i < rec.names().length();i++){
            List tmp = rec.names().toList();
            clientID = rec.getInt("clientID");
            System.out.print("\nO seu ID é " + clientID);
        }
        
        //Lista de tarefas possíveis
        while(!exit)
        {
            String auctionName = "";
            int auctionID = 0;
            int userID = 0;
            double amount = 0;
            
            System.out.print("\n\nEscolha uma opção");
            System.out.print("\n1-Criar leilão");
            System.out.print("\n2-Terminar leilão");
            System.out.print("\n3-Ver leilões ativos");
            System.out.print("\n4-Ver leilões concluídos");
            System.out.print("\n5-Ver licitações de um leilão");
            System.out.print("\n6-Ver licitações feitas por um cliente");
            System.out.print("\n7-Ver resultado de um leilão");
            System.out.print("\n8-Validar recibo");
            System.out.print("\n9-Licitar em leilão");
            System.out.print("\n0-Sair");
            System.out.print("\nOpção: ");
            
            String option = br.readLine();
            switch (option){
                    
                    //Cria leilão
                    case "1":
                        messageType = "cta";
                        boolean firstTime = true;
                        while(auctionName.equals("")){
                            if(!firstTime){
                                System.out.print("\nERRO! O nome não pode estar vazio!");
                            }
                            System.out.print("\nNome: ");
                            auctionName = br.readLine();
                            firstTime = false;
                        }
                        int auctionTime = 0;
                        while(auctionTime == 0){
                            System.out.print("\nDuração(minutos): ");
                            try{
                                auctionTime = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        
                        String auctionType = "";
                        int optionType = 0;
                        while(auctionType.equals("")){
                            System.out.print("\nTipo");
                            System.out.print("\n1-Leilão Inglês");
                            System.out.print("\n2-Leilão Cego");
                            System.out.print("\nOpção: ");
                            try{
                                optionType = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                            }
                            switch (optionType){
                                case 1:
                                    auctionType = "english";
                                    break;
                                case 2:
                                    auctionType = "blind";
                                    break;
                                default:
                                    System.out.print("\nERRO! Opção inválida!");
                                    break;
                            }
                        }
                        //Convert to json, cuidado com a maneria como se constroi a menSagem
                        sendMsg = "{ \"Type\":"+messageType+",\"clientID\":"+clientID+",\"Name\":\""+auctionName+ "\",\"Time\":"+auctionTime+",\"AuctionType\":"+auctionType+"}";
                        
                        //Manda mensagem para o manager com as informações á cerca do leilão a ser criado
                        rec = messageManager(clientSocket, sendMsg);
                        
                        //Output
                        type = rec.getString(("Type"));
                        switch(type){
                            case "SUCCESS":
                                System.out.print("\nLeilão criado.");
                                break;
                            default:
                                System.out.print("\nERRO!");
                                break;
                        }
                        break;
                        
                    //Termina um leilão ativo
                    case "2":
                        //Verifica se existem leilões ativos
                        boolean existAuctions = getActiveAuctionsByClient(clientID);
                        
                        if(existAuctions){
                            messageType = "tta";
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            
                            sendMsg = "{ \"Type\":"+messageType + ",\"ClientID\":"+clientID + ",\"AuctionID\":"+auctionID+"}";
                            
                            //Manda mensagem ao repositorio com as informações do leilão a terminar
                            rec = messageRepository(clientSocket,sendMsg);

                            //Output do resultado do termino
                            String type = rec.getString(("Type"));
                            switch(type){
                                case "SUCCESS":
                                    if(rec.getJSONArray("SUCCESS").isEmpty()){
                                        System.out.print("\nERRO! Leilão não encontrado!");           
                                    }
                                    else{
                                        System.out.print("\nLeilão terminado com sucesso.");
                                        System.out.println("\nLista de bids: " + rec.getJSONArray("SUCCESS"));
                                        System.out.println("Blockchain e bid associado: "+rec.getJSONArray("Chain"));
                                    }
                                break;
                            }
                        }
                        break;
                        
                    //Devolve leilões ativos
                    case "3":
                        getActiveAuctions();
                        break;
                    
                    //Devolve leilões inativos
                    case "4":
                        getInactiveAuctions();
                        break;
                    
                    //Ver licitações de um leilão
                    case "5":
                        existAuctions = false;
                        int optionActiveInactive = 0;
                        
                        //Escolha entre ativos ou inativos
                        while(optionActiveInactive == 0){
                            System.out.print("\nEscolha entre");
                            System.out.print("\n1-Leilões ativos");
                            System.out.print("\n2-Leilões inativos");
                            System.out.print("\nOpção: ");
                            try{
                                optionActiveInactive = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        
                        //Devolução dos leiloes do tipo
                        switch (optionActiveInactive){
                            case 1:
                                existAuctions = getActiveAuctions();
                                break;
                            case 2:
                                existAuctions = getInactiveAuctions();
                                break;
                            default:
                                System.out.print("\nERRO! Opção inválida! De volta ao menu inicial.");
                                break;
                        }
                        
                        if(existAuctions){
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                        System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            messageType = "gba";
                            sendMsg = "{ \"Type\":"+messageType+ ",\"AuctionID\":"+auctionID+"}";
                            
                            //Manda mensagem ao repositorio com a informação sobre o leilão
                            rec = messageRepository(clientSocket,sendMsg);

                            //Output das bids feitas
                            type = rec.getString(("Type"));
                            switch(type){
                                case "Bids":
                                    if(rec.getJSONArray("Bids").length() != 0){
                                        for(int i=0; i < rec.getJSONArray("Bids").length();i++){
                                            System.out.print("\nLicitação" + i + ": " + rec.getJSONArray("Bids").getDouble(i));
                                        }
                                    }else
                                    {
                                        System.out.print("\nNão foi feita nenhuma licitação.");
                                    }
                                break;
                            }
                        }
                        break;
                       
                    //Ver bids feitos por um cliente
                    case "6":
                        //ID do cliente 
                        messageType = "gbc";
                        userID = 0;
                        while(userID == 0){
                            System.out.print("\nInsira o ID do utilizador: ");
                            try{
                                userID = Integer.parseInt(br.readLine());
                            }catch(NumberFormatException e){
                                System.out.print("\nERRO! Insira um número!");
                            }
                        }
                        sendMsg = "{ \"Type\":"+messageType+ ",\"ClientID\":"+userID+"}";

                        rec = messageRepository(clientSocket,sendMsg);
                        
                        //Output das bids feitas
                        type = rec.getString(("Type"));
                        switch(type){
                            case "Bids":
                                if(rec.getJSONArray("Values").length() != 0){
                                    for(int i=0; i < rec.getJSONArray("Values").length();i++){
                                        System.out.print("\nLicitação" + i + ": " + rec.getJSONArray("Values").getDouble(i));
                                    }
                                }else
                                {
                                    System.out.print("\nEste utilizador não existe ou não fez nenhuma licitação.");
                                }
                            break;
                        }
                        break;
                        
                    //Ver resultado de um leilão
                    case "7":
                        //Devolve lista dos leiloes terminados
                        existAuctions = getInactiveAuctions();
                        
                        if(existAuctions){
                            auctionID = 0;
                            while(auctionID == 0){
                                System.out.print("\nInsira o ID do leilão desejado: ");
                                try{
                                    auctionID = Integer.parseInt(br.readLine());
                                }catch(NumberFormatException e){
                                    System.out.print("\nERRO! Insira um número!");
                                }
                            }
                            messageType = "coa";
                            sendMsg = "{ \"Type\":"+messageType+ ",\"AuctionID\":"+auctionID+"}";                            
                            sendObj = new JSONObject(sendMsg);
                            
                            //Informa repo sobre as informações que quer
                            rec = messageRepository(clientSocket,sendMsg);

                            //Devolve resultado de um leilão e informações associadas
                            type = rec.getString(("Type"));
                            switch(type){
                                case "AuctionOutcome":
                                    if(!(rec.getInt("AuctionID") == 0)){
                                        if(rec.getString("Bought").equals("true")){
                                            System.out.print("\nNome do leilão: " + rec.getString("AuctionName"));
                                            System.out.print("\nID do leilão: " + rec.getInt("AuctionID"));
                                            System.out.print("\nID do Comprador: " + rec.getInt("BuyerID"));
                                            System.out.print("\nValor: " + rec.getDouble("Value") + "€");
                                        }else{
                                            System.out.print("\nNome do leilão: " + rec.getString("AuctionName"));
                                            System.out.print("\nID do leilão: " + rec.getInt("AuctionID"));
                                            System.out.print("\nNinguém comprou.");
                                        }
                                    }else{
                                        System.out.print("\nERRO! O leilão com ID " + auctionID + " não existe!");
                                    }
                                break;
                            }
                        }
                        break;
                        
                    //Validar recibo 
                    case "8":
                        
                        String receiptTextRead = "";
                        String fileNameToRead;
                        System.out.print("\nInsira o nome do ficheiro do recibo a validar: ");
                        fileNameToRead = br.readLine();
                        try {
                            File textFileRead = new File(fileNameToRead + ".txt");

                            FileReader fr = new FileReader(textFileRead);
                            BufferedReader buffReader = new BufferedReader(fr);
                            String line;
                            while((line = buffReader.readLine()) != null){
                                receiptTextRead += line + "\n";
                            }
                            buffReader.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        
                        String[] receiptParts = receiptTextRead.split("\n");
                        String receiptAuctionId = receiptParts[0].replace("Id do Leilão: ", "");
                        String receiptAmountId = receiptParts[1].replace("Quantia: ", "");
                        String receiptClientId = receiptParts[2].replace("Id do Cliente: ", "");
                        
                        String receiptSignature = receiptParts[4].replace("Assinatura digital: ", "");
                        
                        String[] Signature = receiptSignature.substring(1, receiptSignature.length()-1).split(",");
                        byte[] Sign = new byte[Signature.length];
                        for(int i=0; i<Signature.length; i++){
                            Sign[i] =  Byte.parseByte(Signature[i]);
                        }
                        
                        String toVerify = ",\"AuctionID\":"+receiptAuctionId+",\"Amount\":"+receiptAmountId+",\"ClientID\":"+receiptClientId+"}";
                        
                        if(SecurityClient.verifySign(Sign,toVerify.getBytes(),repositoryCert)){
                            System.out.println("Receipt has been validated.");
                        }else{
                            System.out.println("Receipt cannot be validated.");
                        }
                        
                        break;
                        
                    //Licita num leilão
                    case "9":
                        messageType = "bid";
                        sendMsg = "{ \"Type\":"+messageType+"}";
                        
                        rec = messageRepository(clientSocket,sendMsg);
                                    
                        //Obter puzzle
                        Gson gson = new Gson();
                        JSONArray data = rec.getJSONArray("Data");
                        byte[] puzzle = gson.fromJson(data.toString(), byte[].class);
                        
                        //Resolver puzzle
                        byte[] solution = SecurityClient.puzzleSolve(puzzle); 
                        String json = ""+gson.toJson(solution);
                        String sendMsgRep = "{ \"Type\":\"Puzzle\",\"Data\":"+json+"}";
                        rec = messageRepository(clientSocket,sendMsgRep);
                        
                        //Obter resposta da solução
                        String result = rec.getString("Result");
                        if(result.equals(("SUCESS"))) bid = true;
                        
                        if(bid){
                            existAuctions = getActiveAuctions();

                            if(existAuctions){
                                messageType = "bid";
                                auctionID = 0;
                                while(auctionID == 0){
                                    System.out.print("\nInsira o ID do leilão desejado: ");
                                    try{
                                        auctionID = Integer.parseInt(br.readLine());
                                    }catch(NumberFormatException e){
                                        System.out.print("\nERRO! Insira um número!");
                                    }
                                }
                                amount = 0.0;
                                while(amount == 0.0){
                                    System.out.print("\nValor(€): ");
                                    try{
                                        amount = Double.parseDouble(br.readLine());
                                    }catch(NumberFormatException e){
                                        System.out.print("\nERRO! Insira um número! (Usar \".\" em vez de \",\" para separar a parte inteira da parte decimal)");
                                    }
                                }

                                String toSign = ",\"AuctionID\":"+auctionID+",\"Amount\":"+amount+",\"ClientID\":"+clientID+"}";
                                String signed = ""+gson.toJson(SecurityClient.sign(toSign.getBytes()));
                                sendMsg = "{ \"Type\":"+messageType+",\"Sign\":"+signed+""+toSign+"}";
                                
                                rec = messageRepository(clientSocket,sendMsg);

                                type = rec.getString(("Type"));
                                System.out.println("\n\ntype: " + type);
                                switch(type){
                                    case "ret":
                                        for(int i=1; i < rec.names().length();i++){
                                            List tmp = rec.names().toList();
                                            System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                                        }
                                        break;
                                    
                                    //Produção de um recibo
                                    case "Receipt":
                                        String receiptText = "";
                                        String fileName;
                                        System.out.print("\nInsira o nome do ficheiro para guardar como recibo: ");
                                        fileName = br.readLine();
                                        
                                        int receiptAuctionID = rec.getInt("AuctionID");
                                        double receiptAmount = rec.getDouble("Amount");
                                        int receiptClientID = rec.getInt("ClientID");
                                        String signature = rec.getString("Sign");
                                        
                                        receiptText += "Id do Leilão: " + receiptAuctionID;
                                        receiptText += "\nQuantia: " + receiptAmount;
                                        receiptText += "\nId do Cliente: " + receiptClientID;
                                        receiptText += "\n\nAssinatura digital: " + signature;

                                        try {
                                            File newTextFile = new File(fileName + ".txt");

                                            FileWriter fw = new FileWriter(newTextFile);
                                            fw.write(receiptText);
                                            fw.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    
                                    default:
                                        for(int i=1; i < rec.names().length();i++){
                                        List tmp = rec.names().toList();
                                        System.out.print("\nServer: " + rec.getString(tmp.get(i).toString()));
                                        }
                                        break;
                                }
                            }
                        }else{
                            System.out.println("ERROR : Solve the puzzle first");
                        }
                                         
                        break;
                        
                    case "0":
                        sendMsg = "exit";
                        exit = true;
                        //messageManager(clientSocket,sendMsg);
                        break;
                    default:
                        System.out.print("\nERRO! Opção inválida!");
            }
        }
        
        clientSocket.close();
        
    }
      
    /**
     * Imprime leilões feitos por um cliente
     * 
     * @param clientID ID Cliente
     * @return true no caso de sucesso
     */
    private static boolean getActiveAuctionsByClient(int clientID) throws IOException, UnknownHostException, GeneralSecurityException{
        messageType = "lgaClient";
        sendMsg = "{ \"Type\":"+messageType+",\"ClientID\":" + clientID + "}";

        rec = messageRepository(clientSocket,sendMsg);

        type = rec.getString(("Type"));
        switch(type){
            case "ActiveAuctions":
                int activeAuctionTotal = rec.getInt("ActiveAuctionsTotal");
                if(activeAuctionTotal == 0){
                    System.out.print("\nNão existem leilões ativos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões ativos:");
                    for(int i=1; i<=activeAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
    }
    
    /**
     * Imprime leilões ativos
     * 
     * @return true no caso de sucesso
     */
    private static boolean getActiveAuctions() throws IOException, UnknownHostException, GeneralSecurityException{
        messageType = "lga";
        sendMsg = "{ \"Type\":"+messageType+"}";

        rec = messageRepository(clientSocket,sendMsg);

        type = rec.getString(("Type"));
        switch(type){
            case "ActiveAuctions":
                int activeAuctionTotal = rec.getInt("ActiveAuctionsTotal");
                if(activeAuctionTotal == 0){
                    System.out.print("\nNão existem leilões ativos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões ativos:");
                    for(int i=1; i<=activeAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
    }
    
    /**
     * Imprime leilões inativos
     * 
     * @param true no caso de sucesso
     */
    private static boolean getInactiveAuctions() throws IOException, UnknownHostException, GeneralSecurityException{
        messageType = "lta";
        sendMsg = "{ \"Type\":"+messageType+"}";

        rec = messageRepository(clientSocket,sendMsg);

        type = rec.getString(("Type"));
        switch(type){
            case "InactiveAuctions":
                int inactiveAuctionTotal = rec.getInt("InactiveAuctionsTotal");
                if(inactiveAuctionTotal == 0){
                    System.out.print("\nNão existem leilões concluídos.");
                    return false;
                    }
                else{
                    System.out.print("\nLeilões concluídos:");
                    for(int i=1; i<=inactiveAuctionTotal; i++){
                        System.out.print("\n" + rec.getInt("AuctionID" + i) + "-" + rec.getString("AuctionName" + i));
                    }
                    return true;
                }
            default:
                System.out.print("\nERRO!");
                return false;
        }
    }
    
     /**
     * Função que manda mensagem para o manager.
     * 
     * @param clientSocket Socket do Cliente
     * @param msg Mensagem a enviar ao manager
     */
    private static JSONObject messageManager(DatagramSocket clientSocket, String msg) throws UnknownHostException, IOException, GeneralSecurityException{
        sendObj = new JSONObject(msg);
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer;
        
        if(sendObj.getString("Type").equals("clientID")){
            sendbuffer = msg.toString().getBytes();        
            DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
            clientSocket.send(sendPacket);
        }else{
            //Gerar IV
            Gson gson = new Gson();
            byte[] initializationVector = new byte[16];
            SecureRandom secRan = new SecureRandom(); 
            secRan.nextBytes(initializationVector);

            //Encriptrar
            sendbuffer = msg.getBytes();
            sendbuffer = SecurityClient.encryptMsgSym(sendbuffer, secretKeyManager,initializationVector);

            byte [] sendbufferIV = new byte[sendbuffer.length+initializationVector.length];
            System.arraycopy(initializationVector, 0, sendbufferIV, 0, initializationVector.length);
            System.arraycopy(sendbuffer, 0, sendbufferIV, initializationVector.length, sendbuffer.length);

            DatagramPacket sendPacket = new DatagramPacket(sendbufferIV, sendbufferIV.length,ServerIP ,ServerPort);
            clientSocket.send(sendPacket);
        }
        
        if(msg.equals(("end"))){
            clientSocket.close();
            System.exit(1);
        }
        
        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        
        //Decriptar
        byte[] receivedBytes = receivePacket.getData();
        byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
        byte[] msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacket.getLength()); //Msg igual
                
        //Decriptar mensagem
        if(receivePacket.getPort()==9876) msgEnc = SecurityClient.decryptMsgSym(msgEnc,secretKeyRepository, IV);
        else  msgEnc = SecurityClient.decryptMsgSym(msgEnc,secretKeyManager, IV);

        String serverData = new String(msgEnc); 
        
        return new JSONObject(serverData);
    }
    
    /**
     * Função que manda mensagem para o repositorio.
     * 
     * @param clientSocket Socket do Cliente
     * @param msg Mensagem a enviar ao manager
     */
    private static JSONObject messageRepository(DatagramSocket clientSocket, String msg) throws UnknownHostException, IOException, GeneralSecurityException{
        sendObj = new JSONObject(msg);
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        
        //Gerar IV
        Gson gson = new Gson();
        byte[] initializationVector = new byte[16];
        SecureRandom secRan = new SecureRandom(); 
        secRan.nextBytes(initializationVector);

        //Encriptrar
        sendbuffer = msg.getBytes();
        sendbuffer = SecurityClient.encryptMsgSym(sendbuffer, secretKeyRepository,initializationVector);
        
        byte [] sendbufferIV = new byte[sendbuffer.length+initializationVector.length];
        System.arraycopy(initializationVector, 0, sendbufferIV, 0, initializationVector.length);
        System.arraycopy(sendbuffer, 0, sendbufferIV, initializationVector.length, sendbuffer.length);
        
        DatagramPacket sendPacket = new DatagramPacket(sendbufferIV, sendbufferIV.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        if(msg.equals(("end"))){
            clientSocket.close();
            System.exit(1);
        }
        
        receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        
        //Decriptar
        byte[] receivedBytes = receivePacket.getData();
        byte[] IV = Arrays.copyOfRange(receivedBytes, 0, 16); //IV igual
        byte[] msgEnc = Arrays.copyOfRange(receivedBytes, 16, receivePacket.getLength()); //Msg igual
                
        //Decriptar mensagem
        if(receivePacket.getPort()==9876) msgEnc = SecurityClient.decryptMsgSym(msgEnc,secretKeyRepository, IV);
        else  msgEnc = SecurityClient.decryptMsgSym(msgEnc,secretKeyManager, IV);

        String serverData = new String(msgEnc); 
                
        return new JSONObject(serverData);
    }
    
    /**
     * Função que inicia a comunicação.
     * Começa por mandar uma mensagem a dizer init, de seguida espera a mensagem do servidor com o certificado, valida o certificado do servidor, manda o seu certificado e fica á espera da confirmação de validação por parte do servidor, quando recebida, ativa a possibildiade de geração de chave simetrica.
     * 
     * @param clientSocket Socket do Cliente
     * @param managerKey Chave publica do manager
     */
    private static void initCommunicationManager(DatagramSocket clientSocket, PublicKey managerKey) throws IOException, CertificateException, KeyStoreException, GeneralSecurityException{
        
        //Manda mensagem em plaintext para o manager a informar que quer iniciar a comunicação
        String sendMsg = "{ \"Type\":init}";
        JSONObject sendObj = new JSONObject(sendMsg);
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer;
        sendbuffer = sendObj.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        //Manager responde com o certificado do servidor 
        byte[] receivebuffer = new byte[30000];
        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        
        byte[] certificateBytes = receivePacket.getData();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
        managerCert = certificate;
        
        //Valida certificado, guarda-lo e envia certificado do cliente
        try{
            certificate.checkValidity();
            managerKey = certificate.getPublicKey();
            messageManagerCert(clientSocket, SecurityClient.getCCCertificate());
        }catch(CertificateExpiredException | CertificateNotYetValidException e){
            System.out.println("Certificate not valid ");
        }
        
        //Recebe informação de validação do certificado do cliente por parte do repo
        clientSocket.receive(receivePacket);
        String ReceivedMsg = new String(receivePacket.getData());
        JSONObject recMsg = new JSONObject(ReceivedMsg);
        System.out.println("\nServer : " + recMsg.toString());
        String type = recMsg.getString(("Type"));
        if(type.equals("cert")){
           String msg = recMsg.getString(("Message"));
           if(msg.equals("Valid certificate")){
               simetricKeyGen = true;
           }
        }
        
        //Ver algoritmos a usar
        if(simetricKeyGen){
            try {
                secretKeyManager= KeyGenerator.getInstance("AES").generateKey();
   
                //Assinar chave
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                byte[] digest = messageDigest.digest(secretKeyManager.getEncoded());
                
                //Encriptar
                byte[] symKey = SecurityClient.encryptMsg(digest,SecurityClient.getPrivateKey());

                //Envia chave simetrica
                Gson gson = new Gson();
                String json = ""+gson.toJson(symKey);
                String json2 = ""+gson.toJson(secretKeyManager.getEncoded());
                sendMsg = "{ \"Sym\":"+json+",\"Data\":"+json2+"}";
                sendObj = new JSONObject(sendMsg);                               
            } catch (NoSuchAlgorithmException ex) {
                System.out.println("Impossible to generate Symetric Key.");
            }
        }
        
        //Envia chave simetrica encritpada com a chave privada do cliente para o qual o repo tem a chave publica
        sendbuffer = sendObj.toString().getBytes();
        sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
    }
    
     /**
     * Função que inicia a comunicação.
     * Começa por mandar uma mensagem a dizer init, de seguida espera a mensagem do servidor com o certificado, valida o certificado do servidor, manda o seu certificado e fica á espera da confirmação de validação por parte do servidor, quando recebida, ativa a possibildiade de geração de chave simetrica.
     * 
     * @param clientSocket Socket do Cliente
     * @param repositoryKey Chave publica do repositorio
     */
    private static void initCommunicationRepository(DatagramSocket clientSocket, PublicKey repositoryKey) throws IOException, CertificateException, KeyStoreException, GeneralSecurityException{
       
        //Manda mensagem em plaintext para o repositorio a informar que quer iniciar a comunicação
        String sendMsg = "{ \"Type\":init}";
        JSONObject sendObj = new JSONObject(sendMsg);  
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        sendbuffer = sendObj.toString().getBytes();        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
        
        //Repositorio responde com o certificado do servidor
        byte[] receivebuffer = new byte[30000];
        DatagramPacket receivePacket = new DatagramPacket(receivebuffer, receivebuffer.length);
        clientSocket.receive(receivePacket);
        
        byte[] certificateBytes = receivePacket.getData();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate)(certificateFactory.generateCertificate( new ByteArrayInputStream(certificateBytes)));
        repositoryCert = certificate;
        
        //Valida certificado, guarda-lo e envia certificado do cliente
        try{
            certificate.checkValidity();
            repositoryKey = certificate.getPublicKey();
            messageRepositoryCert(clientSocket, SecurityClient.getCCCertificate());
        }catch(CertificateExpiredException | CertificateNotYetValidException e){
            System.out.println("Certificate not valid ");
        }
        
        //Recebe informação de validação do certificado do cliente por parte do repo
        clientSocket.receive(receivePacket);
        String ReceivedMsg = new String(receivePacket.getData());
        JSONObject recMsg = new JSONObject(ReceivedMsg);
        System.out.println("\nServer : " + recMsg.toString());
        String type = recMsg.getString(("Type"));
        if(type.equals("cert")){
           String msg = recMsg.getString(("Message"));
           if(msg.equals("Valid certificate")){
               simetricKeyGen = true;
           }
        }
        
        //Gera chave simétrica
        if(simetricKeyGen){
            try {
                secretKeyRepository = KeyGenerator.getInstance("AES").generateKey();
                
                //Assinar chave
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                byte[] digest = messageDigest.digest(secretKeyRepository.getEncoded());
                
                //Encriptar
                byte[] symKey = SecurityClient.encryptMsg(digest,SecurityClient.getPrivateKey());
                
                //Envia chave simetrica
                Gson gson = new Gson();
                String json = ""+gson.toJson(symKey);
                String json2 = ""+gson.toJson(secretKeyRepository.getEncoded());
                sendMsg = "{ \"Sym\":"+json+",\"Data\":"+json2+"}";
                sendObj = new JSONObject(sendMsg);   
            } catch (NoSuchAlgorithmException ex) {
                System.out.println("Impossible to generate Symetric Key.");
            }
        }
        
        //Envia chave simetrica encritpada com a chave privada do cliente para o qual o repo tem a chave publica
        sendbuffer = sendObj.toString().getBytes();        
        sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length,ServerIP ,ServerPort);
        clientSocket.send(sendPacket);        
        
    }
    
    /**
     * Envia o certificado do cliente para o Servidor Manager
     * 
     * @param clientSocket Socket do Cliente
     * @param cert Certificado do CC
     */
    private static void messageManagerCert(DatagramSocket clientSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9877;
        byte[] sendbuffer;
        sendbuffer = cert.getEncoded();
        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
    }
    
    /**
     * Envia o certificado do cliente para o Servidor Repositorio
     * 
     * @param clientSocket Socket do Cliente
     * @param cert Certificado do CC
     */
    private static void messageRepositoryCert(DatagramSocket clientSocket, X509Certificate cert) throws UnknownHostException, IOException, CertificateEncodingException{
        InetAddress ServerIP = InetAddress.getByName("127.0.0.1");
        int ServerPort = 9876;
        byte[] sendbuffer;
        sendbuffer = cert.getEncoded();
        
        DatagramPacket sendPacket = new DatagramPacket(sendbuffer, sendbuffer.length, ServerIP ,ServerPort);
        clientSocket.send(sendPacket);
    }
    
    /**
     * Convert um inteiro num byte array
     * 
     * @param i Inteiro
     * @return conversão para byte array
     * 
     * Adaptado de stackoverflow
     */
    private static byte[] intToByteArray ( final int i ) throws IOException {      
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(i);
        dos.flush();
        return bos.toByteArray();
    }

    /**
     * Convert um byte array em um inteiro
     * 
     * @param i Byte array
     * @return conversão para inteiro
     * 
     * Adaptado de stackoverflow
     */
    private int convertByteArrayToInt(byte[] intBytes){
        ByteBuffer byteBuffer = ByteBuffer.wrap(intBytes);
        return byteBuffer.getInt();
    }    
}

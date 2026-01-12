package com.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorTCP implements Runnable {
    private ServerSocket serverSocket;
    private int portaOuvindo; // A porta real que o SO escolheu
    private long timestamp;
    // Controle de região crítica
    private boolean running = true;
    private GerenciadorParceiros gerenciador = GerenciadorParceiros.getInstancia();
    private List<Mensagem> filaRequisicoes = new CopyOnWriteArrayList<>();
    private Map<Integer, Set<Integer>> respostasRecebidas;

    public ServidorTCP() {

        try {
            // Passar 0 diz ao SO: "Escolha uma porta livre para mim"
            this.serverSocket = new ServerSocket(0);
            
            // IMPORTANTE: Pegamos a porta que foi sorteada
            this.portaOuvindo = this.serverSocket.getLocalPort();
            this.timestamp = 0;
            this.respostasRecebidas = new ConcurrentHashMap<>();
            
            
            System.out.println(">>> Servidor TCP iniciado automaticamente na porta: " + portaOuvindo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPorta() {
        return portaOuvindo;
    }
    
    public String getIp() {
        return serverSocket.getInetAddress().getHostAddress();
    }
  
    @Override
    public void run() {
        // Loop infinito aceitando conexões (igual ao que discutimos antes)
        try {
            while (running) {
                Socket cliente = serverSocket.accept();
                System.out.println(">>> Recebi conexão de " + cliente.getInetAddress().getHostAddress());
                processarMensagem(cliente);
                
            }
        
        } catch(SocketException ex){
            if(running){
                ex.printStackTrace();
            }
            else{
                return; // Servidor foi encerrado normalmente
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void solicitarEntradaRegiaoCritica()
    {
        getMeuEstado().setEstado(EstadoDoProcesso.ESPERANDO_REPLIES);
        int processoId = getMeuEstado().getId();
       //cria uma mensagem Request com região.
        Mensagem mensagem = new Mensagem (TipoMensagem.REQUEST, new Requisicao(getMeuEstado().getId()));
        //Adicionar  própria requisição na fila.
        timestamp = mensagem.getTimestamp();
        synchronized (filaRequisicoes){
            filaRequisicoes.add(mensagem);
        }
        enviarParaTodos(mensagem);

        System.out.println(">>> Processo " + processoId + " solicitou entrada na região crítica (ts=" + timestamp + ")");
        verificarEntradaRegiaoCritica();

    }

    public void enviarParaTodos(Mensagem mensagem)
    {
        List<Parceiro> parceiros = gerenciador.getLista();
        for (Parceiro p : parceiros) {
            if (p.getId() != getMeuEstado().getId()) {
               try {
                Socket socket = new Socket(p.getIp(), p.getPortaTCP());
                ObjectOutputStream dadosClienteOut = new ObjectOutputStream(socket.getOutputStream());
                dadosClienteOut.writeObject(mensagem);
                new Thread(() -> {
                    try {
                        ObjectInputStream dadosClienteIn = new ObjectInputStream(socket.getInputStream());
                        Mensagem resposta = (Mensagem) dadosClienteIn.readObject();
                        while(resposta != null){
                            if(resposta != null && resposta.getTipo() == TipoMensagem.OK){
                                tratarOk(resposta);
                                socket.close();
                                break;
                            }
                            if(resposta != null && resposta.getTipo() == TipoMensagem.WAIT){
                                resposta = (Mensagem) dadosClienteIn.readObject();
                            }
                        }
                    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
               } catch (UnknownHostException e) {
                e.printStackTrace();
               } catch (IOException e) {
                e.printStackTrace();
               }
            }
        }
        System.out.println(">>> Mensagem enviada para todos os parceiros: " + mensagem.toString());
    }

    private void processarMensagem(Socket cliente)
    {

        try {
            ObjectInputStream dadosClienteIn = new ObjectInputStream(cliente.getInputStream());
            ObjectOutputStream dadosClienteOut = new ObjectOutputStream(cliente.getOutputStream());
            
            if( dadosClienteIn == null || dadosClienteOut == null) {
                System.out.println(">>> Dados do cliente nulos. Ignorando mensagem.");
                return;
            }
            Mensagem mensagem = (Mensagem) dadosClienteIn.readObject();
            
            System.out.println(">> Processo " + mensagem.getProcessoId() + " enviou: " + mensagem.toString());
            switch (mensagem.getTipo()) {
                case REQUEST:
                    tratarRequest(mensagem,dadosClienteOut);
                break;
                case OK:
                    tratarOk(mensagem);
                break;
                default:
                    break;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private void tratarRequest(Mensagem mensagem, ObjectOutputStream dadosClienteOut)
    {
        Mensagem reply = null;
        switch (getMeuEstado().getEstado()) {
            case UTILIZANDO_ZONA_CRITICA:
                System.out.println(">>> Processo " + mensagem.getProcessoId() + " está na região crítica. Adiando REQUEST.");
                reply = new Mensagem(TipoMensagem.WAIT,mensagem.getRequisicao());
                synchronized (filaRequisicoes){
                    filaRequisicoes.add(mensagem);
                }
                break;

            case ESPERANDO_REPLIES:

                int processoIdMensagem = mensagem.getProcessoId();
                Date tsRequisicaoRemota = mensagem.getTimestampDate();
                Date tsMinhaRequisicao;
                synchronized (filaRequisicoes){
                    tsMinhaRequisicao = filaRequisicoes.stream()
                    .filter(r -> r.getProcessoId() == getMeuEstado().getId())
                    .findFirst()
                    .map(Mensagem::getRequisicao)
                    .map(Requisicao::getTimestamp)
                    .orElse(new Date());
                }

                boolean minhaPrioridade = (getMeuEstado().getEstado() == EstadoDoProcesso.UTILIZANDO_ZONA_CRITICA) ||
                                        (getMeuEstado().getEstado() == EstadoDoProcesso.ESPERANDO_REPLIES && 
                                        (tsMinhaRequisicao.before(tsRequisicaoRemota) || 
                                        (tsMinhaRequisicao.equals(tsRequisicaoRemota) && getMeuEstado().getId() < processoIdMensagem)));

                if (minhaPrioridade) {
                    reply = new Mensagem(TipoMensagem.WAIT,mensagem.getRequisicao());
                    System.out.println(">>> Adiando REPLY para Processo " + processoIdMensagem);
                } else {
                        reply = new Mensagem(TipoMensagem.OK, mensagem.getRequisicao());
                        System.out.println(">>> Enviando REPLY imediato para Processo " + processoIdMensagem);
                        System.out.println("Meu estado: " + getMeuEstado().getEstado());
                }
                break;

            case NAO_INTERESSADO:
                        reply = new Mensagem(TipoMensagem.OK, new Requisicao(getMeuEstado().getId()));
                        System.out.println(">>> Enviando REPLY imediato para Processo " + mensagem.getProcessoId());
                        System.out.println("Meu estado: " + getMeuEstado().getEstado());
                    
                break;
        
        }
        try {
            dadosClienteOut.writeObject(reply);
            dadosClienteOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        

       
    }
    
    private void tratarOk(Mensagem mensagem)
    {
        int processoId = getMeuEstado().getId();
        respostasRecebidas.computeIfAbsent(processoId, k -> new HashSet<>()).add(mensagem.getProcessoId());
        
        System.out.println(">>> Processo " + processoId + " recebeu OK de " + mensagem.getProcessoId() + 
                         " (" + respostasRecebidas.get(processoId).size() + "/" + gerenciador.getLista().size() + ")");
        verificarEntradaRegiaoCritica();
        
    }

    private void verificarEntradaRegiaoCritica()
    {
        if(getMeuEstado().getEstado() != EstadoDoProcesso.ESPERANDO_REPLIES) return;
        System.out.println(">>> Verificando se posso entrar na região crítica...");
        int processoId = getMeuEstado().getId();
        Set<Integer> respostas = respostasRecebidas.get(processoId);
        boolean recebeuTodasRespostas = (respostas != null && respostas.size() >= gerenciador.getLista().size()-1) ||
                                        (gerenciador.getLista().size() == 1); // Caso seja o único processo
        if(recebeuTodasRespostas)
        {
            getMeuEstado().setEstado(EstadoDoProcesso.UTILIZANDO_ZONA_CRITICA);
            System.out.println(">>> Processo " + processoId + " entrou na região crítica.");
            respostasRecebidas.remove(processoId);
            usarRegiaoCritica();
        }
        
    }

    private void usarRegiaoCritica(){
        try {
            System.out.println(">>> Processo " + getMeuEstado().getId() + " executando na região crítica...");
            Scanner scanner = new Scanner(System.in);
            // 1. Entrada do Usuário
            System.out.print("Digite a mensagem para adicionar ao log: ");
            String mensagemUsuario = scanner.nextLine();

            // --- INÍCIO DA SEÇÃO CRÍTICA (Seu algoritmo de Exclusão Mútua deve proteger daqui...) ---
            
            System.out.println("🔄 Lendo conteúdo atual da nuvem...");
            String conteudoAtual = GistClient.lerGist();
            System.out.println("   -> Conteúdo lido: " + conteudoAtual);

            // 2. Manipulação (Append)
            // Adiciona uma quebra de linha (\n) e a nova mensagem
            String novoConteudoCompleto = conteudoAtual + "\\n" + mensagemUsuario; 

            System.out.println("🔄 Enviando atualização...");
            GistClient.atualizarGist(novoConteudoCompleto);
            System.out.println("✅ Sucesso! Mensagem adicionada sem apagar a anterior.");
            sairRegiaoCritica();

        } catch (Exception e) {
            System.err.println("❌ Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void sairRegiaoCritica()
    {
        respostasRecebidas.remove(getMeuEstado().getId());
        getMeuEstado().setEstado(EstadoDoProcesso.NAO_INTERESSADO);
        filaRequisicoes.removeIf(m -> m.getProcessoId() == getMeuEstado().getId());
        synchronized(filaRequisicoes){
            if(filaRequisicoes.isEmpty()){
                System.out.println(">>> Nenhuma requisição pendente na fila.");
                return;
            }
            Mensagem primeira  = filaRequisicoes.stream()
                                        .sorted((r1, r2) -> {
                                            int cmp = r1.getRequisicao().getTimestamp().compareTo(r2.getRequisicao().getTimestamp());
                                            if (cmp == 0) {
                                                return Integer.compare(r1.getProcessoId(), r2.getProcessoId());
                                            }
                                            return cmp;
                                        })
                                        .findFirst()
                                        .orElse(null);
            Mensagem reply = new Mensagem(TipoMensagem.OK, primeira.getRequisicao());
            try {
                Socket socket = new Socket(primeira.getRequisicao().getIp(), primeira.getRequisicao().getPortaTCP());
                ObjectOutputStream dadosClienteOut = new ObjectOutputStream(socket.getOutputStream());
                dadosClienteOut.writeObject(reply);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(">>> Enviando REPLY imediato para Processo " + primeira.getProcessoId());
            filaRequisicoes.remove(primeira);
        }
       // Envia RELEASE para todos
        responderRequisicoesPendentes();
        System.out.println(">>> ✓ Processo " + getMeuEstado().getId() + " SAIU da região crítica!");

        responderRequisicoesPendentes();
    }

    private void responderRequisicoesPendentes()
    {
        int processoId = getMeuEstado().getId();
        synchronized(filaRequisicoes){
            for (Mensagem mensagem  : filaRequisicoes) {
                if (mensagem.getProcessoId() != processoId) {
                    Mensagem replyMensagem = new Mensagem(TipoMensagem.OK, mensagem.getRequisicao());
                    try {
                        Socket socket = new Socket(mensagem.getRequisicao().getIp(), mensagem.getRequisicao().getPortaTCP());
                        ObjectOutputStream dadosClienteOut = new ObjectOutputStream(socket.getOutputStream());
                        dadosClienteOut.writeObject(replyMensagem);
                        System.out.println(">>> Enviando REPLY imediato para Processo " + mensagem.getProcessoId());
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    public void encerrarSistema() {
        try {
            running = false;
            serverSocket.close();
            System.out.println(">>> Servidor TCP encerrado.");
            responderRequisicoesPendentes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Parceiro getMeuEstado()
    {
        return gerenciador.getLista().get(0);
    }
}

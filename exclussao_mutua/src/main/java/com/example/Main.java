package com.example;

import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        UUID uuid = UUID.randomUUID();
        int meuId = Math.abs(uuid.hashCode());
        // 1. INICIA O SERVIDOR TCP PRIMEIRO (Para pegar a porta)
        ServidorTCP servidor = new ServidorTCP();

        // 2. DESCOBRE QUAL PORTA O SO DEU
        int minhaPortaTCP = servidor.getPorta();
        GerenciadorParceiros gerenciador = GerenciadorParceiros.getInstancia();
        System.out.println("Sou o Processo ID: " + meuId + " na porta " + minhaPortaTCP);
        ServicoDescoberta descoberta = new ServicoDescoberta(meuId, minhaPortaTCP);
        gerenciador.getLista().add(new Parceiro(meuId, servidor.getIp(), minhaPortaTCP));

        new Thread(() -> {
            descoberta.iniciar();
            servidor.run(); // Inicia o servidor TCP em background
        }).start();
        
        System.out.println("Sistema iniciado. Aguardando parceiros...");
        Scanner scanner = new Scanner(System.in);
        while(true) {
           try {
            // Se eu NÃO estou interessado, eu ofereço o menu
            if (servidor.getMeuEstado().getEstado() == EstadoDoProcesso.NAO_INTERESSADO) {
                
                // Mostra o status atual
                System.out.printf("---%nParceiros ativos: %s%n", gerenciador.getLista().toString());
                System.out.println("Deseja escrever uma mensagem ou sair? (Sim/Não/Sair)");

                // VERIFICA SE TEM LINHA PARA LER ANTES DE TENTAR LER
                    if (scanner.hasNextLine()) {
                        String resposta = scanner.nextLine();

                        if (resposta.equalsIgnoreCase("Sim")) {
                            System.out.println("⏳ Aguardando permissão para entrar na Região Crítica...");
                            servidor.solicitarEntradaRegiaoCritica();
                            
                            // DICA: Aqui você provavelmente vai ficar preso esperando a permissão.
                            // O ideal seria que o 'solicitar' não travasse a main thread, 
                            // mas para fins acadêmicos, ok.
                        } 
                        else if (resposta.equalsIgnoreCase("Sair")) {
                            System.out.println("Encerrando...");
                            servidor.encerrarSistema();
                            descoberta.parar();
                            scanner.close();
                            break; // Sai do while
                        }
                    } else {
                        // Se cair aqui, é porque o console morreu ou foi fechado (EOF)
                        System.err.println("Console de entrada fechado. Encerrando.");
                        break;
                    }
                } else {
                    // Se eu ESTOU interessado ou NA região crítica, apenas espero
                    // Aqui o sleep faz sentido para não fritar a CPU enquanto espera o estado mudar
                    Thread.sleep(1000);
                    if(servidor.getMeuEstado().getEstado() == EstadoDoProcesso.ESPERANDO_REPLIES)
                        System.out.println("Processando estado: " + servidor.getMeuEstado().getEstado());
                }

            } catch (Exception e) {
                e.printStackTrace();
                break; // Evita loop infinito de erro
            }
        }

    }
}
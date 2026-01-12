package com.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

public class ServicoDescoberta {

private int meuId;
    private int minhaPortaTCP;
    private GerenciadorParceiros gerenciador;
    private boolean running = true;
    private MulticastSocket socketOuvinte;
    private DatagramSocket socketBeacon;
    
    // Porta fixa que todos concordam usar para "se achar"
    private static final int PORTA_UDP_DISCOVERY = 9876; 
    private static final String ENDERECO_MULTICAST = "230.0.0.1";

    public ServicoDescoberta(int meuId, int minhaPortaTCP) {
        this.meuId = meuId;
        this.minhaPortaTCP = minhaPortaTCP;
        this.gerenciador = GerenciadorParceiros.getInstancia();
    }

    public void iniciar() {
        iniciarBeacon();  // Thread que fala
        iniciarOuvinte(); // Thread que escuta
        iniciarLimpeza(); // Thread que mata os inativos
    }

    // 1. Thread "Gritadora" (Envia "OLA" a cada 3s)
    private void iniciarBeacon() {
        new Thread(() -> {
            try {
                socketBeacon = new DatagramSocket();
                socketBeacon.setBroadcast(true);
                String msg = "OLA|" + meuId + "|" + minhaPortaTCP;
                byte[] buffer = msg.getBytes();
                InetAddress broadcastAddr = InetAddress.getByName(ENDERECO_MULTICAST);

                while (running) {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, broadcastAddr, PORTA_UDP_DISCOVERY);
                    socketBeacon.send(pacote);
                    Thread.sleep(3000); // Grita a cada 3 segundos
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 2. Thread "Ouvinte" (Recebe e atualiza lista)
    private void iniciarOuvinte() {
        new Thread(() -> {
            try {
                socketOuvinte = new MulticastSocket(PORTA_UDP_DISCOVERY);
                socketOuvinte.joinGroup(new InetSocketAddress(ENDERECO_MULTICAST, PORTA_UDP_DISCOVERY), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
                byte[] buffer = new byte[1024];

                while (running) {
                    DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                    socketOuvinte.receive(pacote);

                    String conteudo = new String(pacote.getData(), 0, pacote.getLength());
                    String[] partes = conteudo.split("\\|");

                    if (partes.length == 3 && partes[0].equals("OLA")) {
                        int idRemoto = Integer.parseInt(partes[1]);
                        int portaRemota = Integer.parseInt(partes[2]);
                        
                        // Não adiciona a si mesmo
                        if (idRemoto != meuId) {
                            String ipRemoto = pacote.getAddress().getHostAddress();
                            gerenciador.atualizarOuAdicionar(idRemoto, ipRemoto, portaRemota);
                        }
                    }
                }
            } catch (Exception e) {
                if(running){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public int getMeuId() {
        return meuId;
    }

    // 3. Thread "Faxineira" (Verifica quem morreu a cada 5s)
    private void iniciarLimpeza() {
        new Thread(() -> {
            try {
                while (running) {
                    gerenciador.removerInativos();
                    Thread.sleep(5000); // Roda a verificação a cada 5s
                }
            } catch (Exception e) {
                if(running){
                    e.printStackTrace();
                }   
            }
        }).start();
    }

    public void parar() {
        running = false;
        socketBeacon.close();
        socketOuvinte.close();
    }

}

package com.example;

public class Parceiro {

    private int id;
    private String ip;
    private int portaTCP;
    private long ultimaVezVisto; // O "Heartbeat"
    private EstadoDoProcesso estado = EstadoDoProcesso.NAO_INTERESSADO;

    public Parceiro(int id, String ip, int portaTCP) {
        this.id = id;
        this.ip = ip;
        this.portaTCP = portaTCP;
        this.ultimaVezVisto = System.currentTimeMillis();
    }

    // Chamado sempre que recebermos um sinal de vida dele
    public void atualizarSinalDeVida() {
        this.ultimaVezVisto = System.currentTimeMillis();
    }

    public long getUltimaVezVisto() {
        return ultimaVezVisto;
    }

    public int getId() { return id; }
    public String getIp() { return ip; }
    public int getPortaTCP() { return portaTCP; }

    public EstadoDoProcesso getEstado() {
        return estado;
    }

    public void setEstado(EstadoDoProcesso estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "ID: " + id + " [" + ip + ":" + portaTCP + "]";
    }

}

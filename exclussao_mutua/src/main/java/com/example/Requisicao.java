package com.example;

import java.util.Date;

public class Requisicao implements java.io.Serializable {
    
    private int processoId;
    private Date timestamp;
    private String ip;
    private int portaTCP;

    public Requisicao(int processoId) {
        this.processoId = processoId;
        this.timestamp = new Date();
        this.ip = GerenciadorParceiros.getInstancia().getLista().get(0).getIp();
        this.portaTCP = GerenciadorParceiros.getInstancia().getLista().get(0).getPortaTCP();
    }

    public int getProcessoId() {
        return processoId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortaTCP() {
        return portaTCP;
    }

    public void setPortaTCP(int portaTCP) {
        this.portaTCP = portaTCP;
    }

}

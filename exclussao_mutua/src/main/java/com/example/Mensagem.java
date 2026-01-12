package com.example;

import java.util.Date;

public class Mensagem implements java.io.Serializable {
    
    private TipoMensagem tipo;
    private Requisicao requisicao;

    public Mensagem(TipoMensagem tipo, Requisicao requisicao) {
        this.tipo = tipo;
        this.requisicao = requisicao;
    }

    public TipoMensagem getTipo() {
        return tipo;
    }

    public int getProcessoId() {
        return requisicao.getProcessoId();
    }

    public long getTimestamp() {
        return requisicao.getTimestamp().getTime();
    }

    public Date getTimestampDate() {
        return requisicao.getTimestamp();
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public String toString() {
        return tipo.toString() + ";" + requisicao.getProcessoId() + ";" + requisicao.getTimestamp().getTime();
    }

}

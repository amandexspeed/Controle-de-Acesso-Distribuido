package com.example;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GerenciadorParceiros {
// Lista segura para acesso concorrente (Threads lendo e escrevendo ao mesmo tempo)
    private List<Parceiro> listaParceiros = new CopyOnWriteArrayList<>();
    // Tempo limite: se não ouvir nada em 10 segundos, considera morto
    private static final long TEMPO_LIMITE_MS = 10000; 
    private static GerenciadorParceiros instancia;

    private GerenciadorParceiros() {
    }

    public static GerenciadorParceiros getInstancia() {
        if (instancia == null) {
            instancia = new GerenciadorParceiros();
        }
        return instancia;
    }

    public void atualizarOuAdicionar(int id, String ip, int porta) {
        boolean encontrado = false;
        
        for (Parceiro p : listaParceiros) {
            if (p.getId() == id) {
                // Já conheço! Apenas atualizo o heartbeat (renovo o visto)
                p.atualizarSinalDeVida();
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            System.out.println(">>> Novo parceiro detectado: ID " + id);
            listaParceiros.add(new Parceiro(id, ip, porta));
        }
    }

    // Método crucial para o Heartbeat
    public void removerInativos() {
        long agora = System.currentTimeMillis();
        
        // RemoveIf é seguro de usar com CopyOnWriteArrayList
        listaParceiros.removeIf(p -> {
            if(p.getId() == GerenciadorParceiros.getInstancia().getLista().get(0).getId()) {
                return false; // Nunca remova a si mesmo
            }
            boolean estaMorto = (agora - p.getUltimaVezVisto()) > TEMPO_LIMITE_MS;
            if (estaMorto) {
                System.out.println(">>> Parceiro removido por inatividade (TIMEOUT): ID " + p.getId());
            }
            return estaMorto;
        });
    }

    public List<Parceiro> getLista() {
        return listaParceiros;
    }
}


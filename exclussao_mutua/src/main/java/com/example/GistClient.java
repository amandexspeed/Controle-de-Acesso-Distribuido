package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GistClient {

    // 🔴 CONFIGURAÇÃO: Coloque seus dados aqui
    private static final String GIST_ID = "d247e3ae7e5ffb875a38019c13efef53"; 
    private static final String TOKEN = System.getenv("GITHUB_TOKEN");
    private static final String FILE_NAME = "dados.txt"; // Mesmo nome que criou no site

    private static final HttpClient client = HttpClient.newHttpClient();
    // Função 1: LER (GET)
    public static String lerGist() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/gists/" + GIST_ID))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Falha ao ler: " + response.statusCode());
        }

        // Gambiarra técnica para extrair o texto sem usar biblioteca JSON externa (Gson/Jackson)
        // O JSON vem assim: ... "filename": { "content": "TEXTO AQUI", ...
        String json = response.body();
        String searchKey = "\"content\":";
        int startIndex = json.indexOf(searchKey);
        
        if (startIndex == -1) return ""; // Não achou conteúdo
        
        // Pega o conteúdo bruto (simplificado para fins didáticos)
        // Nota: Em um app real, use a biblioteca 'Jackson' ou 'Gson' para fazer isso
        String temp = json.substring(startIndex + searchKey.length());
        int firstQuote = temp.indexOf("\"");
        int lastQuote = temp.indexOf("\"", firstQuote + 1);
        
        // O texto pode vir com escapes do JSON, aqui pegamos o básico
        String content = temp.substring(firstQuote + 1, lastQuote);
        
        // Remove caracteres de escape de nova linha antigos para visualização limpa
        return content.replace("\\n", "\n"); 
    }

    // Função 2: ESCREVER (PATCH)
    public static void atualizarGist(String novoTexto) throws Exception {
        // Precisamos escapar as quebras de linha para o JSON ser válido
        String textoJson = novoTexto.replace("\n", "\\n").replace("\r", "");

        // Monta o JSON manual
        String jsonBody = String.format("{\"files\": {\"%s\": {\"content\": \"%s\"}}}", FILE_NAME, textoJson);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/gists/" + GIST_ID))
                .header("Authorization", "Bearer " + TOKEN)
                .header("Accept", "application/vnd.github.v3+json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Falha ao escrever: " + response.body());
        }
    }
}

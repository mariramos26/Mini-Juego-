import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final List<ClientHandler> waiting = new ArrayList<>();
    
    public static void main(String[] args) throws IOException {
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor iniciado en puerto " + port);

        while (true) {
            // Espera un cliente
            Socket clientSocket = serverSocket.accept();
            System.out.println("Nuevo cliente conectado: " + clientSocket.getRemoteSocketAddress());

            // Crea handler
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();

            // Sincronizamos acceso a la lista de espera
            synchronized (waiting) {
                // Eliminamos de la lista los que ya no están conectados
                waiting.removeIf(h -> !h.isConnected());

                waiting.add(handler);
                if (waiting.size() >= 2) {
                    ClientHandler a = waiting.remove(0);
                    ClientHandler b = waiting.remove(0);

                    if (a.isConnected() && b.isConnected()) {
                        a.setOpponent(b);
                        b.setOpponent(a);

                        a.sendMessage("MATCH_START");
                        b.sendMessage("MATCH_START");

                        // Iniciar turnos
                        a.startMatchTurn();

                        if (a.getFighter() != null && b.getFighter() != null) {
                            System.out.println("Emparejados: " +
                                a.getFighter().getName() + " vs " +
                                b.getFighter().getName());
                        } else {
                            System.out.println("Emparejados: esperando creación de personajes...");
                        }
                    }
                }
            }
        }
    }
}
import java.io.*;
import java.net.*;
import java.util.Random;

public class ClientHandler extends Thread {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private ClientHandler opponent;
    private Fighter fighter;

    private volatile boolean connected = true; // bandera de conexión
    private volatile boolean myTurn = false;   // Control de turno
    private int turnsSinceSpecial = 0;         // contador de turnos desde el último SPECIAL

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.fighter = null; // aún no creado hasta que mande CREATE
    }

    public void setOpponent(ClientHandler opp) {
        this.opponent = opp;
        
    }

    // Inicializa el turno al azar entre los dos jugadores
    public void startMatchTurn() {
        if (opponent == null) return;
        boolean first = new java.util.Random().nextBoolean();
        if (first) {
            this.myTurn = true;
            this.sendMessage("TU_TURNO");
            opponent.myTurn = false;
            opponent.sendMessage("ESPERA_TURNO");
        } else {
            this.myTurn = false;
            this.sendMessage("ESPERA_TURNO");
            opponent.myTurn = true;
            opponent.sendMessage("TU_TURNO");
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public boolean isConnected() {
        return connected && !socket.isClosed();
    }

    public Fighter getFighter() {
        return this.fighter;
    }

    @Override
    public void run() {
        try {
            String line;
            Random rand = new Random();

            while ((line = in.readLine()) != null) {
                System.out.println("Recibido: " + line);
                ClientHandler opponent1 = opponent; // copia local

                // --- CREACIÓN DEL PERSONAJE ---
                if (line.startsWith("JUGADOR:")) {
                    String[] parts = line.split(":");
                    if (parts.length < 3) {
                        sendMessage("ERROR: formato inválido. Usa CREATE:CLASE:NOMBRE");
                        continue;
                    }

                    String clase = parts[1].toUpperCase();
                    String playerName = parts[2];

                    switch (clase) {
                        case "ARQUERA" -> fighter = new Fighter(playerName, 250, 20, 30, "ARQUERA");
                        case "GUERRERO" -> fighter = new Fighter(playerName, 300, 15, 25, "GUERRERO");
                        case "MAGO" -> fighter = new Fighter(playerName, 200, 25, 40, "MAGO");
                        default -> {
                            sendMessage("ERROR: clase inválida (" + clase + ")");
                            continue;
                        }
                    }

                    sendMessage("WELCOME " + fighter.getName() + " [" + clase + "]");
                }

                // --- ATAQUE ---
                else if (line.equalsIgnoreCase("ATTACK")) {
                    if (fighter == null) {
                        sendMessage("ERROR: aún no has creado tu personaje.");
                        continue;
                    }
                    if (!myTurn) {
                        sendMessage("NO_ES_TU_TURNO");
                        continue;
                    }
                    if (opponent1 != null && opponent1.isConnected() && opponent1.fighter != null) {
                        synchronized (opponent1) {
                            boolean evade = rand.nextDouble() < 0.2; // 20%  evadir
                            boolean crit = rand.nextDouble() < 0.15; // 15% crítico

                            if (evade) {
                                opponent1.sendMessage("EVADE: esquivaste el ataque de " + fighter.getName());
                                sendMessage("EVADE: tu ataque fue esquivado por " + opponent1.fighter.getName());
                                    System.out.println("[SERVER] EVADE: " + opponent1.fighter.getName() + " esquivó el ataque de " + fighter.getName());
                            } else {
                                int damage = fighter.rollDamage();
                                if (crit) {
                                    damage *= 2;
                                    opponent1.sendMessage("CRITICO: recibiste " + damage + " de " + fighter.getName());
                                    sendMessage("CRITICO: golpeaste a " + opponent1.fighter.getName() + " con " + damage);
                                        System.out.println("[SERVER] CRITICO: " + fighter.getName() + " golpeó a " + opponent1.fighter.getName() + " con " + damage);
                                } else {
                                    opponent1.sendMessage("DAMAGE: " + damage + " de " + fighter.getName());
                                    sendMessage("ATACASTE a " + opponent1.fighter.getName() + " por " + damage);
                                        System.out.println("[SERVER] ATAQUE: " + fighter.getName() + " hizo " + damage + " de daño a " + opponent1.fighter.getName());
                                }

                                opponent1.fighter.takeDamage(damage, fighter.getName());

                                if (!opponent1.fighter.isAlive()) {
                                    sendMessage("YOU_WIN");
                                    opponent1.sendMessage("YOU_LOSE");
                                }
                            }
                            // Cambiar turno
                            myTurn = false;
                            opponent1.myTurn = true;
                            turnsSinceSpecial++; // sumamos un turno para el cooldown
                            sendMessage("ESPERA_TURNO");
                            opponent1.sendMessage("TU_TURNO");
                        }
                    } else {
                        sendMessage("No tienes un oponente disponible.");
                    }
                }

                // --- CURAR ---
                else if (line.equalsIgnoreCase("HEAL")) {
                    if (fighter == null) {
                        sendMessage("ERROR: aún no has creado tu personaje.");
                        continue;
                    }
                    if (!myTurn) {
                        sendMessage("NO_ES_TU_TURNO");
                        continue;
                    }
                    int amount = 10 + rand.nextInt(11);
                    fighter.heal(amount);
                    sendMessage("Te curaste " + amount + " puntos. HP actual: " + fighter.getHp());

                    // Cambiar turno
                    myTurn = false;
                    turnsSinceSpecial++; // también cuenta como un turno
                    if (opponent1 != null) {
                        opponent1.myTurn = true;
                        opponent1.sendMessage("TU_TURNO");
                    }
                    sendMessage("ESPERA_TURNO");
                }

                // --- SPECIAL ---
             else if (line.equalsIgnoreCase("SPECIAL")) {
              if (fighter == null) {
                  sendMessage("ERROR: aún no has creado tu personaje.");
                 continue;
                }
              if (!myTurn) {
                 sendMessage("NO_ES_TU_TURNO");
                 continue;
                }
              if (turnsSinceSpecial < 3) {
               sendMessage("SPECIAL_NO_DISPONIBLE: Debes esperar " + (3 - turnsSinceSpecial) + " turnos más.");
                  continue;
                }
              if (opponent1 != null && opponent1.isConnected() && opponent1.fighter != null) {
                 synchronized (opponent1) {
                 int damage = fighter.rollSpecialDamage();
                 opponent1.fighter.takeDamage(damage, fighter.getName());

               //  mensaje según clase
                String specialMsg = switch (fighter.getClase()) {
                  case "ARQUERA" -> fighter.getName() + " desató una Lluvia de Flechas";
                  case "GUERRERO" -> fighter.getName() + " lanzó un Golpe de Espada Colosal";
                  case "MAGO" -> fighter.getName() + " invocó una Explosión Arcana";
                  default -> fighter.getName() + " usó un ataque especial";
                };

              sendMessage("SPECIAL: " + specialMsg + " causando " + damage + " de daño a " + opponent1.fighter.getName());
              opponent1.sendMessage("SPECIAL: " + specialMsg + " y recibiste " + damage + " de daño");

              System.out.println("[SERVER] SPECIAL: " + specialMsg + " causando " + damage + " de daño a " + opponent1.fighter.getName());

            if (!opponent1.fighter.isAlive()) {
                sendMessage("YOU_WIN");
                opponent1.sendMessage("YOU_LOSE");
            }

            // Resetear cooldown
            turnsSinceSpecial = 0;

            // Cambiar turno
            myTurn = false;
            opponent1.myTurn = true;
            sendMessage("ESPERA_TURNO");
            opponent1.sendMessage("TU_TURNO");
        }
    } else {
        sendMessage("No tienes un oponente disponible.");
    }
}

                // --- STATUS ---
                else if (line.equalsIgnoreCase("STATUS")) {
                    if (fighter == null) {
                        sendMessage("ERROR: aún no has creado tu personaje.");
                        continue;
                    }
                    sendMessage("HP: " + fighter.getHp() + "/" + fighter.getMaxHp());
                }

                // --- SALIR ---
                else if (line.equalsIgnoreCase("EXIT")) {
                    sendMessage("Saliendo del juego...");
                    connected = false;
                    if (opponent1 != null && opponent1.isConnected()) {
                        opponent1.sendMessage("Tu oponente se ha desconectado. Ganaste por abandono.");
                    }
                    break;
                }

                // --- COMANDO INVÁLIDO ---
                else {
                    sendMessage("UNKNOWN_CMD");
                }
            }
        } catch (IOException e) {
            System.out.println("Error en handler: " + e.getMessage());
        } finally {
            connected = false;
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("Jugador desconectado: " + (fighter != null ? fighter.getName() : "SinNombre"));
        }
    }
}
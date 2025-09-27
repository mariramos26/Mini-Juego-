import java.util.Random;

public class Fighter {
    private final String name;
    private int hp;
    private final int maxHp;
    private final int minAtk;
    private final int maxAtk;
    private final String type;  // clase del luchador
    private final Random rand = new Random();

    // Constructor con ataque personalizado + tipo
    public Fighter(String name, int hp, int minAtk, int maxAtk, String type) {
        this.name = name;
        this.hp = hp;
        this.maxHp = hp;
        this.minAtk = minAtk;
        this.maxAtk = maxAtk;
        this.type = type;
    }

    // Daño calculado entre min y max
    public synchronized int rollDamage() {
        return minAtk + rand.nextInt(maxAtk - minAtk + 1);
    }

    // Daño especial según tipo de luchador
    public synchronized int rollSpecialDamage() {
        int damage = 0;
        switch (type) {
            case "ARQUERA" -> damage = 40 + rand.nextInt(21);   // 40-60
            case "GUERRERO" -> damage = 50 + rand.nextInt(26);  // 50-75
            case "MAGO" -> damage = 60 + rand.nextInt(41);      // 60-100
            default -> damage = 30; // por si acaso
        }
        return damage;
    }

    public synchronized void takeDamage(int amount, String attacker) {
        if (!isAlive()) return;
        hp -= amount;
        if (hp < 0) hp = 0;
        System.out.println(attacker + " golpea a " + name + " por " + amount + " de daño.");
    }

    public synchronized void heal(int amount) {
        if (!isAlive()) return;
        hp += amount;
        if (hp > maxHp) hp = maxHp;
        System.out.println(name + " se curó " + amount + " puntos de vida.");
    }

    public synchronized boolean isAlive() {
        return hp > 0;
    }

    public synchronized int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    //  alias para usar en ClientHandler
    public String getClase() {
        return type;
    }
}

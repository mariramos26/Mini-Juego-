public enum FighterType {
    ARQUERA("Arquera", 250, 20, 30),
    GUERRERO("Guerrero", 300, 15, 25),
    MAGO("Mago", 200, 25, 40);

    private final String displayName;
    private final int hp;
    private final int minAtk;
    private final int maxAtk;

    FighterType(String displayName, int hp, int minAtk, int maxAtk) {
        this.displayName = displayName;
        this.hp = hp;
        this.minAtk = minAtk;
        this.maxAtk = maxAtk;
    }

    public String getDisplayName() { return displayName; }
    public int getHp() { return hp; }
    public int getMinAtk() { return minAtk; }
    public int getMaxAtk() { return maxAtk; }
}
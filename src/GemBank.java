public class GemBank {
    private int gems = 200000;
    private int totalEarned;

    public void add(int amount) {
        gems += amount;
        totalEarned += amount;
    }

    public boolean spend(int amount) {
        if (!canAfford(amount)) {
            return false;
        }

        gems -= amount;
        return true;
    }

    public boolean canAfford(int amount) {
        return gems >= amount;
    }

    public int getGems() {
        return gems;
    }

    public int getTotalEarned() {
        return totalEarned;
    }

    public int getNestGrowthLevel() {
        return Math.min(5, totalEarned / 300);
    }
}

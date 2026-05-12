import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class Bird {
    private static final int SIZE = 42;
    private static final int MAX_STAT_LEVEL = 5;

    private final String name;
    private final Color color;
    private final Color darkColor;
    private final String gemName;
    private final Color gemColor;
    private final int gemValue;
    private final int baseCarryCapacity;
    private final int strengthCarryBonus;
    private final double baseSpeed;
    private final double speedBonus;
    private final double baseMiningSeconds;
    private final double miningSpeedBonus;
    private final double costMultiplier;

    private int speedLevel = 1;
    private int strengthLevel = 1;
    private int miningLevel = 1;
    private int carriedGems;
    private double x;
    private double animationTime;
    private double harvestTimer;
    private double harvestDuration;
    private boolean movingToMine = true;
    private boolean harvesting;
    private boolean activeTrip;
    private boolean autoMiningUnlocked;

    public Bird(
            String name,
            Color color,
            Color darkColor,
            String gemName,
            Color gemColor,
            int gemValue,
            double baseSpeed,
            double speedBonus,
            int baseCarryCapacity,
            int strengthCarryBonus,
            double baseMiningSeconds,
            double miningSpeedBonus,
            double costMultiplier
    ) {
        this.name = name;
        this.color = color;
        this.darkColor = darkColor;
        this.gemName = gemName;
        this.gemColor = gemColor;
        this.gemValue = gemValue;
        this.baseSpeed = baseSpeed;
        this.speedBonus = speedBonus;
        this.baseCarryCapacity = baseCarryCapacity;
        this.strengthCarryBonus = strengthCarryBonus;
        this.baseMiningSeconds = baseMiningSeconds;
        this.miningSpeedBonus = miningSpeedBonus;
        this.costMultiplier = costMultiplier;
    }

    public int update(double deltaSeconds, int mineX, int basketX) {
        animationTime += deltaSeconds;

        if (!activeTrip && !autoMiningUnlocked) {
            return 0;
        }

        activeTrip = true;

        if (harvesting) {
            harvestTimer -= deltaSeconds;
            if (harvestTimer <= 0) {
                carriedGems = getCarryCapacity();
                harvesting = false;
                movingToMine = false;
            }
            return 0;
        }

        double targetX = movingToMine ? mineX : basketX;
        double direction = Math.signum(targetX - x);
        x += direction * getSpeed() * deltaSeconds;

        if (Math.abs(targetX - x) < 3) {
            x = targetX;

            if (movingToMine) {
                harvesting = true;
                harvestDuration = getMiningSeconds();
                harvestTimer = harvestDuration;
            } else {
                movingToMine = true;
                int delivered = carriedGems;
                carriedGems = 0;
                activeTrip = autoMiningUnlocked;
                return delivered;
            }
        }

        return 0;
    }

    public void setHomePosition(int basketX) {
        x = basketX;
        movingToMine = true;
        harvesting = false;
        carriedGems = 0;
        activeTrip = autoMiningUnlocked;
    }

    public boolean upgradeSpeed() {
        if (!canUpgradeSpeed()) {
            return false;
        }

        speedLevel++;
        return true;
    }

    public boolean upgradeStrength() {
        if (!canUpgradeStrength()) {
            return false;
        }

        strengthLevel++;
        return true;
    }

    public boolean upgradeMining() {
        if (!canUpgradeMining()) {
            return false;
        }

        miningLevel++;
        return true;
    }

    public boolean unlockAutoMining() {
        if (!canUnlockAutoMining()) {
            return false;
        }

        autoMiningUnlocked = true;
        activeTrip = true;
        return true;
    }

    public boolean tryStartMining(int mouseX, int mouseY, int floorY, int laneOffset) {
        if (autoMiningUnlocked || activeTrip) {
            return false;
        }

        int birdX = (int) x;
        int birdY = floorY - SIZE - 14 + laneOffset;
        boolean contains = mouseX >= birdX - 10
                && mouseX <= birdX + SIZE + 18
                && mouseY >= birdY - 18
                && mouseY <= birdY + SIZE + 24;

        if (contains) {
            activeTrip = true;
            return true;
        }

        return false;
    }

    public void draw(Graphics2D g, int floorY, int mineX, int laneOffset) {
        int birdX = (int) x;
        int bob = (int) (Math.sin(animationTime * 8) * 3);
        int birdY = floorY - SIZE - 14 + bob + laneOffset;
        boolean facingRight = !movingToMine;

        drawShadow(g, birdX, floorY + laneOffset);
        drawLegs(g, birdX, birdY);
        drawTail(g, birdX, birdY, facingRight);
        drawWing(g, birdX, birdY);
        drawBody(g, birdX, birdY, facingRight);
        drawGemPack(g, birdX, birdY);
        drawHarvestEffect(g, birdX, birdY, mineX, facingRight);
        drawCarryBubble(g, birdX, birdY);
    }

    private void drawShadow(Graphics2D g, int birdX, int floorY) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(birdX - 8, floorY - 12, SIZE + 20, 12);
    }

    private void drawLegs(Graphics2D g, int birdX, int birdY) {
        int step = (int) (Math.sin(animationTime * 12) * 4);
        g.setColor(new Color(255, 176, 60));
        g.drawLine(birdX + 15, birdY + 36, birdX + 10 + step, birdY + 48);
        g.drawLine(birdX + 28, birdY + 36, birdX + 31 - step, birdY + 48);
    }

    private void drawTail(Graphics2D g, int birdX, int birdY, boolean facingRight) {
        int tailX = facingRight ? birdX - 11 : birdX + 34;
        g.setColor(darkColor);
        g.fillPolygon(
                new int[]{tailX, tailX + (facingRight ? 17 : -17), tailX + (facingRight ? 15 : -15)},
                new int[]{birdY + 24, birdY + 14, birdY + 34},
                3
        );
    }

    private void drawWing(Graphics2D g, int birdX, int birdY) {
        int flap = (int) (Math.sin(animationTime * 10) * 5);
        g.setColor(darkColor);
        g.fillOval(birdX + 8, birdY + 20 + flap / 2, 30, 18);
        g.setColor(color.brighter());
        g.drawArc(birdX + 12, birdY + 23 + flap / 2, 20, 12, 20, 140);
    }

    private void drawBody(Graphics2D g, int birdX, int birdY, boolean facingRight) {
        int faceX = facingRight ? birdX + 28 : birdX + 4;
        int beakPointX = facingRight ? birdX + 58 : birdX - 10;
        int beakBaseX = facingRight ? birdX + 40 : birdX + 2;

        g.setColor(color);
        g.fillOval(birdX, birdY, SIZE, SIZE);

        g.setColor(color.brighter());
        g.fillOval(birdX + 9, birdY + 5, 21, 16);

        g.setColor(new Color(255, 214, 87));
        g.fillPolygon(
                new int[]{beakBaseX, beakPointX, beakBaseX},
                new int[]{birdY + 17, birdY + 24, birdY + 31},
                3
        );

        g.setColor(Color.WHITE);
        g.fillOval(faceX, birdY + 10, 10, 10);

        g.setColor(Color.BLACK);
        g.fillOval(faceX + 4, birdY + 13, 4, 4);

        g.setColor(darkColor);
        g.fillPolygon(
                new int[]{birdX + 15, birdX + 22, birdX + 29},
                new int[]{birdY - 2, birdY - 13, birdY - 2},
                3
        );

        drawMinerHelmet(g, birdX + 4, birdY - 4);
    }

    private void drawMinerHelmet(Graphics2D g, int x, int y) {
        g.setColor(new Color(220, 151, 45));
        g.fillArc(x, y, 34, 25, 0, 180);
        g.setColor(new Color(115, 72, 32));
        g.fillRoundRect(x + 2, y + 12, 31, 7, 5, 5);
        g.setColor(new Color(126, 225, 255));
        g.fillOval(x + 22, y + 5, 12, 12);
        g.setColor(Color.WHITE);
        g.drawOval(x + 22, y + 5, 12, 12);
    }

    private void drawGemPack(Graphics2D g, int birdX, int birdY) {
        if (carriedGems <= 0) {
            return;
        }

        g.setColor(new Color(95, 58, 42));
        g.fillRoundRect(birdX + 9, birdY - 12, 27, 15, 7, 7);

        for (int i = 0; i < 2; i++) {
            int gemX = birdX + 13 + i * 13;
            g.setColor(gemColor);
            g.fillPolygon(
                    new int[]{gemX + 5, gemX + 10, gemX + 5, gemX},
                    new int[]{birdY - 10, birdY - 5, birdY + 1, birdY - 5},
                    4
            );
        }
    }

    private void drawHarvestEffect(Graphics2D g, int birdX, int birdY, int mineX, boolean facingRight) {
        if (!harvesting) {
            return;
        }

        int sparkX = mineX - 8;
        int swing = (int) (Math.sin(animationTime * 22) * 9);
        int toolX = facingRight ? birdX + 33 : birdX - 2;

        g.setColor(new Color(140, 95, 55));
        g.drawLine(toolX, birdY + 20, toolX + 18, birdY + 4 + swing);
        g.setColor(new Color(215, 225, 235));
        g.drawLine(toolX + 14, birdY + 3 + swing, toolX + 28, birdY + 8 + swing);

        g.setColor(new Color(255, 220, 80));
        g.fillOval(sparkX, birdY + 15, 7, 7);
        g.setColor(gemColor);
        g.fillOval(sparkX + 8, birdY + 29, 7, 7);
        g.fillOval(sparkX + 17, birdY + 20, 5, 5);
    }

    private void drawCarryBubble(Graphics2D g, int birdX, int birdY) {
        String text;
        if (!activeTrip && !autoMiningUnlocked) {
            text = "tap";
        } else if (harvesting) {
            text = String.format("%.1fs", Math.max(0, harvestTimer));
        } else {
            text = carriedGems + " " + gemName;
        }

        g.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics metrics = g.getFontMetrics();
        int bubbleWidth = metrics.stringWidth(text) + (harvesting ? 42 : 18);
        int bubbleX = birdX + SIZE / 2 - bubbleWidth / 2;
        int bubbleY = birdY - 42;

        g.setColor(new Color(9, 17, 20, 215));
        g.fillRoundRect(bubbleX, bubbleY, bubbleWidth, 24, 12, 12);
        g.setColor(gemColor);
        g.drawRoundRect(bubbleX, bubbleY, bubbleWidth, 24, 12, 12);

        if (harvesting) {
            drawClock(g, bubbleX + 8, bubbleY + 5);
            g.setColor(Color.WHITE);
            g.drawString(text, bubbleX + 31, bubbleY + 16);
        } else {
            g.setColor(Color.WHITE);
            g.drawString(text, bubbleX + 9, bubbleY + 16);
        }
    }

    private void drawClock(Graphics2D g, int x, int y) {
        int progressDegrees = harvestDuration <= 0 ? 0 : (int) (360 * (1 - harvestTimer / harvestDuration));
        g.setColor(new Color(236, 243, 225));
        g.fillOval(x, y, 14, 14);
        g.setColor(gemColor);
        g.fillArc(x + 2, y + 2, 10, 10, 90, -progressDegrees);
        g.setColor(new Color(30, 35, 34));
        g.drawOval(x, y, 14, 14);
        g.drawLine(x + 7, y + 7, x + 7, y + 2);
        g.drawLine(x + 7, y + 7, x + 11, y + 7);
    }

    private double getSpeed() {
        return baseSpeed + (speedLevel - 1) * speedBonus;
    }

    private double getMiningSeconds() {
        return Math.max(0.55, baseMiningSeconds - (miningLevel - 1) * miningSpeedBonus);
    }

    public boolean canUpgradeSpeed() {
        return speedLevel < MAX_STAT_LEVEL;
    }

    public boolean canUpgradeStrength() {
        return strengthLevel < MAX_STAT_LEVEL;
    }

    public boolean canUpgradeMining() {
        return miningLevel < MAX_STAT_LEVEL;
    }

    public boolean isLevelFifteen() {
        return getProgressLevel() >= 15;
    }

    public boolean canUnlockAutoMining() {
        return isLevelFifteen() && !autoMiningUnlocked;
    }

    public int getSpeedCost(int mineId) {
        return scaledCost(45 + speedLevel * 55, mineId);
    }

    public int getStrengthCost(int mineId) {
        return scaledCost(55 + strengthLevel * 60, mineId);
    }

    public int getMiningCost(int mineId) {
        return scaledCost(70 + miningLevel * 70, mineId);
    }

    public int getAutoMiningCost(int mineId) {
        return scaledCost(900, mineId);
    }

    private int scaledCost(int baseCost, int mineId) {
        return (int) Math.round(baseCost * costMultiplier * (1.0 + (mineId - 1) * 0.7));
    }

    public int getCarryCapacity() {
        return baseCarryCapacity + (strengthLevel - 1) * strengthCarryBonus;
    }

    public int getProgressLevel() {
        return speedLevel + strengthLevel + miningLevel + (autoMiningUnlocked ? 1 : 0);
    }

    public String getName() {
        return name;
    }

    public String getGemName() {
        return gemName;
    }

    public Color getGemColor() {
        return gemColor;
    }

    public int getGemValue() {
        return gemValue;
    }

    public int getSpeedLevel() {
        return speedLevel;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public int getMiningLevel() {
        return miningLevel;
    }

    public boolean hasAutoMining() {
        return autoMiningUnlocked;
    }
}

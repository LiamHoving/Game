import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Bird {
    private static final int SIZE = 48;
    private static final int MAX_STAT_LEVEL = 5;
    private static final Image BLUE_MINER_SPRITE = loadSprite("assets/blue_bird_new.png");

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
        boolean facingRight = movingToMine;

        if (drawBlueMinerSprite(g, birdX, floorY, laneOffset, facingRight, mineX)) {
            return;
        }

        drawShadow(g, birdX, floorY + laneOffset);
        drawLegs(g, birdX, birdY);
        drawTail(g, birdX, birdY, facingRight);
        drawWing(g, birdX, birdY);
        drawBody(g, birdX, birdY, facingRight);
        drawGemCart(g, birdX, birdY, facingRight);
        drawHarvestEffect(g, birdX, birdY, mineX, facingRight);
        drawCarryBubble(g, birdX, birdY);
    }

    private static Image loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException exception) {
            return null;
        }
    }

    private boolean drawBlueMinerSprite(Graphics2D g, int birdX, int floorY, int laneOffset, boolean facingRight, int mineX) {
        if (!"Blue Bird".equals(name) || BLUE_MINER_SPRITE == null) {
            return false;
        }

        int spriteWidth = 50;
        int spriteHeight = 58;
        int spriteX = birdX - 7;
        int spriteY = floorY - spriteHeight - 3 + laneOffset;

        drawShadow(g, birdX + 4, floorY + laneOffset);
        if (facingRight) {
            g.drawImage(BLUE_MINER_SPRITE, spriteX, spriteY, spriteWidth, spriteHeight, null);
        } else {
            g.drawImage(BLUE_MINER_SPRITE, spriteX + spriteWidth, spriteY, -spriteWidth, spriteHeight, null);
        }

        int bubbleX = birdX - 2;
        int bubbleY = spriteY + 18;
        drawHarvestEffect(g, bubbleX, bubbleY, mineX, facingRight);
        drawCarryBubble(g, bubbleX, spriteY + 35);
        return true;
    }

    private void drawShadow(Graphics2D g, int birdX, int floorY) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(birdX - 8, floorY - 12, SIZE + 20, 12);
    }

    private void drawLegs(Graphics2D g, int birdX, int birdY) {
        int step = (int) (Math.sin(animationTime * 12) * 4);
        g.setColor(new Color(255, 176, 60));
        g.drawLine(birdX + 17, birdY + 42, birdX + 11 + step, birdY + 54);
        g.drawLine(birdX + 32, birdY + 42, birdX + 35 - step, birdY + 54);
    }

    private void drawTail(Graphics2D g, int birdX, int birdY, boolean facingRight) {
        int tailX = facingRight ? birdX - 13 : birdX + 39;
        g.setColor(darkColor);
        g.fillPolygon(
                new int[]{tailX, tailX + (facingRight ? 21 : -21), tailX + (facingRight ? 18 : -18)},
                new int[]{birdY + 27, birdY + 15, birdY + 39},
                3
        );
    }

    private void drawWing(Graphics2D g, int birdX, int birdY) {
        int flap = (int) (Math.sin(animationTime * 10) * 5);
        g.setColor(darkColor);
        g.fillOval(birdX + 7, birdY + 23 + flap / 2, 34, 20);
        g.setColor(color.brighter());
        g.drawArc(birdX + 12, birdY + 26 + flap / 2, 23, 12, 20, 140);
    }

    private void drawBody(Graphics2D g, int birdX, int birdY, boolean facingRight) {
        int eyeX = facingRight ? birdX + 30 : birdX + 6;
        int beakPointX = facingRight ? birdX + 61 : birdX - 11;
        int beakBaseX = facingRight ? birdX + 43 : birdX + 5;

        g.setColor(new Color(15, 21, 31, 95));
        g.fillOval(birdX - 2, birdY + 3, SIZE + 4, SIZE);
        g.setColor(color);
        g.fillOval(birdX, birdY + 1, SIZE, SIZE - 1);

        g.setColor(color.brighter());
        g.fillOval(birdX + 12, birdY + 8, 23, 16);

        g.setColor(new Color(255, 255, 255, 52));
        g.fillOval(birdX + 8, birdY + 7, 15, 12);

        g.setColor(new Color(255, 214, 87));
        g.fillPolygon(
                new int[]{beakBaseX, beakPointX, beakBaseX},
                new int[]{birdY + 20, birdY + 27, birdY + 34},
                3
        );
        g.setColor(new Color(205, 126, 28));
        g.drawLine(beakBaseX, birdY + 27, beakPointX - (facingRight ? 5 : -5), birdY + 27);

        g.setColor(Color.WHITE);
        g.fillOval(eyeX, birdY + 12, 14, 14);

        g.setColor(Color.BLACK);
        g.fillOval(eyeX + (facingRight ? 7 : 3), birdY + 16, 5, 5);
        g.setColor(Color.WHITE);
        g.fillOval(eyeX + (facingRight ? 9 : 5), birdY + 16, 2, 2);

        g.setColor(new Color(255, 145, 115, 95));
        g.fillOval(facingRight ? birdX + 25 : birdX + 13, birdY + 29, 9, 6);

        g.setColor(darkColor);
        g.fillPolygon(
                new int[]{birdX + 16, birdX + 24, birdX + 32},
                new int[]{birdY + 1, birdY - 13, birdY + 1},
                3
        );

        g.setColor(new Color(83, 47, 31));
        g.fillRoundRect(birdX + 14, birdY + 38, 25, 6, 5, 5);

        drawMinerHelmet(g, birdX + 4, birdY - 6);
    }

    private void drawMinerHelmet(Graphics2D g, int x, int y) {
        g.setColor(new Color(220, 151, 45));
        g.fillArc(x, y, 40, 28, 0, 180);
        g.setColor(new Color(255, 199, 75));
        g.fillArc(x + 7, y + 3, 22, 14, 0, 180);
        g.setColor(new Color(115, 72, 32));
        g.fillRoundRect(x + 1, y + 14, 38, 8, 5, 5);
        g.setColor(new Color(126, 225, 255));
        g.fillOval(x + 25, y + 5, 14, 14);
        g.setColor(Color.WHITE);
        g.drawOval(x + 25, y + 5, 14, 14);
    }

    private void drawGemCart(Graphics2D g, int birdX, int birdY, boolean facingRight) {
        if (carriedGems <= 0) {
            return;
        }

        int cartX = facingRight ? birdX + 38 : birdX - 33;
        int cartY = birdY + 30;

        g.setColor(new Color(78, 48, 33));
        g.fillRoundRect(cartX + 2, cartY + 15, 38, 19, 6, 6);
        g.setColor(new Color(139, 82, 45));
        g.fillPolygon(new int[]{cartX, cartX + 44, cartX + 37, cartX + 7}, new int[]{cartY + 6, cartY + 6, cartY + 28, cartY + 28}, 4);
        g.setColor(new Color(59, 38, 29));
        g.fillOval(cartX + 5, cartY + 28, 9, 9);
        g.fillOval(cartX + 31, cartY + 28, 9, 9);

        for (int i = 0; i < 4; i++) {
            drawTinyGem(g, cartX + 9 + i * 8, cartY - 2 + (i % 2) * 4);
        }
    }

    private void drawTinyGem(Graphics2D g, int x, int y) {
        g.setColor(gemColor);
        g.fillPolygon(new int[]{x + 5, x + 10, x + 5, x}, new int[]{y, y + 5, y + 11, y + 5}, 4);
        g.setColor(Color.WHITE);
        g.drawLine(x + 5, y + 1, x + 9, y + 5);
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
        return !autoMiningUnlocked;
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
        return scaledCost(100, mineId);
    }

    private int scaledCost(int baseCost, int mineId) {
        return (int) Math.round(baseCost * costMultiplier * (1.0 + (mineId - 1) * 0.7));
    }

    public int getCarryCapacity() {
        return baseCarryCapacity + (strengthLevel - 1) * strengthCarryBonus;
    }

    public int getProgressLevel() {
        return speedLevel + strengthLevel + miningLevel;
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

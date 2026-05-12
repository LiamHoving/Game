import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class Courier {
    private static final int MAX_STAT_LEVEL = 5;

    private final String name;
    private final Color color;
    private final boolean strongCarrier;
    private final double baseSpeed;
    private final int baseCapacity;

    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private int moveLevel = 1;
    private int pickupLevel = 1;
    private int capacityLevel = 1;
    private double progress;
    private double pickupTimer;
    private boolean busy;
    private boolean pickingUp;
    private int load;

    public Courier(
            String name,
            int startX,
            int startY,
            int endX,
            int endY,
            Color color,
            boolean strongCarrier,
            double baseSpeed,
            int baseCapacity
    ) {
        this.name = name;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = color;
        this.strongCarrier = strongCarrier;
        this.baseSpeed = baseSpeed;
        this.baseCapacity = baseCapacity;
    }

    public void setRoute(int startX, int startY, int endX, int endY) {
        if (busy) {
            return;
        }

        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public void startTrip(int load) {
        this.load = load;
        progress = 0;
        pickupTimer = getPickupSeconds();
        pickingUp = true;
        busy = true;
    }

    public int update(double deltaSeconds) {
        if (!busy) {
            return 0;
        }

        if (pickingUp) {
            pickupTimer -= deltaSeconds;
            if (pickupTimer <= 0) {
                pickingUp = false;
            }
            return 0;
        }

        progress += getMoveSpeed() * deltaSeconds;
        if (progress >= 1) {
            busy = false;
            progress = 0;
            int delivered = load;
            load = 0;
            return delivered;
        }

        return 0;
    }

    public boolean upgradeMoveSpeed() {
        if (!canUpgradeMove()) {
            return false;
        }

        moveLevel++;
        return true;
    }

    public boolean upgradePickupSpeed() {
        if (!canUpgradePickup()) {
            return false;
        }

        pickupLevel++;
        return true;
    }

    public boolean upgradeCapacity() {
        if (!canUpgradeCapacity()) {
            return false;
        }

        capacityLevel++;
        return true;
    }

    public void draw(Graphics2D g) {
        draw(g, false);
    }

    public void draw(Graphics2D g, boolean selected) {
        drawRoute(g);

        int x = getCurrentX();
        int y = getCurrentY();

        if (selected) {
            g.setColor(new Color(255, 225, 105, 140));
            g.fillOval(x - 12, y - 15, strongCarrier ? 70 : 58, 62);
        }

        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(x - 18, y + 26, strongCarrier ? 55 : 42, 10);

        if (strongCarrier) {
            drawStrongLiftBird(g, x, y);
        } else {
            drawRunnerBird(g, x, y);
        }

        if (load > 0) {
            drawGemLoad(g, x, y);
        }

        drawStatusBubble(g, x, y);
    }

    public boolean contains(int mouseX, int mouseY) {
        int x = getCurrentX();
        int y = getCurrentY();
        return mouseX >= x - 20
                && mouseX <= x + (strongCarrier ? 62 : 48)
                && mouseY >= y - 20
                && mouseY <= y + 55;
    }

    private void drawRoute(Graphics2D g) {
        g.setColor(new Color(100, 175, 105, 130));
        g.drawLine(startX + 12, startY + 12, endX + 12, endY + 12);

        for (int i = 0; i < 6; i++) {
            double step = i / 5.0;
            int leafX = (int) (startX + (endX - startX) * step);
            int leafY = (int) (startY + (endY - startY) * step);
            g.setColor(new Color(63, 135, 77, 150));
            g.fillOval(leafX + 6, leafY + 6, 12, 7);
        }
    }

    private void drawStrongLiftBird(Graphics2D g, int x, int y) {
        g.setColor(color.darker());
        g.fillOval(x + 4, y + 8, 34, 28);

        g.setColor(color);
        g.fillOval(x, y, 42, 31);

        g.setColor(new Color(75, 120, 58));
        g.fillArc(x - 6, y - 10, 50, 25, 0, 180);

        g.setColor(new Color(250, 210, 92));
        g.fillPolygon(new int[]{x + 39, x + 55, x + 39}, new int[]{y + 12, y + 18, y + 24}, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + 27, y + 8, 8, 8);
        g.setColor(Color.BLACK);
        g.fillOval(x + 30, y + 10, 4, 4);
    }

    private void drawRunnerBird(Graphics2D g, int x, int y) {
        g.setColor(color.darker());
        g.fillOval(x + 3, y + 13, 25, 18);

        g.setColor(color);
        g.fillOval(x, y, 31, 25);

        g.setColor(new Color(250, 210, 92));
        g.fillPolygon(new int[]{x + 29, x + 43, x + 29}, new int[]{y + 10, y + 16, y + 21}, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + 20, y + 7, 7, 7);
        g.setColor(Color.BLACK);
        g.fillOval(x + 23, y + 9, 3, 3);

        g.setColor(new Color(255, 176, 60));
        g.drawLine(x + 10, y + 24, x + 6, y + 35);
        g.drawLine(x + 20, y + 24, x + 25, y + 35);
    }

    private void drawGemLoad(Graphics2D g, int x, int y) {
        if (strongCarrier) {
            g.setColor(new Color(92, 56, 35));
            g.fillRoundRect(x - 11, y + 34, 31, 18, 5, 5);
        } else {
            g.setColor(new Color(96, 59, 37));
            g.fillRoundRect(x - 17, y + 30, 31, 16, 5, 5);
            g.setColor(new Color(60, 42, 32));
            g.fillOval(x - 13, y + 41, 8, 8);
            g.fillOval(x + 1, y + 41, 8, 8);
        }

        g.setColor(new Color(95, 205, 255));
        g.fillOval(x - 5, y + 29, 7, 7);
        g.fillOval(x + 4, y + 31, 6, 6);

        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(load), x - 7, y + 46);
    }

    private void drawStatusBubble(Graphics2D g, int x, int y) {
        if (!busy) {
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.setColor(new Color(235, 245, 230));
            g.drawString(name, x - 16, y - 12);
            return;
        }

        String label = pickingUp
                ? String.format("pick %.1fs", Math.max(0, pickupTimer))
                : String.format("move %.1fs", Math.max(0, (1 - progress) / getMoveSpeed()));

        int bubbleWidth = 78;
        int bubbleX = x - 22;
        int bubbleY = y - 28;

        g.setColor(new Color(9, 17, 20, 215));
        g.fillRoundRect(bubbleX, bubbleY, bubbleWidth, 22, 11, 11);
        g.setColor(new Color(238, 226, 136));
        g.drawRoundRect(bubbleX, bubbleY, bubbleWidth, 22, 11, 11);
        drawClock(g, bubbleX + 6, bubbleY + 4);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        g.drawString(label, bubbleX + 24, bubbleY + 15);
    }

    private void drawClock(Graphics2D g, int x, int y) {
        g.setColor(new Color(236, 243, 225));
        g.fillOval(x, y, 13, 13);
        g.setColor(new Color(60, 90, 76));
        g.drawOval(x, y, 13, 13);
        g.drawLine(x + 6, y + 6, x + 6, y + 2);
        g.drawLine(x + 6, y + 6, x + 10, y + 6);
    }

    private int getCurrentX() {
        if (pickingUp) {
            return startX;
        }
        return busy ? (int) (startX + (endX - startX) * progress) : startX;
    }

    private int getCurrentY() {
        if (pickingUp) {
            return startY;
        }
        return busy ? (int) (startY + (endY - startY) * progress) : startY;
    }

    private double getPickupSeconds() {
        double basePickup = strongCarrier ? 2.2 : 1.7;
        return Math.max(0.55, basePickup - (pickupLevel - 1) * 0.28);
    }

    private double getMoveSpeed() {
        return baseSpeed + (moveLevel - 1) * 0.05;
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean canUpgradeMove() {
        return moveLevel < MAX_STAT_LEVEL;
    }

    public boolean canUpgradePickup() {
        return pickupLevel < MAX_STAT_LEVEL;
    }

    public boolean canUpgradeCapacity() {
        return capacityLevel < MAX_STAT_LEVEL;
    }

    public int getCapacity() {
        return baseCapacity + (capacityLevel - 1) * (strongCarrier ? 20 : 15);
    }

    public int getMoveCost() {
        return 130 + moveLevel * 115;
    }

    public int getPickupCost() {
        return 115 + pickupLevel * 105;
    }

    public int getCapacityCost() {
        return 155 + capacityLevel * 135;
    }

    public int getMoveLevel() {
        return moveLevel;
    }

    public int getPickupLevel() {
        return pickupLevel;
    }

    public int getCapacityLevel() {
        return capacityLevel;
    }

    public String getName() {
        return name;
    }
}

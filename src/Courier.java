import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Courier {
    private static final int MAX_STAT_LEVEL = 30;
    private static final Image LIFT_BIRD_SPRITE = loadSprite("assets/yellow_bird_new.png");
    private static final Image RUNNER_BIRD_SPRITE = loadSprite("assets/purple_bird_new.png");

    private final String name;
    private final Color color;
    private final boolean strongCarrier;
    private final double baseSpeed;
    private final int baseCapacity;

    private int startX;
    private int startY;
    private int endX;
    private int endY;
    private int idleX;
    private int idleY;
    private int moveLevel = 1;
    private int pickupLevel = 1;
    private int capacityLevel = 1;
    private double progress;
    private double pickupTimer;
    private boolean busy;
    private boolean travelingToPickup;
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
        this.idleX = startX;
        this.idleY = startY;
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
        travelingToPickup = idleX != startX || idleY != startY;
        pickingUp = !travelingToPickup;
        pickupTimer = pickingUp ? getPickupSeconds() : 0;
        busy = true;
    }

    public int update(double deltaSeconds) {
        if (!busy) {
            return 0;
        }

        if (travelingToPickup) {
            progress += getMoveSpeed() * deltaSeconds;
            if (progress >= 1) {
                idleX = startX;
                idleY = startY;
                progress = 0;
                travelingToPickup = false;
                pickingUp = true;
                pickupTimer = getPickupSeconds();
            }
            return 0;
        } else if (pickingUp) {
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
            idleX = endX;
            idleY = endY;
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
        if (LIFT_BIRD_SPRITE != null) {
            g.drawImage(LIFT_BIRD_SPRITE, x - 18, y - 23, 58, 67, null);
            return;
        }

        g.setColor(color.darker());
        g.fillPolygon(new int[]{x - 10, x + 7, x + 5}, new int[]{y + 25, y + 14, y + 37}, 3);
        g.fillOval(x + 8, y + 24, 34, 18);

        g.setColor(color);
        g.fillOval(x, y + 6, 48, 40);
        g.setColor(color.brighter());
        g.fillOval(x + 12, y + 12, 23, 14);
        g.setColor(new Color(250, 210, 92));
        g.fillPolygon(new int[]{x + 40, x + 59, x + 40}, new int[]{y + 23, y + 29, y + 35}, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + 30, y + 16, 14, 14);
        g.setColor(Color.BLACK);
        g.fillOval(x + 36, y + 20, 5, 5);

        g.setColor(new Color(220, 151, 45));
        g.fillArc(x + 4, y - 2, 40, 26, 0, 180);
        g.setColor(new Color(115, 72, 32));
        g.fillRoundRect(x + 6, y + 12, 36, 7, 5, 5);
        g.setColor(new Color(126, 225, 255));
        g.fillOval(x + 30, y + 4, 13, 13);
        g.setColor(Color.WHITE);
        g.drawOval(x + 30, y + 4, 13, 13);

        g.setColor(new Color(86, 55, 37));
        g.fillRoundRect(x - 12, y + 34, 70, 27, 7, 7);
        g.setColor(new Color(141, 88, 50));
        g.fillRoundRect(x - 6, y + 29, 58, 25, 7, 7);
        g.setColor(new Color(55, 35, 28));
        g.drawLine(x - 4, y + 41, x + 50, y + 41);
        g.setColor(new Color(190, 129, 70));
        for (int i = 0; i < 4; i++) {
            g.fillOval(x + 1 + i * 12, y + 45, 4, 4);
        }
    }

    private static Image loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException exception) {
            return null;
        }
    }

    private void drawRunnerBird(Graphics2D g, int x, int y) {
        if (RUNNER_BIRD_SPRITE != null) {
            g.drawImage(RUNNER_BIRD_SPRITE, x - 16, y - 20, 54, 61, null);
            return;
        }

        g.setColor(color.darker());
        g.fillPolygon(new int[]{x - 8, x + 6, x + 5}, new int[]{y + 23, y + 13, y + 33}, 3);
        g.fillOval(x + 5, y + 20, 27, 17);

        g.setColor(color);
        g.fillOval(x, y + 4, 39, 32);
        g.setColor(color.brighter());
        g.fillOval(x + 9, y + 10, 18, 11);

        g.setColor(new Color(250, 210, 92));
        g.fillPolygon(new int[]{x + 34, x + 49, x + 34}, new int[]{y + 17, y + 23, y + 29}, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + 24, y + 11, 12, 12);
        g.setColor(Color.BLACK);
        g.fillOval(x + 29, y + 15, 5, 5);

        g.setColor(new Color(220, 151, 45));
        g.fillArc(x + 1, y - 3, 34, 23, 0, 180);
        g.setColor(new Color(115, 72, 32));
        g.fillRoundRect(x + 3, y + 9, 31, 7, 5, 5);
        g.setColor(new Color(126, 225, 255));
        g.fillOval(x + 23, y + 1, 11, 11);
        g.setColor(Color.WHITE);
        g.drawOval(x + 23, y + 1, 11, 11);

        g.setColor(new Color(255, 176, 60));
        g.drawLine(x + 13, y + 34, x + 8, y + 45);
        g.drawLine(x + 25, y + 34, x + 30, y + 45);
    }

    private void drawGemLoad(Graphics2D g, int x, int y) {
        if (strongCarrier) {
            g.setColor(new Color(75, 48, 34));
            g.fillRoundRect(x - 15, y + 41, 40, 15, 6, 6);
            g.setColor(new Color(142, 88, 45));
            g.drawLine(x - 12, y + 43, x + 22, y + 43);
            g.setColor(new Color(95, 205, 255));
            g.fillOval(x - 8, y + 35, 7, 7);
            g.fillOval(x + 2, y + 37, 7, 7);
            g.fillOval(x + 12, y + 35, 7, 7);
        } else {
            g.setColor(new Color(96, 59, 37));
            g.fillRoundRect(x - 17, y + 30, 31, 16, 5, 5);
            g.setColor(new Color(60, 42, 32));
            g.fillOval(x - 13, y + 41, 8, 8);
            g.fillOval(x + 1, y + 41, 8, 8);

            g.setColor(new Color(95, 205, 255));
            g.fillOval(x - 5, y + 29, 7, 7);
            g.fillOval(x + 4, y + 31, 6, 6);
        }

        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(load), x - 7, y + 46);
    }

    private void drawStatusBubble(Graphics2D g, int x, int y) {
        if (!busy) {
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
        if (travelingToPickup) {
            return (int) (idleX + (startX - idleX) * progress);
        }
        if (pickingUp) {
            return startX;
        }
        return busy ? (int) (startX + (endX - startX) * progress) : idleX;
    }

    private int getCurrentY() {
        if (travelingToPickup) {
            return (int) (idleY + (startY - idleY) * progress);
        }
        if (pickingUp) {
            return startY;
        }
        return busy ? (int) (startY + (endY - startY) * progress) : idleY;
    }

    private double getPickupSeconds() {
        double basePickup = strongCarrier ? 2.2 : 1.7;
        return Math.max(0.35, basePickup * Math.pow(0.94, pickupLevel - 1));
    }

    private double getMoveSpeed() {
        return baseSpeed * (1.0 + (moveLevel - 1) * 0.09);
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isPickingUp() {
        return busy && pickingUp;
    }

    public boolean isTravelingToPickup() {
        return busy && travelingToPickup;
    }

    public int getLoad() {
        return load;
    }

    public int getDrawX() {
        return getCurrentX();
    }

    public int getDrawY() {
        return getCurrentY();
    }

    public String getStatusText() {
        if (!busy) {
            return "";
        }

        if (travelingToPickup) {
            double remaining = Math.max(0, (1 - progress) / getMoveSpeed());
            return strongCarrier ? String.format("down %.1fs", remaining) : String.format("back %.1fs", remaining);
        } else if (pickingUp) {
            return String.format("pick %.1fs", Math.max(0, pickupTimer));
        }
        return String.format("move %.1fs", Math.max(0, (1 - progress) / getMoveSpeed()));
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
        return baseCapacity + (capacityLevel - 1) * (strongCarrier ? 18 : 14);
    }

    public int getMoveCost() {
        return 130 + moveLevel * 115 + moveLevel * moveLevel * 6;
    }

    public int getPickupCost() {
        return 115 + pickupLevel * 105 + pickupLevel * pickupLevel * 5;
    }

    public int getCapacityCost() {
        return 155 + capacityLevel * 135 + capacityLevel * capacityLevel * 7;
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

    public int getMaxStatLevel() {
        return MAX_STAT_LEVEL;
    }

    public String getName() {
        return name;
    }
}

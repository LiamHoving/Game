import java.awt.Color;
import java.awt.Graphics2D;

public class Egg {
    private final int x;
    private final int y;
    private final int cost;

    private boolean unlocked;
    private double pulse;

    public Egg(int x, int y, int cost) {
        this.x = x;
        this.y = y;
        this.cost = cost;
    }

    public void update(double deltaSeconds) {
        pulse += deltaSeconds * 3.5;
    }

    public void unlock() {
        unlocked = true;
    }

    public void draw(Graphics2D g) {
        if (unlocked) {
            drawCrackedEgg(g);
            return;
        }

        int glow = (int) (20 + Math.sin(pulse) * 10);
        g.setColor(new Color(90, 240, 120, 35 + glow));
        g.fillOval(x - 18, y - 16, 70, 76);

        g.setColor(new Color(238, 246, 225));
        g.fillOval(x, y, 38, 50);

        g.setColor(new Color(90, 220, 100));
        g.fillOval(x + 9, y + 16, 12, 16);
        g.fillOval(x + 22, y + 24, 8, 10);
    }

    private void drawCrackedEgg(Graphics2D g) {
        g.setColor(new Color(215, 230, 210));
        g.fillArc(x - 2, y + 18, 22, 30, 180, 180);
        g.fillArc(x + 18, y + 18, 22, 30, 180, 180);

        g.setColor(new Color(90, 225, 110));
        g.fillOval(x + 6, y - 2, 28, 34);
        g.setColor(Color.WHITE);
        g.fillOval(x + 22, y + 6, 7, 7);
        g.setColor(Color.BLACK);
        g.fillOval(x + 25, y + 8, 3, 3);
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getCost() {
        return cost;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x - 18
                && mouseX <= x + 52
                && mouseY >= y - 16
                && mouseY <= y + 60;
    }
}

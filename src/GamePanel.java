import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private static final int WIDTH = 1120;
    private static final int HEIGHT = 720;
    private static final int FIRST_MINE_FLOOR_Y = 350;
    private static final int MINE_ROW_GAP = 112;
    private static final int SURFACE_Y = 170;
    private static final int LIFT_X = 92;
    private static final int TOWER_X = 918;
    private static final int TOWER_W = 146;

    private final GemBank bank = new GemBank();
    private final List<Mine> mines = new ArrayList<>();
    private final Courier leafLift = new Courier(
            "Strong Lift Bird",
            LIFT_X,
            290,
            LIFT_X,
            SURFACE_Y,
            new Color(214, 151, 73),
            true,
            0.20,
            25
    );
    private final Courier nestRunner = new Courier(
            "Swift Nest Bird",
            LIFT_X,
            SURFACE_Y,
            TOWER_X + 72,
            SURFACE_Y,
            new Color(203, 97, 172),
            false,
            0.16,
            25
    );

    private Selection selection = Selection.MINE;
    private int selectedMineIndex;
    private boolean selectedGreenBird;
    private int surfaceGems;
    private int surfaceStationX = LIFT_X;
    private double worldTime;
    private double mineScrollY;
    private long lastUpdateTime;

    private final Button optionOne = new Button();
    private final Button optionTwo = new Button();
    private final Button optionThree = new Button();
    private final Button optionFour = new Button();
    private final Button optionFive = new Button();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(96, 187, 240));
        setFocusable(true);

        mines.add(createMine(1));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleClick(event.getX(), event.getY());
            }
        });

        addMouseWheelListener(event -> {
            mineScrollY += event.getWheelRotation() * 44;
            mineScrollY = Math.max(0, Math.min(mineScrollY, getMaxScrollY()));
        });

        lastUpdateTime = System.nanoTime();
        Timer timer = new Timer(16, event -> updateGame());
        timer.start();
    }

    private Mine createMine(int id) {
        return new Mine(id, FIRST_MINE_FLOOR_Y + (id - 1) * MINE_ROW_GAP, 230);
    }

    private void updateGame() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = now;
        worldTime += deltaSeconds;

        for (Mine mine : mines) {
            mine.update(deltaSeconds);
        }

        if (!leafLift.isBusy()) {
            Mine sourceMine = findMineWithGems();
            if (sourceMine != null) {
                int load = sourceMine.collectFromStation(leafLift.getCapacity());
                int liftPickupY = screenY(sourceMine.getFloorY()) - 55;
                leafLift.setRoute(LIFT_X, liftPickupY, LIFT_X, SURFACE_Y);
                leafLift.startTrip(load);
                surfaceStationX = LIFT_X;
            }
        }

        int liftedGems = leafLift.update(deltaSeconds);
        if (liftedGems > 0) {
            surfaceGems += liftedGems;
        }

        if (surfaceGems > 0 && !nestRunner.isBusy()) {
            int load = Math.min(surfaceGems, nestRunner.getCapacity());
            surfaceGems -= load;
            nestRunner.setRoute(surfaceStationX, SURFACE_Y, TOWER_X + 72, SURFACE_Y);
            nestRunner.startTrip(load);
        }

        int deliveredGems = nestRunner.update(deltaSeconds);
        if (deliveredGems > 0) {
            bank.add(deliveredGems);
        }

        repaint();
    }

    private Mine findMineWithGems() {
        for (Mine mine : mines) {
            if (mine.getStationGems() > 0) {
                return mine;
            }
        }
        return null;
    }

    private void handleClick(int mouseX, int mouseY) {
        positionContextButtons();

        if (handleUpgradeClick(mouseX, mouseY)) {
            return;
        }

        if (tryStartBird(mouseX, mouseY)) {
            return;
        }

        if (leafLiftPanelContains(mouseX, mouseY)) {
            selection = Selection.LEAF_LIFT;
            return;
        }

        if (runnerPanelContains(mouseX, mouseY)) {
            selection = Selection.NEST_RUNNER;
            return;
        }

        if (newMineCardContains(mouseX, mouseY)) {
            selection = Selection.ADD_MINE;
            return;
        }

        int rawMouseY = rawY(mouseY);
        for (int i = 0; i < mines.size(); i++) {
            if (mines.get(i).contains(mouseX, rawMouseY)) {
                selectedMineIndex = i;
                selectedGreenBird = false;
                selection = Selection.MINE;
                return;
            }
        }
    }

    private boolean handleUpgradeClick(int mouseX, int mouseY) {
        if (selection == Selection.MINE) {
            if (optionOne.contains(mouseX, mouseY)) {
                buySelectedBirdLevel();
                return true;
            }
            if (optionTwo.contains(mouseX, mouseY)) {
                buySelectedBirdAuto();
                return true;
            }
            if (optionThree.contains(mouseX, mouseY)) {
                handleGreenBirdCard();
                return true;
            }
        } else if (selection == Selection.LEAF_LIFT) {
            if (optionOne.contains(mouseX, mouseY)) {
                upgradeLeafLiftMove();
                return true;
            }
            if (optionTwo.contains(mouseX, mouseY)) {
                upgradeLeafLiftPickup();
                return true;
            }
            if (optionThree.contains(mouseX, mouseY)) {
                upgradeLeafLiftCapacity();
                return true;
            }
        } else if (selection == Selection.NEST_RUNNER) {
            if (optionOne.contains(mouseX, mouseY)) {
                upgradeNestRunnerMove();
                return true;
            }
            if (optionTwo.contains(mouseX, mouseY)) {
                upgradeNestRunnerPickup();
                return true;
            }
            if (optionThree.contains(mouseX, mouseY)) {
                upgradeNestRunnerCapacity();
                return true;
            }
        } else if (selection == Selection.ADD_MINE && optionOne.contains(mouseX, mouseY)) {
            buyNewMine();
            return true;
        }

        return false;
    }

    private boolean tryStartBird(int mouseX, int mouseY) {
        for (Mine mine : mines) {
            int floorY = screenY(mine.getFloorY());
            int lane = 0;
            for (Bird bird : mine.getBirds()) {
                if (bird.tryStartMining(mouseX, mouseY, floorY, lane * 18)) {
                    return true;
                }
                lane++;
            }
        }
        return false;
    }

    private void buySelectedBirdSpeed() {
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();
        int cost = bird.getSpeedCost(mine.getId());
        if (bird.canUpgradeSpeed() && bank.spend(cost)) {
            bird.upgradeSpeed();
        }
    }

    private void buySelectedBirdStrength() {
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();
        int cost = bird.getStrengthCost(mine.getId());
        if (bird.canUpgradeStrength() && bank.spend(cost)) {
            bird.upgradeStrength();
        }
    }

    private void buySelectedBirdMining() {
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();
        int cost = bird.getMiningCost(mine.getId());
        if (bird.canUpgradeMining() && bank.spend(cost)) {
            bird.upgradeMining();
        }
    }

    private void buySelectedBirdAuto() {
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();
        int cost = bird.getAutoMiningCost(mine.getId());
        if (bird.canUnlockAutoMining() && bank.spend(cost)) {
            bird.unlockAutoMining();
        }
    }

    private void handleGreenBirdCard() {
        Mine mine = getSelectedMine();
        if (mine.hasGreenBird()) {
            selectedGreenBird = !selectedGreenBird;
            return;
        }

        int cost = mine.getGreenBirdCost();
        if (mine.canUnlockGreenBird() && bank.spend(cost)) {
            mine.unlockGreenBird();
            selectedGreenBird = true;
        }
    }

    private void upgradeLeafLiftMove() {
        int cost = leafLift.getMoveCost();
        if (leafLift.canUpgradeMove() && bank.spend(cost)) {
            leafLift.upgradeMoveSpeed();
        }
    }

    private void upgradeLeafLiftPickup() {
        int cost = leafLift.getPickupCost();
        if (leafLift.canUpgradePickup() && bank.spend(cost)) {
            leafLift.upgradePickupSpeed();
        }
    }

    private void upgradeLeafLiftCapacity() {
        int cost = leafLift.getCapacityCost();
        if (leafLift.canUpgradeCapacity() && bank.spend(cost)) {
            leafLift.upgradeCapacity();
        }
    }

    private void upgradeNestRunnerMove() {
        int cost = nestRunner.getMoveCost();
        if (nestRunner.canUpgradeMove() && bank.spend(cost)) {
            nestRunner.upgradeMoveSpeed();
        }
    }

    private void upgradeNestRunnerPickup() {
        int cost = nestRunner.getPickupCost();
        if (nestRunner.canUpgradePickup() && bank.spend(cost)) {
            nestRunner.upgradePickupSpeed();
        }
    }

    private void upgradeNestRunnerCapacity() {
        int cost = nestRunner.getCapacityCost();
        if (nestRunner.canUpgradeCapacity() && bank.spend(cost)) {
            nestRunner.upgradeCapacity();
        }
    }

    private void buyNewMine() {
        int cost = getNewMineCost();
        if (bank.spend(cost)) {
            int id = mines.size() + 1;
            mines.add(createMine(id));
            selectedMineIndex = mines.size() - 1;
            selectedGreenBird = false;
            selection = Selection.MINE;
            mineScrollY = Math.min(getMaxScrollY(), Math.max(0, mineScrollY + MINE_ROW_GAP));
        }
    }

    private Mine getSelectedMine() {
        if (selectedMineIndex >= mines.size()) {
            selectedMineIndex = mines.size() - 1;
        }
        return mines.get(selectedMineIndex);
    }

    private Bird getSelectedBird() {
        Mine mine = getSelectedMine();
        if (selectedGreenBird && mine.hasGreenBird()) {
            return mine.getGreenBird();
        }

        selectedGreenBird = false;
        return mine.getBlueBird();
    }

    private void buySelectedBirdLevel() {
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();
        String nextUpgrade = getNextBirdUpgradeType(bird);

        if ("Mining".equals(nextUpgrade)) {
            buySelectedBirdMining();
        } else if ("Walk".equals(nextUpgrade)) {
            buySelectedBirdSpeed();
        } else if ("Strength".equals(nextUpgrade)) {
            buySelectedBirdStrength();
        }
    }

    private String getNextBirdUpgradeType(Bird bird) {
        int bestLevel = Integer.MAX_VALUE;
        String type = "";

        if (bird.canUpgradeMining() && bird.getMiningLevel() < bestLevel) {
            bestLevel = bird.getMiningLevel();
            type = "Mining";
        }
        if (bird.canUpgradeSpeed() && bird.getSpeedLevel() < bestLevel) {
            bestLevel = bird.getSpeedLevel();
            type = "Walk";
        }
        if (bird.canUpgradeStrength() && bird.getStrengthLevel() < bestLevel) {
            type = "Strength";
        }

        return type;
    }

    private int getNextBirdUpgradeCost(Mine mine, Bird bird) {
        String nextUpgrade = getNextBirdUpgradeType(bird);
        if ("Mining".equals(nextUpgrade)) {
            return bird.getMiningCost(mine.getId());
        }
        if ("Walk".equals(nextUpgrade)) {
            return bird.getSpeedCost(mine.getId());
        }
        if ("Strength".equals(nextUpgrade)) {
            return bird.getStrengthCost(mine.getId());
        }
        return 0;
    }

    private boolean canUpgradeBirdLevel(Bird bird) {
        return bird.canUpgradeMining() || bird.canUpgradeSpeed() || bird.canUpgradeStrength();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g);
        drawTopHud(g);
        drawSurfaceAndTower(g);
        drawMines(g);
        drawNewMineCard(g);
        drawActors(g);
        drawContextPanel(g);
    }

    private void drawBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(14, 48, 42), 0, 270, new Color(104, 176, 93));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 238, 138, 72));
        for (int i = 0; i < 5; i++) {
            int drift = (int) (Math.sin(worldTime * 0.35 + i) * 18);
            int x = 235 + i * 78 + drift;
            g.fillPolygon(new int[]{x, x + 72, x - 160}, new int[]{0, 0, 262}, 3);
        }

        for (int i = 0; i < 8; i++) {
            int trunkX = -90 + i * 178;
            int sway = (int) (Math.sin(worldTime * 0.45 + i) * 4);
            g.setColor(new Color(48, 39, 27, 170));
            g.fillRoundRect(trunkX + sway, 42, 38, 245, 20, 20);
            g.setColor(new Color(20, 81, 48, 210));
            g.fillOval(trunkX - 62 + sway, 0, 180, 110);
            g.setColor(new Color(41, 126, 62, 210));
            g.fillOval(trunkX - 6 + sway, 8, 142, 94);
        }

        for (int i = 0; i < 70; i++) {
            int x = (i * 61 + (int) (worldTime * 9)) % (WIDTH + 80) - 40;
            int y = 44 + (i * 37) % 185;
            g.setColor(i % 3 == 0 ? new Color(120, 201, 91, 150) : new Color(32, 112, 64, 145));
            g.fillOval(x, y, 18 + i % 16, 9 + i % 9);
        }

        g.setColor(new Color(71, 139, 61));
        g.fillRect(0, 248, WIDTH, 25);
        g.setColor(new Color(50, 100, 48));
        for (int x = 0; x < WIDTH; x += 38) {
            g.fillOval(x - 8, 238 + (x % 3) * 3, 54, 28);
        }

        g.setColor(new Color(116, 67, 45));
        g.fillRect(0, 270, WIDTH, HEIGHT - 270);

        g.setColor(new Color(91, 52, 40));
        for (int y = 300; y < HEIGHT; y += 52) {
            for (int x = -20; x < WIDTH; x += 115) {
                g.fillPolygon(
                        new int[]{x, x + 18, x + 34, x + 24, x + 7},
                        new int[]{y + 16, y, y + 17, y + 33, y + 30},
                        5
                );
            }
        }

        g.setColor(new Color(61, 38, 30, 115));
        for (int y = 320; y < HEIGHT; y += 72) {
            g.drawLine(0, y, WIDTH, y - 12);
        }
    }

    private void drawMountains(Graphics2D g) {
        g.setColor(new Color(210, 241, 245));
        g.fillPolygon(new int[]{350, 460, 570}, new int[]{250, 120, 250}, 3);
        g.fillPolygon(new int[]{470, 610, 760}, new int[]{250, 70, 250}, 3);
        g.setColor(new Color(144, 195, 199));
        g.fillPolygon(new int[]{460, 570, 610}, new int[]{120, 250, 250}, 3);
        g.fillPolygon(new int[]{610, 760, 685}, new int[]{70, 250, 250}, 3);
    }

    private void drawPineTrees(Graphics2D g) {
        for (int x = 150; x < 820; x += 115) {
            int h = 80 + (x % 3) * 18;
            g.setColor(new Color(72, 106, 80));
            g.fillRect(x + 22, 235 - h / 3, 14, h / 2);
            g.setColor(new Color(58, 130, 87));
            g.fillPolygon(new int[]{x, x + 30, x + 60}, new int[]{248, 248 - h, 248}, 3);
            g.setColor(new Color(42, 104, 73));
            g.fillPolygon(new int[]{x + 5, x + 30, x + 55}, new int[]{222, 222 - h / 2, 222}, 3);
        }
    }

    private void drawTopHud(Graphics2D g) {
        drawInventoryBadge(g, 24, 20);
        drawSmallTopButton(g, 805, 24, "Collection", new Color(139, 86, 207));
        drawSmallTopButton(g, 906, 24, "Awards", new Color(209, 146, 51));
        drawSmallTopButton(g, 1007, 24, "Settings", new Color(126, 137, 154));
    }

    private void drawInventoryBadge(Graphics2D g, int x, int y) {
        drawDarkGoldPanel(g, x, y, 226, 75, 18);
        drawGem(g, x + 18, y + 14, 44, new Color(85, 190, 255));
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(bank.getGems()), x + 82, y + 42);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("gems saved", x + 83, y + 65);
    }

    private void drawSmallTopButton(Graphics2D g, int x, int y, String label, Color color) {
        drawDarkGoldPanel(g, x, y, 78, 70, 18);
        g.setColor(color);
        g.fillOval(x + 18, y + 9, 42, 42);
        g.setColor(Color.WHITE);
        g.fillOval(x + 29, y + 20, 20, 20);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        drawCentered(g, label, x + 39, y + 66);
    }

    private void drawSurfaceAndTower(Graphics2D g) {
        if (selection == Selection.LEAF_LIFT) {
            g.setColor(new Color(255, 231, 100, 120));
            g.fillRoundRect(18, 82, 150, 166, 18, 18);
        }

        g.setColor(new Color(85, 54, 33));
        g.fillRoundRect(34, 118, 22, 126, 9, 9);
        g.fillRoundRect(130, 118, 22, 126, 9, 9);
        g.setColor(new Color(137, 86, 43));
        g.fillRoundRect(44, 132, 98, 82, 12, 12);
        g.setColor(new Color(49, 31, 26));
        g.fillRoundRect(55, 145, 76, 58, 10, 10);
        g.setColor(new Color(222, 161, 64));
        g.fillPolygon(new int[]{22, 92, 162}, new int[]{121, 82, 121}, 3);
        g.setColor(new Color(255, 218, 116));
        g.drawLine(34, 121, 150, 121);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        drawCentered(g, "Lift Birds", 92, 234);

        g.setColor(new Color(101, 73, 45));
        g.setStroke(new BasicStroke(5));
        g.drawLine(156, SURFACE_Y + 14, TOWER_X + TOWER_W / 2, SURFACE_Y + 14);
        g.setStroke(new BasicStroke(1));

        drawTransportPanelCard(g, 176, 112, "Surface", surfaceGems, true, selection == Selection.LEAF_LIFT);
        drawHomeTower(g);
    }

    private void drawTransportPanelCard(Graphics2D g, int x, int y, String title, int gems, boolean lift, boolean selected) {
        if (selected) {
            g.setColor(new Color(255, 231, 100, 120));
            g.fillRoundRect(x - 7, y - 7, 172, 92, 18, 18);
        }
        drawDarkGoldPanel(g, x, y, 158, 78, 14);
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        drawCentered(g, title, x + 79, y + 27);
        drawGem(g, x + 35, y + 43, 18, new Color(85, 190, 255));
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.drawString(gems + " gems", x + 63, y + 59);
    }

    private void drawHomeTower(Graphics2D g) {
        int x = TOWER_X;
        int y = 120;
        if (selection == Selection.NEST_RUNNER) {
            g.setColor(new Color(255, 231, 100, 120));
            g.fillRoundRect(x - 8, y - 8, TOWER_W + 16, 474, 16, 16);
        }

        g.setColor(new Color(81, 47, 30));
        g.fillRoundRect(x + 48, y + 34, 50, 430, 24, 24);
        g.setColor(new Color(122, 72, 39));
        g.fillRoundRect(x + 62, y + 42, 18, 420, 11, 11);

        g.setColor(new Color(50, 95, 49));
        g.fillOval(x - 12, y - 42, TOWER_W + 24, 104);
        g.setColor(new Color(76, 138, 64));
        g.fillOval(x + 22, y - 60, 106, 88);

        g.setColor(new Color(115, 72, 42));
        g.fillRoundRect(x + 5, y + 30, TOWER_W - 10, 52, 24, 24);
        g.setColor(new Color(166, 104, 49));
        g.fillRoundRect(x + 15, y + 39, TOWER_W - 30, 34, 18, 18);
        g.setColor(new Color(55, 33, 26));
        g.fillOval(x + 31, y + 42, 84, 30);
        drawMascotBird(g, x + 52, y + 2, new Color(205, 97, 172), new Color(115, 44, 103), true);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        drawCentered(g, "Home Nest", x + TOWER_W / 2, y + 103);

        int pile = Math.min(15, 3 + bank.getGems() / 900);
        drawGemPile(g, x + 37, y + 352, pile, new Color(85, 190, 255));

        g.setColor(new Color(46, 31, 27, 218));
        for (int row = 0; row < 3; row++) {
            g.fillRoundRect(x + 19, y + 123 + row * 69, 108, 42, 9, 9);
            g.setColor(new Color(176, 107, 50));
            g.fillRoundRect(x + 13, y + 158 + row * 69, 120, 8, 6, 6);
            drawGem(g, x + 39, y + 134 + row * 69, 18, row == 0 ? new Color(85, 190, 255) : new Color(105, 230, 120));
            g.setColor(Color.WHITE);
            g.drawString(row == 0 ? "Saved" : "Stock", x + 67, y + 150 + row * 69);
            g.setColor(new Color(46, 31, 27, 218));
        }
    }

    private void drawMascotBird(Graphics2D g, int x, int y, Color body, Color dark, boolean helmet) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(x - 4, y + 42, 55, 10);
        g.setColor(dark);
        g.fillPolygon(new int[]{x - 10, x + 8, x + 6}, new int[]{y + 27, y + 16, y + 38}, 3);
        g.fillOval(x + 7, y + 25, 34, 17);
        g.setColor(body);
        g.fillOval(x, y + 7, 45, 40);
        g.setColor(body.brighter());
        g.fillOval(x + 11, y + 13, 22, 14);
        g.setColor(new Color(255, 214, 87));
        g.fillPolygon(new int[]{x + 37, x + 54, x + 37}, new int[]{y + 23, y + 29, y + 35}, 3);
        g.setColor(Color.WHITE);
        g.fillOval(x + 28, y + 17, 13, 13);
        g.setColor(Color.BLACK);
        g.fillOval(x + 34, y + 21, 5, 5);
        g.setColor(new Color(255, 145, 115, 90));
        g.fillOval(x + 24, y + 34, 9, 6);

        if (helmet) {
            g.setColor(new Color(220, 151, 45));
            g.fillArc(x + 2, y, 40, 25, 0, 180);
            g.setColor(new Color(115, 72, 32));
            g.fillRoundRect(x + 4, y + 13, 36, 7, 5, 5);
            g.setColor(new Color(126, 225, 255));
            g.fillOval(x + 28, y + 5, 13, 13);
            g.setColor(Color.WHITE);
            g.drawOval(x + 28, y + 5, 13, 13);
        }
    }

    private void drawMines(Graphics2D g) {
        for (int i = 0; i < mines.size(); i++) {
            drawMineRow(g, mines.get(i), i == selectedMineIndex && selection == Selection.MINE);
        }
    }

    private void drawMineRow(Graphics2D g, Mine mine, boolean selected) {
        int caveX = mine.getCaveX();
        int caveY = screenY(mine.getCaveY());
        int floorY = screenY(mine.getFloorY());

        if (floorY < 245 || caveY > HEIGHT + 40) {
            return;
        }

        if (selected) {
            g.setColor(new Color(255, 232, 98, 135));
            g.fillRoundRect(caveX - 7, caveY - 7, mine.getCaveWidth() + 46, 91, 14, 14);
        }

        g.setColor(new Color(121, 67, 45));
        g.fillRoundRect(caveX - 8, caveY + 7, mine.getCaveWidth() + 42, 76, 12, 12);
        g.setColor(new Color(70, 42, 35));
        g.fillRoundRect(mine.getLeftX(), caveY + 26, mine.getTunnelLength() + 20, 41, 9, 9);
        g.setColor(new Color(43, 34, 31));
        g.fillRect(mine.getLeftX() - 14, caveY + 20, 13, 55);

        drawRockTexture(g, caveX + 2, caveY + 14, mine.getCaveWidth() + 26, 52);
        drawLiftConnector(g, LIFT_X + 39, floorY - 25, mine.getBasketX() - 4);
        drawBlueCrystalWall(g, mine.getGemWallX() - 26, caveY + 28, mine.hasGreenBird());
        drawTrack(g, mine.getBasketX() + 42, floorY - 8, mine.getGemWallX() + 22);
        drawConnectedStorage(g, mine.getBasketX() - 6, floorY - 63, mine.getStationGems(), mine.getPrimaryGemColor());

        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString(mine.getName(), caveX + 5, caveY - 8);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Blue level " + mine.getBlueBird().getProgressLevel() + "/15 | waiting: " + mine.getStationGems() + " gems", caveX + 8, floorY + 18);

        if (mine.hasGreenBird()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Green bird active", caveX + mine.getCaveWidth() - 90, floorY + 18);
        } else if (mine.getBlueBird().isLevelFifteen()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Green ready", caveX + mine.getCaveWidth() - 70, floorY + 18);
        }
    }

    private void drawLiftConnector(Graphics2D g, int startX, int y, int endX) {
        g.setColor(new Color(55, 36, 29));
        g.fillRoundRect(startX, y, Math.max(1, endX - startX), 8, 5, 5);
        g.setColor(new Color(151, 94, 48));
        g.drawLine(startX, y + 1, endX, y + 1);
    }

    private void drawRockTexture(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(147, 72, 49));
        for (int i = 0; i < 14; i++) {
            int rx = x + 20 + (i * 47) % Math.max(1, w - 60);
            int ry = y + 12 + (i * 29) % Math.max(1, h - 30);
            g.fillPolygon(new int[]{rx, rx + 13, rx + 27, rx + 20, rx + 5}, new int[]{ry + 12, ry, ry + 10, ry + 24, ry + 22}, 5);
        }
    }

    private void drawBlueCrystalWall(Graphics2D g, int x, int y, boolean hasGreen) {
        g.setColor(new Color(26, 31, 54));
        g.fillRoundRect(x - 10, y - 8, 76, 63, 13, 13);
        g.setColor(new Color(11, 20, 33, 160));
        g.fillOval(x - 2, y, 64, 53);

        int[] gemX = {5, 26, 46, 15, 36, 52, 8, 30};
        int[] gemY = {2, 6, 4, 23, 26, 24, 43, 42};
        int[] gemSize = {18, 15, 16, 16, 19, 14, 14, 16};
        for (int i = 0; i < gemX.length; i++) {
            Color color = hasGreen && i > 5 ? new Color(105, 230, 120) : new Color(85, 190, 255);
            drawCrystal(g, x + gemX[i], y + gemY[i], gemSize[i], color);
        }
    }

    private void drawCrystal(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 65));
        g.fillOval(x - 5, y - 5, size + 10, size + 10);
        g.setColor(color.brighter());
        g.fillPolygon(new int[]{x + size / 2, x + size, x + size / 2, x}, new int[]{y, y + size / 2, y + size, y + size / 2}, 4);
        g.setColor(Color.WHITE);
        g.drawLine(x + size / 2, y + 2, x + size - 4, y + size / 2);
        g.setColor(color.darker());
        g.drawPolygon(new int[]{x + size / 2, x + size, x + size / 2, x}, new int[]{y, y + size / 2, y + size, y + size / 2}, 4);
    }

    private void drawTrack(Graphics2D g, int startX, int y, int endX) {
        int left = Math.min(startX, endX);
        int right = Math.max(startX, endX);
        g.setColor(new Color(86, 56, 43));
        g.setStroke(new BasicStroke(5));
        g.drawLine(left, y, right, y);
        g.drawLine(left, y + 17, right, y + 17);
        g.setStroke(new BasicStroke(2));
        for (int x = left + 4; x < right; x += 34) {
            g.drawLine(x, y - 6, x + 13, y + 25);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawConnectedStorage(Graphics2D g, int x, int y, int gems, Color gemColor) {
        g.setColor(new Color(67, 43, 31));
        g.fillRoundRect(x - 44, y + 39, 48, 12, 6, 6);
        g.setColor(new Color(127, 77, 43));
        g.fillPolygon(new int[]{x - 4, x + 63, x + 51, x + 4}, new int[]{y + 24, y + 24, y + 50, y + 50}, 4);
        g.setColor(new Color(187, 121, 58));
        g.drawLine(x + 1, y + 28, x + 58, y + 28);
        g.setColor(new Color(54, 34, 27));
        g.fillOval(x + 5, y + 46, 13, 13);
        g.fillOval(x + 43, y + 46, 13, 13);
        g.setColor(new Color(86, 53, 34));
        g.drawLine(x - 1, y + 38, x + 54, y + 38);

        for (int i = 0; i < Math.min(gems, 8); i++) {
            drawCrystal(g, x + 5 + i * 6, y + 12 + (i % 2) * 5, 10, gemColor);
        }
    }

    private void drawNewMineCard(Graphics2D g) {
        int rawY = FIRST_MINE_FLOOR_Y + mines.size() * MINE_ROW_GAP - 110;
        int y = screenY(rawY);
        if (y < 250 || y > HEIGHT - 30) {
            return;
        }

        int x = 96;
        int w = 610;
        int h = 94;
        if (selection == Selection.ADD_MINE) {
            g.setColor(new Color(255, 231, 100, 125));
            g.fillRoundRect(x - 8, y - 8, w + 16, h + 16, 18, 18);
        }

        drawPurplePanel(g, x, y, w, h, 18);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.drawString("Open Mine " + (mines.size() + 1), x + 25, y + 37);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.drawString("Starts with a blue bird that must be tapped until Auto Mine is unlocked.", x + 25, y + 65);
        drawGoldButton(g, x + w - 148, y + 21, 122, 52, String.valueOf(getNewMineCost()));
    }

    private boolean newMineCardContains(int mouseX, int mouseY) {
        int rawY = FIRST_MINE_FLOOR_Y + mines.size() * MINE_ROW_GAP - 110;
        int y = screenY(rawY);
        return mouseX >= 96 && mouseX <= 706 && mouseY >= y && mouseY <= y + 94;
    }

    private boolean leafLiftPanelContains(int mouseX, int mouseY) {
        return mouseX >= 26 && mouseX <= 158 && mouseY >= 126 && mouseY <= 236;
    }

    private boolean runnerPanelContains(int mouseX, int mouseY) {
        return mouseX >= TOWER_X && mouseX <= TOWER_X + TOWER_W && mouseY >= 120 && mouseY <= 578;
    }

    private void drawActors(Graphics2D g) {
        for (Mine mine : mines) {
            int lane = 0;
            int floorY = screenY(mine.getFloorY());
            for (Bird bird : mine.getBirds()) {
                bird.draw(g, floorY, mine.getGemWallX(), lane * 18);
                lane++;
            }
        }
        leafLift.draw(g, selection == Selection.LEAF_LIFT);
        nestRunner.draw(g, selection == Selection.NEST_RUNNER);
    }

    private void drawContextPanel(Graphics2D g) {
        positionContextButtons();

        int x = 735;
        int y = 292;
        int w = 350;
        int h = selection == Selection.MINE ? 420 : 232;
        drawDarkGoldPanel(g, x, y, w, h, 18);

        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);

        if (selection == Selection.MINE) {
            Mine mine = getSelectedMine();
            Bird bird = getSelectedBird();
            drawMineshaftUpgradePanel(g, mine, bird, x, y, w);
        } else if (selection == Selection.LEAF_LIFT) {
            drawCourierPanel(g, "Strong Lift Bird", leafLift, x, y);
        } else if (selection == Selection.NEST_RUNNER) {
            drawCourierPanel(g, "Swift Nest Bird", nestRunner, x, y);
        } else {
            g.drawString("Open Mine " + (mines.size() + 1), x + 18, y + 30);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.setColor(new Color(226, 238, 255));
            g.drawString("A new mine starts below the others.", x + 18, y + 52);
            optionOne.drawWideCard(g, "Buy Mine", getNewMineCost() + " gems", "Costs more each time.", "tunnel", bank.canAfford(getNewMineCost()));
        }
    }

    private void drawMineshaftUpgradePanel(Graphics2D g, Mine mine, Bird bird, int x, int y, int w) {
        int level = bird.getProgressLevel();
        int nextCost = getNextBirdUpgradeCost(mine, bird);
        boolean canLevel = canUpgradeBirdLevel(bird);
        boolean canAffordLevel = canLevel && bank.canAfford(nextCost);

        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("Mineshaft " + mine.getId() + " Level " + level, x + 22, y + 34);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(226, 238, 255));
        g.drawString("Selected: " + bird.getName(), x + 22, y + 57);

        drawVideoStatRow(g, x + 24, y + 86, "Total Extraction", String.format("%.1f/s", getExtractionPreview(bird)));
        drawVideoStatRow(g, x + 24, y + 126, "Miners", mine.hasGreenBird() ? "2" : "1");
        drawVideoStatRow(g, x + 24, y + 166, "Walking Speed", String.valueOf(bird.getSpeedLevel()));
        drawVideoStatRow(g, x + 24, y + 206, "Mining Speed", (4 + bird.getMiningLevel()) + "/s");
        drawVideoStatRow(g, x + 24, y + 246, "Worker Capacity", String.valueOf(bird.getCarryCapacity()));

        g.setColor(new Color(255, 230, 115));
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString(level < 15 ? "Next boost at level 15." : "Green bird boost is ready.", x + 24, y + 282);

        drawVideoUpgradeButton(g, optionOne, canLevel ? "UPGRADE" : "MAX LEVEL", canLevel ? String.valueOf(nextCost) : "DONE", canAffordLevel);

        String autoText = bird.hasAutoMining() ? "ON" : bird.getAutoMiningCost(mine.getId()) + " gems";
        drawSmallVideoButton(g, optionTwo, "Auto Mine", autoText, bird.canUnlockAutoMining() && bank.canAfford(bird.getAutoMiningCost(mine.getId())));

        String greenText = mine.hasGreenBird() ? "Switch bird" : (mine.canUnlockGreenBird() ? mine.getGreenBirdCost() + " gems" : "Needs L15");
        drawSmallVideoButton(g, optionThree, mine.hasGreenBird() ? (selectedGreenBird ? "Blue Bird" : "Green Bird") : "Green Bird", greenText, mine.hasGreenBird() || (mine.canUnlockGreenBird() && bank.canAfford(mine.getGreenBirdCost())));
    }

    private double getExtractionPreview(Bird bird) {
        double miningPower = 3.0 + bird.getMiningLevel() * 0.7;
        double walkingPower = 1.0 + bird.getSpeedLevel() * 0.12;
        return bird.getCarryCapacity() * bird.getGemValue() * walkingPower / miningPower;
    }

    private void drawVideoStatRow(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(new Color(25, 37, 51, 230));
        g.fillRoundRect(x, y - 22, 292, 31, 8, 8);
        g.setColor(new Color(66, 92, 115));
        g.drawRoundRect(x, y - 22, 292, 31, 8, 8);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(220, 232, 244));
        g.drawString(label, x + 13, y - 2);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(value, x + 279 - metrics.stringWidth(value), y - 2);
    }

    private void drawVideoUpgradeButton(Graphics2D g, Button button, String title, String cost, boolean enabled) {
        Color top = enabled ? new Color(81, 193, 72) : new Color(90, 94, 99);
        Color bottom = enabled ? new Color(43, 132, 48) : new Color(57, 60, 64);
        GradientPaint paint = new GradientPaint(button.x, button.y, top, button.x, button.y + button.height, bottom);
        g.setPaint(paint);
        g.fillRoundRect(button.x, button.y, button.width, button.height, 12, 12);
        g.setColor(enabled ? new Color(196, 255, 165) : new Color(135, 139, 145));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 12, 12);
        g.setStroke(new BasicStroke(1));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(Color.WHITE);
        drawCentered(g, title, button.x + button.width / 2, button.y + 25);
        drawGem(g, button.x + 92, button.y + 33, 16, new Color(85, 190, 255));
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(cost, button.x + 116, button.y + 47);
    }

    private void drawSmallVideoButton(Graphics2D g, Button button, String title, String value, boolean enabled) {
        g.setColor(enabled ? new Color(31, 73, 118, 238) : new Color(55, 58, 66, 230));
        g.fillRoundRect(button.x, button.y, button.width, button.height, 10, 10);
        g.setColor(enabled ? new Color(120, 216, 255) : new Color(120, 124, 132));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 10, 10);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        drawCentered(g, title, button.x + button.width / 2, button.y + 18);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(226, 238, 255));
        drawCentered(g, value, button.x + button.width / 2, button.y + 38);
    }

    private void drawCourierPanel(Graphics2D g, String title, Courier courier, int x, int y) {
        g.drawString(title, x + 18, y + 30);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(226, 238, 255));
        g.drawString("Capacity: " + courier.getCapacity() + " gems", x + 18, y + 52);
        optionOne.drawSmallCard(g, "Move", "Lv." + courier.getMoveLevel(), courier.canUpgradeMove() ? courier.getMoveCost() + "" : "max", "runner", courier.canUpgradeMove() && bank.canAfford(courier.getMoveCost()));
        optionTwo.drawSmallCard(g, "Pickup", "Lv." + courier.getPickupLevel(), courier.canUpgradePickup() ? courier.getPickupCost() + "" : "max", "clock", courier.canUpgradePickup() && bank.canAfford(courier.getPickupCost()));
        optionThree.drawSmallCard(g, "Capacity", "Lv." + courier.getCapacityLevel(), courier.canUpgradeCapacity() ? courier.getCapacityCost() + "" : "max", "box", courier.canUpgradeCapacity() && bank.canAfford(courier.getCapacityCost()));
    }

    private void positionContextButtons() {
        int x = 755;
        int y = 360;
        if (selection == Selection.MINE) {
            optionOne.setBounds(774, 586, 272, 58);
            optionTwo.setBounds(774, 653, 128, 46);
            optionThree.setBounds(918, 653, 128, 46);
            optionFour.setBounds(-100, -100, 1, 1);
            optionFive.setBounds(-100, -100, 1, 1);
        } else if (selection == Selection.LEAF_LIFT || selection == Selection.NEST_RUNNER) {
            optionOne.setBounds(x, y, 96, 92);
            optionTwo.setBounds(x + 110, y, 96, 92);
            optionThree.setBounds(x + 220, y, 96, 92);
            optionFour.setBounds(-100, -100, 1, 1);
            optionFive.setBounds(-100, -100, 1, 1);
        } else {
            optionOne.setBounds(x, y, 210, 86);
            optionTwo.setBounds(-100, -100, 1, 1);
            optionThree.setBounds(-100, -100, 1, 1);
            optionFour.setBounds(-100, -100, 1, 1);
            optionFive.setBounds(-100, -100, 1, 1);
        }
    }

    private int getNewMineCost() {
        int nextMine = mines.size() + 1;
        return 420 + nextMine * nextMine * 180;
    }

    private double getMaxScrollY() {
        int bottom = FIRST_MINE_FLOOR_Y + mines.size() * MINE_ROW_GAP + 85;
        return Math.max(0, bottom - HEIGHT);
    }

    private int screenY(int rawY) {
        return rawY - (int) mineScrollY;
    }

    private int rawY(int screenY) {
        return screenY + (int) mineScrollY;
    }

    private void drawDarkGoldPanel(Graphics2D g, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(9, 18, 28, 228));
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(new Color(213, 148, 58));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, w, h, arc, arc);
        g.setColor(new Color(255, 229, 132, 85));
        g.drawRoundRect(x + 3, y + 3, w - 6, h - 6, arc, arc);
        g.setStroke(new BasicStroke(1));
    }

    private void drawPurplePanel(Graphics2D g, int x, int y, int w, int h, int arc) {
        GradientPaint paint = new GradientPaint(x, y, new Color(91, 37, 109, 235), x + w, y + h, new Color(35, 32, 92, 235));
        g.setPaint(paint);
        g.fillRoundRect(x, y, w, h, arc, arc);
        g.setColor(new Color(255, 218, 116));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, w, h, arc, arc);
        g.setStroke(new BasicStroke(1));
    }

    private void drawGoldButton(Graphics2D g, int x, int y, int w, int h, String text) {
        GradientPaint paint = new GradientPaint(x, y, new Color(255, 208, 75), x, y + h, new Color(187, 112, 33));
        g.setPaint(paint);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        drawCentered(g, text, x + w / 2, y + 34);
    }

    private void drawGem(Graphics2D g, int x, int y, int size, Color color) {
        int half = size / 2;
        g.setColor(color.brighter());
        g.fillPolygon(new int[]{x + half, x + size, x + half, x}, new int[]{y, y + half, y + size, y + half}, 4);
        g.setColor(Color.WHITE);
        g.drawLine(x + half, y + 2, x + size - 4, y + half);
        g.setColor(color.darker());
        g.drawPolygon(new int[]{x + half, x + size, x + half, x}, new int[]{y, y + half, y + size, y + half}, 4);
    }

    private void drawGemPile(Graphics2D g, int x, int y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            int row = i / 5;
            int col = i % 5;
            drawCrystal(g, x + col * 14 - row * 3, y + 46 - row * 11, 18, i % 2 == 0 ? color : color.darker());
        }
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private enum Selection {
        MINE,
        LEAF_LIFT,
        NEST_RUNNER,
        ADD_MINE
    }

    private static class Button {
        private int x;
        private int y;
        private int width;
        private int height;

        void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void drawSmallCard(Graphics2D g, String title, String level, String cost, String icon, boolean enabled) {
            drawBase(g, enabled);
            drawIcon(g, icon, x + 31, y + 11, enabled);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.setColor(Color.WHITE);
            drawCentered(g, title, y + 51);
            g.setFont(new Font("Arial", Font.PLAIN, 11));
            g.drawString(level, x + width - 34, y + 17);
            g.setColor(new Color(224, 240, 255));
            drawCentered(g, cost, y + 69);
            g.setColor(enabled ? new Color(75, 158, 55) : new Color(70, 70, 70));
            g.fillRoundRect(x + 12, y + 74, width - 24, 15, 5, 5);
        }

        void drawWideCard(Graphics2D g, String title, String cost, String description, String icon, boolean enabled) {
            drawBase(g, enabled);
            drawIcon(g, icon, x + 14, y + 20, enabled);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(Color.WHITE);
            g.drawString(title, x + 64, y + 29);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.setColor(new Color(224, 242, 218));
            g.drawString(cost, x + 64, y + 50);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.drawString(description, x + 64, y + 70);
        }

        private void drawBase(Graphics2D g, boolean enabled) {
            g.setColor(enabled ? new Color(34, 68, 120, 238) : new Color(50, 52, 60, 230));
            g.fillRoundRect(x, y, width, height, 13, 13);
            g.setColor(enabled ? new Color(127, 214, 255) : new Color(116, 118, 132));
            g.drawRoundRect(x, y, width, height, 13, 13);
        }

        private void drawIcon(Graphics2D g, String icon, int iconX, int iconY, boolean enabled) {
            Color main = enabled ? new Color(238, 226, 136) : new Color(154, 158, 145);
            Color accent = enabled ? new Color(113, 208, 235) : new Color(119, 126, 119);
            g.setColor(main);
            if ("feather".equals(icon)) {
                g.fillOval(iconX + 5, iconY, 18, 30);
                g.setColor(accent);
                g.drawLine(iconX + 14, iconY + 3, iconX + 7, iconY + 30);
            } else if ("muscle".equals(icon)) {
                g.fillOval(iconX + 2, iconY + 10, 20, 18);
                g.fillRoundRect(iconX + 16, iconY + 16, 18, 10, 8, 8);
            } else if ("clock".equals(icon)) {
                g.fillOval(iconX + 3, iconY + 2, 31, 31);
                g.setColor(new Color(35, 58, 50));
                g.drawLine(iconX + 18, iconY + 17, iconX + 18, iconY + 7);
                g.drawLine(iconX + 18, iconY + 17, iconX + 28, iconY + 17);
            } else if ("box".equals(icon)) {
                g.fillRoundRect(iconX + 2, iconY + 10, 30, 22, 5, 5);
                g.setColor(new Color(88, 58, 35));
                g.drawLine(iconX + 2, iconY + 20, iconX + 32, iconY + 20);
            } else if ("runner".equals(icon)) {
                g.fillOval(iconX + 4, iconY + 5, 25, 18);
                g.setColor(accent);
                g.drawLine(iconX + 2, iconY + 28, iconX + 29, iconY + 28);
            } else if ("egg".equals(icon)) {
                g.fillOval(iconX + 6, iconY + 2, 24, 32);
                g.setColor(accent);
                g.fillOval(iconX + 14, iconY + 15, 9, 12);
            } else if ("gear".equals(icon)) {
                g.fillOval(iconX + 5, iconY + 5, 26, 26);
                g.setColor(new Color(40, 55, 54));
                g.fillOval(iconX + 12, iconY + 12, 12, 12);
            } else if ("tunnel".equals(icon)) {
                g.fillRoundRect(iconX + 2, iconY + 8, 32, 26, 12, 12);
                g.setColor(new Color(35, 32, 29));
                g.fillRoundRect(iconX + 10, iconY + 17, 16, 17, 8, 8);
            }
        }

        private void drawCentered(Graphics2D g, String text, int baselineY) {
            FontMetrics metrics = g.getFontMetrics();
            int textX = x + (width - metrics.stringWidth(text)) / 2;
            g.drawString(text, textX, baselineY);
        }
    }
}

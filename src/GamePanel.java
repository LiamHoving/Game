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
    private static final int FIRST_MINE_FLOOR_Y = 360;
    private static final int MINE_ROW_GAP = 145;
    private static final int SURFACE_Y = 170;
    private static final int TOWER_X = 918;
    private static final int TOWER_W = 146;

    private final GemBank bank = new GemBank();
    private final List<Mine> mines = new ArrayList<>();
    private final Courier leafLift = new Courier(
            "Strong Lift Bird",
            430,
            290,
            430,
            SURFACE_Y,
            new Color(214, 151, 73),
            true,
            0.20,
            25
    );
    private final Courier nestRunner = new Courier(
            "Swift Nest Bird",
            430,
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
    private int surfaceStationX = 430;
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
        return new Mine(id, FIRST_MINE_FLOOR_Y + (id - 1) * MINE_ROW_GAP, 170);
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
                int basketCenterX = sourceMine.getBasketCenterX();
                leafLift.setRoute(basketCenterX, screenY(sourceMine.getFloorY()) - 68, basketCenterX, SURFACE_Y);
                leafLift.startTrip(load);
                surfaceStationX = basketCenterX;
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
                buySelectedBirdSpeed();
                return true;
            }
            if (optionTwo.contains(mouseX, mouseY)) {
                buySelectedBirdStrength();
                return true;
            }
            if (optionThree.contains(mouseX, mouseY)) {
                buySelectedBirdMining();
                return true;
            }
            if (optionFour.contains(mouseX, mouseY)) {
                buySelectedBirdAuto();
                return true;
            }
            if (optionFive.contains(mouseX, mouseY)) {
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
        GradientPaint sky = new GradientPaint(0, 0, new Color(57, 169, 239), 0, 260, new Color(182, 232, 252));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(255, 255, 255, 70));
        for (int i = 0; i < 4; i++) {
            int offset = (int) ((worldTime * 8 + i * 260) % 900);
            g.fillPolygon(new int[]{120 + offset, 230 + offset, -40 + offset}, new int[]{85, 85, 520}, 3);
        }

        drawMountains(g);
        drawPineTrees(g);

        g.setColor(new Color(66, 169, 66));
        g.fillRect(0, 248, WIDTH, 22);

        g.setColor(new Color(183, 73, 47));
        g.fillRect(0, 270, WIDTH, HEIGHT - 270);

        g.setColor(new Color(161, 62, 42));
        for (int y = 300; y < HEIGHT; y += 52) {
            for (int x = -20; x < WIDTH; x += 115) {
                g.fillPolygon(
                        new int[]{x, x + 18, x + 34, x + 24, x + 7},
                        new int[]{y + 16, y, y + 17, y + 33, y + 30},
                        5
                );
            }
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
        g.setColor(new Color(38, 47, 56));
        g.fillRoundRect(26, 126, 132, 110, 10, 10);
        g.setColor(new Color(79, 154, 196));
        g.fillRoundRect(38, 138, 108, 70, 8, 8);
        g.setColor(new Color(218, 70, 34));
        g.fillPolygon(new int[]{28, 92, 156}, new int[]{126, 88, 126}, 3);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        drawCentered(g, "Lift Tower", 92, 183);

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

        g.setColor(new Color(55, 72, 82));
        g.fillRoundRect(x, y, TOWER_W, 458, 10, 10);
        g.setColor(new Color(112, 167, 186));
        g.fillRoundRect(x + 14, y + 20, TOWER_W - 28, 405, 8, 8);

        g.setColor(new Color(217, 72, 35));
        g.fillPolygon(new int[]{x - 10, x + TOWER_W / 2, x + TOWER_W + 10}, new int[]{y + 20, y - 26, y + 20}, 3);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        drawCentered(g, "Home Tower", x + TOWER_W / 2, y + 56);

        int pile = Math.min(15, 3 + bank.getGems() / 900);
        drawGemPile(g, x + 36, y + 330, pile, new Color(85, 190, 255));

        g.setColor(new Color(30, 48, 58));
        for (int row = 0; row < 3; row++) {
            g.fillRoundRect(x + 24, y + 92 + row * 74, 98, 46, 8, 8);
            drawGem(g, x + 44, y + 104 + row * 74, 20, row == 0 ? new Color(85, 190, 255) : new Color(105, 230, 120));
            g.setColor(Color.WHITE);
            g.drawString(row == 0 ? "Saved" : "Stock", x + 72, y + 121 + row * 74);
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
            g.fillRoundRect(caveX - 8, caveY - 8, mine.getCaveWidth() + 86, 138, 20, 20);
        }

        g.setColor(new Color(124, 63, 46));
        g.fillRoundRect(caveX - 26, caveY - 4, mine.getCaveWidth() + 88, 132, 18, 18);
        g.setColor(new Color(83, 45, 38));
        g.fillRoundRect(mine.getLeftX(), caveY + 28, mine.getTunnelLength() + 30, 72, 16, 16);

        drawRockTexture(g, caveX - 15, caveY + 8, mine.getCaveWidth() + 68, 108);
        drawBlueCrystalWall(g, mine.getGemWallX() - 35, caveY + 34, mine.hasGreenBird());
        drawTrack(g, mine.getGemWallX() + 25, floorY + 10, mine.getBasketX() + 45);
        drawConnectedStorage(g, mine.getBasketX() + 12, floorY - 50, mine.getStationGems(), mine.getPrimaryGemColor());

        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        g.drawString(mine.getName(), caveX + 12, caveY - 12);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Blue level " + mine.getBlueBird().getProgressLevel() + "/16 | waiting: " + mine.getStationGems() + " gems", caveX + 15, floorY + 42);

        if (mine.hasGreenBird()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Green bird active", caveX + mine.getCaveWidth() - 75, floorY + 42);
        } else if (mine.getBlueBird().isLevelFifteen()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Green ready", caveX + mine.getCaveWidth() - 58, floorY + 42);
        }
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
        g.fillRoundRect(x - 12, y - 10, 88, 86, 15, 15);
        for (int i = 0; i < 9; i++) {
            int gx = x + (i % 3) * 24;
            int gy = y + (i / 3) * 24;
            Color color = hasGreen && i > 5 ? new Color(105, 230, 120) : new Color(85, 190, 255);
            drawCrystal(g, gx, gy, 21, color);
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
        g.setColor(new Color(86, 56, 43));
        g.setStroke(new BasicStroke(5));
        g.drawLine(startX, y, endX, y);
        g.drawLine(startX, y + 17, endX, y + 17);
        g.setStroke(new BasicStroke(2));
        for (int x = startX + 4; x < endX; x += 34) {
            g.drawLine(x, y - 6, x + 13, y + 25);
        }
        g.setStroke(new BasicStroke(1));
    }

    private void drawConnectedStorage(Graphics2D g, int x, int y, int gems, Color gemColor) {
        g.setColor(new Color(76, 45, 30));
        g.fillRoundRect(x - 22, y + 25, 96, 35, 9, 9);
        g.setColor(new Color(141, 86, 48));
        g.fillRoundRect(x - 12, y + 18, 76, 30, 8, 8);
        g.setColor(new Color(54, 34, 27));
        g.fillOval(x - 9, y + 52, 16, 16);
        g.fillOval(x + 44, y + 52, 16, 16);
        g.setColor(new Color(86, 53, 34));
        g.drawLine(x - 12, y + 34, x + 64, y + 34);
        for (int i = 0; i < Math.min(gems, 8); i++) {
            drawCrystal(g, x - 8 + i * 8, y + 7 + (i % 2) * 5, 12, gemColor);
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
        int h = selection == Selection.MINE ? 304 : 232;
        drawDarkGoldPanel(g, x, y, w, h, 18);

        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);

        if (selection == Selection.MINE) {
            Mine mine = getSelectedMine();
            Bird bird = getSelectedBird();
            g.drawString("Mine " + mine.getId() + " Upgrades", x + 18, y + 29);
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.setColor(new Color(226, 238, 255));
            g.drawString("Editing " + bird.getName() + " | level " + bird.getProgressLevel() + "/16", x + 18, y + 50);

            optionOne.drawSmallCard(g, "Walk", "Lv." + bird.getSpeedLevel(), bird.canUpgradeSpeed() ? bird.getSpeedCost(mine.getId()) + "" : "max", "feather", bird.canUpgradeSpeed() && bank.canAfford(bird.getSpeedCost(mine.getId())));
            optionTwo.drawSmallCard(g, "Strength", "Lv." + bird.getStrengthLevel(), bird.canUpgradeStrength() ? bird.getStrengthCost(mine.getId()) + "" : "max", "muscle", bird.canUpgradeStrength() && bank.canAfford(bird.getStrengthCost(mine.getId())));
            optionThree.drawSmallCard(g, "Mining", "Lv." + bird.getMiningLevel(), bird.canUpgradeMining() ? bird.getMiningCost(mine.getId()) + "" : "max", "clock", bird.canUpgradeMining() && bank.canAfford(bird.getMiningCost(mine.getId())));

            String autoCost = bird.hasAutoMining() ? "on" : (bird.canUnlockAutoMining() ? bird.getAutoMiningCost(mine.getId()) + "" : "L15");
            optionFour.drawSmallCard(g, "Auto", "Lv.16", autoCost, "gear", bird.canUnlockAutoMining() && bank.canAfford(bird.getAutoMiningCost(mine.getId())));

            String greenCost = mine.hasGreenBird() ? "switch" : (mine.canUnlockGreenBird() ? mine.getGreenBirdCost() + "" : "L15");
            optionFive.drawSmallCard(g, mine.hasGreenBird() ? (selectedGreenBird ? "Blue" : "Green") : "Green", "Bird", greenCost, "egg", mine.hasGreenBird() || (mine.canUnlockGreenBird() && bank.canAfford(mine.getGreenBirdCost())));
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
            optionOne.setBounds(x, y, 96, 92);
            optionTwo.setBounds(x + 110, y, 96, 92);
            optionThree.setBounds(x + 220, y, 96, 92);
            optionFour.setBounds(x, y + 110, 96, 92);
            optionFive.setBounds(x + 110, y + 110, 96, 92);
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

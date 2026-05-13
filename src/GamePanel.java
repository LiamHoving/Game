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
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class GamePanel extends JPanel {
    private static final int WIDTH = 1120;
    private static final int HEIGHT = 720;
    private static final int FIRST_MINE_FLOOR_Y = 360;
    private static final int MINE_ROW_GAP = 118;
    private static final int SURFACE_Y = 170;
    private static final int LIFT_X = 92;
    private static final int TOWER_X = 918;
    private static final int TOWER_W = 146;
    private static final int MINE_VIEW_TOP = 270;
    private static final int MINE_VIEW_BOTTOM = HEIGHT - 12;
    private static final int MINE_VIEW_RIGHT = 724;
    private static final Image SURFACE_BACKGROUND_ART = loadSprite("assets/surface_forest_background.png");
    private static final Image MINE_DIRT_BACKGROUND_ART = loadSprite("assets/mine_dirt_background.png");
    private static final Image MINE_ROW_ART = loadSprite("assets/mine_row_empty_design.png");
    private static final Image HOME_NEST_ART = loadSprite("assets/home_nest_design.png");
    private static final Image LIFT_EMPTY_ART = loadSprite("assets/lift_empty_design.png");
    private static final Image LIFT_FULL_ART = loadSprite("assets/lift_full_design.png");
    private static final Image GEM_SINGLE_ART = loadSprite("assets/gem_single_design.png");
    private static final Image GEM_SMALL_PILE_ART = loadSprite("assets/gem_small_pile_design.png");
    private static final Image GEM_MEDIUM_PILE_ART = loadSprite("assets/gem_medium_pile_design.png");
    private static final Image GEM_LARGE_PILE_ART = loadSprite("assets/gem_large_pile_design.png");

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
    private boolean mineUpgradePopupOpen;
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
    private final Button closePopup = new Button();

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
            if (mineUpgradePopupOpen) {
                return;
            }
            mineScrollY += event.getWheelRotation() * 34;
            mineScrollY = Math.max(0, Math.min(mineScrollY, getMaxScrollY()));
        });

        lastUpdateTime = System.nanoTime();
        Timer timer = new Timer(16, event -> updateGame());
        timer.start();
    }

    private static Image loadSprite(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException exception) {
            return null;
        }
    }

    private Mine createMine(int id) {
        return new Mine(id, FIRST_MINE_FLOOR_Y + (id - 1) * MINE_ROW_GAP, 300);
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

        if (mineUpgradePopupOpen) {
            if (!minePopupContains(mouseX, mouseY)) {
                mineUpgradePopupOpen = false;
            }
            return;
        }

        if (tryStartBird(mouseX, mouseY)) {
            return;
        }

        if (leafLiftPanelContains(mouseX, mouseY)) {
            mineUpgradePopupOpen = false;
            selection = Selection.LEAF_LIFT;
            return;
        }

        if (runnerPanelContains(mouseX, mouseY)) {
            mineUpgradePopupOpen = false;
            selection = Selection.NEST_RUNNER;
            return;
        }

        if (newMineCardContains(mouseX, mouseY)) {
            mineUpgradePopupOpen = false;
            selection = Selection.ADD_MINE;
            return;
        }

        if (mouseY >= MINE_VIEW_TOP && mouseY <= MINE_VIEW_BOTTOM && mouseX <= MINE_VIEW_RIGHT) {
            int rawMouseY = rawY(mouseY);
            for (int i = 0; i < mines.size(); i++) {
                if (mines.get(i).contains(mouseX, rawMouseY)) {
                    selectedMineIndex = i;
                    selectedGreenBird = false;
                    selection = Selection.MINE;
                    mineUpgradePopupOpen = true;
                    return;
                }
            }
        }
    }

    private boolean handleUpgradeClick(int mouseX, int mouseY) {
        if (selection == Selection.MINE) {
            if (!mineUpgradePopupOpen) {
                return false;
            }
            if (closePopup.contains(mouseX, mouseY)) {
                mineUpgradePopupOpen = false;
                return true;
            }
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

    private boolean minePopupContains(int mouseX, int mouseY) {
        int popupX = 230;
        int popupY = 118;
        int popupW = 660;
        int popupH = 486;
        return mouseX >= popupX && mouseX <= popupX + popupW
                && mouseY >= popupY && mouseY <= popupY + popupH;
    }

    private boolean tryStartBird(int mouseX, int mouseY) {
        if (mouseY < MINE_VIEW_TOP || mouseY > MINE_VIEW_BOTTOM || mouseX > MINE_VIEW_RIGHT) {
            return false;
        }

        for (Mine mine : mines) {
            int floorY = screenY(mine.getFloorY());
            int lane = 0;
            for (Bird bird : mine.getBirds()) {
                if (bird.tryStartMining(mouseX, mouseY, floorY, lane * 14)) {
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
            mineUpgradePopupOpen = true;
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
        drawMineViewport(g);
        drawTransportActors(g);
        if (mineUpgradePopupOpen) {
            drawMineUpgradePopup(g);
        } else {
            drawContextPanel(g);
        }
    }

    private void drawMineViewport(Graphics2D g) {
        Shape oldClip = g.getClip();
        g.setClip(0, MINE_VIEW_TOP, MINE_VIEW_RIGHT, MINE_VIEW_BOTTOM - MINE_VIEW_TOP);
        drawMines(g);
        drawNewMineCard(g);
        drawMineActors(g);
        g.setClip(oldClip);
        drawMineScrollBar(g);
    }

    private void drawMineScrollBar(Graphics2D g) {
        double maxScroll = getMaxScrollY();
        if (maxScroll <= 0) {
            return;
        }

        int trackX = MINE_VIEW_RIGHT - 16;
        int trackY = MINE_VIEW_TOP + 8;
        int trackH = MINE_VIEW_BOTTOM - MINE_VIEW_TOP - 16;
        int thumbH = Math.max(42, (int) (trackH * ((double) trackH / (trackH + maxScroll))));
        int thumbY = trackY + (int) ((trackH - thumbH) * (mineScrollY / maxScroll));

        g.setColor(new Color(16, 25, 32, 130));
        g.fillRoundRect(trackX, trackY, 7, trackH, 5, 5);
        g.setColor(new Color(255, 207, 82, 210));
        g.fillRoundRect(trackX - 1, thumbY, 9, thumbH, 6, 6);
    }

    private void drawBackground(Graphics2D g) {
        if (SURFACE_BACKGROUND_ART != null) {
            g.drawImage(SURFACE_BACKGROUND_ART, 0, 0, WIDTH, MINE_VIEW_TOP, null);
        } else {
            drawPaintedSurfaceBackground(g);
        }

        g.setColor(new Color(71, 139, 61));
        g.fillRect(0, 252, WIDTH, 22);
        g.setColor(new Color(50, 100, 48));
        for (int x = 0; x < WIDTH; x += 38) {
            g.fillOval(x - 8, 241 + (x % 3) * 3, 54, 28);
        }

        g.setColor(new Color(93, 51, 34));
        g.fillRect(0, MINE_VIEW_TOP, WIDTH, HEIGHT - MINE_VIEW_TOP);
        if (MINE_DIRT_BACKGROUND_ART != null) {
            g.drawImage(MINE_DIRT_BACKGROUND_ART, 0, MINE_VIEW_TOP, WIDTH, HEIGHT - MINE_VIEW_TOP, null);
        } else {
            drawPaintedMineBackground(g);
        }

        drawFloatingLeaves(g);
    }

    private void drawPaintedSurfaceBackground(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(14, 48, 42), 0, 270, new Color(104, 176, 93));
        g.setPaint(sky);
        g.fillRect(0, 0, WIDTH, MINE_VIEW_TOP);

        g.setColor(new Color(255, 238, 138, 72));
        for (int i = 0; i < 5; i++) {
            int drift = (int) (Math.sin(worldTime * 0.35 + i) * 18);
            int x = 235 + i * 78 + drift;
            g.fillPolygon(new int[]{x, x + 72, x - 160}, new int[]{0, 0, 262}, 3);
        }
    }

    private void drawPaintedMineBackground(Graphics2D g) {
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
    }

    private void drawFloatingLeaves(Graphics2D g) {
        for (int i = 0; i < 42; i++) {
            int x = (i * 71 + (int) (worldTime * 8)) % (WIDTH + 80) - 40;
            int y = 40 + (i * 31) % 190;
            g.setColor(i % 3 == 0 ? new Color(154, 218, 98, 90) : new Color(32, 112, 64, 85));
            g.fillOval(x, y, 16 + i % 14, 8 + i % 8);
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
        GradientPaint paint = new GradientPaint(x, y, new Color(35, 159, 218), x + 170, y + 52, new Color(23, 76, 143));
        g.setPaint(paint);
        g.fillRoundRect(x, y, 176, 52, 13, 13);
        g.setColor(new Color(255, 205, 61));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, 176, 52, 13, 13);
        g.setStroke(new BasicStroke(1));
        drawGem(g, x + 14, y + 11, 30, new Color(96, 238, 255));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf(bank.getGems()), x + 56, y + 30);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(new Color(221, 247, 255));
        g.drawString("GEMS SAVED", x + 57, y + 45);
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
        drawLiftTower(g);

        g.setColor(new Color(101, 73, 45));
        g.setStroke(new BasicStroke(5));
        g.drawLine(156, SURFACE_Y + 14, TOWER_X + TOWER_W / 2, SURFACE_Y + 14);
        g.setStroke(new BasicStroke(1));

        drawTransportPanelCard(g, 176, 112, "Surface", surfaceGems, true, selection == Selection.LEAF_LIFT);
        drawHomeTower(g);
    }

    private void drawLiftTower(Graphics2D g) {
        Image liftArt = leafLift.getLoad() > 0 && !leafLift.isPickingUp() && LIFT_FULL_ART != null
                ? LIFT_FULL_ART
                : LIFT_EMPTY_ART;

        if (liftArt != null) {
            int x = 9;
            int y = 92;
            int w = 168;
            int h = 414;

            if (selection == Selection.LEAF_LIFT) {
                g.setColor(new Color(255, 231, 100, 100));
                g.fillRoundRect(x + 8, y + 4, w - 16, h - 8, 18, 18);
            }

            g.drawImage(liftArt, x, y, w, h, null);
            drawLiftPickupGemFlow(g, x, y, w, h);
            drawLiftStatusText(g, x + 84, leafLift.getDrawY());

            return;
        }

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
    }

    private void drawLiftPickupGemFlow(Graphics2D g, int liftX, int liftY, int liftW, int liftH) {
        if (!leafLift.isPickingUp() || leafLift.getLoad() <= 0) {
            return;
        }

        int chestX = liftX + liftW / 2 - 5;
        int chestY = Math.max(liftY + 150, Math.min(liftY + liftH - 120, leafLift.getDrawY() - 8));
        int sourceX = chestX + 112;
        int sourceY = chestY + 18;

        for (int i = 0; i < 5; i++) {
            double phase = (worldTime * 1.7 + i * 0.18) % 1.0;
            int gemX = (int) (sourceX + (chestX - sourceX) * phase);
            int gemY = (int) (sourceY + (chestY - sourceY) * phase + Math.sin(phase * Math.PI) * -16);
            drawGem(g, gemX, gemY, 12, new Color(85, 190, 255));
        }
    }

    private void drawLiftStatusText(Graphics2D g, int centerX, int actorY) {
        String status = leafLift.getStatusText();
        if (status.isEmpty()) {
            return;
        }

        int bubbleY = Math.max(96, Math.min(398, actorY - 32));
        g.setColor(new Color(9, 17, 20, 220));
        g.fillRoundRect(centerX - 38, bubbleY, 76, 22, 11, 11);
        g.setColor(new Color(238, 226, 136));
        g.drawRoundRect(centerX - 38, bubbleY, 76, 22, 11, 11);
        g.setFont(new Font("Arial", Font.BOLD, 10));
        g.setColor(Color.WHITE);
        drawCentered(g, status, centerX, bubbleY + 15);
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

        if (HOME_NEST_ART != null) {
            int artX = x - 76;
            int artY = y - 20;
            int artW = 260;
            int artH = 217;

            if (selection == Selection.NEST_RUNNER) {
                g.setColor(new Color(255, 231, 100, 120));
                g.fillRoundRect(artX - 8, artY - 8, artW + 16, artH + 66, 16, 16);
            }

            g.drawImage(HOME_NEST_ART, artX, artY, artW, artH, null);

            g.setFont(new Font("Arial", Font.BOLD, 17));
            g.setColor(Color.WHITE);
            drawCentered(g, "Home Nest", x + TOWER_W / 2, y + 210);
            return;
        }

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
        int laneTop = floorY - 80;
        int laneHeight = 94;

        if (floorY < MINE_VIEW_TOP - 18 || caveY > MINE_VIEW_BOTTOM + 40) {
            return;
        }

        g.setColor(mine.getId() % 2 == 0 ? new Color(88, 48, 36, 112) : new Color(72, 41, 34, 100));
        g.fillRoundRect(206, laneTop, MINE_VIEW_RIGHT - 232, laneHeight, 8, 8);
        g.setColor(new Color(48, 31, 27, 170));
        g.fillRoundRect(214, laneTop + laneHeight - 8, MINE_VIEW_RIGHT - 248, 6, 4, 4);

        if (selected) {
            g.setColor(new Color(85, 222, 255, 170));
            g.fillRoundRect(222, laneTop + 4, 4, laneHeight - 8, 4, 4);
        }

        if (MINE_ROW_ART != null) {
            int artX = caveX - 8;
            int artY = floorY - 92;
            int artW = mine.getCaveWidth() + 84;
            int artH = 86;

            drawLiftConnector(g, LIFT_X + 39, floorY - 26, artX + 42);
            g.drawImage(MINE_ROW_ART, artX, artY, artW, artH, null);

            if (mine.getStationGems() > 0) {
                drawGemPile(g, mine.getBasketX() - 8, floorY - 68, Math.min(10, mine.getStationGems()), mine.getPrimaryGemColor());
            }
        } else {
            g.setColor(new Color(121, 67, 45));
            g.fillRoundRect(caveX - 8, caveY + 9, mine.getCaveWidth() + 34, 56, 11, 11);
            g.setColor(new Color(70, 42, 35));
            g.fillRoundRect(mine.getLeftX(), caveY + 24, mine.getTunnelLength() + 18, 30, 8, 8);
            g.setColor(new Color(43, 34, 31));
            g.fillRect(mine.getLeftX() - 12, caveY + 22, 11, 38);

            drawRockTexture(g, caveX + 2, caveY + 15, mine.getCaveWidth() + 18, 34);
            drawLiftConnector(g, LIFT_X + 39, floorY - 26, mine.getBasketX() - 4);
            drawBlueCrystalWall(g, mine.getGemWallX() - 20, caveY + 23, mine.hasGreenBird());
            drawTrack(g, mine.getBasketX() + 37, floorY - 10, mine.getGemWallX() + 18);
            drawConnectedStorage(g, mine.getBasketX() - 8, floorY - 62, mine.getStationGems(), mine.getPrimaryGemColor());
        }

        int labelX = 190;
        int labelY = laneTop + 25;
        g.setColor(new Color(12, 20, 25, 185));
        g.fillRoundRect(labelX - 10, labelY - 21, 132, 47, 8, 8);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString("Mine " + mine.getId(), labelX, labelY - 4);

        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(235, 243, 230));
        g.drawString("Blue L" + mine.getBlueBird().getProgressLevel() + "/15", labelX, labelY + 14);
        g.setColor(new Color(101, 219, 255));
        g.drawString(mine.getStationGems() + " waiting", labelX, labelY + 31);

        if (mine.hasGreenBird()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Green", labelX + 76, labelY + 14);
        } else if (mine.getBlueBird().isLevelFifteen()) {
            g.setColor(new Color(112, 240, 126));
            g.drawString("Ready", labelX + 76, labelY + 14);
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
        g.fillRoundRect(x - 9, y - 7, 62, 50, 11, 11);
        g.setColor(new Color(11, 20, 33, 160));
        g.fillOval(x - 2, y, 52, 42);

        if (GEM_MEDIUM_PILE_ART != null) {
            g.drawImage(GEM_MEDIUM_PILE_ART, x - 7, y - 4, 64, 50, null);
            if (hasGreen) {
                drawCrystal(g, x + 40, y + 27, 13, new Color(105, 230, 120));
            }
            return;
        }

        int[] gemX = {5, 22, 38, 13, 30, 42, 8};
        int[] gemY = {1, 5, 4, 20, 22, 21, 36};
        int[] gemSize = {15, 12, 13, 13, 15, 11, 11};
        for (int i = 0; i < gemX.length; i++) {
            Color color = hasGreen && i > 5 ? new Color(105, 230, 120) : new Color(85, 190, 255);
            drawCrystal(g, x + gemX[i], y + gemY[i], gemSize[i], color);
        }
    }

    private void drawCrystal(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        g.fillOval(x - 4, y - 3, size + 8, size + 8);
        int half = size / 2;
        g.setColor(color.brighter());
        g.fillPolygon(
                new int[]{x + half, x + size - 2, x + size, x + half, x, x + 2},
                new int[]{y, y + 3, y + half, y + size, y + half, y + 3},
                6
        );
        g.setColor(new Color(255, 255, 255, 170));
        g.drawLine(x + half, y + 2, x + size - 4, y + half);
        g.drawLine(x + half, y + 2, x + half, y + size - 3);
        g.setColor(color.darker());
        g.drawPolygon(
                new int[]{x + half, x + size - 2, x + size, x + half, x, x + 2},
                new int[]{y, y + 3, y + half, y + size, y + half, y + 3},
                6
        );
    }

    private void drawGem(Graphics2D g, int x, int y, int size, Color color) {
        if (GEM_SINGLE_ART != null) {
            int gemW = size + 8;
            int gemH = size + 12;
            g.drawImage(GEM_SINGLE_ART, x - 4, y - 6, gemW, gemH, null);
            return;
        }

        int half = size / 2;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g.fillOval(x - 4, y - 2, size + 8, size + 8);
        g.setColor(color.brighter());
        g.fillPolygon(
                new int[]{x + half, x + size - 2, x + size, x + half, x, x + 2},
                new int[]{y, y + 4, y + half, y + size, y + half, y + 4},
                6
        );
        g.setColor(Color.WHITE);
        g.drawLine(x + half, y + 2, x + size - 5, y + half);
        g.drawLine(x + half, y + 2, x + half, y + size - 4);
        g.setColor(color.darker());
        g.drawPolygon(
                new int[]{x + half, x + size - 2, x + size, x + half, x, x + 2},
                new int[]{y, y + 4, y + half, y + size, y + half, y + 4},
                6
        );
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
        int rawY = FIRST_MINE_FLOOR_Y + (mines.size() - 1) * MINE_ROW_GAP + 48;
        int y = screenY(rawY);
        if (y < MINE_VIEW_TOP - 20 || y > MINE_VIEW_BOTTOM - 10) {
            return;
        }

        int x = 230;
        int w = 486;
        int h = 74;
        if (selection == Selection.ADD_MINE) {
            g.setColor(new Color(255, 231, 100, 125));
            g.fillRoundRect(x - 7, y - 7, w + 14, h + 14, 16, 16);
        }

        drawPurplePanel(g, x, y, w, h, 14);
        g.setFont(new Font("Arial", Font.BOLD, 21));
        g.setColor(Color.WHITE);
        g.drawString("Open Mine " + (mines.size() + 1), x + 24, y + 30);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.drawString("Starts with a blue bird. Scroll to manage lower mines.", x + 24, y + 54);
        drawGoldButton(g, x + w - 126, y + 16, 102, 43, String.valueOf(getNewMineCost()));
    }

    private boolean newMineCardContains(int mouseX, int mouseY) {
        int rawY = FIRST_MINE_FLOOR_Y + (mines.size() - 1) * MINE_ROW_GAP + 48;
        int y = screenY(rawY);
        return mouseX >= 230 && mouseX <= 716 && mouseY >= y && mouseY <= y + 74
                && mouseY >= MINE_VIEW_TOP && mouseY <= MINE_VIEW_BOTTOM;
    }

    private boolean leafLiftPanelContains(int mouseX, int mouseY) {
        return mouseX >= 9 && mouseX <= 177 && mouseY >= 68 && mouseY <= 482;
    }

    private boolean runnerPanelContains(int mouseX, int mouseY) {
        return mouseX >= TOWER_X && mouseX <= TOWER_X + TOWER_W && mouseY >= 120 && mouseY <= 578;
    }

    private void drawMineActors(Graphics2D g) {
        for (Mine mine : mines) {
            int lane = 0;
            int floorY = screenY(mine.getFloorY());
            if (floorY < MINE_VIEW_TOP - 20 || floorY > MINE_VIEW_BOTTOM + 30) {
                continue;
            }
            for (Bird bird : mine.getBirds()) {
                bird.draw(g, floorY, mine.getGemWallX(), lane * 14);
                lane++;
            }
        }
    }

    private void drawTransportActors(Graphics2D g) {
        if (LIFT_EMPTY_ART == null) {
            leafLift.draw(g, selection == Selection.LEAF_LIFT);
        }
        nestRunner.draw(g, selection == Selection.NEST_RUNNER);
    }

    private void drawMineUpgradePopup(Graphics2D g) {
        positionContextButtons();
        Mine mine = getSelectedMine();
        Bird bird = getSelectedBird();

        int x = 230;
        int y = 118;
        int w = 660;
        int h = 486;

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        drawDarkGoldPanel(g, x, y, w, h, 18);

        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.setColor(Color.WHITE);
        g.drawString("Mine " + mine.getId() + " Upgrades", x + 28, y + 43);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(new Color(222, 236, 255));
        g.drawString(bird.getName() + " level " + bird.getProgressLevel() + "/15", x + 30, y + 68);
        g.drawString(mine.getStationGems() + " gems waiting at this mine", x + 30, y + 90);

        closePopup.setBounds(x + w - 45, y + 18, 28, 28);
        drawCloseButton(g, closePopup);

        drawBirdSwitchRow(g, mine, bird, x + 386, y + 32);

        drawUpgradeCard(
                g,
                optionOne,
                "Speed",
                "Walks faster",
                "Lv. " + bird.getSpeedLevel() + "/5",
                bird.canUpgradeSpeed() ? bird.getSpeedCost(mine.getId()) + " gems" : "max",
                "feather",
                bird.canUpgradeSpeed() && bank.canAfford(bird.getSpeedCost(mine.getId()))
        );
        drawUpgradeCard(
                g,
                optionTwo,
                "Capacity",
                "Carries more gems",
                "Lv. " + bird.getStrengthLevel() + "/5",
                bird.canUpgradeStrength() ? bird.getStrengthCost(mine.getId()) + " gems" : "max",
                "box",
                bird.canUpgradeStrength() && bank.canAfford(bird.getStrengthCost(mine.getId()))
        );
        drawUpgradeCard(
                g,
                optionThree,
                "Mining Speed",
                "Mines quicker",
                "Lv. " + bird.getMiningLevel() + "/5",
                bird.canUpgradeMining() ? bird.getMiningCost(mine.getId()) + " gems" : "max",
                "clock",
                bird.canUpgradeMining() && bank.canAfford(bird.getMiningCost(mine.getId()))
        );

        drawModalActionButton(
                g,
                optionFour,
                "Auto Mine",
                bird.hasAutoMining() ? "Enabled" : bird.getAutoMiningCost(mine.getId()) + " gems",
                bird.canUnlockAutoMining() && bank.canAfford(bird.getAutoMiningCost(mine.getId()))
        );

        String greenText = mine.hasGreenBird()
                ? (selectedGreenBird ? "Switch to Blue" : "Switch to Green")
                : (mine.canUnlockGreenBird() ? mine.getGreenBirdCost() + " gems" : "Needs blue L15");
        drawModalActionButton(
                g,
                optionFive,
                "Green Bird",
                greenText,
                mine.hasGreenBird() || (mine.canUnlockGreenBird() && bank.canAfford(mine.getGreenBirdCost()))
        );
    }

    private void drawBirdSwitchRow(Graphics2D g, Mine mine, Bird bird, int x, int y) {
        g.setColor(new Color(18, 32, 45, 210));
        g.fillRoundRect(x, y, 205, 60, 12, 12);
        drawGem(g, x + 14, y + 15, 24, bird.getGemColor());
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString(selectedGreenBird ? "Editing Green Bird" : "Editing Blue Bird", x + 48, y + 25);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(220, 235, 245));
        g.drawString(mine.hasGreenBird() ? "Use Green Bird button to switch" : "Green unlocks at level 15", x + 48, y + 44);
    }

    private void drawCloseButton(Graphics2D g, Button button) {
        g.setColor(new Color(52, 63, 72, 230));
        g.fillOval(button.x, button.y, button.width, button.height);
        g.setColor(new Color(255, 218, 116));
        g.drawOval(button.x, button.y, button.width, button.height);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawLine(button.x + 9, button.y + 9, button.x + 19, button.y + 19);
        g.drawLine(button.x + 19, button.y + 9, button.x + 9, button.y + 19);
        g.setStroke(new BasicStroke(1));
    }

    private void drawUpgradeCard(
            Graphics2D g,
            Button button,
            String title,
            String description,
            String level,
            String cost,
            String icon,
            boolean enabled
    ) {
        g.setColor(enabled ? new Color(30, 72, 118, 238) : new Color(50, 54, 63, 235));
        g.fillRoundRect(button.x, button.y, button.width, button.height, 15, 15);
        g.setColor(enabled ? new Color(122, 219, 255) : new Color(111, 116, 130));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 15, 15);
        g.setStroke(new BasicStroke(1));

        drawPopupIcon(g, icon, button.x + button.width / 2 - 20, button.y + 18, enabled);

        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(Color.WHITE);
        drawCentered(g, title, button.x + button.width / 2, button.y + 78);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(220, 236, 245));
        drawCentered(g, description, button.x + button.width / 2, button.y + 101);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(255, 232, 112));
        drawCentered(g, level, button.x + button.width / 2, button.y + 126);

        g.setColor(enabled ? new Color(72, 174, 56) : new Color(78, 80, 83));
        g.fillRoundRect(button.x + 22, button.y + button.height - 42, button.width - 44, 30, 9, 9);
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        drawCentered(g, cost, button.x + button.width / 2, button.y + button.height - 22);
    }

    private void drawPopupIcon(Graphics2D g, String icon, int x, int y, boolean enabled) {
        Color main = enabled ? new Color(255, 229, 106) : new Color(156, 160, 152);
        Color accent = enabled ? new Color(101, 221, 255) : new Color(120, 126, 132);
        g.setColor(main);
        if ("feather".equals(icon)) {
            g.fillOval(x + 8, y, 28, 44);
            g.setColor(accent);
            g.drawLine(x + 22, y + 4, x + 10, y + 43);
            g.drawLine(x + 21, y + 16, x + 34, y + 12);
            g.drawLine(x + 17, y + 29, x + 31, y + 26);
        } else if ("box".equals(icon)) {
            g.fillRoundRect(x + 3, y + 15, 38, 29, 6, 6);
            g.setColor(new Color(92, 56, 35));
            g.drawLine(x + 3, y + 28, x + 41, y + 28);
            g.setColor(accent);
            g.fillOval(x + 12, y + 4, 8, 8);
            g.fillOval(x + 24, y + 6, 8, 8);
        } else if ("clock".equals(icon)) {
            g.fillOval(x + 2, y + 4, 42, 42);
            g.setColor(new Color(35, 58, 50));
            g.drawOval(x + 2, y + 4, 42, 42);
            g.drawLine(x + 23, y + 25, x + 23, y + 11);
            g.drawLine(x + 23, y + 25, x + 36, y + 25);
        }
    }

    private void drawModalActionButton(Graphics2D g, Button button, String title, String value, boolean enabled) {
        g.setColor(enabled ? new Color(28, 78, 128, 238) : new Color(54, 57, 66, 235));
        g.fillRoundRect(button.x, button.y, button.width, button.height, 12, 12);
        g.setColor(enabled ? new Color(127, 217, 255) : new Color(123, 127, 137));
        g.drawRoundRect(button.x, button.y, button.width, button.height, 12, 12);
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        g.drawString(title, button.x + 18, button.y + 25);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(226, 238, 255));
        g.drawString(value, button.x + 18, button.y + 47);
    }

    private void drawContextPanel(Graphics2D g) {
        if (selection == Selection.MINE) {
            return;
        }

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
            if (mineUpgradePopupOpen) {
                optionOne.setBounds(260, 260, 188, 178);
                optionTwo.setBounds(466, 260, 188, 178);
                optionThree.setBounds(672, 260, 188, 178);
                optionFour.setBounds(260, 468, 286, 64);
                optionFive.setBounds(574, 468, 286, 64);
                closePopup.setBounds(845, 136, 28, 28);
            } else {
                optionOne.setBounds(-100, -100, 1, 1);
                optionTwo.setBounds(-100, -100, 1, 1);
                optionThree.setBounds(-100, -100, 1, 1);
                optionFour.setBounds(-100, -100, 1, 1);
                optionFive.setBounds(-100, -100, 1, 1);
                closePopup.setBounds(-100, -100, 1, 1);
            }
        } else if (selection == Selection.LEAF_LIFT || selection == Selection.NEST_RUNNER) {
            optionOne.setBounds(x, y, 96, 92);
            optionTwo.setBounds(x + 110, y, 96, 92);
            optionThree.setBounds(x + 220, y, 96, 92);
            optionFour.setBounds(-100, -100, 1, 1);
            optionFive.setBounds(-100, -100, 1, 1);
            closePopup.setBounds(-100, -100, 1, 1);
        } else {
            optionOne.setBounds(x, y, 210, 86);
            optionTwo.setBounds(-100, -100, 1, 1);
            optionThree.setBounds(-100, -100, 1, 1);
            optionFour.setBounds(-100, -100, 1, 1);
            optionFive.setBounds(-100, -100, 1, 1);
            closePopup.setBounds(-100, -100, 1, 1);
        }
    }

    private int getNewMineCost() {
        int nextMine = mines.size() + 1;
        return 420 + nextMine * nextMine * 180;
    }

    private double getMaxScrollY() {
        int newMineCardBottom = FIRST_MINE_FLOOR_Y + (mines.size() - 1) * MINE_ROW_GAP + 48 + 90;
        return Math.max(0, newMineCardBottom - MINE_VIEW_BOTTOM + 10);
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

    private void drawGemPile(Graphics2D g, int x, int y, int count, Color color) {
        Image art = getGemPileArt(count);
        if (art != null) {
            if (count <= 3) {
                g.drawImage(art, x, y + 18, 27, 34, null);
            } else if (count <= 7) {
                g.drawImage(art, x - 4, y + 11, 55, 42, null);
            } else if (count <= 11) {
                g.drawImage(art, x - 7, y + 4, 76, 52, null);
            } else {
                g.drawImage(art, x - 10, y - 2, 98, 62, null);
            }
            return;
        }

        for (int i = 0; i < count; i++) {
            int row = i / 5;
            int col = i % 5;
            drawCrystal(g, x + col * 14 - row * 3, y + 46 - row * 11, 18, i % 2 == 0 ? color : color.darker());
        }
    }

    private static Image getGemPileArt(int count) {
        if (count <= 1) {
            return GEM_SINGLE_ART;
        }
        if (count <= 5) {
            return GEM_SMALL_PILE_ART;
        }
        if (count <= 10) {
            return GEM_MEDIUM_PILE_ART;
        }
        return GEM_LARGE_PILE_ART;
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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Mine {
    private static final int TUNNEL_LENGTH = 230;

    private final int id;
    private final String name;
    private final int floorY;
    private final int leftX;
    private final List<Bird> birds = new ArrayList<>();

    private int stationGems;
    private boolean greenBirdUnlocked;

    public Mine(int id, int floorY, int leftX) {
        this.id = id;
        this.floorY = floorY;
        this.leftX = leftX;
        this.name = "Mine " + id + " - Blue Gem Burrow";
        birds.add(createBlueBird());
        getBlueBird().setHomePosition(getBasketX());
    }

    private Bird createBlueBird() {
        return new Bird(
                "Blue Bird",
                new Color(58, 145, 225),
                new Color(31, 77, 151),
                "blue",
                new Color(85, 190, 255),
                1,
                82,
                16,
                4,
                3,
                3.2,
                0.46,
                1.0
        );
    }

    private Bird createGreenBird() {
        return new Bird(
                "Green Bird",
                new Color(83, 190, 96),
                new Color(35, 118, 60),
                "green",
                new Color(105, 230, 120),
                3,
                105,
                21,
                8,
                4,
                2.6,
                0.50,
                1.65
        );
    }

    public void update(double deltaSeconds) {
        for (Bird bird : birds) {
            int minedGems = bird.update(deltaSeconds, getGemWallX(), getBasketX());
            stationGems += minedGems * bird.getGemValue();
        }
    }

    public int collectFromStation(int maxLoad) {
        int load = Math.min(stationGems, maxLoad);
        stationGems -= load;
        return load;
    }

    public boolean unlockGreenBird() {
        if (!canUnlockGreenBird()) {
            return false;
        }

        greenBirdUnlocked = true;
        Bird greenBird = createGreenBird();
        greenBird.setHomePosition(getBasketX());
        birds.add(greenBird);
        return true;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= getCaveX()
                && mouseX <= getCaveX() + getCaveWidth()
                && mouseY >= getCaveY()
                && mouseY <= getCaveY() + 98;
    }

    public boolean canUnlockGreenBird() {
        return getBlueBird().isLevelFifteen() && !greenBirdUnlocked;
    }

    public int getGreenBirdCost() {
        return scaleForMine(750);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getFloorY() {
        return floorY;
    }

    public int getCaveX() {
        return leftX - 44;
    }

    public int getCaveY() {
        return floorY - 105;
    }

    public int getLeftX() {
        return leftX;
    }

    public int getGemWallX() {
        return leftX + getTunnelLength();
    }

    public int getBasketX() {
        return leftX + 34;
    }

    public int getTunnelLength() {
        return TUNNEL_LENGTH;
    }

    public int getCaveWidth() {
        return getTunnelLength() + 112;
    }

    public int getBasketCenterX() {
        return getBasketX() + 37;
    }

    public int getStationGems() {
        return stationGems;
    }

    public Bird getBlueBird() {
        return birds.get(0);
    }

    public Bird getGreenBird() {
        return greenBirdUnlocked ? birds.get(1) : null;
    }

    public List<Bird> getBirds() {
        return birds;
    }

    public Color getPrimaryGemColor() {
        return getBlueBird().getGemColor();
    }

    public boolean hasGreenBird() {
        return greenBirdUnlocked;
    }

    private int scaleForMine(int baseCost) {
        return (int) Math.round(baseCost * (1.0 + (id - 1) * 0.85));
    }
}

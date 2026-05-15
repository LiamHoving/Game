import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.util.Random;

public class FlyingBirdSprite {
    private final Image[] frames;
    private final Random random = new Random();
    private final double radiusX;
    private final double radiusY;
    private final double scale;
    private final double angleSpeed;
    private final int baseFrameWidth;
    private final int baseFrameHeight;

    private double centerX;
    private double centerY;
    private double centerMoveX;
    private double centerMoveY;
    private double angle;
    private double x;
    private double y;
    private double lastX;
    private int frameIndex;
    private int animationCounter;
    private final int animationDelay = 11;
    private boolean animationForward = true;

    public FlyingBirdSprite(
            String[] imagePaths,
            double centerX,
            double centerY,
            double radiusX,
            double radiusY,
            double scale,
            double angleSpeed
    ) {
        frames = new Image[imagePaths.length];
        int firstWidth = 1;
        int firstHeight = 1;
        for (int i = 0; i < imagePaths.length; i++) {
            ImageIcon icon = new ImageIcon(imagePaths[i]);
            frames[i] = icon.getImage();
            if (i == 0) {
                firstWidth = Math.max(1, icon.getIconWidth());
                firstHeight = Math.max(1, icon.getIconHeight());
            }
        }

        this.centerX = centerX;
        this.centerY = centerY;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.scale = scale;
        this.angleSpeed = angleSpeed;
        this.baseFrameWidth = firstWidth;
        this.baseFrameHeight = firstHeight;
        this.centerMoveX = -0.10 + random.nextDouble() * 0.20;
        this.centerMoveY = -0.04 + random.nextDouble() * 0.08;
        this.angle = random.nextDouble() * Math.PI * 2;
        this.x = centerX;
        this.y = centerY;
        this.lastX = x;
    }

    public void update(int screenWidth, int minY, int maxY) {
        lastX = x;
        angle += angleSpeed;

        centerX += centerMoveX;
        centerY += centerMoveY;

        if (centerX - radiusX < -70 || centerX + radiusX > screenWidth + 70) {
            centerMoveX *= -1;
        }
        if (centerY - radiusY < minY || centerY + radiusY > maxY) {
            centerMoveY *= -1;
        }

        x = centerX + Math.cos(angle) * radiusX;
        y = centerY + Math.sin(angle) * radiusY;
        updateWingFlap();
    }

    private void updateWingFlap() {
        if (frames.length <= 1) {
            return;
        }

        animationCounter++;
        if (animationCounter < animationDelay) {
            return;
        }

        animationCounter = 0;
        if (animationForward) {
            frameIndex++;
            if (frameIndex >= frames.length - 1) {
                frameIndex = frames.length - 1;
                animationForward = false;
            }
        } else {
            frameIndex--;
            if (frameIndex <= 0) {
                frameIndex = 0;
                animationForward = true;
            }
        }
    }

    public void draw(Graphics2D g2, ImageObserver observer) {
        Image currentFrame = frames[frameIndex];
        if (currentFrame.getWidth(observer) <= 0 || currentFrame.getHeight(observer) <= 0) {
            return;
        }

        int drawWidth = (int) (baseFrameWidth * scale);
        int drawHeight = (int) (baseFrameHeight * scale);

        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(Math.sin(angle) * 0.12);

        if (x < lastX) {
            g.scale(-1, 1);
        }

        g.drawImage(currentFrame, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight, observer);
        g.dispose();
    }
}

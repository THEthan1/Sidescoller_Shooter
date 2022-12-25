package pepse.world;

import danogl.collisions.GameObjectCollection;
import danogl.gui.rendering.RectangleRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.Layers;
import pepse.util.ColorSupplier;
import pepse.util.PerlinNoise;
import java.awt.*;
import java.util.ArrayList;

/**
 * Responsible for the creation and management of terrain.
 * @author Ohad Klein, Ethan Glick
 * */
public class Terrain {
    /**
     * Tag for terrain object.
     */
    public static final String TERRAIN_TAG = "ground";

    /**
     * Tag for top terrain object(aka terrain object which blocks collision)
     */
    public static final String TERRAIN_TOP_TAG = "ground top";

    private static final float GROUND_HEIGHT_INITIAL_FACTOR = 2f/3f;
    private static final Color BASE_GROUND_COLOR = new Color(212, 123, 74);
    private static final int TERRAIN_DEPTH = 25;

    private final GameObjectCollection gameObjects;
    private final PerlinNoise noiseGenerator;
    private final float groundHeightAtX0;
    private int firstX;
    private int lastX;
    private final ArrayList<Block> blocksTop;
    private final ArrayList<Block> blocks;
    private final int groundLayerForCollision;

    /**
     * Constructor.
     * @param gameObjects - The collection of all participating game objects.
     * @param windowDimensions - The dimensions of the windows.
     * @param seed - A seed for a random number generator.
     * */
    public Terrain(GameObjectCollection gameObjects, int groundLayer,
                   Vector2 windowDimensions, int seed) {
        this.groundHeightAtX0 = windowDimensions.y() * GROUND_HEIGHT_INITIAL_FACTOR;
        this.gameObjects = gameObjects;
        this.noiseGenerator = new PerlinNoise(seed);
        this.blocksTop = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.groundLayerForCollision = groundLayer;
    }

    /**
     * Getter.
     * @return first x position of the terrain.
     */
    public int getFirstX() {
        return this.firstX;
    }

    /**
     * Getter.
     * @return last x position of the terrain.
     */
    public int getLastX() {
        return this.lastX;
    }

    /**
     * This method creates terrain in a given range of x-values.
     * @param minX - The lower bound of the given range (will be rounded to a multiple of Block.SIZE).
     * @param maxX - The upper bound of the given range (will be rounded to a multiple of Block.SIZE).
     * */
    public void createInRange(int minX, int maxX) {
        this.firstX = (minX/Block.SIZE) * Block.SIZE;
        this.lastX = (maxX/Block.SIZE) * Block.SIZE + Block.SIZE;
        for (int curX = this.firstX; curX < this.lastX; curX += Block.SIZE) {
            int firstY = ((int)(groundHeightAt(curX)/Block.SIZE)) * Block.SIZE;
            int lastY = firstY + Block.SIZE * TERRAIN_DEPTH;
            for (int curY = firstY; curY < lastY; curY += Block.SIZE) {
                Renderable renderable =
                        new RectangleRenderable(ColorSupplier.approximateColor(BASE_GROUND_COLOR));
                Block block = new Block(new Vector2(curX, curY), renderable);
                if (curY <= firstY + Block.SIZE) {
                    // consider as TERRAIN_TOP for collision calculations
                    this.gameObjects.addGameObject(block, groundLayerForCollision);
                    this.blocksTop.add(block);
                    block.setTag(TERRAIN_TOP_TAG);
                } else {
                    this.gameObjects.addGameObject(block, Layers.TERRAIN.value);
                    this.blocks.add(block);
                    block.setTag(TERRAIN_TAG);
                }
            }
        }
    }

    /**
     * Destroys the terrain objects.
     */
    public void destroy() {
        for (Block block : blocksTop) {
            this.gameObjects.removeGameObject(block, groundLayerForCollision);
        }
        for (Block block : blocks) {
            this.gameObjects.removeGameObject(block, Layers.TERRAIN.value);
        }
    }

    /**
     * This method returns the ground height at a given location.
     * @param x - A number.
     * @return The ground height at the given location.
     * */
    public float groundHeightAt(float x) {
        return noiseGenerator.noise(x) + groundHeightAtX0;
    }
}



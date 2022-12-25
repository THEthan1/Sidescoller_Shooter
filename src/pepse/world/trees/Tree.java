package pepse.world.trees;

import danogl.collisions.GameObjectCollection;
import danogl.gui.rendering.RectangleRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.Layers;
import pepse.util.ColorSupplier;
import pepse.world.Block;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

/**
 * Responsible for the creation and management of trees.
 * @author Ohad Klein, Ethan Glick
 * */
public class Tree {
    /**
     * Tag for tree trunk objects
     */
    public static final String TREE_TAG = "tree";

    private static final Color TRUNK_COLOR = new Color(100, 50, 20);
    private static final int MIN_HEIGHT = 7;
    private static final int MAX_HEIGHT_DIFFERENCE = 7;
    private static final float PLANT_PROBABILITY = 0.1f;

    private final Function<Integer, Float> groundHeightFunction;
    private final Random random;
    private final GameObjectCollection gameObjects;
    private final ArrayList<Block> trunk;
    private final ArrayList<Block> leaves;

    /**
     * Constructor.
     * @param gameObjects          The collection of all participating game objects.
     * @param groundHeightFunction The function to determine where to start planting a tree.
     * @param seed                 A seed for a random number generator.
     */
    public Tree(GameObjectCollection gameObjects,
                Function<Integer, Float> groundHeightFunction, int seed) {
        this.gameObjects = gameObjects;
        this.groundHeightFunction = groundHeightFunction;
        this.random = new Random(seed);
        this.trunk = new ArrayList<>();
        this.leaves = new ArrayList<>();
    }


    /**
     * This method creates trees in a given range of x-values.
     * @param minX - The lower bound of the given range (will be rounded to a multiple of Block.SIZE).
     * @param  maxX - The upper bound of the given range (will be rounded to a multiple of Block.SIZE).
     * */
    public void createInRange(int minX, int maxX) {
        int firstX = (minX/ Block.SIZE) * Block.SIZE;
        int lastX = (maxX/Block.SIZE) * Block.SIZE + Block.SIZE;
        for (int curX = firstX; curX < lastX; curX += Block.SIZE) {
            if (shouldPlantTree()) {
                plantTree(curX);
            }
        }
    }

    /*
     * determines if a tree should be planted or not.
     */
    private boolean shouldPlantTree() {
        return random.nextFloat() <= PLANT_PROBABILITY;
    }

    /*
     * plants a tree in the given x location.
     */
    private void plantTree(int x) {
        int treeHeight = MIN_HEIGHT + random.nextInt(MAX_HEIGHT_DIFFERENCE);
        int lastY = ((int)(this.groundHeightFunction.apply(x)/Block.SIZE)) * Block.SIZE;
        int firstY = lastY - (treeHeight * Block.SIZE);
        for (int y = firstY; y < lastY; y += Block.SIZE) {
            Renderable renderable = new RectangleRenderable(ColorSupplier.approximateColor(TRUNK_COLOR));
            Block trunkBlock = new Block(new Vector2(x, y), renderable);
            trunkBlock.setTag(TREE_TAG);
            this.gameObjects.addGameObject(trunkBlock, Layers.TREES.value);
            this.trunk.add(trunkBlock);
        }
        Vector2 top = new Vector2(x, firstY);
        createLeaves(treeHeight, top);
    }

    /*
     * create leaves on top of a tree. The radius of square to create leaves on is determined
     * by the given treeHeight.
     */
    private void createLeaves(int treeHeight, Vector2 center) {
        int radius = calcRadius(treeHeight);
        int firstX = (int)center.x() - (radius - 1) * Block.SIZE;
        int lastX = (int)center.x() + radius * Block.SIZE;
        int firstY = (int)center.y() - (radius - 1) * Block.SIZE;
        int lastY = (int)center.y() + radius * Block.SIZE;
        for (int curX = firstX; curX < lastX; curX += Block.SIZE) {
            for (int curY = firstY; curY < lastY; curY += Block.SIZE) {
                Renderable renderable =
                        new RectangleRenderable(ColorSupplier.approximateColor(Leaf.COLOR));
                Leaf leaf = new Leaf(new Vector2(curX, curY), renderable);
                this.gameObjects.addGameObject(leaf, Layers.LEAVES.value);
                this.leaves.add(leaf);
            }
        }
    }

    /**
     * Destroys the tree objects.
     */
    public void destroy() {
        for (Block block : this.trunk) {
            this.gameObjects.removeGameObject(block, Layers.TREES.value);
        }
        for (Block block : this.leaves) {
            this.gameObjects.removeGameObject(block, Layers.LEAVES.value);
        }
    }

    /*
     * calculates radius for leaves by the given treeHeight.
     */
    private static int calcRadius(int treeHeight) {
        return (treeHeight + 2) / 3;
    }
}

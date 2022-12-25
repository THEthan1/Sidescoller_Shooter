package pepse.world.npcs;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.Transition;
import danogl.gui.rendering.OvalRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.Layers;
import pepse.world.Damageable;
import pepse.world.Projectile;
import java.awt.*;
import java.util.Random;

/**
 * BONUS! Class for a single bird.
 * @author Ohad Klein, Ethan Glick
 */
public class Bird extends GameObject implements Damageable {
    /**
     * Tag for bird object.
     */
    public static final String BIRD_TAG = "bird";

    private static final float FLY_SPEED = -100f;
    private static final float FLY_VOLATILITY = 100f;
    private static final float TRANSITION_TIME = 0.5f;
    private static final int DROPPING_PROBABILITY_BOUND = 500;
    private static final Color BROWN = new Color(150, 75, 0);
    private static final Vector2 DROPPING_SIZE = new Vector2(7,7);
    private static final float DROPPING_DAMAGE = 15;
    private static final float DROPPING_SPEED_Y = 300;
    private final GameObjectCollection gameObjects;
    private final int directionMultiplier;
    private final Random random;
    private float health = 10;

    /**
     * Constructor.
     * @param topLeftCorner         The location of the top-left corner of the created bird.
     * @param dimensions            The bird's size.
     * @param renderable            A renderable to render as the bird.
     * @param directionMultiplier   1 if bird is going left, -1 if going right
     */
    public Bird(Vector2 topLeftCorner, Vector2 dimensions, Renderable renderable,
                GameObjectCollection gameObjects, int directionMultiplier, int seed) {
        super(topLeftCorner, dimensions, renderable);
        this.gameObjects = gameObjects;
        this.random = new Random(seed);
        this.directionMultiplier = directionMultiplier;
        this.setTag(BIRD_TAG);
        physics().preventIntersectionsFromDirection(Vector2.UP);
        fly();
    }

    /*
     * Makes the bird fly.
     */
    private void fly() {
        float flightDirection = FLY_SPEED * this.directionMultiplier;
        new Transition<>(
                this, //the game object being changed
                this.transform()::setVelocity,  //the method to call
                new Vector2(flightDirection, FLY_VOLATILITY),    //initial transition value
                new Vector2(flightDirection, -FLY_VOLATILITY),   //final transition value
                Transition.CUBIC_INTERPOLATOR_VECTOR,  //use a cubic interpolator
                TRANSITION_TIME,   //transition over transition time
                Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
                null);
    }

    /*
     * Makes the bird release droppings.
     */
    private void releaseDroppings() {
        Projectile dropping = new Projectile(
                this.getCenter(), DROPPING_SIZE, new OvalRenderable(BROWN), gameObjects,
                DROPPING_DAMAGE, new Vector2(0, DROPPING_SPEED_Y), BIRD_TAG, this::getTopLeftCorner);
        gameObjects.addGameObject(dropping, Layers.OBJECTS.value);
    }

    /**
     * Override of the update function.
     * Used to make bird release droppings at random intervals.
     * @param deltaTime the time since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (random.nextInt(DROPPING_PROBABILITY_BOUND) == 0)
            releaseDroppings();
    }

    /**
     * Get the bird's health (floored to an int).
     * @return the bird's health (floored to an int).
     */
    @Override
    public float getHealth() {
        return health;
    }

    /**
     * Override the Damageable takeDamage() function.
     * Decreases bird's health by given amount.
     * @param damage the amount to decrease bird's health by.
     */
    @Override
    public void takeDamage(float damage) {
        health -= (health - damage > 0) ? damage : health;
    }
}

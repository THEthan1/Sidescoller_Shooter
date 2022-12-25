package pepse.world.trees;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.components.ScheduledTask;
import danogl.components.Transition;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.world.Block;
import java.awt.*;
import java.util.Random;
import static pepse.world.World.randomBetween;

/**
 * This class handles the creation and behavior of a single leaf.
 * @author Ohad Klein, Ethan Glick
 */
class Leaf extends Block {

    /**
     * the base color of leaves
     */
    public static final Color COLOR = new Color(50, 200, 30);

    /**
     * Tag for leaf object.
     */
    public static final String LEAF_TAG = "leaf";

    private static final float TRANSITION_ANGLE_DELTA = 10f;
    private static final float TRANSITION_ANGLE_CYCLE = 7f;
    private static final float TRANSITION_DIMENSIONS_DELTA = 0.9f;
    private static final float TRANSITION_DIMENSIONS_CYCLE = 0.7f;
    private static final float MIN_LIFE_TIME = 5f;
    private static final float MAX_LIFE_TIME = 60f;
    private static final float MIN_DEATH_TIME = 5f;
    private static final float MAX_DEATH_TIME = 20f;
    private static final float FADEOUT_TIME = 10f;
    private static final float FADE_IN_TIME = 3f;
    private static final float FALL_SPEED = 50f;
    private static final float FALL_VOLATILITY = 50f;
    private static final float FALL_STOP_TIME = 0.3f;

    private final Vector2 baseDimensions;
    private Transition<Float> angleTransition;
    private Transition<Vector2> dimensionsTransition;
    private Transition<Vector2> fallTransition;
    private boolean isFalling = false;
    private final Vector2 topLeftCorner;

    /**
     * Constructor.
     * @param topLeftCorner The location of the top-left corner of the created leaf.
     * @param renderable A renderable to render as the leaf.
     */
    public Leaf(Vector2 topLeftCorner, Renderable renderable) {
        super(topLeftCorner, renderable);
        this.topLeftCorner = topLeftCorner;
        this.baseDimensions = new Vector2(this.getDimensions().x(), this.getDimensions().y());
        // init leaf's dimensions to start of movement
        this.setDimensions(this.getDimensions().multY(TRANSITION_DIMENSIONS_DELTA));
        this.setTag(LEAF_TAG);
        this.physics().setMass(0f); // so leaf won't push other objects on collision
        // movement is started at random, to make each leaf move differently:
        float movementStartTime = new Random().nextFloat();
        new ScheduledTask(this, movementStartTime,
                    false, this::startMovement);
        startCycleOfLife();
    }

    /*
     * Initializes the movement transitions for the leaf.
     */
    private void startMovement() {
        this.angleTransition = new Transition<>(  // Angle
                this, //the game object being changed
                this.renderer()::setRenderableAngle,  //the method to call
                -TRANSITION_ANGLE_DELTA,    //initial transition value
                TRANSITION_ANGLE_DELTA,   //final transition value
                Transition.CUBIC_INTERPOLATOR_FLOAT,  //use a cubic interpolator
                TRANSITION_ANGLE_CYCLE,   //transition over transition time
                Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
                null);  //nothing further to execute upon reaching final value
        this.dimensionsTransition = new Transition<>(  // Dimensions
                this, //the game object being changed
                this::setDimensions,  //the method to call
                this.baseDimensions.multY(TRANSITION_DIMENSIONS_DELTA),    //initial transition value
                this.baseDimensions.multX(TRANSITION_DIMENSIONS_DELTA),   //final transition value
                Transition.CUBIC_INTERPOLATOR_VECTOR,  //use a cubic interpolator
                TRANSITION_DIMENSIONS_CYCLE,   //transition over transition time
                Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
                null);  //nothing further to execute upon reaching final value
    }

    /*
     * Starts the leaf's life-cycle - by randomly determining its lifetime.
     */
    private void startCycleOfLife() {
        float lifeTime = randomBetween(MIN_LIFE_TIME, MAX_LIFE_TIME, new Random());
        new ScheduledTask(this, lifeTime, false, this::fall);
    }

    /*
     * The second phase of the leaf's life-cycle - makes the leaf fall while fading out, and then die.
     */
    private void fall() {
        this.isFalling = true;
        this.fallTransition = new Transition<>(
                this, //the game object being changed
                this.transform()::setVelocity,  //the method to call
                new Vector2(FALL_VOLATILITY, FALL_SPEED),    //initial transition value
                new Vector2(-FALL_VOLATILITY, FALL_SPEED),   //final transition value
                Transition.CUBIC_INTERPOLATOR_VECTOR,  //use a cubic interpolator
                TRANSITION_DIMENSIONS_CYCLE,   //transition over transition time
                Transition.TransitionType.TRANSITION_BACK_AND_FORTH,
                null);
        this.transform().setVelocity(Vector2.DOWN.mult(FALL_SPEED)); // improves leaf behavior more
        this.renderer().fadeOut(FADEOUT_TIME, this::death);
    }

    /*
     * The third phase of the leaf's life-cycle - randomly determining time of death, and reincarnates leaf
     * afterwards.
     */
    private void death() {
        float deathTime = randomBetween(MIN_DEATH_TIME, MAX_DEATH_TIME, new Random());
        new ScheduledTask(this, deathTime, false, this::reincarnation);
    }

    /*
     * The final phase of the leaf's life-cycle - fade in and start cycle again.
     */
    private void reincarnation() {
        this.setTopLeftCorner(this.topLeftCorner);
        this.renderer().fadeIn(FADE_IN_TIME, this::startCycleOfLife);
    }

    /**
     * Overriding the shouldCollideWith method - only falling leaves should collide.
     * @param other other object to collide with.
     * @return true if they should collide, false otherwise.
     */
    @Override
    public boolean shouldCollideWith(GameObject other) {
        if (!isFalling) {
            return false;
        }
        return super.shouldCollideWith(other);
    }

    /**
     * Overriding the onCollisionEnter method - to stop the leaf's movement(velocity)
     * @param other     other object the leaf has collided with.
     * @param collision information regarding this collision.
     */
    @Override
    public void onCollisionEnter(GameObject other, Collision collision) {
        super.onCollisionEnter(other, collision);
        this.removeComponent(this.angleTransition);
        this.removeComponent(this.dimensionsTransition);
        this.removeComponent(this.fallTransition);
        new Transition<>( // stop movement
                this, //the game object being changed
                this.transform()::setVelocity,  //the method to call
                this.getVelocity(),    //initial transition value
                Vector2.ZERO,   //final transition value
                Transition.CUBIC_INTERPOLATOR_VECTOR,  //use a cubic interpolator
                FALL_STOP_TIME,   //transition over transition time
                Transition.TransitionType.TRANSITION_ONCE,
                null);
    }
}

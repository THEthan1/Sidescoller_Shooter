package pepse.world.npcs;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.ScheduledTask;
import danogl.components.Transition;
import danogl.gui.ImageReader;
import danogl.gui.rendering.AnimationRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.Layers;
import java.util.LinkedList;
import java.util.Random;
import java.util.function.Supplier;
import static pepse.world.World.randomBetween;

/**
 * BONUS! This class makes birds.
 * @author Ohad Klein, Ethan Glick
 */
public class BirdMaker extends GameObject {
    private static final float DELETION_DISTANCE = 2000f;
    private static final float DEATH_TRANSITION_TIME = 3;
    private static final float MIN_TIME_FOR_NEW_BIRD = 2f;
    private static final float MAX_TIME_FOR_NEW_BIRD = 15f;
    private static final float MIN_HEIGHT = -100f;
    private static final float MAX_HEIGHT = 100f;
    private static final float DEFAULT_DISTANCE = 1000f;
    private static final float SIZE = 48f;
    private static final double TIME_BETWEEN_CLIPS = 0.3f;
    private static final int FLOCK_COLS = 4;
    private static final float FLOCK_PROBABILITY = 0.3f;
    private static final int MULTIPLIER_LEFT = -1;
    private static final int MULTIPLIER_RIGHT = 1;
    private static final String[] FLYING_IMAGES = {
            "assets/birds/1_1.png",
            "assets/birds/1_2.png",
            "assets/birds/1_3.png",
            "assets/birds/1_4.png",
            "assets/birds/1_5.png",
            "assets/birds/1_6.png",
            "assets/birds/1_7.png",
            "assets/birds/1_8.png"};
    private static final String DEAD_IMAGE = "assets/birds/bird_dead.png";

    private GameObjectCollection gameObjects;
    private Supplier<Vector2> avatarLocationSupplier;
    private Runnable scoreIncrementer;
    private LinkedList<Bird> birds;
    private AnimationRenderable birdRenderableFlying;
    private Renderable birdRenderableDead;
    private Random random;
    private int seed;

    /**
     * Constructor.
     * @param topLeftCorner Should be Vector2.ZERO.
     * @param dimensions    Should be Vector2.ZERO.
     * @param renderable    Should be null.
     */
    private BirdMaker(Vector2 topLeftCorner, Vector2 dimensions, Renderable renderable) {
        super(topLeftCorner, dimensions, renderable);
    }

    /**
     * Creates and returns BirdMaker object.
     * @param topLeftCorner          Should be Vector2.ZERO.
     * @param dimensions             Should be Vector2.ZERO.
     * @param renderable             Should be null.
     * @param imageReader            Used for reading images from disk or from within a jar.
     * @param gameObjects            The collection of all participating game objects.
     * @param avatarLocationSupplier A function to supply the avatar's current location.
     */
    public static GameObject create(Vector2 topLeftCorner, Vector2 dimensions, Renderable renderable,
                     ImageReader imageReader, GameObjectCollection gameObjects,
                     Supplier<Vector2> avatarLocationSupplier, Runnable scoreIncrementer, int seed) {
        BirdMaker birdMaker = new BirdMaker(topLeftCorner, dimensions, renderable);
        birdMaker.seed = seed;
        birdMaker.random = new Random(seed);
        birdMaker.gameObjects = gameObjects;
        birdMaker.avatarLocationSupplier = avatarLocationSupplier;
        birdMaker.birds = new LinkedList<>();
        birdMaker.birdRenderableFlying =
                new AnimationRenderable(FLYING_IMAGES, imageReader,true, TIME_BETWEEN_CLIPS);
        birdMaker.scoreIncrementer = scoreIncrementer;
        birdMaker.birdRenderableDead = imageReader.readImage(DEAD_IMAGE, true);
        birdMaker.createBirds();
        gameObjects.addGameObject(birdMaker);
        return birdMaker;
    }

    /*
     * Creates a flock of birds.
     */
    private void createFlock(Vector2 startLocation, int directionMultiplier) {
        float xPos = startLocation.x();
        float yPos = startLocation.y();
        Vector2 topLeftCorner;
        for (int birdCol = 1; birdCol <= FLOCK_COLS; birdCol++) {
            float curY = yPos;

            for (int birdNum = 0; birdNum < birdCol; birdNum++) {
                float curX = xPos + birdCol * SIZE * directionMultiplier;
                topLeftCorner = new Vector2(curX, curY);
                createSingleBird(topLeftCorner, directionMultiplier);
                curY -= 2 * SIZE;
            }
            xPos += SIZE * directionMultiplier;
            yPos += SIZE;
        }
    }

    /*
     * Creates a single bird.
     */
    private void createSingleBird(Vector2 topLeftCorner, int directionMultiplier) {
        Bird bird = new Bird(topLeftCorner.add(Vector2.RIGHT.multX(100)), Vector2.ONES.mult(SIZE),
                birdRenderableFlying, gameObjects, directionMultiplier, seed++);
        bird.renderer().setIsFlippedHorizontally(directionMultiplier == MULTIPLIER_LEFT);
        this.birds.add(bird);
        this.gameObjects.addGameObject(bird, Layers.OBJECTS.value);
    }

    /*

     * Create birds for the game. Can randomly create a single bird or a flock, in a random direction.
     * This method is called only once - it schedules the next call from inside.
     */
    private void createBirds() {
        float yPos = randomBetween(MIN_HEIGHT, MAX_HEIGHT, random);
        float xPos = avatarLocationSupplier.get().x();
        // choosing direction:
        boolean isGoingLeft = random.nextBoolean();
        int directionMultiplier = isGoingLeft ? MULTIPLIER_LEFT : MULTIPLIER_RIGHT;
        xPos += isGoingLeft ? -DEFAULT_DISTANCE : DEFAULT_DISTANCE;

        Vector2 topLeftCorner = new Vector2(xPos, yPos);
        // choosing between single bird and flock:
        if (random.nextFloat() <= FLOCK_PROBABILITY)
            createFlock(topLeftCorner, directionMultiplier);
        else
            createSingleBird(topLeftCorner, directionMultiplier);

        // scheduling next event:
        float timeForNextBird = randomBetween(MIN_TIME_FOR_NEW_BIRD, MAX_TIME_FOR_NEW_BIRD, random);
        new ScheduledTask(this, timeForNextBird, false, this::createBirds);
    }

    /*
     * Returns if the given bird is too far from the avatar.
     */
    private boolean birdTooFar(Bird bird) {
        return Math.abs(avatarLocationSupplier.get().x() - 
                        bird.getTopLeftCorner().x()) >= DELETION_DISTANCE;
    }

    /**
     * Overriding the update method to check for bird removal.
     * @param deltaTime the time since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // check if it needs to remove bird:
        birds.removeIf(b -> birdKilled(b) ||
                (birdTooFar(b) && gameObjects.removeGameObject(b, Layers.OBJECTS.value)));
    }

    /*
    * Checks if a given bird has been killed - and, if so, handles its death and removal from game.
    * */
    private boolean birdKilled(Bird bird) {
        if (bird.getHealth() > 0)
            return false;

        bird.setVelocity(Vector2.ZERO);
        bird.renderer().setRenderable(birdRenderableDead);
        scoreIncrementer.run();

        new Transition<>(
                bird, //the game object being changed
                bird::setCenter, //the method to call
                bird.getCenter(), //initial transition value
                bird.getCenter().add(Vector2.DOWN.multY(DELETION_DISTANCE)), //final transition value
                Transition.CUBIC_INTERPOLATOR_VECTOR, //use a cubic interpolator
                DEATH_TRANSITION_TIME, //transition over transition time
                Transition.TransitionType.TRANSITION_ONCE,
                ()->this.gameObjects.removeGameObject(bird, Layers.OBJECTS.value));

        return true;
    }
}

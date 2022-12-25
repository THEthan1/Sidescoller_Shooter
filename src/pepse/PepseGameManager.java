package pepse;

import danogl.GameManager;
import danogl.GameObject;
import danogl.components.Transition;
import danogl.gui.ImageReader;
import danogl.gui.SoundReader;
import danogl.gui.UserInputListener;
import danogl.gui.WindowController;
import danogl.gui.rendering.Camera;
import danogl.util.Vector2;
import pepse.world.*;
import pepse.world.daynight.Night;
import pepse.world.daynight.Sun;
import pepse.world.daynight.SunHalo;
import pepse.world.npcs.BirdMaker;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedList;

/**
 * The main class of the simulator.
 * The main() function is here.
 * @author Ohad Klein, Ethan Glick
 */
public class PepseGameManager extends GameManager {
    private static final String TITLE = "P.E.P.S.E: People Engage Pigeons to Save the Earth!";
    private static final String ENERGY_TEXT = "Energy: ";
    private static final String HEALTH_TEXT = "Health: ";
    private static final String TIME_TEXT = "Days Survived: ";
    private static final String SCORE_TEXT = "Pigeons Killed: ";
    private static final int WIN_SCORE = 100;
    private static final int INITIAL_SEED = 420;
    private static final int CYCLE_LENGTH_SEC = 30;
    private static final float HUD_ELEMENT_WIDTH = 250;
    private static final float HUD_ELEMENT_HEIGHT = 20;
    private static final float HUD_ELEMENT_BUFFER = 5;
    private static final float AVATAR_START_HEIGHT = 100;
    private static final float DEATH_ANIMATION_TIME = 2;
    private static final float MIN_HEALTH = 1;

    private static final Color HALO_COLOR = new Color(255, 255, 0, 20);
    private static final Color HEALTH_COLOR = Color.RED;
    private static final Color ENERGY_COLOR = Color.BLUE;
    private static final Color TIME_COLOR = Color.GREEN;
    private static final Color SCORE_COLOR = Color.BLACK;
    private static final String WELCOME_MESSAGE =
            "Welcome to....\n\n%s" +
            "\n\nThe world has been overrun by giant toxic pigeons!\n" +
            "You are the Earth's last hope: kill the pigeons, " +
            "before their droppings kill you!\n\n" +
            "Controls:\n%s";
    private static final String CONTROLS =
            "LEFT_ARROW: Move Left\n" +
            "RIGHT_ARROW: Move Right\n" +
            "SPACE: Jump\n" +
            "SHIFT+SPACE: Fly\n" +
            "S: Shoot (in current direction)\n" +
            "S+UP_ARROW: Shoot Up\n\n" +
            "How many days can YOU survive?";
    private static final String GAME_OVER_TEXT =
            "After just %d days, the pigeons have defeated you!\n" +
            "And you only managed to take down %d of them!\n" +
            "You are not the hero the Earth deserves....\n\n" +
            "Would you like to try again?";
    private static final String GAME_OVER_WIN_TEXT =
            "After %d days, the pigeons have defeated you!\n" +
            "But you managed to take down %d of them!\n" +
            "Well done! You are the hero the earth deserves!\n\n" +
            "But the war against the pigeons rages on....\n\n" +
            "Would you like to continue the fight?";

    private Avatar avatar;
    private LinkedList<World> worlds;
    private int worldWidth;
    private int currentWorldIndex;
    private WindowController windowController;
    private Vector2 windowDimensions;
    private long startTime = 0;
    private int score;

    /**
     * The method will be called once when a GameGUIComponent is created,
     * and again after every invocation of windowController.resetGame().
     * @param imageReader      Contains a single method: readImage, which reads an image from disk.
     * @param soundReader      Contains a single method: readSound, which reads a wav file from disk.
     * @param inputListener    Contains a single method: isKeyPressed,
 *                             which returns whether a given key is currently pressed by the user or not.
     * @param windowController Contains an array of helpful, self-explanatory methods concerning the window.
     */
    @Override
    public void initializeGame(ImageReader imageReader, SoundReader soundReader,
                               UserInputListener inputListener, WindowController windowController) {
        super.initializeGame(imageReader, soundReader, inputListener, windowController);
        this.windowController = windowController;
        this.windowDimensions = windowController.getWindowDimensions();
        this.worldWidth = (int) this.windowDimensions.x();
        this.score = 0;
        this.avatar = Avatar.create(gameObjects(), Layers.OBJECTS.value, new Vector2(this.worldWidth/2f,
                AVATAR_START_HEIGHT), inputListener, imageReader);
        setCamera(new Camera(avatar, Vector2.ZERO, windowDimensions, windowDimensions));
        Sky.create(gameObjects(), windowController.getWindowDimensions(), Layers.SKY.value);
        Night.create(gameObjects(), Layers.NIGHT.value, windowDimensions, CYCLE_LENGTH_SEC);
        GameObject sun = Sun.create(gameObjects(), Layers.SUN.value, windowDimensions, CYCLE_LENGTH_SEC);
        SunHalo.create(gameObjects(), Layers.SUN_HALO.value, sun, HALO_COLOR);
        BirdMaker.create(Vector2.ZERO, Vector2.ZERO, null, imageReader,
                gameObjects(), this.avatar::getTopLeftCorner, ()->score++, INITIAL_SEED);

        addHUD_Elements();
        createWorlds();
        setCollisionRules();
        showWelcomeMessage();
        this.startTime = System.nanoTime(); // to track elapsed time of game
    }

    /*
    * Adds HUD elements (score, health, etc.) to the screen.
    * */
    private void addHUD_Elements() {
        /*HUD elements */
        Vector2 elementSize = new Vector2(HUD_ELEMENT_WIDTH, HUD_ELEMENT_HEIGHT);
        // Left side of screen:
        // Health
        HUD_Element.createValueTrackingElement(gameObjects(), Layers.UI.value, Vector2.ZERO, elementSize,
                HEALTH_TEXT, HEALTH_COLOR, this.avatar::getHealth);
        // Energy
        HUD_Element.createValueTrackingElement(gameObjects(), Layers.UI.value,
                new Vector2(0, HUD_ELEMENT_HEIGHT+HUD_ELEMENT_BUFFER), elementSize, ENERGY_TEXT,
                ENERGY_COLOR, this.avatar::getEnergy);
        // Right side of screen:
        // Time
        HUD_Element.createValueTrackingElement(gameObjects(), Layers.UI.value,
                new Vector2(windowDimensions.x()-HUD_ELEMENT_WIDTH, 0),
                elementSize, TIME_TEXT, TIME_COLOR, this::getDayCount);
        // Score
        HUD_Element.createValueTrackingElement(gameObjects(), Layers.UI.value,
                new Vector2(windowDimensions.x()-HUD_ELEMENT_WIDTH,
                            HUD_ELEMENT_HEIGHT+HUD_ELEMENT_BUFFER),
                elementSize, SCORE_TEXT, SCORE_COLOR, this::getScore);
    }

    /*
    * Shows a welcome message to the player, including the list of controls
    * */
    private void showWelcomeMessage() {
        windowController.showMessageBox(String.format(WELCOME_MESSAGE, TITLE, CONTROLS));
    }

    /*
    * Returns the number of days (cycles) the player has been alive
    * */
    private int getDayCount() {
        long oneBillion = 1000000000; // to convert from nano-seconds to seconds
        return (int) (((System.nanoTime() - startTime)/(oneBillion))/CYCLE_LENGTH_SEC);
    }

    /*
    * Returns the players current score
    * */
    private int getScore() {
        return score;
    }

    /*
     * sets up which layers should interact with one another
     */
    private void setCollisionRules() {
        gameObjects().layers().shouldLayersCollide(Layers.LEAVES.value,
                Layers.TERRAIN_TOP.value, true);
        gameObjects().layers().shouldLayersCollide(Layers.OBJECTS.value,
                Layers.TERRAIN_TOP.value, true);
        gameObjects().layers().shouldLayersCollide(Layers.OBJECTS.value,
                Layers.TERRAIN.value, true);
        gameObjects().layers().shouldLayersCollide(Layers.OBJECTS.value,
                Layers.TREES.value, true);
    }

    /*
     * creates the world for the game.
     */
    private void createWorlds() {
        int start = -this.worldWidth, end = -30;
        this.worlds = new LinkedList<>();
        World right = new World(INITIAL_SEED - 1, start, end,
                                this.gameObjects(), this.windowDimensions);
        start = right.getLastX();
        end = start + this.worldWidth;
        World mid = new World(INITIAL_SEED, start, end, this.gameObjects(), this.windowDimensions);
        start = mid.getLastX();
        end = start + this.worldWidth;
        World left = new World(INITIAL_SEED + 1, start, end,
                                this.gameObjects(), this.windowDimensions);
        Collections.addAll(this.worlds, right, mid, left);
        this.currentWorldIndex = 0;
    }

    /*
     * Checks if there is a need to add a new world to the game.
     */
    private void checkForWorldUpdate() {
        if (this.avatar.getTopLeftCorner().x() > this.worldWidth * (this.currentWorldIndex + 1)) {
            this.currentWorldIndex++;
            // add a world to the right
            int start = this.worlds.getLast().getLastX() + 1;
            int end = start + this.worldWidth;
            int newSeed = this.currentWorldIndex + 1;
            this.worlds.addLast(
                    new World(newSeed, start, end, this.gameObjects(), this.windowDimensions));
            // remove the world from the left
            this.worlds.getFirst().destroy();
            this.worlds.removeFirst();
        } else if (this.avatar.getTopLeftCorner().x() < this.worldWidth * this.currentWorldIndex) {
            this.currentWorldIndex--;
            // add a world to the left
            int end = this.worlds.getFirst().getFirstX() - 1;
            int start = end - this.worldWidth;
            int newSeed = this.currentWorldIndex - 1;
            this.worlds.addFirst(
                    new World(newSeed, start, end, this.gameObjects(), this.windowDimensions));
            // remove the world from the right
            this.worlds.getLast().destroy();
            this.worlds.removeLast();
        }
    }

    /*
    * Check if avatar has collided with the ground and managed to break through due to low frame rate
    * */
    private void checkForAvatarBreakthrough() {
        Vector2 avatarFeet = avatar.getTopLeftCorner().add(avatar.getDimensions());
        float delta = worlds.get(1).getTerrain().groundHeightAt(avatar.getCenter().x()) - avatarFeet.y();

        if (delta < 0) {
            avatar.setTopLeftCorner(new Vector2(avatar.getTopLeftCorner().x(),
                                             avatar.getTopLeftCorner().y()+delta));
        }
    }

    /**
     * Overriding the update method to update different game aspects.
     * @param deltaTime time between updates. For internal use by game engine.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        checkForAvatarBreakthrough();
        checkForWorldUpdate();
        checkForGameOver();
    }

    /*
    * Checks if the player has lost all his health - and if so, begins the game over sequence.
    * */
    private void checkForGameOver() {
        if (avatar.getHealth() < MIN_HEALTH) {
            new Transition<>( // stop movement
                    avatar, //the game object being changed
                    avatar::setDimensions,  //the method to call
                    avatar.getDimensions(),    //initial transition value
                    Vector2.ZERO,   //final transition value
                    Transition.CUBIC_INTERPOLATOR_VECTOR,  //use a cubic interpolator
                    DEATH_ANIMATION_TIME,   //transition over transition time
                    Transition.TransitionType.TRANSITION_ONCE,
                    this::endGame);
        }
    }

    /*
    * Ends the game, presenting a game over message and an option to play again.
    * */
    private void endGame() {
        gameObjects().removeGameObject(avatar);
        String endMessage = (score < WIN_SCORE) ? GAME_OVER_TEXT : GAME_OVER_WIN_TEXT;
        if (windowController.openYesNoDialog(String.format(endMessage, getDayCount(), score)))
            windowController.resetGame();
        else
            windowController.closeWindow();
    }

    /**
     * Entry point for program.
     * Initializes and runs a game of PEPSE.
     */
    public static void main(String[] args) {
        new PepseGameManager().run();
    }
}

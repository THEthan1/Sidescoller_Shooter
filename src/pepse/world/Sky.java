package pepse.world;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.CoordinateSpace;
import danogl.gui.rendering.RectangleRenderable;
import danogl.util.Vector2;
import java.awt.*;

/**
 * Represents the sky.
 * @author Ohad Klein, Ethan Glick
 * */
public class Sky {
    /**
     * Tag for sky object.
     */
    public static final String SKY_TAG = "sky";

    private static final Color SKY_COLOR = Color.decode("#80C6E5");

    /**
     * This function creates a light blue rectangle which is always at the back of the window.
     * @param gameObjects - The collection of all participating game objects.
     * @param windowDimensions - The number of the layer to which the created game object should be added.
     * @param skyLayer - The number of the layer to which the created sky should be added.
     * @return A new game object representing the sky.
     * */
    public static GameObject create(GameObjectCollection gameObjects, Vector2 windowDimensions,
                                     int skyLayer) {
        GameObject sky = new GameObject(Vector2.ZERO, windowDimensions,
                                        new RectangleRenderable(SKY_COLOR));
        sky.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
        sky.setTag(SKY_TAG);
        gameObjects.addGameObject(sky, skyLayer);

        return sky;
    }
}

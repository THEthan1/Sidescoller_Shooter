package pepse.world.daynight;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.CoordinateSpace;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import java.awt.*;

/**
 * Represents the halo of the sun.
 * @author Ohad Klein, Ethan Glick
 * */
public class SunHalo {
    /**
     * Tag for sun halo object.
     */
    public static final String HALO_TAG = "halo";

    private static final float SIZE_MULTIPLIER = 3;

    /**
     * This function creates a halo around a given object that represents the sun.
     * @param gameObjects - The collection of all participating game objects.
     * @param layer - The number of the layer to which the created halo should be added.
     * @param sun - A game object representing the sun (it will be followed by the created game object).
     * @param color - The color of the halo.
     * @return A new game object representing the sun's halo.
     * */
    public static GameObject create(GameObjectCollection gameObjects, int layer, GameObject sun,
                                    Color color) {
        GameObject halo = new GameObject(Vector2.ZERO, sun.getDimensions().mult(SIZE_MULTIPLIER),
                new OvalRenderable(color));
        halo.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
        halo.setTag(HALO_TAG);
        gameObjects.addGameObject(halo, layer);
        halo.addComponent(t->halo.setCenter(sun.getCenter()));
        return halo;
    }
}

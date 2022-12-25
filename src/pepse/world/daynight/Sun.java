package pepse.world.daynight;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.CoordinateSpace;
import danogl.components.Transition;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import java.awt.*;

/**
 * Represents the sun - moves across the sky in an elliptical path.
 * @author Ohad Klein, Ethan Glick
 * */
public class Sun {
    /**
     * Tag for sun object.
     */
    public static final String SUN_TAG = "sun";

    private static final float FULL_CIRCLE_VALUE = 360;
    private static final double DEG2RAD = Math.PI/180; // convert degrees to radians

    /**
     * This function creates a yellow circle that moves in the sky in an
     * elliptical path (in camera coordinates).
     * @param gameObjects - The collection of all participating game objects.
     * @param layer - The number of the layer to which the created sun should be added.
     * @param windowDimensions - The dimensions of the windows.
     * @param cycleLength - The amount of seconds it should take the game object to complete a full cycle.
     * @return A new game object representing the sun.
     * */
    public static GameObject create(GameObjectCollection gameObjects, int layer, Vector2 windowDimensions,
                                    float cycleLength) {
        float size = windowDimensions.y() * 0.2f;

        GameObject sun = new GameObject(Vector2.ZERO, new Vector2(size, size),
                new OvalRenderable(Color.YELLOW));
        sun.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
        sun.setTag(SUN_TAG);
        gameObjects.addGameObject(sun, layer);

        float xRadius = windowDimensions.x()*0.6f; // slightly more than half screen so sun goes out of view
        float yRadius = windowDimensions.y()/2;

        new Transition<Float>(
                sun, //the game object being changed
                (a -> sun.setCenter(new Vector2(
                        (float) -(Math.cos((a-90)*DEG2RAD)*xRadius) + windowDimensions.x()/2,
                        (float) Math.sin((a-90)*DEG2RAD)*yRadius + windowDimensions.y()*2/3))),
                0f,    //initial transition value
                FULL_CIRCLE_VALUE,   //final transition value
                Transition.CUBIC_INTERPOLATOR_FLOAT,  //use a cubic interpolator
                cycleLength,   //transition fully over one day
                Transition.TransitionType.TRANSITION_LOOP,
                null);  //nothing further to execute upon reaching final value

        return sun;
    }
}

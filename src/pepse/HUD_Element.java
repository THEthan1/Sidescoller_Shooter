package pepse;

import danogl.GameObject;
import danogl.collisions.GameObjectCollection;
import danogl.components.CoordinateSpace;
import danogl.gui.rendering.Renderable;
import danogl.gui.rendering.TextRenderable;
import danogl.util.Vector2;
import java.awt.*;
import java.util.function.Supplier;

/**
 * A class for creating HUD elements - visual displays on the screen that present updating information.
 * @author Ethan Glick, Ohad Klein
 */
public class HUD_Element extends GameObject {
    private Runnable updateFunction;

    /*
     * private constructor.
     */
    private HUD_Element(Vector2 topLeftCorner, Vector2 dimensions, Renderable renderable) {
        super(topLeftCorner, dimensions, renderable);
    }

    /**
     * Creates and returns a new HUD element.
     * @param gameObjects the GameObjectsCollection to add this object to.
     * @param layer the layer to add the HUD element to.
     * @param topLeftCorner the topLeftCorner coordinate for the element.
     * @param dimensions the dimension of the element.
     * @param text the persisting text for the element (to be shown before the changing value).
     * @param textColor the color for the element's text.
     * @param supplier a supplier that provides the current value that is to be displayed by the element.
     * @return the HUD element that was created.
     */
    public static GameObject createValueTrackingElement(GameObjectCollection gameObjects, int layer,
                                                        Vector2 topLeftCorner, Vector2 dimensions,
                                                        String text, Color textColor,
                                                        Supplier<Number> supplier) {
        TextRenderable renderable = new TextRenderable("");
        renderable.setColor(textColor);
        HUD_Element element = new HUD_Element(topLeftCorner, dimensions, renderable);
        element.updateFunction =
                ()->((TextRenderable)                   // convert float values to int for nicer visual
                        element.renderer().getRenderable()).setString(text + supplier.get().intValue());

        element.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
        gameObjects.addGameObject(element, layer);

        return element;
    }

    /**
     * The update function for the HUD element.
     * Updates the value displayed by the element.
     * @param deltaTime the time since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateFunction.run();
    }
}

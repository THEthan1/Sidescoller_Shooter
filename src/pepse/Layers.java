package pepse;

import danogl.collisions.Layer;

/**
 * An enum holding values for the different game-object layers in the game.
 * @author Ohad Klein, Ethan Glick
 */
public enum Layers {
    SKY(Layer.BACKGROUND), // -200
    SUN(-160),
    SUN_HALO(-150),
    TERRAIN(Layer.STATIC_OBJECTS), // -100
    TERRAIN_TOP(-99),
    TREES(-70),
    LEAVES(-60),
    OBJECTS(Layer.DEFAULT), // 0
    NIGHT(Layer.FOREGROUND), // 100
    UI(Layer.UI); // 200

    /**
     * the layer's value to be used.
     */
    public final int value;

    /* private constructor */
    Layers(int value) {
        this.value = value;
    }
}

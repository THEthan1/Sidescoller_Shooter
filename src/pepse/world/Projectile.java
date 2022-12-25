package pepse.world;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.collisions.GameObjectCollection;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import java.util.function.Supplier;

/**
 * An object representing a projectile, such as a bullet.
 * @author Ethan Glick, Ohad Klein
 */
public class Projectile extends GameObject {
    /**
     * Tag for projectile object.
     */
    public static final String PROJECTILE_TAG = "projectile";

    private static final float DELETION_DISTANCE = 1000;
    private final float damage;
    private final String ignoreCollisionTag;
    private final GameObjectCollection gameObjects;
    private final Supplier<Vector2> avatarLocationSupplier;

    /**
     * Constructor.
     * @param center the center coordinates for this object.
     * @param dimensions the dimensions for this object.
     * @param renderable the renderable for this object.
     * @param gameObjects the GameObjectsCollection to add this object to.
     * @param damage the damage that this object should cause to Damageable objects it impacts.
     * @param velocity the speed and direction for this object.
     * @param ignoreTag the tag of the object that released this projectile, to ignore collisions with it.
     * @param avatarLocationSupplier a Supplier that provides the location of the avatar (in order to
     *                               determine if the projectile has traveled out of view).
     */
    public Projectile(Vector2 center, Vector2 dimensions, Renderable renderable,
                      GameObjectCollection gameObjects, float damage, Vector2 velocity, String ignoreTag,
                      Supplier<Vector2> avatarLocationSupplier) {
        super(Vector2.ZERO, dimensions, renderable);
        this.setCenter(center);
        this.ignoreCollisionTag = ignoreTag;
        this.damage = damage;
        this.gameObjects = gameObjects;
        this.setVelocity(velocity);
        this.avatarLocationSupplier = avatarLocationSupplier;
        this.setTag(PROJECTILE_TAG);
    }

    /**
     * Override the shouldCollideWith.
     * Prevents projectiles from colliding with one another.
     * @param other the object that this object might collide with.
     */
    @Override
    public boolean shouldCollideWith(GameObject other) {
        if (other instanceof Projectile) {
            return false;
        }
        return super.shouldCollideWith(other);
    }

    /**
     * Override onCollisionEnter.
     * If this object collides with a Damageable object,
     * the Damageable's takeDamage function will be called.
     * @param other the object that this object collided with.
     * @param collision information about the collision.
     */
    @Override
    public void onCollisionEnter(GameObject other, Collision collision) {
        super.onCollisionEnter(other, collision);
        if (!other.getTag().equals(ignoreCollisionTag)) {
            if (other instanceof Damageable)
                ((Damageable) other).takeDamage(damage);
            gameObjects.removeGameObject(this);
        }
    }

    /**
     * Override the update function.
     * Removes this object from the game if it has gone out of view.
     * @param deltaTime the time since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        if (projectileTooFar()) {
            gameObjects.removeGameObject(this);
        }
    }

    /*
    * checks if this object has gone out of view.
    * */
    private boolean projectileTooFar() {
        return Math.abs(avatarLocationSupplier.get().x() - this.getTopLeftCorner().x())
                >= DELETION_DISTANCE;
    }
}

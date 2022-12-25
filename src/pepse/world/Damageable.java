package pepse.world;

/**
 * An interface for objects that have health and the ability to take damage.
 * @author Ohad Klein, Ethan Glick
 */
public interface Damageable {
    /**
     * Gets health of damageable.
     * @return health float
     */
    float getHealth();

    /**
     * cause damage for damageable object.
     * @param damage damage to be taken
     */
    void takeDamage(float damage);
}

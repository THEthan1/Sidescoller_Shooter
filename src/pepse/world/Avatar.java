package pepse.world;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.collisions.GameObjectCollection;
import danogl.components.ScheduledTask;
import danogl.gui.ImageReader;
import danogl.gui.UserInputListener;
import danogl.gui.rendering.AnimationRenderable;
import danogl.gui.rendering.OvalRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import pepse.world.npcs.Bird;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * An avatar that can move around the world.
 * @author Ethan Glick, Ohad Klein
 */
public class Avatar extends GameObject implements Damageable {
    /**
     * Tag for avatar object.
     */
    public static final String AVATAR_TAG = "avatar";

    private static final float WALK_VELOCITY = 300;
    private static final float JUMP_VELOCITY = -300;
    private static final float FLY_VELOCITY = -250;
    private static final float GRAVITY = 800;
    private static final float MAX_ENERGY = 100;
    private static final float MINIMUM_IMPACT_FOR_DAMAGE = 500;
    private static final float BULLET_SPEED = 500;
    private static final float BULLET_DAMAGE = 30;
    private static final float energyIncrement = 0.5f;
    private static final double TIME_BETWEEN_CLIPS = 0.2f;
    private static final double TIME_BETWEEN_SHOOTING_CLIPS = 0.1f;
    private static final float PARTIAL_HEALING_INCREMENT = 0.02f;
    private static final float VELOCITY_DAMAGE_DIVIDER = 100;
    private static final String STANDING_IMAGE = "assets/avatar/stand.png";
    private static final String[] WALKING_IMAGES = {
            "assets/avatar/walk_1.png",
            "assets/avatar/walk_2.png",
            "assets/avatar/walk_3.png",
            "assets/avatar/walk_4.png"};
    private static final String[] JUMPING_IMAGES = {
            "assets/avatar/jump_1.png",
            "assets/avatar/jump_2.png",
            "assets/avatar/jump_3.png",
            "assets/avatar/jump_4.png",
            "assets/avatar/jump_5.png"};
    private static final String[] SHOOTING_IMAGES = {
            "assets/avatar/shoot_1.png",
            "assets/avatar/shoot_2.png",
            "assets/avatar/shoot_3.png",
            "assets/avatar/shoot_4.png",
            "assets/avatar/shoot_5.png"};
    private static final String[] HURT_IMAGES = {
            "assets/avatar/hurt_1.png",
            "assets/avatar/hurt_2.png",
            "assets/avatar/hurt_3.png",
            "assets/avatar/hurt_4.png"};
    private static final Vector2 AVATAR_SIZE = new Vector2(70, 120);
    private static final Vector2 BULLET_SIZE = new Vector2(3,5);
    private static final Vector2 BULLET_START_VECTOR = new Vector2(35, -15);
    private Renderable standingRenderable;
    private AnimationRenderable walkingRenderable;
    private AnimationRenderable jumpingRenderable;
    private AnimationRenderable shootingRenderable;
    private AnimationRenderable hurtRenderable;
    private UserInputListener inputListener;
    private GameObjectCollection gameObjects;
    private int layer;
    private float energy = 100;
    private float health = 100;
    private boolean isFacingLeft = false;

    /*
    * private constructor.
    * */
    private Avatar(Vector2 topLeftCorner, Vector2 dimensions, Renderable renderable) {
        super(topLeftCorner, dimensions, renderable);
    }

    /**
     * This function creates an avatar that can travel the world and is followed by the camera.
     * It can stand, walk, jump and fly, and never reaches the end of the world.
     * @param gameObjects   The collection of all participating game objects.
     * @param layer         The number of the layer to which the created avatar should be added.
     * @param topLeftCorner The location of the top-left corner of the created avatar.
     * @param inputListener Used for reading input from the user.
     * @param imageReader   Used for reading images from disk or from within a jar.
     * @return A newly created representing the avatar.
     */
    public static Avatar create(GameObjectCollection gameObjects, int layer, Vector2 topLeftCorner,
                                UserInputListener inputListener, ImageReader imageReader) {
        Avatar avatar = new Avatar(topLeftCorner, AVATAR_SIZE,
                                    imageReader.readImage(STANDING_IMAGE,true));
        avatar.inputListener = inputListener;
        avatar.gameObjects = gameObjects;
        avatar.layer = layer;
        avatar.physics().preventIntersectionsFromDirection(Vector2.ZERO);
        avatar.transform().setAccelerationY(GRAVITY);
        avatar.setTag(AVATAR_TAG);
        avatar.walkingRenderable =
                new AnimationRenderable(WALKING_IMAGES, imageReader,
                        true, TIME_BETWEEN_CLIPS);
        avatar.jumpingRenderable =
                new AnimationRenderable(JUMPING_IMAGES, imageReader,
                        true, TIME_BETWEEN_CLIPS);
        avatar.shootingRenderable =
                new AnimationRenderable(SHOOTING_IMAGES, imageReader,
                        true, TIME_BETWEEN_SHOOTING_CLIPS);
        avatar.hurtRenderable =
                new AnimationRenderable(HURT_IMAGES, imageReader,
                        true, TIME_BETWEEN_CLIPS);
        avatar.standingRenderable = imageReader.readImage(STANDING_IMAGE, true);
        gameObjects.addGameObject(avatar, layer);

        return avatar;
    }

    /**
     * Getter.
     * @return health (floored to int)
     */
    @Override
    public float getHealth() {
        return health;
    }

    /**
     * Getter.
     * @return energy (floored to int)
     */
    public float getEnergy() {
        return energy;
    }

    /**
     * Override Damageable takeDamage() function.
     * Decreases avatars health by the given amount.
     * @param damage the amount to decrease the avatars health by.
     */
    @Override
    public void takeDamage(float damage) {
        health -= (health - damage > 0) ? damage : health;
        renderer().setRenderable(hurtRenderable);
        new ScheduledTask(this,
                (float) TIME_BETWEEN_CLIPS * HURT_IMAGES.length, false,
                ()-> {if (renderer().getRenderable() == hurtRenderable)
                    renderer().setRenderable(standingRenderable);});
    }

    /**
     * Override onCollisionEnter function.
     * Check for collision with the ground.
     * @param other the object this object collided with.
     * @param collision information about the collision.
     */
    @Override
    public void onCollisionEnter(GameObject other, Collision collision) {
        super.onCollisionEnter(other, collision);
        // stops avatar when it falls onto blocks
        if (other.getTag().equals(Terrain.TERRAIN_TOP_TAG)) {
            checkForImpactDamage();
            transform().setVelocityY(0);
        }
    }

    /*
     * checks if avatar impacted the ground with enough force to cause damage.
     */
    private void checkForImpactDamage() {
        if (transform().getVelocity().y() >= MINIMUM_IMPACT_FOR_DAMAGE)
            takeDamage(transform().getVelocity().y() / VELOCITY_DAMAGE_DIVIDER);
    }

    /**
     * Override onCollisionStay function.
     * If avatar lands on bird, the avatar's velocity should become that of the birds.
     * @param other the object this object collided with.
     * @param collision information about the collision.
     */
    @Override
    public void onCollisionStay(GameObject other, Collision collision) {
        super.onCollisionStay(other, collision);
        if (other instanceof Bird) {
            transform().setVelocityX(this.getVelocity().x() + other.getVelocity().x());
        }
    }

    /**
     * Override update function.
     * Adjust avatar's position, renderable, and if its is shooting, based on user input,
     * as well as partially heal avatar if possible.
     * @param deltaTime the time since the previous frame.
     */
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        Renderable renderableX = handleMotionAxisX();
        Renderable renderableY = handleMotionAxisY();
        Renderable renderableS = handleShooting();

        // assign renderable based on priority: 1) shooting 2) hurt 3) jumping 4) walking
        Renderable renderable = renderableY != null ? renderableY : renderableX;
        renderable = (renderer().getRenderable() == hurtRenderable) ? hurtRenderable : renderable;
        renderable = renderableS != null ? renderableS : renderable;

        if (renderable != null) {
            renderer().setRenderable(renderable);
            renderer().setIsFlippedHorizontally(isFacingLeft);
        }

        partiallyHeal();
    }

    /*
     * increases the avatar's health until it reaches a multiple of 10.
     */
    private void partiallyHeal() {
        if ((int) health < getNearest10((int) health))
            health += PARTIAL_HEALING_INCREMENT;
    }

    /*
     * returns the nearest multiple of 10 greater than or equal to the given number.
     */
    private static int getNearest10(int num) {
        return ((num+9) / 10) * 10;
    }

    /*
     * handles user input for shooting.
     */
    private Renderable handleShooting() {
        if (inputListener.isKeyPressed(KeyEvent.VK_S)) {
            boolean goUp = inputListener.isKeyPressed(KeyEvent.VK_UP);
            Vector2 bulletVelocity =
                    new Vector2(goUp? 0 : 1, goUp? -1 : 0).mult(BULLET_SPEED).multX(isFacingLeft? -1 : 1);
            Vector2 startPos = this.getCenter().add(BULLET_START_VECTOR.multX((isFacingLeft? -1 : 1)));
            Projectile bullet = new Projectile(
                    startPos, BULLET_SIZE,new OvalRenderable(Color.YELLOW),
                    gameObjects, BULLET_DAMAGE, bulletVelocity, AVATAR_TAG, this::getTopLeftCorner);
            gameObjects.addGameObject(bullet, layer);

            new ScheduledTask(this,
                    (float) TIME_BETWEEN_SHOOTING_CLIPS * SHOOTING_IMAGES.length, false,
                    ()-> {if (renderer().getRenderable() == shootingRenderable)
                        renderer().setRenderable(standingRenderable);});
            return shootingRenderable;
        }

        return null;
    }

    /*
     * handles user input for Y axis motion.
     */
    private Renderable handleMotionAxisY() {
        // fly - only when we have energy
        if (inputListener.isKeyPressed(KeyEvent.VK_SPACE) &&
                inputListener.isKeyPressed(KeyEvent.VK_SHIFT) && energy > 0) {
            energy -= energyIncrement;
            transform().setVelocityY(FLY_VELOCITY);
        }
        // jump - only when on ground
        else if (inputListener.isKeyPressed(KeyEvent.VK_SPACE) && getVelocity().y() == 0) {
            transform().setVelocityY(JUMP_VELOCITY);
            new ScheduledTask(this,
                    (float) TIME_BETWEEN_CLIPS * JUMPING_IMAGES.length, false,
                    ()-> {if (renderer().getRenderable() == jumpingRenderable)
                        renderer().setRenderable(standingRenderable);});
            if (renderer().getRenderable() != shootingRenderable)
                return jumpingRenderable;
        }
        else if (getVelocity().y() == 0 && energy < MAX_ENERGY) {
            energy += energyIncrement;
        }

        return null;
    }

    /*
     * handles user input for X axis motion.
     */
    private Renderable handleMotionAxisX() {
        float xVel = 0;
        Renderable renderable = null;
        if (inputListener.isKeyPressed(KeyEvent.VK_LEFT)) {
            xVel -= WALK_VELOCITY;
            isFacingLeft = true;
            renderable = walkingRenderable;
        }
        else if (inputListener.isKeyPressed(KeyEvent.VK_RIGHT)) {
            xVel += WALK_VELOCITY;
            isFacingLeft = false;
            renderable = walkingRenderable;
        }
        else {
            renderable = standingRenderable;
        }

        if (renderer().getRenderable() == jumpingRenderable ||
                renderer().getRenderable() == shootingRenderable)
            renderable = null;

        transform().setVelocityX(xVel);

        return renderable;
    }
}

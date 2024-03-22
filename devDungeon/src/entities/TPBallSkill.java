package entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import contrib.utils.components.health.DamageType;
import contrib.utils.components.skill.DamageProjectile;
import core.Game;
import core.level.utils.LevelElement;
import core.utils.Point;
import core.utils.components.path.IPath;
import core.utils.components.path.SimpleIPath;
import java.util.function.Supplier;
import utils.EntityUtils;

/**
 * Subclass of {@link DamageProjectile}.
 *
 * <p>The TPBallSkill class extends the functionality of {@link DamageProjectile} to implement the
 * specific behavior of a teleporting ball skill.
 *
 * <p>The projectile will fly through the dungeon, and if it hits an entity, it will teleport the
 * entity to a random or specific location. It will also be removed from the game if it hits a wall
 * or has reached the maximum distance.
 */
public final class TPBallSkill extends DamageProjectile {
  private static final IPath PROJECTILE_TEXTURES = new SimpleIPath("skills/fireball");
  private static final IPath PROJECTILE_SOUND = new SimpleIPath("sounds/fireball.wav");
  private static final float PROJECTILE_SPEED = 7.0f;
  private static final int DAMAGE_AMOUNT = 1;
  private static final DamageType DAMAGE_TYPE = DamageType.MAGIC;
  private static final Point HIT_BOX_SIZE = new Point(1, 1);
  private static final float PROJECTILE_RANGE = 7f;
  private Point tpTarget = null;

  /**
   * Create a {@link DamageProjectile} that looks like a magic ball and will cause magic damage and
   * teleport the entity to a random or specific location.
   *
   * @param targetSelection A function used to select the point where the projectile should fly to.
   * @param tpTarget The target point to teleport the entity to. If null, a random point will be
   *     selected.
   * @see DamageProjectile
   */
  public TPBallSkill(final Supplier<Point> targetSelection, Point tpTarget) {
    super(
        PROJECTILE_TEXTURES,
        PROJECTILE_SPEED,
        DAMAGE_AMOUNT,
        DAMAGE_TYPE,
        HIT_BOX_SIZE,
        targetSelection,
        PROJECTILE_RANGE,
        DamageProjectile.DEFAULT_ON_WALL_HIT,
        (projectile, entity) -> {
          Point targetPos = tpTarget;
          if (targetPos == null) {
            targetPos = Game.currentLevel().randomTilePoint(LevelElement.FLOOR);
          }
          EntityUtils.teleportEntityTo(entity, targetPos);
        });
    this.tpTarget = tpTarget;
    this.tintColor(0xFF00FFFF);
  }

  @Override
  protected void playSound() {
    Sound soundEffect = Gdx.audio.newSound(Gdx.files.internal(PROJECTILE_SOUND.pathString()));

    // Generate a random pitch between 1.5f and 2.0f
    float minPitch = 2f;
    float maxPitch = 3f;
    float randomPitch = MathUtils.random(minPitch, maxPitch);

    // Play the sound with the adjusted pitch
    long soundId = soundEffect.play();
    soundEffect.setPitch(soundId, randomPitch);

    // Set the volume
    soundEffect.setVolume(soundId, 0.05f);
  }
}

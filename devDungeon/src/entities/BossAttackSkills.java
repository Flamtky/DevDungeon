package entities;

import com.badlogic.gdx.math.Vector2;
import contrib.components.CollideComponent;
import contrib.components.HealthComponent;
import contrib.components.SpikyComponent;
import contrib.entities.AIFactory;
import contrib.utils.components.health.DamageType;
import contrib.utils.components.skill.DamageProjectile;
import contrib.utils.components.skill.FireballSkill;
import contrib.utils.components.skill.Skill;
import contrib.utils.components.skill.SkillTools;
import core.Entity;
import core.Game;
import core.components.DrawComponent;
import core.components.PositionComponent;
import core.level.Tile;
import core.level.utils.Coordinate;
import core.level.utils.LevelElement;
import core.utils.Point;
import core.utils.components.MissingComponentException;
import core.utils.components.path.SimpleIPath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import level.utils.LevelUtils;
import systems.EventScheduler;

public class BossAttackSkills {

  /**
   * Shoots a fire wall (made of fireballs) towards the hero.
   *
   * @param wallWidth The width of the wall. The wall will be centered on the boss.
   * @return The skill that shoots the fire wall.
   */
  public static Skill fireWall(int wallWidth) {
    return new Skill(
        (skillUser) -> {
          // Firewall
          Point heroPos = SkillTools.heroPositionAsPoint();
          Point bossPos =
              skillUser
                  .fetch(PositionComponent.class)
                  .orElseThrow(
                      () -> MissingComponentException.build(skillUser, PositionComponent.class))
                  .position();
          Vector2 direction = new Vector2(heroPos.x - bossPos.x, heroPos.y - bossPos.y);
          // Main shoot is directly at the hero
          // every other fireball is offset left and right of the main shoot
          Vector2 right = new Vector2(direction).rotateDeg(90).nor();
          Vector2 left = new Vector2(direction).rotateDeg(-90).nor();
          for (int i = -wallWidth / 2; i < wallWidth / 2; i++) {
            if (i == 0) {
              launchFireBall(bossPos, heroPos, bossPos, skillUser);
            } else {
              launchFireBall(
                  new Point(bossPos.x + right.x * i, bossPos.y + right.y * i),
                  new Point(heroPos.x + right.x * i, heroPos.y + right.y * i),
                  bossPos,
                  skillUser);
              launchFireBall(
                  new Point(bossPos.x + left.x * i, bossPos.y + left.y * i),
                  new Point(heroPos.x + left.x * i, heroPos.y + left.y * i),
                  bossPos,
                  skillUser);
            }
          }
        },
        AIFactory.FIREBALL_COOL_DOWN * 2);
  }

  public static Skill fireShockWave(int radius) {
    return new Skill(
        (skillUser) -> {
          Point bossPos =
              skillUser
                  .fetch(PositionComponent.class)
                  .orElseThrow(
                      () -> MissingComponentException.build(skillUser, PositionComponent.class))
                  .position();
          Tile bossTile = Game.currentLevel().tileAt(bossPos);
          if (bossTile == null) {
            return;
          }
          List<Coordinate> placedPositions = new ArrayList<>();
          LevelUtils.explosionAt(
              bossTile.coordinate(),
              radius,
              250L,
              (tile -> {
                if (tile == null
                    || tile.levelElement() == LevelElement.WALL
                    || tile.coordinate().equals(bossTile.coordinate())
                    || placedPositions.contains(tile.coordinate())) {
                  return;
                }
                placedPositions.add(tile.coordinate());

                Entity entity = new Entity("fire");
                PositionComponent posComp =
                    new PositionComponent(tile.coordinate().toCenteredPoint());
                entity.add(posComp);
                entity.add(new CollideComponent());
                try {
                  DrawComponent drawComp = new DrawComponent(new SimpleIPath("skills/fireball"));
                  drawComp.currentAnimation("run_down");
                  entity.add(drawComp);
                } catch (IOException e) {
                  throw new RuntimeException("Could not load fireball texture" + e);
                }
                entity.add(new SpikyComponent(1, DamageType.FIRE, Game.frameRate() / 2));
                Game.add(entity);

                EventScheduler.getInstance()
                    .scheduleAction(
                        () -> {
                          Game.remove(entity);
                        },
                        2000);
              }));
        },
        10 * 1000);
  }

  public static Skill fireCone() {
    return new Skill(
        (skillUser) -> {
          int degree = 40;
          Point heroPos = SkillTools.heroPositionAsPoint();
          Point bossPos =
              skillUser
                  .fetch(PositionComponent.class)
                  .orElseThrow(
                      () -> MissingComponentException.build(skillUser, PositionComponent.class))
                  .position();
          Vector2 direction = new Vector2(heroPos.x - bossPos.x, heroPos.y - bossPos.y).nor();

          // Function to calculate the fireball target position
          Function<Integer, Point> calculateFireballTarget =
              (angle) -> {
                Vector2 offset =
                    new Vector2(direction)
                        .rotateDeg(angle)
                        .scl(new Vector2(heroPos.x - bossPos.x, heroPos.y - bossPos.y).len());
                return new Point(bossPos.x + offset.x, bossPos.y + offset.y);
              };

          // Launch fireballs
          launchFireBall(bossPos, calculateFireballTarget.apply(degree), bossPos, skillUser);
          launchFireBall(bossPos, calculateFireballTarget.apply(0), bossPos, skillUser);
          launchFireBall(bossPos, calculateFireballTarget.apply(-degree), bossPos, skillUser);

          // Schedule another round of fireballs
          EventScheduler.getInstance()
              .scheduleAction(
                  () -> {
                    launchFireBall(
                        bossPos, calculateFireballTarget.apply(degree - 5), bossPos, skillUser);
                    launchFireBall(bossPos, calculateFireballTarget.apply(0), bossPos, skillUser);
                    launchFireBall(
                        bossPos, calculateFireballTarget.apply(-(degree - 5)), bossPos, skillUser);
                  },
                  125);
        },
        AIFactory.FIREBALL_COOL_DOWN * 2);
  }

  public static Skill fireStorm() {
    return new Skill(
        (skillUser) -> {
          int totalFireballs = 16;
          long delayBetweenFireballs = 100;
          // Fire Storm
          Point bossPos =
              skillUser
                  .fetch(PositionComponent.class)
                  .orElseThrow(
                      () -> MissingComponentException.build(skillUser, PositionComponent.class))
                  .position();

          for (int i = 0; i < totalFireballs; i++) {
            final int degree = i * 360 / totalFireballs;
            EventScheduler.getInstance()
                .scheduleAction(
                    () -> {
                      Point target =
                          new Point(
                              (float) (bossPos.x + Math.cos(Math.toRadians(degree)) * 10),
                              (float) (bossPos.y + Math.sin(Math.toRadians(degree)) * 10));
                      launchFireBall(bossPos, target, bossPos, skillUser);
                    },
                    i * delayBetweenFireballs);
          }
        },
        AIFactory.FIREBALL_COOL_DOWN * 2);
  }

  public static void launchFireBall(Point start, Point target, Point bossPos, Entity skillUser) {
    Entity shooter;
    DamageProjectile skill = new FireballSkill(() -> target, 30f, 5.00f, 1);
    skill.ignoreEntity(skillUser);
    if (start.equals(bossPos)) {
      shooter = skillUser;
    } else {
      shooter = new Entity("Fireball Shooter");
      shooter.add(new PositionComponent(start));
      shooter.add(new CollideComponent());
    }

    skill.accept(shooter);
    EventScheduler.getInstance().scheduleAction(skill::disposeSounds, 1000);
  }

  public static Skill getFinalBossSkill() {
    Entity boss =
        Game.entityStream().filter(e -> e.name().contains("Final Boss")).findFirst().orElse(null);
    if (boss == null) {
      return null;
    }
    double healthPercentage = calculateBossHealthPercentage(boss);
    Random random = Game.currentLevel().RANDOM;

    // Example logic for selecting an attack based on the boss's state
    if (healthPercentage > 75) {
      return (getBossAttackChance()) ? fireCone() : normalAttack();
    } else if (healthPercentage > 50) {
      return (getBossAttackChance())
          ? fireWall(5)
          : getBossAttackChance() ? fireStorm() : fireCone();
    } else {
      // Low health - more defensive or desperate attacks
      return (getBossAttackChance())
          ? fireWall(10)
          : getBossAttackChance() ? fireStorm() : fireShockWave(6);
    }
  }

  /**
   * This method returns random boolean. Based on the current Boss health percentage, and the
   * current Stage of the Boss fight
   *
   * <p>E.g. 100% - 75% stage -> current 75% HP -> 90% chance to return true
   */
  public static boolean getBossAttackChance() {
    Entity boss =
        Game.entityStream().filter(e -> e.name().contains("Final Boss")).findFirst().orElse(null);
    if (boss == null) {
      return false;
    }
    double healthPercentage = calculateBossHealthPercentage(boss);
    Random random = Game.currentLevel().RANDOM;

    // Example logic for selecting an attack based on the boss's state
    if (healthPercentage > 75) {
      return random.nextDouble() < 0.9;
    } else if (healthPercentage > 50) {
      return random.nextDouble() < 0.8;
    } else {
      // Low health - more defensive or desperate attacks
      return random.nextDouble() < 0.7;
    }
  }

  public static double calculateBossHealthPercentage(Entity bossEntity) {
    HealthComponent healthComponent =
        bossEntity
            .fetch(HealthComponent.class)
            .orElseThrow(() -> MissingComponentException.build(bossEntity, HealthComponent.class));
    return (double) healthComponent.currentHealthpoints()
        / healthComponent.maximalHealthpoints()
        * 100;
  }

  /**
   * An enchantment version of a normal attack. Shoots two fireballs at the hero. One directly at
   * the hero and one is trying to predict the hero's movement.
   *
   * @return The skill that shoots the fireballs.
   */
  public static Skill normalAttack() {
    return new Skill(
        (skillUser) -> {
          Point heroPos = SkillTools.heroPositionAsPoint();
          Point bossPos =
              skillUser
                  .fetch(PositionComponent.class)
                  .orElseThrow(
                      () -> MissingComponentException.build(skillUser, PositionComponent.class))
                  .position();
          launchFireBall(bossPos, heroPos, bossPos, skillUser);
          EventScheduler.getInstance()
              .scheduleAction(
                  () -> {
                    Point heroPos2 = SkillTools.heroPositionAsPoint();
                    Vector2 heroDirection =
                        new Vector2(heroPos2.x - heroPos.x, heroPos2.y - heroPos.y).nor();
                    heroDirection.scl((float) (bossPos.distance(heroPos)) * 2);
                    Point predictedHeroPos =
                        new Point(heroPos2.x + heroDirection.x, heroPos2.y + heroDirection.y);
                    launchFireBall(bossPos, predictedHeroPos, bossPos, skillUser);
                  },
                  50L);
        },
        AIFactory.FIREBALL_COOL_DOWN);
  }
}

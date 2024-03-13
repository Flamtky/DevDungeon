package starter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import contrib.crafting.Crafting;
import contrib.systems.*;
import contrib.utils.components.Debugger;
import contrib.utils.components.skill.SkillTools;
import core.Entity;
import core.Game;
import core.System;
import core.game.ECSManagment;
import core.level.Tile;
import core.systems.LevelSystem;
import core.utils.Point;
import core.utils.components.path.SimpleIPath;
import entities.DevHeroFactory;
import entities.EntityUtils;
import entities.MonsterType;
import java.io.IOException;
import level.utils.DungeonLoader;
import systems.*;
import systems.DevHealthSystem;

public class DevDungeon {

  private static final String BACKGROUND_MUSIC = "sounds/background.wav";

  private static final boolean SKIP_TUTORIAL = false;

  public static void main(String[] args) throws IOException {
    Game.initBaseLogger();
    Debugger debugger = new Debugger();
    configGame();
    onSetup();

    Game.userOnLevelLoad(
        (firstTime) -> {
          // Check if FogOfWar exists, if so reset it
          if (Game.systems().containsKey(FogOfWarSystem.class)) {
            FogOfWarSystem fogOfWarSystem =
                (FogOfWarSystem) Game.systems().get(FogOfWarSystem.class);
            fogOfWarSystem.reset();
          }
        });

    onFrame(debugger);

    // build and start game
    Game.run();
    Game.windowTitle("Dev Dungeon");
  }

  private static void onFrame(Debugger debugger) {
    Game.userOnFrame(debugger::execute);
  }

  private static void onSetup() {
    Game.userOnSetup(
        () -> {
          LevelSystem levelSystem = (LevelSystem) ECSManagment.systems().get(LevelSystem.class);
          levelSystem.onEndTile(DungeonLoader::loadNextLevel);

          createSystems();
          try {
            createHero();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          setupMusic();
          Crafting.loadRecipes();
          if (SKIP_TUTORIAL) {
            DungeonLoader.loadNextLevel(); // First Level start at 1
          } else {
            DungeonLoader.loadLevel(1); // Tutorial at 0
          }
        });
  }

  private static void createHero() throws IOException {
    Entity hero = DevHeroFactory.newHero();
    Game.add(hero);
  }

  private static void setupMusic() {
    Music backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(BACKGROUND_MUSIC));
    backgroundMusic.setLooping(true);
    // backgroundMusic.play();
    backgroundMusic.setVolume(.1f);
  }

  private static void configGame() throws IOException {
    Game.loadConfig(
        new SimpleIPath("dungeon_config.json"),
        contrib.configuration.KeyboardConfig.class,
        core.configuration.KeyboardConfig.class);
    Game.frameRate(30);
    Game.disableAudio(false);
    Game.windowTitle("DevDungeon");
  }

  private static void createSystems() {
    Game.add(new CollisionSystem());
    Game.add(new AISystem());
    Game.add(new ReviveSystem());
    Game.add(new DevHealthSystem());
    Game.add(new ProjectileSystem());
    Game.add(new HealthBarSystem());
    Game.add(new HudSystem());
    Game.add(new SpikeSystem());
    Game.add(new IdleSoundSystem());
    Game.add(new FallingSystem());
    Game.add(new PathSystem());
    Game.add(new LevelEditorSystem());
    Game.add(new LevelTickSystem());
    Game.add(new PitSystem());
    // Game.add(new FogOfWarSystem());
    Game.add(
        new System() {
          @Override
          public void execute() {
            Point mosPos = SkillTools.cursorPositionAsPoint();
            mosPos = new Point(mosPos.x - 0.5f, mosPos.y - 0.25f);
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
              EntityUtils.spawnMonster(MonsterType.CHORT, mosPos);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
              EntityUtils.spawnMonster(MonsterType.IMP, mosPos);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
              EntityUtils.spawnMonster(MonsterType.ZOMBIE, mosPos);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
              EntityUtils.spawnSign("Hello World", "Schild", mosPos);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) {
              Tile tile = LevelSystem.level().tileAt(mosPos);
              if (tile != null) {
                java.lang.System.out.println(
                    "Tile - Coords: "
                        + tile.coordinate()
                        + " Accessible: "
                        + tile.isAccessible()
                        + " CanSeeThrough: "
                        + tile.canSeeThrough()
                        + " Texture: "
                        + tile.texturePath()
                        + " LevelElement: "
                        + tile.levelElement());
              } else {
                java.lang.System.out.println("Tile - null");
              }
            }
          }
        });
  }
}

package starter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import contrib.crafting.Crafting;
import contrib.entities.EntityFactory;
import contrib.systems.*;
import contrib.utils.components.Debugger;
import contrib.utils.components.skill.SkillTools;
import core.Entity;
import core.Game;
import core.System;
import core.level.Tile;
import core.level.utils.*;
import core.systems.LevelSystem;
import core.utils.Point;
import core.utils.components.path.SimpleIPath;
import level.LevelLoader;

import java.io.IOException;

public class DevDungeon {

  private static final String BACKGROUND_MUSIC = "sounds/background.wav";

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
          createSystems();
          try {
            createHero();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          setupMusic();
          Crafting.loadRecipes();
          LevelLoader.loadNextLevel();
        });
  }

  private static void createHero() throws IOException {
    Entity hero = EntityFactory.newHero();
    Game.add(hero);
  }

  private static void setupMusic() {
    Music backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(BACKGROUND_MUSIC));
    backgroundMusic.setLooping(true);
    backgroundMusic.play();
    backgroundMusic.setVolume(.1f);
  }

  private static void configGame() throws IOException {
    Game.loadConfig(
        new SimpleIPath("dungeon_config.json"),
        contrib.configuration.KeyboardConfig.class,
        core.configuration.KeyboardConfig.class);
    Game.frameRate(30);
    Game.disableAudio(false);
    Game.windowTitle("My Dungeon");
  }

  private static void createSystems() {
    Game.add(new CollisionSystem());
    Game.add(new AISystem());
    Game.add(new HealthSystem());
    Game.add(new ProjectileSystem());
    Game.add(new HealthBarSystem());
    Game.add(new HudSystem());
    Game.add(new SpikeSystem());
    Game.add(new IdleSoundSystem());
    Game.add(new FallingSystem());
    Game.add(new PathSystem());
    // Game.add(new FogOfWarSystem());
    Game.add(
        new System() {
          @Override
          public void execute() {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
              Point mosPos = SkillTools.cursorPositionAsPoint();
              mosPos = new Point(mosPos.x - 0.5f, mosPos.y - 0.25f);
              Tile tile = Game.tileAT(mosPos);

              LevelSystem.level().removeTile(tile);
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) {
              java.lang.System.out.println(Game.currentLevel().printLevel());
            }
          }
        });
  }
}

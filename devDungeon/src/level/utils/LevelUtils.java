package level.utils;

import core.Game;
import core.level.Tile;
import core.level.utils.Coordinate;

/** Utility class for level-related operations. */
public class LevelUtils {

  /**
   * Changes the visibility of a rectangular area within the level. The area is defined by the top
   * left and bottom right coordinates. If a tile within the specified area is null, it is skipped.
   *
   * @param topLeft The top left coordinate of the area.
   * @param bottomRight The bottom right coordinate of the area.
   * @param visible The visibility status to be set for the area.
   */
  public static void ChangeVisibilityForArea(
      Coordinate topLeft, Coordinate bottomRight, boolean visible) {
    for (int x = topLeft.x; x <= bottomRight.x; x++) {
      for (int y = bottomRight.y; y <= topLeft.y; y++) {
        Tile tile = Game.tileAT(new Coordinate(x, y));
        if (tile != null) {
          tile.visible(visible);
        }
      }
    }
  }
}

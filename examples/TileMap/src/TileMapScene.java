// TileMapScene
// Shows a scrolling tile-map with click-to-move.
// Pixel-snapping is used to improve rendering speed of the map.
// Images from http://www.lostgarden.com/2007/05/dancs-miraculously-flexible-game.html

import pulpcore.animation.Easing;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.Stage;

public class TileMapScene extends Scene2D {
    
    String[] tiles = {
        "Water Block.png",
        "Stone Block.png",
        "Grass Block.png",
        "Dirt Block.png",
    };
    
    String[] map = {
        "WWWWWWWWWWWWWWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWWWWWWWWWWWWWW",
        "WSSSSSWWSSSSSSSSSSSSSSSSSWW",
        "WSGGGSWWSGGGGGGGGGGGGGGGSWW",
        "WSGGGSWWSSSSSSSGGSSSSSGGSWW",
        "WSGGGSWWWWWSDDSGGSDDDSGGSWW",
        "WSGGGSWWWWWSDDSGGSDDDSGGSWW",
        "WSGGGSWWWWWSDDSGGSDDDSGGSWW",
        "WSGGGSSSSSSSSSSGGSSSSSGGSWW",
        "WSGGGGGGGGGGGGGGGGGGGGGGSWW",
        "WSSSSSSSSSSSSSSSSSSSSSSSSWW",
        "WWWWWWWWWWWWWWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWWWWWWWWWWWWWW",
    };
    
    TileMap tileMap;
    ImageSprite cursor;
    Group mapSprites;
    
    public void load() {
        // Add the background (sky-blue)
        add(new FilledSprite(0xB9D1FF));
        
        // Add the tileset
        tileMap = createTileMap(tiles, map, 100, 80);
        add(tileMap);
        
        // Add some trees
        CoreImage tree = CoreImage.load("Tree Tall.png");
        mapSprites = new Group();
        mapSprites.pixelSnapping.set(true);
        mapSprites.add(new ImageSprite(tree, 300, 280));
        mapSprites.add(new ImageSprite(tree, 1900, 440));
        add(mapSprites);
        
        // Add the cursor
        Input.setCursor(Input.CURSOR_OFF);
        cursor = new ImageSprite("cursor.png", 0, 0);
        cursor.setComposite(CoreGraphics.COMPOSITE_ADD);
        add(cursor);
    }
    
    public void update(int elapsedTime) {
        mapSprites.setLocation(tileMap.viewX.get(), tileMap.viewY.get());
        cursor.setLocation(Input.getMouseX(), Input.getMouseY());
        
        // When scrolling, disable dirty rectangles and hide the cursor
        setDirtyRectanglesEnabled(!tileMap.isScrolling());
        cursor.visible.set(!tileMap.isScrolling());

        // Click-to-scroll
        if (Input.isMousePressed()) {
            int x = Input.getMousePressX();
            int y = Input.getMousePressY();
            double goalX = tileMap.viewX.get() - (x - Stage.getWidth() / 2);
            double goalY = tileMap.viewY.get() - (y - Stage.getHeight() / 2);
            tileMap.viewX.animateTo(goalX, 500, Easing.REGULAR_OUT);
            tileMap.viewY.animateTo(goalY, 500, Easing.REGULAR_OUT);
        }
    }
    
    TileMap createTileMap(String[] tiles, String[] map, int tileWidth, int tileHeight) {
        // Load tile images
        CoreImage[] tileImages = new CoreImage[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            tileImages[i] = CoreImage.load(tiles[i]);
        }
        
        // Create the tile map
        int mapWidth = map[0].length();
        int mapHeight = map.length;
        CoreImage[][] tileMap = new CoreImage[mapWidth][mapHeight];
        for (int i = 0; i < mapWidth; i++) {
            for (int j = 0; j < mapHeight; j++) {
                
                // Convert the map char to the first letter of the tile name
                // i.e., 'W' = "Water Block.png"
                char ch = map[j].charAt(i);
                int index = 0;
                for (int k = 0; k < tiles.length; k++) {
                    if (tiles[k].charAt(0) == ch) {
                        index = k;
                        break;
                    }
                }
                tileMap[i][j] = tileImages[index]; 
            }
        }
        return new TileMap(tileMap, tileWidth, tileHeight);
    }
}

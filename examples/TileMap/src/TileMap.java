// TileMap
// Drag the map to scroll (like Google Maps)
// Rendering speed can be improved by using opaque tiles.
// Tile images from http://www.lostgarden.com/2007/05/dancs-miraculously-flexible-game.html
import pulpcore.animation.Int;
import pulpcore.image.Colors;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.Input;
import pulpcore.scene.Scene2D;
import pulpcore.sprite.FilledSprite;
import pulpcore.sprite.Group;
import pulpcore.sprite.ImageSprite;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

public class TileMap extends Scene2D {
    
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
    
    TileMapSprite tileMap;
    Group mapSprites;
    
    @Override
    public void load() {
        // Add the background (sky-blue)
        add(new FilledSprite(Colors.rgb(185, 209, 255)));
        
        // Add the tileset
        tileMap = createTileMapSprite(tiles, map, 100, 80);
        add(tileMap);
        
        // Add some trees
        mapSprites = new Group();
        mapSprites.add(new ImageSprite("Tree Tall.png", 300, 280));
        mapSprites.add(new ImageSprite("Tree Tall.png", 1900, 440));
        mapSprites.x.bindTo(tileMap.viewX);
        mapSprites.y.bindTo(tileMap.viewY);
        add(mapSprites);
    }
    
    TileMapSprite createTileMapSprite(String[] tiles, String[] map, int tileWidth, int tileHeight) {
        // Load tile images
        CoreImage[] tileImages = new CoreImage[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            tileImages[i] = CoreImage.load(tiles[i]);
        }
        
        // Create the tile map
        int mapWidth = map[0].length();
        int mapHeight = map.length;
        CoreImage[][] tileMapImages = new CoreImage[mapWidth][mapHeight];
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
                tileMapImages[i][j] = tileImages[index];
            }
        }
        return new TileMapSprite(tileMapImages, tileWidth, tileHeight);
    }
    
    /**
        A simple tile map.
        Limitation: the maximum width and height of a TileMap 
        (i.e. tileWidth*numTilesAcross and tileHeight*numTilesDown) should be less than 32768.
    */
    static class TileMapSprite extends Sprite {
        
        private CoreImage[][] tileMap;
        private int tileWidth;
        private int tileHeight;
        private int numTilesAcross;
        private int numTilesDown;

        private boolean mouseDragging = false;
        private int mouseDeltaX = 0;
        private int mouseDeltaY = 0;
        
        public final Int viewX = new Int(this);
        public final Int viewY = new Int(this);
        
        public TileMapSprite(CoreImage[][] tileMap, int tileWidth, int tileHeight) {
            super(0, 0, Stage.getWidth(), Stage.getHeight());
            this.tileMap = tileMap;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            numTilesAcross = tileMap.length;
            numTilesDown = tileMap[0].length;
            
            setCursor(Input.CURSOR_MOVE);
        }
        
        public int getMapWidth() {
            return tileWidth * numTilesAcross;
        }
        
        public int getMapHeight() {
            return tileHeight * numTilesDown;
        }
        
        @Override 
        public void update(int elapsedTime) {
            super.update(elapsedTime);
            viewX.update(elapsedTime);
            viewY.update(elapsedTime);

            // Handle dragging
            if (isMousePressed()) {
                mouseDragging = true;
                mouseDeltaX = Input.getMouseX() - (int)viewX.get();
                mouseDeltaY = Input.getMouseY() - (int)viewY.get();
            }
            if (Input.isMouseReleased()) {
                mouseDragging = false;
            }
            if (mouseDragging && Input.isMouseMoving()) {
                viewX.set(Input.getMouseX() - mouseDeltaX);
                viewY.set(Input.getMouseY() - mouseDeltaY);
            }
        }
        
        @Override
        protected void drawSprite(CoreGraphics g) {
            int ty = viewY.get();
            for (int j = 0; j < numTilesDown; j++) {
                int tx = viewX.get();
                for (int i = 0; i < numTilesAcross; i++) {
                    g.drawImage(tileMap[i][j], tx, ty);
                    tx += tileWidth;
                }
                ty += tileHeight;
            }
        }
    }
}

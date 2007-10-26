import pulpcore.animation.Fixed;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Transform;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

public class TileMap extends Sprite {
    
    private CoreImage[][] tileMap;
    private int tileWidth;
    private int tileHeight;
    private int numTilesAcross;
    private int numTilesDown;
    
    public final Fixed viewX = new Fixed(this);
    public final Fixed viewY = new Fixed(this);
    
    public TileMap(CoreImage[] tiles, int[][] map, int tileWidth, int tileHeight) {
        super(0, 0, Stage.getWidth(), Stage.getHeight());
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        
        // Turn on pixel snapping so we don't see lines between tiles
        pixelSnapping.set(true);
        
        // Create the tile map
        numTilesAcross = map.length;
        numTilesDown = map[0].length;
        tileMap = new CoreImage[numTilesAcross][numTilesDown];
        for (int i = 0; i < numTilesAcross; i++) {
            for (int j = 0; j < numTilesDown; j++) {
                int tileIndex = map[i][j];
                tileMap[i][j] = tiles[tileIndex];
            }
        }
    }
    
    public int getMapWidth() {
        return tileWidth * numTilesAcross;
    }
    
    public int getMapHeight() {
        return tileHeight * numTilesDown;
    }
    
    public boolean isScrolling() {
        return viewX.isAnimating() || viewY.isAnimating();
    }
    
    public void update(int elapsedTime) {
        super.update(elapsedTime);
        viewX.update(elapsedTime);
        viewY.update(elapsedTime);
    }
    
    protected void drawSprite(CoreGraphics g) {
        int fViewX = viewX.getAsFixed();
        int fViewY = viewY.getAsFixed();
        int fTileWidth = CoreMath.toFixed(tileWidth);
        int fTileHeight = CoreMath.toFixed(tileHeight);
        
        if (pixelSnapping.get()) {
            fViewX = CoreMath.intPart(fViewX);
            fViewY = CoreMath.intPart(fViewY);
        }
        
        Transform t = g.getTransform();
        g.pushTransform();
        t.translate(fViewX, fViewY);
        for (int j = 0; j < numTilesDown; j++) {
            for (int i = 0; i < numTilesAcross; i++) {
                g.drawImage(tileMap[i][j]);
                t.translate(fTileWidth, 0);
            }
            t.translate(-numTilesAcross*fTileWidth, fTileHeight);
        }
        g.popTransform();
    }
}

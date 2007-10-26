import pulpcore.animation.Fixed;
import pulpcore.image.CoreGraphics;
import pulpcore.image.CoreImage;
import pulpcore.math.CoreMath;
import pulpcore.math.Transform;
import pulpcore.sprite.Sprite;
import pulpcore.Stage;

/**
    A simple tile map.
    Limitation: the maximum width and height of a TileMap 
    (i.e. tileWidth*numTilesAcross and tileHeight*numTilesDown) should be less than 32768.
*/
public class TileMap extends Sprite {
    
    private CoreImage[][] tileMap;
    private int tileWidth;
    private int tileHeight;
    private int numTilesAcross;
    private int numTilesDown;
    
    public final Fixed viewX = new Fixed(this);
    public final Fixed viewY = new Fixed(this);
    
    public TileMap(CoreImage[][] tileMap, int tileWidth, int tileHeight) {
        super(0, 0, Stage.getWidth(), Stage.getHeight());
        this.tileMap = tileMap;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        numTilesAcross = tileMap.length;
        numTilesDown = tileMap[0].length;
        
        // Turn on pixel snapping so we don't see lines between tiles
        pixelSnapping.set(true);
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

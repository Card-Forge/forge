package forge.adventure.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.EffectData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.RewardData;
import forge.adventure.util.Current;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Reward;

/**
 * EnemySprite
 * Character sprite that represents an Enemy
 */
public class EnemySprite extends CharacterSprite {
    EnemyData data;
    public MapDialog dialog; //Dialog to show on contact. Overrides standard battle (can be started as an action)
    public MapDialog defeatDialog; //Dialog to show on defeat. Overrides standard death (can be removed as an action)
    public EffectData effect; //Battle effect for this enemy. Similar to a player's blessing.
    public String nameOverride = ""; //Override name of this enemy in battles.

    public EnemySprite(EnemyData enemyData) {
        this(0,enemyData);
    }

    public EnemySprite(int id, EnemyData enemyData) {
        super(id,enemyData.sprite);
        data = enemyData;
    }

    @Override
    void updateBoundingRect() { //We want enemies to take the full tile.
        boundingRect = new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public void moveTo(Actor other, float delta) {
        Vector2 diff = new Vector2(other.getX(), other.getY()).sub(pos());

        diff.setLength(data.speed*delta);
        moveBy(diff.x, diff.y);
    }

    public EnemyData getData() {
        return data;
    }

    public Array<Reward> getRewards() {
        Array<Reward> ret=new Array<Reward>();
        if(data.rewards == null)
            return ret;
        for(RewardData rdata:data.rewards) {
            ret.addAll(rdata.generate(false,(Current.latestDeck()!=null? Current.latestDeck().getMain().toFlatList():null)));
        }
        return ret;
    }

    private void drawColorHints(Batch batch){
        int size = Math.min(data.colors.length(), 6);
        float DX = getX() - 2f;
        float DY = getY();

        for(int i = 0; i < size; i++){
            char C = data.colors.toUpperCase().charAt(i);
            switch (C) {
                default: break;
                case 'C': {
                    batch.setColor(Color.DARK_GRAY);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'B': {
                    batch.setColor(Color.PURPLE);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'G': {
                    batch.setColor(Color.GREEN);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'R': {
                    batch.setColor(Color.RED);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'U': {
                    batch.setColor(Color.BLUE);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'W': {
                    batch.setColor(Color.WHITE);
                    batch.draw(Forge.getGraphics().getDummyTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if(Current.player().hasColorView() && !data.colors.isEmpty()) {
            drawColorHints(batch);
        }
        if(dialog != null && dialog.canShow()){ //Draw a talk icon on top.
            Texture T = Current.world().getGlobalTexture();
            TextureRegion TR = new TextureRegion(T, 0, 0, 16, 16);
            batch.draw(TR, getX(), getY() + 16, 16, 16);
        }
    }

}


package forge.adventure.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.google.common.base.Predicates;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.data.EffectData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.RewardData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Current;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Reward;
import forge.card.CardRarity;
import forge.card.CardRulesPredicates;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.List;
import java.util.stream.Collectors;

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
    public RewardData[] rewards; //Additional rewards for this enemy.
    public DialogData.ConditionData spawnCondition; //Condition to spawn.

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
        Array<Reward> ret=new Array<>();
        //Collect custom rewards for chaos battles
        if (data.copyPlayerDeck && AdventurePlayer.current().isFantasyMode()) {
            if (Current.latestDeck() != null) {
                List<PaperCard> paperCardList = Current.latestDeck().getMain().toFlatList().stream()
                        .filter(paperCard -> !paperCard.isVeryBasicLand() && !paperCard.getName().startsWith("Mox"))
                        .collect(Collectors.toList());
                //random uncommons from deck
                List<PaperCard> uncommonCards = paperCardList.stream()
                        .filter(paperCard -> CardRarity.Uncommon.equals(paperCard.getRarity()) || CardRarity.Special.equals(paperCard.getRarity()))
                        .collect(Collectors.toList());
                if (!uncommonCards.isEmpty()) {
                    ret.add(new Reward(Aggregates.random(uncommonCards)));
                    ret.add(new Reward(Aggregates.random(uncommonCards)));
                }
                //random commons from deck
                List<PaperCard> commmonCards = paperCardList.stream()
                        .filter(paperCard -> CardRarity.Common.equals(paperCard.getRarity()))
                        .collect(Collectors.toList());
                if (!commmonCards.isEmpty()) {
                    ret.add(new Reward(Aggregates.random(commmonCards)));
                    ret.add(new Reward(Aggregates.random(commmonCards)));
                    ret.add(new Reward(Aggregates.random(commmonCards)));
                }
                //random rare from deck
                List<PaperCard> rareCards = paperCardList.stream()
                        .filter(paperCard -> CardRarity.Rare.equals(paperCard.getRarity()) || CardRarity.MythicRare.equals(paperCard.getRarity()))
                        .collect(Collectors.toList());
                if (!rareCards.isEmpty()) {
                    ret.add(new Reward(Aggregates.random(rareCards)));
                    ret.add(new Reward(Aggregates.random(rareCards)));
                }
            }
            int val = ((MyRandom.getRandom().nextInt(2)+1)*100)+(MyRandom.getRandom().nextInt(101));
            ret.add(new Reward(val));
            ret.add(new Reward(Reward.Type.Life, 1));
        } else {
            if(data.rewards != null) { //Collect standard rewards.
                Deck enemyDeck = Current.latestDeck(); // By popular demand, remove basic lands from the reward pool.
                CardPool deckNoBasicLands = enemyDeck.getMain().getFilteredPool(Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND), PaperCard.FN_GET_RULES));
                for (RewardData rdata : data.rewards) {
                    ret.addAll(rdata.generate(false, deckNoBasicLands.toFlatList() ));
                }
            }
            if(rewards != null) { //Collect additional rewards.
                for(RewardData rdata:rewards) {
                    //Do not filter in case we want to FORCE basic lands. If it ever becomes a problem just repeat the same as above.
                    ret.addAll(rdata.generate(false,(Current.latestDeck() != null ? Current.latestDeck().getMain().toFlatList() : null)));
                }
            }
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
        if(effect != null){ //Draw a crown icon on top.
            Texture T = Current.world().getGlobalTexture();
            TextureRegion TR = new TextureRegion(T, 16, 0, 16, 16);
            batch.draw(TR, getX(), getY() + 16, 16, 16);
        }
    }

}


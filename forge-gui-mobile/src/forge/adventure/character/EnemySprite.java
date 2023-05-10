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
import forge.adventure.data.DialogData;
import forge.adventure.data.EffectData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.RewardData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Current;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Reward;
import forge.card.CardRarity;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.LinkedList;
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
    public LinkedList<MovementBehavior> movementBehaviors = new LinkedList<>();

    public Vector2 targetVector;
    private final Vector2 _previousPosition = new Vector2();
    private final Vector2 _previousPosition2 = new Vector2();
    private final Vector2 _previousPosition3 = new Vector2();
    private final Vector2 _previousPosition4 = new Vector2();
    private final Vector2 _previousPosition5 = new Vector2();
    private final Vector2 _previousPosition6 = new Vector2();
    private final Float _movementTimeout = 150.0f;
    private boolean _freeze = false; //freeze movement after defeating player
    public float unfreezeRange = 30.0f;
    public float threatRange = 0.0f;
    public float fleeRange = 0.0f;
    public boolean ignoreDungeonEffect = false;
    public String questStageID;

    public EnemySprite(EnemyData enemyData) {
        this(0,enemyData);
    }

    public EnemySprite(int id, EnemyData enemyData) {
        super(id,enemyData.sprite);
        data = enemyData;
    }

    public void parseWaypoints(String waypoints){
        String[] wp = waypoints.replaceAll("\\s", "").split(",");
        for (String s : wp) {
            movementBehaviors.addLast(new MovementBehavior());
            if (s.startsWith("wait")) {
                movementBehaviors.peekLast().duration = Float.parseFloat(s.substring(4));
            } else {
                movementBehaviors.peekLast().destination = Integer.parseInt(s);
            }
        }
    }

    public Rectangle navigationBoundingRect() {
        //This version of the bounds will be used for navigation purposes to allow mobile mobs to not need pixel perfect pathing
        return new Rectangle(boundingRect).setSize(boundingRect.width * collisionHeight, boundingRect.height * collisionHeight);
    }

    @Override
    void updateBoundingRect() { //We want enemies to take the full tile.
        float scale = data == null ? 1f : data.scale;
        if (scale < 0)
            scale = 1f;
        boundingRect.set(getX(), getY(), getWidth()*scale, getHeight()*scale);
        unfreezeRange = 30f * scale;
    }

    public void moveTo(Actor other, float delta) {
        Vector2 diff = new Vector2(other.getX(), other.getY()).sub(pos());

        diff.setLength(data.speed*delta);
        moveBy(diff.x, diff.y,delta);
    }

    public void freezeMovement(){
        _freeze = true;
        setPosition(_previousPosition6.x, _previousPosition6.y);
        targetVector.setZero();
        // This will move the enemy back a few frames of movement.
        // Combined with player doing the same, should no longer be colliding to immediately re-enter battle if mob still present
    }

    public Vector2 getTargetVector(PlayerSprite player, float delta) {
        //todo - this can be integrated into overworld movement as well, giving flee behaviors or moving to generated waypoints
        Vector2 target = pos();
        Vector2 routeToPlayer = new Vector2(player.pos()).sub(target);

        if (_freeze){
            //Mob has defeated player in battle, hold still until player has a chance to move away.
            //Without this moving enemies can immediately restart battle.
            if (routeToPlayer.len() < unfreezeRange) {
                timer += delta;
                return Vector2.Zero;
            }
            else{
                _freeze = false; //resume normal behavior
            }
        }

        if (threatRange > 0 || fleeRange > 0){

            if (routeToPlayer.len() <= threatRange)
            {
                return routeToPlayer;
            }
            if (routeToPlayer.len() <= fleeRange)
            {
                Float fleeDistance = fleeRange - routeToPlayer.len();
                return new Vector2(target).sub(player.pos()).setLength(fleeDistance);
            }
        }

        if (movementBehaviors.size() > 0){

            if (movementBehaviors.peek().getDuration() == 0 && target.equals(_previousPosition6) && timer >= _movementTimeout)
            {
                //stationary in an untimed behavior, move on to next behavior attempt to get unstuck
                if (movementBehaviors.size() > 1) {
                    movementBehaviors.addLast(movementBehaviors.pop());
                    timer = 0.0f;
                }
            }
            else if (movementBehaviors.peek().pos().sub(pos()).len() < 0.3){
                //this is a location based behavior that has been completed. Move on if there are more behaviors
                if (movementBehaviors.size() > 1) {
                    movementBehaviors.addLast(movementBehaviors.pop());
                    timer = 0.0f;
                }
            }
            else if ( movementBehaviors.peek().getDuration() > 0)
            {
                if (timer >= movementBehaviors.peek().getDuration() + delta)
                {
                    //this is a timed behavior that has been completed. Move to the next behavior and restart the timer
                    movementBehaviors.addLast(movementBehaviors.pop());
                    timer = 0.0f;
                }
                else{
                    timer += delta;//this is a timed behavior that has not been completed, continue this behavior
                }
            }
            if (movementBehaviors.peek().pos().len() > 0.3)
                target = new Vector2(movementBehaviors.peek().pos()).sub(pos());
            else target = Vector2.Zero;
        }
        else target = Vector2.Zero;
        return target;
    }
    public void updatePositon()
    {
        _previousPosition6.set(_previousPosition5);
        _previousPosition5.set(_previousPosition4);
        _previousPosition4.set(_previousPosition3);
        _previousPosition3.set(_previousPosition2);
        _previousPosition2.set(_previousPosition);
        _previousPosition.set(pos());
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
                Deck enemyDeck = Current.latestDeck();
                /*// By popular demand, remove basic lands from the reward pool.
                CardPool deckNoBasicLands = enemyDeck.getMain().getFilteredPool(Predicates.compose(Predicates.not(CardRulesPredicates.Presets.IS_BASIC_LAND), PaperCard.FN_GET_RULES));*/

                for (RewardData rdata : data.rewards) {
                    ret.addAll(rdata.generate(false,  enemyDeck == null ? null : enemyDeck.getMain().toFlatList(),true ));
                }
            }
            if(rewards != null) { //Collect additional rewards.
                for(RewardData rdata:rewards) {
                    //Do not filter in case we want to FORCE basic lands. If it ever becomes a problem just repeat the same as above.

                    ret.addAll(rdata.generate(false,(Current.latestDeck() != null ? Current.latestDeck().getMain().toFlatList() : null), true));
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

    public float speed() {
        return data.speed;
    }

    public float getLifetime() {
        //default and minimum value for time to remain on overworld map
        Float lifetime = 20f;
        return Math.max(data.lifetime, lifetime);
    }


    public class MovementBehavior {

        //temporary placeholders for overworld behavior integration
        public boolean wander = false;
        public boolean flee = false;
        public boolean stop = false;
        //end temporary

        float duration = 0.0f;
        float x = 0.0f;
        float y = 0.0f;

        int destination = 0;

        public float getX(){
            return x;
        }
        public float getY(){
            return y;
        }
        public float getDuration(){
            return duration;
        }
        public int getDestination(){
            return destination;
        }

        public void setX(float newVal){
            x = newVal;
        }
        public void setY(float newVal){
            y = newVal;
        }

        public Vector2 pos() {
            return new Vector2(getX(), getY());
        }
    }
}


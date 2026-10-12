package forge.adventure.character;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.*;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.StaticData;
import forge.adventure.data.DialogData;
import forge.adventure.data.EffectData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.RewardData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.MapDialog;
import forge.adventure.util.Reward;
import forge.adventure.util.pathfinding.MovementBehavior;
import forge.adventure.util.pathfinding.NavigationVertex;
import forge.adventure.util.pathfinding.ProgressableGraphPath;
import forge.card.CardRarity;
import forge.card.CardRulesPredicates;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.Aggregates;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * EnemySprite
 * Character sprite that represents an Enemy
 */
public class EnemySprite extends CharacterSprite implements Steerable<Vector2> {

    private static final SteeringAcceleration<Vector2> steerOutput =
            new SteeringAcceleration<Vector2>(new Vector2());

    Vector2 position;
    float orientation;
    Vector2 linearVelocity = new Vector2(1, 0);
    float angularVelocity;
    float maxSpeed;
    boolean independentFacing;
    SteeringBehavior<Vector2> behavior;
    boolean tagged;

    EnemyData data;
    public MapDialog dialog; //Dialog to show on contact. Overrides standard battle (can be started as an action)
    public MapDialog defeatDialog; //Dialog to show on defeat. Overrides standard death (can be removed as an action)
    public EffectData effect; //Battle effect for this enemy. Similar to a player's blessing.
    public String nameOverride = ""; //Override name of this enemy in battles.
    public String bossInsult = ""; //Override the generated insult text when you are defeated.
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
    public float threatRange = 0.0f; //If range < threatRange, begin pursuit
    public float pursueRange = 0.0f; //If range > pursueRange, abandon pursuit
    public float fleeRange = 0.0f; //If range < fleeRange, attempt to move away to fleeRange
    public float speedModifier = 0.0f; // Increase or decrease default speed
    public boolean aggro = false;
    public boolean ignoreDungeonEffect = false;
    public String questStageID;
    private ProgressableGraphPath<NavigationVertex> navPath;
    public Vector2 fleeTarget;
    private final Vector2 spriteToPlayerVec = new Vector2();
    private final Vector2 candidateToPlayerVec = new Vector2();
    private final Vector2 targetVec = new Vector2();
    private final Vector2 diffVec = new Vector2();
    private final Array<Reward> rewardCollectionPool = new Array<>(16);
    private static final HashSet<Object> uniqueRulesCollectorSet = new HashSet<>();
    private static final ArrayList<PaperCard> flatCardsFilterList = new ArrayList<>(256);
    private ArrayList<PaperCard> uncommonCards = new ArrayList<>();
    private ArrayList<PaperCard> commonCards = new ArrayList<>();
    private ArrayList<PaperCard> rareCards = new ArrayList<>();
    public EnemySprite(EnemyData enemyData) {
        this(0,enemyData);
    }

    public EnemySprite(int id, EnemyData enemyData) {
        super(id,enemyData.sprite);
        data = enemyData;
        float scale = data.scale;
        if (scale < 0)
            scale = 1f;
        setWidth(getWidth() * scale);
        setHeight(getHeight() * scale);
        updateBoundingRect();
        initializeBaseMovementBehavior();
    }

    public void parseWaypoints(String waypoints){
        String[] wp = waypoints.replaceAll("\\s", "").split(",");
        for (String s : wp) {
            movementBehaviors.addLast(new MovementBehavior());
            if (!movementBehaviors.isEmpty()) {
                if (s.startsWith("wait")) {
                    movementBehaviors.peekLast().duration = Float.parseFloat(s.substring(4));
                } else {
                    movementBehaviors.peekLast().destination = s;
                }
            }
        }
    }

    @Override
    void updateBoundingRect() { //We want enemies to take the full tile.
        float scale = data == null ? 1f : data.scale;
        if (scale < 0)
            scale = 1f;
        boundingRect.set(getX(), getY(), getWidth(), getHeight());
        unfreezeRange = 30f * scale;
    }

    public void moveTo(Actor other, float delta) {
        diffVec.set(other.getX(), other.getY()).sub(pos());
        diffVec.setLength(data.speed * delta);
        moveBy(diffVec.x, diffVec.y, delta);
    }

    public void initializeBaseMovementBehavior() {
        Location<Vector2> seekTarget = new Location<Vector2>() {
            @Override
            public Vector2 getPosition() {
                return navPath.nodes.get(0).pos;
            }

            @Override
            public float getOrientation() {
                return 0;
            }

            @Override
            public void setOrientation(float orientation) {

            }

            @Override
            public float vectorToAngle(Vector2 vector) {
                return 0;
            }

            @Override
            public Vector2 angleToVector(Vector2 outVector, float angle) {
                return null;
            }

            @Override
            public Location<Vector2> newLocation() {
                return null;
            }
        };
        Seek<Vector2> seek = new Seek<>(this);
        seek.setTarget(seekTarget);

        Array<Vector2> wp = new Array<>();
        if (navPath != null && navPath.nodes != null) {
            for (NavigationVertex v : navPath.nodes)
                wp.add(v.pos);
        }
        LinePath<Vector2> linePath = null;
        FollowPath<Vector2, LinePath.LinePathParam> followWaypoints = null;
        if (wp.size == 1) {
            wp.insert(0, pos());
        }
        if (wp.size >= 2) {
            linePath = new LinePath<Vector2>(wp, false);
            followWaypoints = new FollowPath<>(this, linePath);
            followWaypoints.setPathOffset(0.5f);
        }

        Arrive<Vector2> moveDirectlyToDestination = new Arrive<>(this, new Location<Vector2>() {
            @Override
            public Vector2 getPosition() {
                if (navPath == null || navPath.nodes.size == 0)
                    return pos();
                return navPath.get(0).pos;
            }

            @Override
            public float getOrientation() {
                return 0;
            }

            @Override
            public void setOrientation(float orientation) {

            }

            @Override
            public float vectorToAngle(Vector2 vector) {
                return 0;
            }

            @Override
            public Vector2 angleToVector(Vector2 outVector, float angle) {
                return null;
            }

            @Override
            public Location<Vector2> newLocation() {
                return null;
            }
        })
                .setTimeToTarget(0.01f)
                .setArrivalTolerance(0f)
                .setDecelerationRadius(10);

        if (followWaypoints != null)
            setBehavior(followWaypoints);
        else
            setBehavior(moveDirectlyToDestination);
    }

    public void setBehavior(SteeringBehavior<Vector2> behavior) {
        this.behavior = behavior;
    }

    public SteeringBehavior<Vector2> getBehavior() {
        return behavior;
    }

    public void update(float delta) {
        if(behavior != null) {
            behavior.calculateSteering(steerOutput);
            while (steerOutput.isZero() && navPath != null && navPath.getCount() > 1) {
                navPath.remove(0);
                behavior.calculateSteering(steerOutput);
            }
            applySteering(delta);
        }
    }

    private void applySteering(float delta) {
        if(!steerOutput.linear.isZero()) {
            Vector2 force = steerOutput.linear.scl(delta);
            force.setLength(Math.min(speed() * delta, force.len()));
            moveBy(force.x, force.y);
        }
    }

    @Override
    public float vectorToAngle (Vector2 vector) {
        return (float)Math.atan2(-vector.x, vector.y);
    }

    @Override
    public Vector2 angleToVector (Vector2 outVector, float angle) {
        outVector.x = -(float)Math.sin(angle);
        outVector.y = (float)Math.cos(angle);
        return outVector;
    }

    @Override
    public Vector2 getLinearVelocity() {
        return linearVelocity;
    }

    @Override
    public float getAngularVelocity() {
        return angularVelocity;
    }
    @Override
    public float getBoundingRadius() {
        return getWidth()/2;
    }

    @Override
    public boolean isTagged() {
        return tagged;
    }

    @Override
    public Vector2 getPosition() {
        return pos();
    }

    @Override
    public float getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(float value) {
        orientation = value;
    }

    @Override
    public Location<Vector2> newLocation() {
        return null;
    }

    @Override
    public void setTagged(boolean value) {
        tagged = value;
    }

    public void freezeMovement(){
        _freeze = true;
        setPosition(_previousPosition6.x, _previousPosition6.y);
        // This will move the enemy back a few frames of movement.
        // Combined with player doing the same, should no longer be colliding to immediately re-enter battle if mob still present
    }

    public Vector2 getTargetVector(PlayerSprite player, ArrayList<NavigationVertex> sortedGraphNodes, float delta) {
        //todo - this can be integrated into overworld movement as well, giving flee behaviors or moving to generated waypoints
        Vector2 target = pos();

        spriteToPlayerVec.set(player.pos()).sub(target);

        if (_freeze){
            //Mob has defeated player in battle, hold still until player has a chance to move away.
            //Without this moving enemies can immediately restart battle.
            float distance = spriteToPlayerVec.len();
            if (distance < unfreezeRange) {
                timer += delta;
                return Vector2.Zero;
            }
            else{
                _freeze = false; // resume normal behavior
            }
        }

        NavigationVertex targetPoint = null;
        if (threatRange > 0 || fleeRange > 0){
            float spriteToPlayerLenSq = spriteToPlayerVec.len2();

            if (spriteToPlayerVec.len() <= threatRange || (aggro && spriteToPlayerVec.len() <= pursueRange))
            {
                if (sortedGraphNodes != null) {
                    int nodeCount = sortedGraphNodes.size();
                    for (int i = 0; i < nodeCount; i++) {
                        NavigationVertex candidate = sortedGraphNodes.get(i);
                        if (candidate == null) continue;

                        candidateToPlayerVec.set(candidate.pos).sub(player.pos());
                        if (candidateToPlayerVec.len2() < spriteToPlayerLenSq) {
                            targetPoint = candidate;
                            break;
                        }
                    }
                }
                aggro = true;
                if (targetPoint != null) {
                    return targetPoint.pos;
                }
                targetVec.set(player.pos());
                return targetVec;
            }
            if (spriteToPlayerVec.len() <= fleeRange)
            {
                //todo: replace with inverse A* variant, seeking max total distance from player in X generations
                // of movement, valuing each node by distance from player divided by closest distance(s) in path
                // in order to make close passes to escape less appealing than maintaining moderate distance
                float fleeDistance = fleeRange - spriteToPlayerVec.len();
                targetVec.set(pos()).sub(player.pos()).setLength(fleeDistance).add(pos());
                return targetVec;
            }
            if (aggro && spriteToPlayerVec.len() > pursueRange) {
                aggro = false;
                if (navPath != null)
                    navPath.clear();
                initializeBaseMovementBehavior();
            }
        }

        if (movementBehaviors.peek() != null){
            MovementBehavior peek = movementBehaviors.peek();
            //TODO - This first block needs to be redone, doesn't work as intended and can also possibly skip behaviors in rare situations
//            if (peek.getDuration() == 0 && target.equals(_previousPosition6) && timer >= _movementTimeout)
//            {
//                //stationary in an untimed behavior, move on to next behavior attempt to get unstuck
//                if (movementBehaviors.size() > 1) {
//                    MovementBehavior current =  movementBehaviors.pop();
//                    current.currentTargetVector = null;
//                    movementBehaviors.addLast(current);
//                }
//            }
            //else
            if (peek.getDuration() == 0 && peek.getNextTargetVector(objectId, pos()).dst(pos()) < 2){
                // this is a location based behavior that has been completed. Move on to the next behavior
                MovementBehavior current = movementBehaviors.pop();
                current.currentTargetVector = null;
                movementBehaviors.addLast(current);
            }
            else if (peek.getDuration() > 0)
            {
                if (timer >= peek.getDuration() + delta)
                {
                    // this is a timed behavior that has been completed. Move to the next behavior and restart the timer
                    MovementBehavior current = movementBehaviors.pop();
                    current.currentTargetVector = null;
                    movementBehaviors.addLast(current);
                }
                else{
                    timer += delta;
                    targetVec.set(pos());
                    return targetVec;
                }
            }
            if (peek.getNextTargetVector(objectId, pos()).dst(pos()) > 0.3) {
                targetVec.set(peek.getNextTargetVector(objectId, pos()));
                target = targetVec;
            }
            else {
                targetVec.set(pos());
                target = targetVec;
            }
        }
        else {
            targetVec.set(pos());
            target = targetVec;
        }
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

    public void overrideDeck(String deckPath) {
        data.deck = new String[1];
        data.deck[0] = deckPath;
    }

    @Override
    public String getName() {
        if (nameOverride == null || nameOverride.isEmpty())
            return data.getName();
        return nameOverride;
    }
    public String getBossInsult(){
        return data.bossInsult;
    }
    public String getBossIntro(){
        return data.bossIntro;
    }
    public Array<Reward> getRewards() {
        rewardCollectionPool.clear();
        // Collect custom rewards for chaos battles

        if (data.copyPlayerDeck && Current.latestDeck() != null) {
            flatCardsFilterList.clear();

            CardPool mainDeckPool = Current.latestDeck().getMain();
            List<PaperCard> sourceFlatList = mainDeckPool.toFlatList();
            int cardCount = sourceFlatList.size();

            for (int i = 0; i < cardCount; i++) {
                PaperCard paperCard = sourceFlatList.get(i);
                if (paperCard != null && !paperCard.isVeryBasicLand()) {
                    flatCardsFilterList.add(paperCard);
                }
            }

            uniqueRulesCollectorSet.clear();
            for (int i = 0; i < flatCardsFilterList.size(); i++) {
                uniqueRulesCollectorSet.add(flatCardsFilterList.get(i).getRules());
                if (uniqueRulesCollectorSet.size() >= 4) { // max limit
                    break;
                }
            }
            int uniqueRules = uniqueRulesCollectorSet.size();

            if (uniqueRules < 4 || flatCardsFilterList.size() < 10) {
                rewardCollectionPool.add(new Reward(199));
                rewardCollectionPool.add(new Reward(Reward.Type.Shards, 9));

                PaperCard cheese = StaticData.instance().fetchCard("The Cheese Stands Alone");
                if (cheese != null) {
                    rewardCollectionPool.add(new Reward(cheese));
                }
                return rewardCollectionPool;
            }

            if (AdventurePlayer.current().isFantasyMode()) {
                uncommonCards.clear();
                commonCards.clear();
                rareCards.clear();

                for (int i = 0; i < flatCardsFilterList.size(); i++) {
                    PaperCard card = flatCardsFilterList.get(i);
                    if (card.getRarity() == CardRarity.Uncommon || card.getRarity() == CardRarity.Special) {
                        uncommonCards.add(card);
                    } else if (card.getRarity() == CardRarity.Common) {
                        commonCards.add(card);
                    } else if (card.getRarity() == CardRarity.Rare || card.getRarity() == CardRarity.MythicRare) {
                        rareCards.add(card);
                    }
                }
                // random uncommons from deck
                if (!uncommonCards.isEmpty()) {
                    rewardCollectionPool.add(new Reward(Aggregates.random(uncommonCards)));
                    rewardCollectionPool.add(new Reward(Aggregates.random(uncommonCards)));
                }
                // random commons from deck
                if (!commonCards.isEmpty()) {
                    rewardCollectionPool.add(new Reward(Aggregates.random(commonCards)));
                    rewardCollectionPool.add(new Reward(Aggregates.random(commonCards)));
                    rewardCollectionPool.add(new Reward(Aggregates.random(commonCards)));
                }
                // random rare from deck
                if (!rareCards.isEmpty()) {
                    rewardCollectionPool.add(new Reward(Aggregates.random(rareCards)));
                    rewardCollectionPool.add(new Reward(Aggregates.random(rareCards)));
                }

                int val = ((MyRandom.getRandom().nextInt(2) + 1) * 100) + (MyRandom.getRandom().nextInt(101));
                rewardCollectionPool.add(new Reward(val));
                rewardCollectionPool.add(new Reward(Reward.Type.Life, 1));

                return rewardCollectionPool;
            }
        }

        if (data.rewards != null) { // Collect standard rewards.
            Deck enemyDeck = Current.latestDeck();
            CardPool deckNoRestrictedEditions = enemyDeck.getMain().getFilteredPool(PaperCardPredicates.onlyPrintedInEditions(Config.instance().getConfigData().restrictedEditions).negate());
            CardPool deckNoBasicLands = deckNoRestrictedEditions.getFilteredPool(PaperCardPredicates.fromRules(CardRulesPredicates.NOT_BASIC_LAND));

            for (RewardData rdata : data.rewards) {
                rewardCollectionPool.addAll(rdata.generate(false, enemyDeck == null ? null : deckNoBasicLands.toFlatList(), true));
            }
        }
        if (this.rewards != null) { // Collect additional rewards.
            for (RewardData rdata : this.rewards) {
                rewardCollectionPool.addAll(rdata.generate(false, (Current.latestDeck() != null ? Current.latestDeck().getMain().toFlatList() : null), true));
            }
        }
        return rewardCollectionPool;
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
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'B': {
                    batch.setColor(Color.PURPLE);
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'G': {
                    batch.setColor(Color.GREEN);
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'R': {
                    batch.setColor(Color.RED);
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'U': {
                    batch.setColor(Color.BLUE);
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
                case 'W': {
                    batch.setColor(Color.WHITE);
                    batch.draw(Forge.getAssets().getWhiteTexture(), DX, DY, 2, 2);
                    DY += 2; break;
                }
            }
        }
        batch.setColor(Color.WHITE);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (inactive || hidden)
            return;
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
            batch.draw(TR, getX(), getY() + 16, 16*getScaleX(), 16*getScaleY());
        }
    }

    public float speed() {
        return Float.max(data.speed + speedModifier, 0);
    }

    public float getLifetime() {
        //default and minimum value for time to remain on overworld map
        float lifetime = 20f;
        return Math.max(data.lifetime, lifetime);
    }

    //Pathfinding integration below this line

    public void setNavPath(ProgressableGraphPath<NavigationVertex> navPath) {
        this.navPath = navPath;
    }

    public ProgressableGraphPath<NavigationVertex> getNavPath() {
        return navPath;
    }

    @Override
    public float getZeroLinearSpeedThreshold() {
        return 0;
    }

    @Override
    public void setZeroLinearSpeedThreshold(float value) {

    }

    @Override
    public float getMaxLinearSpeed() {
        return 500;
    }

    @Override
    public void setMaxLinearSpeed(float maxLinearSpeed) {

    }

    @Override
    public float getMaxLinearAcceleration() {
        return 5000;
    }

    @Override
    public void setMaxLinearAcceleration(float maxLinearAcceleration) {

    }

    @Override
    public float getMaxAngularSpeed() {
        return 0;
    }

    @Override
    public void setMaxAngularSpeed(float maxAngularSpeed) {

    }

    @Override
    public float getMaxAngularAcceleration() {
        return 0;
    }

    @Override
    public void setMaxAngularAcceleration(float maxAngularAcceleration) {

    }

    public void steer(Vector2 currentVector) {

    }

    public boolean isFrozen() {
        return _freeze;
    }
}


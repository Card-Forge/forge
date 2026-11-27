package forge.adventure.character;

import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.RewardData;
import forge.adventure.data.ShopData;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.RewardScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Reward;
import forge.adventure.world.WorldSave;

import java.util.HashSet;
import java.util.Set;


/**
 * Map actor that will open the Shop on collision
 */
public class ShopActor extends MapActor {
    private final MapStage stage;
    private ShopData shopData;
    //Changes of the map this shop was loaded for. Used to look up the shop seed when the stock is generated.
    private final PointOfInterestChanges loadChanges;
    //Generated lazily the first time the shop is opened (or restocked), so that loading a town doesn't have to
    //generate the cards of every shop in it.
    private Array<Reward> rewardData;
    // exclusions quietly thin a themed shop, so the player is told once per shop each session
    private static final Set<String> warnedShops = new HashSet<>();
    private boolean namedCardsChanged;

    public ShopActor(MapStage stage, int id, ShopData data, PointOfInterestChanges loadChanges) {
        super(id);
        this.stage = stage;
        this.shopData = data;
        this.loadChanges = loadChanges;
    }

    public float getPriceModifier() {
        PointOfInterestChanges changes = stage.getChanges();
        float townPricemodifier = changes == null ? 1f : changes.getTownPriceModifier();
        float shopPriceModifier = changes == null ? 1f : changes.getShopPriceModifier(objectId);
        return shopPriceModifier * townPricemodifier;
    }

    public MapStage getMapStage() {
        return stage;
    }

    @Override
    public void onPlayerCollide() {
        stage.getPlayerSprite().stop();
        RewardScene scene = RewardScene.instance();
        scene.loadRewards(getRewardData(), RewardScene.Type.Shop, this);
        if (Forge.switchScene(scene))
            warnOfExcludedCards(scene);
    }

    /** Tells the player once a session per shop when their exclusions changed or left out some of its stock. */
    public void warnOfExcludedCards(RewardScene scene) {
        if (warnedShops.contains(shopData.name) || !namedCardsChanged && !shopData.losesExcludedCards())
            return;
        warnedShops.add(shopData.name);
        scene.showDialog(scene.createGenericDialog(Forge.getLocalizer().getMessage("lblExcludedSets"),
                Forge.getLocalizer().getMessage("lblShopHasExcludedCards"), Forge.getLocalizer().getMessage("lblOK"),
                null, scene::removeDialog, null, false, "", true));
    }


    public boolean isUnlimited() {
        return shopData.unlimited;
    }

    @Override
    public String getName() {
        return shopData.name;
    }

    public String getDescription() {
        return shopData.getDescription();
    }

    public int getRestockPrice() {
        return shopData.restockPrice;
    }

    public boolean canRestock() {
        return getRestockPrice() > 0;
    }

    public ShopData getShopData() {
        return shopData;
    }

    // called right after the stock is generated, which records whether exclusions changed a named card in it
    public void setRewardData(Array<Reward> data) {
        rewardData = data;
        namedCardsChanged = RewardData.takeRewardChangedByExclusions();
    }

    /**
     * Returns the stock of this shop, generating it from the shop seed on first use.
     * Same seed and same generation as before, only the moment it happens has moved from town load to shop entry.
     */
    public Array<Reward> getRewardData() {
        if (rewardData == null) {
            Array<Reward> generated = new Array<>();
            WorldSave.getCurrentSave().getWorld().getRandom().setSeed(loadChanges.getShopSeed(objectId));
            for (RewardData rdata : new Array.ArrayIterator<>(shopData.rewards)) {
                generated.addAll(rdata.generate(false, false));
            }
            setRewardData(generated);
        }
        return rewardData;
    }
}

package forge.game.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.keyword.KeywordCollection.KeywordCollectionView;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.util.FCollectionView;


public class PlayerView extends GameEntityView {
    public static PlayerView get(Player p) {
        return p == null ? null : p.getView();
    }

    public static TrackableCollection<PlayerView> getCollection(Iterable<Player> players) {
        if (players == null) {
            return null;
        }
        TrackableCollection<PlayerView> collection = new TrackableCollection<PlayerView>();
        for (Player p : players) {
            collection.add(p.getView());
        }
        return collection;
    }

    public PlayerView(int id0) {
        super(id0);

        set(TrackableProperty.Mana, Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1));
    }

    public int getAvatarIndex() {
        return get(TrackableProperty.AvatarIndex);
    }
    void updateAvatarIndex(Player p) {
        set(TrackableProperty.AvatarIndex, p.getLobbyPlayer().getAvatarIndex());
    }

    public FCollectionView<PlayerView> getOpponents() {
        return get(TrackableProperty.Opponents);
    }
    void updateOpponents(Player p) {
        set(TrackableProperty.Opponents, PlayerView.getCollection(p.getOpponents()));
    }

    public boolean isOpponentOf(final PlayerView other) {
        return getOpponents().contains(other);
    }

    @Override
    public String toString() {
        return getName();
    }

    public int getLife() {
        return get(TrackableProperty.Life);
    }
    void updateLife(Player p) {
        set(TrackableProperty.Life, p.getLife());
    }

    public int getPoisonCounters() {
        return get(TrackableProperty.PoisonCounters);
    }
    void updatePoisonCounters(Player p) {
        set(TrackableProperty.PoisonCounters, p.getPoisonCounters());
    }

    public int getMaxHandSize() {
        return get(TrackableProperty.MaxHandSize);
    }
    void updateMaxHandSize(Player p) {
        set(TrackableProperty.MaxHandSize, p.getMaxHandSize());
    }

    public boolean hasUnlimitedHandSize() {
        return get(TrackableProperty.HasUnlimitedHandSize);
    }
    void updateUnlimitedHandSize(Player p) {
        set(TrackableProperty.HasUnlimitedHandSize, p.isUnlimitedHandSize());
    }

    public int getNumDrawnThisTurn() {
        return get(TrackableProperty.NumDrawnThisTurn);
    }
    void updateNumDrawnThisTurn(Player p) {
        set(TrackableProperty.NumDrawnThisTurn, p.getNumDrawnThisTurn());
    }

    public KeywordCollectionView getKeywords() {
        return get(TrackableProperty.Keywords);
    }
    public boolean hasKeyword(String keyword) {
        return getKeywords().contains(keyword);
    }
    void updateKeywords(Player p) {
        set(TrackableProperty.Keywords, p.getKeywords());
    }

    public CardView getCommander() {
        return get(TrackableProperty.Commander);
    }
    void updateCommander(Player p) {
        set(TrackableProperty.Commander, PlayerView.get(p.getCommander()));
    }

    public Map<Integer, Integer> getCommanderDamage() {
        return get(TrackableProperty.CommanderDamage);
    }
    void updateCommanderDamage(Player p) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (Entry<Card, Integer> entry : p.getCommanderDamage()) {
            map.put(entry.getKey().getId(), entry.getValue());
        }
        set(TrackableProperty.CommanderDamage, map);
    }

    public String getCommanderInfo() {
        throw new NotImplementedException("Not implemented");
    }

    public PlayerView getMindSlaveMaster() {
        return get(TrackableProperty.Commander);
    }
    void updateMindSlaveMaster(Player p) {
        set(TrackableProperty.Commander, PlayerView.get(p.getMindSlaveMaster()));
    }

    public FCollectionView<CardView> getAnte() {
        return get(TrackableProperty.Ante);
    }
    public int getAnteSize() {
        return getZoneSize(TrackableProperty.Ante);
    }

    public FCollectionView<CardView> getBattlefield() {
        return get(TrackableProperty.Battlefield);
    }
    public int getBattlefieldSize() {
        return getZoneSize(TrackableProperty.Battlefield);
    }

    public FCollectionView<CardView> getCommand() {
        return get(TrackableProperty.Command);
    }
    public int getCommandSize() {
        return getZoneSize(TrackableProperty.Command);
    }

    public FCollectionView<CardView> getExile() {
        return get(TrackableProperty.Exile);
    }
    public int getExileSize() {
        return getZoneSize(TrackableProperty.Exile);
    }

    public FCollectionView<CardView> getFlashback() {
        return get(TrackableProperty.Flashback);
    }
    public int getFlashbackSize() {
        return getZoneSize(TrackableProperty.Flashback);
    }

    public FCollectionView<CardView> getGraveyard() {
        return get(TrackableProperty.Graveyard);
    }
    public int getGraveyardSize() {
        return getZoneSize(TrackableProperty.Graveyard);
    }

    public FCollectionView<CardView> getHand() {
        return get(TrackableProperty.Hand);
    }
    public int getHandSize() {
        return getZoneSize(TrackableProperty.Hand);
    }

    public FCollectionView<CardView> getLibrary() {
        return get(TrackableProperty.Library);
    }
    public int getLibrarySize() {
        return getZoneSize(TrackableProperty.Library);
    }

    public FCollectionView<CardView> getCards(final ZoneType zone) {
        TrackableProperty prop = getZoneProp(zone);
        if (prop != null) {
            return get(prop);
        }
        return null;
    }
    private int getZoneSize(TrackableProperty zoneProp) {
        TrackableCollection<CardView> cards = get(zoneProp);
        return cards == null ? 0 : cards.size();
    }
    private TrackableProperty getZoneProp(final ZoneType zone) {
        switch (zone) {
        case Ante:
            return TrackableProperty.Ante;
        case Battlefield:
            return TrackableProperty.Battlefield;
        case Command:
            return TrackableProperty.Command;
        case Exile:
            return TrackableProperty.Exile;
        case Graveyard:
            return TrackableProperty.Graveyard;
        case Hand:
            return TrackableProperty.Hand;
        case Library:
            return TrackableProperty.Library;
        default:
            return null; //other zones not represented
        }
    }
    void updateZone(PlayerZone zone) {
        TrackableProperty prop = getZoneProp(zone.getZoneType());
        if (prop == null) { return; }
        set(prop, CardView.getCollection(zone.getCards()));
    }

    public int getMana(final byte color) {
        Integer count = getMana().get(color);
        return count != null ? count.intValue() : 0;
    }
    private Map<Byte, Integer> getMana() {
        return get(TrackableProperty.Mana);
    }
    void updateMana(Player p) {
        Map<Byte, Integer> mana = new HashMap<Byte, Integer>();
        for (byte b : MagicColor.WUBRGC) {
            mana.put(b, p.getManaPool().getAmountOfColor(b));
        }
        set(TrackableProperty.Mana, mana);
    }

    //TODO: Find better way to do this
    public LobbyPlayer getLobbyPlayer() {
        return Player.get(this).getLobbyPlayer();
    }
}
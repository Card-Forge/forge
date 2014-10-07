package forge.game.player;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.GameEntityView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardView;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;


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

    public LobbyPlayer getLobbyPlayer() {
        return get(TrackableProperty.LobbyPlayer);
    }
    void updateLobbyPlayer(Player p) {
        set(TrackableProperty.LobbyPlayer, p.getLobbyPlayer());
    }

    public Iterable<PlayerView> getOpponents() {
        return get(TrackableProperty.Opponents);
    }
    private TrackableCollection<PlayerView> getOpponentCollection() {
        return get(TrackableProperty.Opponents);
    }

    public boolean isOpponentOf(final PlayerView other) {
        return getOpponentCollection().contains(other);
    }

    public String getName() {
        return getLobbyPlayer().getName();
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

    public Iterable<String> getKeywords() {
        return get(TrackableProperty.Keywords);
    }
    void updateKeywords(Player p) {
        set(TrackableProperty.Keywords, p.getKeywords());
    }

    public String getCommanderInfo() {
        return get(TrackableProperty.CommanderInfo);
    }
    void updateCommanderInfo(Player p) {
        set(TrackableProperty.CommanderInfo, CardFactoryUtil.getCommanderInfo(p).trim().replace("\r\n", "; "));
    }

    public Iterable<CardView> getAnte() {
        return get(TrackableProperty.Ante);
    }

    public Iterable<CardView> getBattlefield() {
        return get(TrackableProperty.Battlefield);
    }

    public Iterable<CardView> getCommand() {
        return get(TrackableProperty.Command);
    }

    public Iterable<CardView> getExile() {
        return get(TrackableProperty.Exile);
    }

    public Iterable<CardView> getFlashback() {
        return get(TrackableProperty.Flashback);
    }

    public Iterable<CardView> getGraveyard() {
        return get(TrackableProperty.Graveyard);
    }

    public Iterable<CardView> getHand() {
        return get(TrackableProperty.Hand);
    }

    public Iterable<CardView> getLibrary() {
        return get(TrackableProperty.Library);
    }

    public Iterable<CardView> getCards(final ZoneType zone) {
        TrackableProperty prop = getZoneProp(zone);
        if (prop != null) {
            return get(prop);
        }
        return null;
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

    private Map<Byte, Integer> getMana() {
        return get(TrackableProperty.Mana);
    }
    void updateMana(Player p) {
        boolean changed = false;
        Map<Byte, Integer> mana = getMana();
        for (byte b : MagicColor.WUBRGC) {
            int value = p.getManaPool().getAmountOfColor(b);
            if (mana.put(b, value) != value) {
                changed = true;
            }
        }
        if (changed) {
            flagAsChanged(TrackableProperty.Mana);
        }
    }

    public int getMana(final byte color) {
        Integer count = getMana().get(color);
        return count != null ? count.intValue() : 0;
    }
}
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
import forge.trackable.TrackableProperty.PlayerProp;


public class PlayerView extends GameEntityView<PlayerProp> {
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
        super(id0, PlayerProp.class);

        set(PlayerProp.Mana, Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1));
    }

    public LobbyPlayer getLobbyPlayer() {
        return get(PlayerProp.LobbyPlayer);
    }
    void updateLobbyPlayer(Player p) {
        set(PlayerProp.LobbyPlayer, p.getLobbyPlayer());
    }

    public Iterable<PlayerView> getOpponents() {
        return get(PlayerProp.Opponents);
    }
    private TrackableCollection<PlayerView> getOpponentCollection() {
        return get(PlayerProp.Opponents);
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
        return get(PlayerProp.Life);
    }
    void updateLife(Player p) {
        set(PlayerProp.Life, p.getLife());
    }

    public int getPoisonCounters() {
        return get(PlayerProp.PoisonCounters);
    }
    void updatePoisonCounters(Player p) {
        set(PlayerProp.PoisonCounters, p.getPoisonCounters());
    }

    public int getMaxHandSize() {
        return get(PlayerProp.MaxHandSize);
    }
    void updateMaxHandSize(Player p) {
        set(PlayerProp.MaxHandSize, p.getMaxHandSize());
    }

    public boolean hasUnlimitedHandSize() {
        return get(PlayerProp.HasUnlimitedHandSize);
    }
    void updateUnlimitedHandSize(Player p) {
        set(PlayerProp.HasUnlimitedHandSize, p.isUnlimitedHandSize());
    }

    public int getNumDrawnThisTurn() {
        return get(PlayerProp.NumDrawnThisTurn);
    }
    void updateNumDrawnThisTurn(Player p) {
        set(PlayerProp.NumDrawnThisTurn, p.getNumDrawnThisTurn());
    }

    public Iterable<String> getKeywords() {
        return get(PlayerProp.Keywords);
    }
    void updateKeywords(Player p) {
        set(PlayerProp.Keywords, p.getKeywords());
    }

    public String getCommanderInfo() {
        return get(PlayerProp.CommanderInfo);
    }
    void updateCommanderInfo(Player p) {
        set(PlayerProp.CommanderInfo, CardFactoryUtil.getCommanderInfo(p).trim().replace("\r\n", "; "));
    }

    public Iterable<CardView> getAnte() {
        return get(PlayerProp.Ante);
    }

    public Iterable<CardView> getBattlefield() {
        return get(PlayerProp.Battlefield);
    }

    public Iterable<CardView> getCommand() {
        return get(PlayerProp.Command);
    }

    public Iterable<CardView> getExile() {
        return get(PlayerProp.Exile);
    }

    public Iterable<CardView> getFlashback() {
        return get(PlayerProp.Flashback);
    }

    public Iterable<CardView> getGraveyard() {
        return get(PlayerProp.Graveyard);
    }

    public Iterable<CardView> getHand() {
        return get(PlayerProp.Hand);
    }

    public Iterable<CardView> getLibrary() {
        return get(PlayerProp.Library);
    }

    public Iterable<CardView> getCards(final ZoneType zone) {
        PlayerProp prop = getZoneProp(zone);
        if (prop != null) {
            return get(prop);
        }
        return null;
    }
    private PlayerProp getZoneProp(final ZoneType zone) {
        switch (zone) {
        case Ante:
            return PlayerProp.Ante;
        case Battlefield:
            return PlayerProp.Battlefield;
        case Command:
            return PlayerProp.Command;
        case Exile:
            return PlayerProp.Exile;
        case Graveyard:
            return PlayerProp.Graveyard;
        case Hand:
            return PlayerProp.Hand;
        case Library:
            return PlayerProp.Library;
        default:
            return null; //other zones not represented
        }
    }
    void updateZone(PlayerZone zone) {
        PlayerProp prop = getZoneProp(zone.getZoneType());
        if (prop == null) { return; }
        set(prop, CardView.getCollection(zone.getCards()));
    }

    private Map<Byte, Integer> getMana() {
        return get(PlayerProp.Mana);
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
            flagAsChanged(PlayerProp.Mana);
        }
    }

    public int getMana(final byte color) {
        Integer count = getMana().get(color);
        return count != null ? count.intValue() : 0;
    }

    @Override
    protected PlayerProp preventNextDamageProp() {
        return PlayerProp.PreventNextDamage;
    }

    @Override
    protected PlayerProp enchantedByProp() {
        return PlayerProp.EnchantedBy;
    }
}
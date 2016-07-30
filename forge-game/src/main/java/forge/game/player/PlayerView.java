package forge.game.player;

import java.util.*;
import java.util.Map.Entry;

import forge.card.CardType;
import forge.card.mana.ManaAtom;
import forge.game.card.CounterType;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.Lang;

public class PlayerView extends GameEntityView {
    private static final long serialVersionUID = 7005892740909549086L;

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

    public PlayerView(final int id0, final Tracker tracker) {
        super(id0, tracker);

        set(TrackableProperty.Mana, Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1));
    }

    public boolean isAI()   {
        return get(TrackableProperty.IsAI);
    }
    void updateIsAI(Player p) {
        set(TrackableProperty.IsAI, p.getController().isAI());
    }

    public String getLobbyPlayerName() {
        return get(TrackableProperty.LobbyPlayerName);
    }
    void updateLobbyPlayerName(Player p) {
        set(TrackableProperty.LobbyPlayerName, p.getLobbyPlayer().getName());
    }
    public boolean isLobbyPlayer(LobbyPlayer p) {
        return getLobbyPlayerName().equals(p.getName());
    }

    public int getAvatarIndex() {
        return get(TrackableProperty.AvatarIndex);
    }
    void updateAvatarIndex(Player p) {
        set(TrackableProperty.AvatarIndex, p.getLobbyPlayer().getAvatarIndex());
    }

    public String getAvatarCardImageKey() {
        return get(TrackableProperty.AvatarCardImageKey);
    }
    void updateAvatarCardImageKey(Player p) {
        set(TrackableProperty.AvatarCardImageKey, p.getLobbyPlayer().getAvatarCardImageKey());
    }

    public FCollectionView<PlayerView> getOpponents() {
        return Objects.firstNonNull(this.<FCollectionView<PlayerView>>get(TrackableProperty.Opponents), new FCollection<PlayerView>());
    }
    void updateOpponents(Player p) {
        set(TrackableProperty.Opponents, PlayerView.getCollection(p.getOpponents()));
    }

    public boolean isOpponentOf(final PlayerView other) {
        FCollectionView<PlayerView> opponents = getOpponents();
        return opponents != null && opponents.contains(other);
    }

    public final String getCommanderInfo() {
        final CardView commander = getCommander();
        if (commander == null) {
            return StringUtils.EMPTY;
        }

        final StringBuilder sb = new StringBuilder();
        Iterable<PlayerView> opponents = getOpponents();
        if (opponents == null) {
            opponents = Collections.emptyList();
        }
        for (final PlayerView p : Iterables.concat(Collections.singleton(this), opponents)) {
            final int damage = p.getCommanderDamage(this.getCommander());
            if (damage > 0) {
                final String text = String.format("Commander damage to %s:", p);
                sb.append(String.format(text + " %d\r\n", damage));
            }
        }
        return sb.toString();
    }

    public final List<String> getPlayerCommanderInfo() {
        final CardView commander = getCommander();
        if (commander == null) {
            return Collections.emptyList();
        }

        final FCollectionView<PlayerView> opponents = getOpponents();
        final List<String> info = Lists.newArrayListWithExpectedSize(opponents.size());
        info.add(String.format("Commander: %s", commander));
        for (final PlayerView p : Iterables.concat(Collections.singleton(this), opponents)) {
            final int damage = this.getCommanderDamage(p.getCommander());
            if (damage > 0) {
                final String text;
                if (p.equals(this)) {
                    text = "Commander damage from own commander:";
                } else {
                    text = String.format("Commander damage from %s:", p);
                }
                info.add(String.format(text + " %d\r\n", damage));
            }
        }
        return info;
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

    public Map<CounterType, Integer> getCounters() {
        return get(TrackableProperty.Counters);
    }
    public int getCounters(CounterType counterType) {
        final Map<CounterType, Integer> counters = getCounters();
        if (counters != null) {
            Integer count = counters.get(counterType);
            if (count != null) {
                return count;
            }
        }
        return 0;
    }
    void updateCounters(Player p) {
        set(TrackableProperty.Counters, p.getCounters());
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

    public String getMaxHandString() {
        return hasUnlimitedHandSize() ? "unlimited" : String.valueOf(getMaxHandSize());
    }

    public int getNumDrawnThisTurn() {
        return get(TrackableProperty.NumDrawnThisTurn);
    }
    void updateNumDrawnThisTurn(Player p) {
        set(TrackableProperty.NumDrawnThisTurn, p.getNumDrawnThisTurn());
    }

    public ImmutableMultiset<String> getKeywords() {
        return get(TrackableProperty.Keywords);
    }
    public List<String> getDisplayableKeywords() {
        final List<String> allKws;
        final ImmutableMultiset<String> kws = getKeywords();
        synchronized (kws) {
            allKws = Lists.newArrayList(kws.elementSet());
        }
        allKws.remove("CanSeeOpponentsFaceDownCards");
        return allKws;
    }
    public boolean hasKeyword(String keyword) {
        return getKeywords().contains(keyword);
    }
    void updateKeywords(Player p) {
        set(TrackableProperty.Keywords, ImmutableMultiset.copyOf(p.getKeywords()));
    }

    public CardView getCommander() {
        return get(TrackableProperty.Commander);
    }
    void updateCommander(Player p) {
        set(TrackableProperty.Commander, GameEntityView.get(p.getCommander()));
    }

    public int getCommanderDamage(CardView commander) {
        Map<Integer, Integer> map = get(TrackableProperty.CommanderDamage);
        if (map == null) { return 0; }
        Integer damage = map.get(commander.getId());
        return damage == null ? 0 : damage.intValue();
    }
    void updateCommanderDamage(Player p) {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (Entry<Card, Integer> entry : p.getCommanderDamage()) {
            map.put(entry.getKey().getId(), entry.getValue());
        }
        set(TrackableProperty.CommanderDamage, map);
    }

    public PlayerView getMindSlaveMaster() {
        return get(TrackableProperty.MindSlaveMaster);
    }
    void updateMindSlaveMaster(Player p) {
        set(TrackableProperty.MindSlaveMaster, PlayerView.get(p.getMindSlaveMaster()));
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

    public int getZoneTypes(TrackableProperty zoneProp) {
        TrackableCollection<CardView> cards = get(zoneProp);
        HashSet<CardType.CoreType> types = new HashSet<>();
        if (cards == null)
            return 0;

        for(CardView c : cards) {
            types.addAll((Collection<? extends CardType.CoreType>) c.getCurrentState().getType().getCoreTypes());
        }

        return types.size();
    }

    private static TrackableProperty getZoneProp(final ZoneType zone) {
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
        case Flashback:
            return TrackableProperty.Flashback;
        default:
            return null; //other zones not represented
        }
    }
    void updateZone(PlayerZone zone) {
        TrackableProperty prop = getZoneProp(zone.getZoneType());
        if (prop == null) { return; }
        set(prop, CardView.getCollection(zone.getCards(false)));

        //update flashback zone when graveyard, library, or exile zones updated
        switch (zone.getZoneType()) {
        case Graveyard:
        case Library:
        case Exile:
            set(TrackableProperty.Flashback, CardView.getCollection(zone.getPlayer().getCardsIn(ZoneType.Flashback)));
            break;
        default:
            break;
        }
    }

    void updateFlashbackForPlayer(Player p) {
        set(TrackableProperty.Flashback, CardView.getCollection(p.getCardsIn(ZoneType.Flashback)));
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
        for (byte b : ManaAtom.MANATYPES) {
            mana.put(b, p.getManaPool().getAmountOfColor(b));
        }
        set(TrackableProperty.Mana, mana);
    }

    private List<String> getDetailsList() {
        final List<String> details = Lists.newArrayListWithCapacity(8);
        details.add(String.format("Life: %d", getLife()));

        Map<CounterType, Integer> counters = getCounters();
        if (counters != null) {
            for (Entry<CounterType, Integer> p : counters.entrySet()) {
                if (p.getValue() > 0) {
                    details.add(String.format("%s counters: %d", p.getKey().getName(), p.getValue()));
                }
            }
        }

        details.add(String.format("Cards in hand: %d/%s", getHandSize(), getMaxHandString()));
        details.add(String.format("Cards drawn this turn: %d", getNumDrawnThisTurn()));
        details.add(String.format("Damage prevention: %d", getPreventNextDamage()));
        final String keywords = Lang.joinHomogenous(getDisplayableKeywords());
        if (!keywords.isEmpty()) {
            details.add(keywords);
        }
        final FCollectionView<CardView> ante = getAnte();
        if (ante != null && !ante.isEmpty()) {
            details.add(String.format("Ante'd: %s", Lang.joinHomogenous(ante)));
        }
        details.addAll(getPlayerCommanderInfo());
        return details;
    }
    public String getDetails() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getName());
        builder.append('\n');
        for (final String detailsPart : getDetailsList()) {
            builder.append(detailsPart);
            builder.append('\n');
        }
        return builder.toString();
    }
    public String getDetailsHtml() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append(getName());
        builder.append("<hr/>");
        for (final String line : getDetailsList()) {
            builder.append(line);
            builder.append("<br/>");
        }
        builder.append("</html>");
        return builder.toString();
    }
}
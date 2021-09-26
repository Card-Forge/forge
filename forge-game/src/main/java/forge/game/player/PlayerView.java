package forge.game.player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class PlayerView extends GameEntityView {
    private static final long serialVersionUID = 7005892740909549086L;

    public static PlayerView get(Player p) {
        return p == null ? null : p.getView();
    }

    public static TrackableCollection<PlayerView> getCollection(Iterable<Player> players) {
        if (players == null) {
            return null;
        }
        TrackableCollection<PlayerView> collection = new TrackableCollection<>();
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

    public int getSleeveIndex() {
        return get(TrackableProperty.SleeveIndex);
    }
    void updateSleeveIndex(Player p) {
        set(TrackableProperty.SleeveIndex, p.getLobbyPlayer().getSleeveIndex());
    }

    public String getCurrentPlaneName() { return get(TrackableProperty.CurrentPlane); }
    void updateCurrentPlaneName( String plane ) {
        set(TrackableProperty.CurrentPlane, plane);
    }

    public FCollectionView<PlayerView> getOpponents() {
        return MoreObjects.firstNonNull(this.<FCollectionView<PlayerView>>get(TrackableProperty.Opponents), new FCollection<>());
    }
    void updateOpponents(Player p) {
        set(TrackableProperty.Opponents, PlayerView.getCollection(p.getOpponents()));
    }

    public boolean isOpponentOf(final PlayerView other) {
        return getOpponents().contains(other);
    }

    public final String getCommanderInfo(CardView v) {
        if (v == null) {
            return StringUtils.EMPTY;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append(Localizer.getInstance().getMessage("lblCommanderCastCard", String.valueOf(getCommanderCast(v))));
        sb.append("\n");

        for (final PlayerView p : Iterables.concat(Collections.singleton(this), getOpponents())) {
            final int damage = p.getCommanderDamage(v);
            if (damage > 0) {
                sb.append(Localizer.getInstance().getMessage("lblCommanderDealNDamageToPlayer", p.toString(), CardTranslation.getTranslatedName(v.getName()), String.valueOf(damage)));
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public final List<String> getPlayerCommanderInfo() {
        final List<CardView> commanders = getCommanders();
        if (commanders == null || commanders.isEmpty()) {
            return Collections.emptyList();
        }

        final FCollectionView<PlayerView> opponents = getOpponents();
        for (PlayerView opponent: opponents) {
            if (opponent.getCommanders() == null) {
                return Collections.emptyList();
            }
        }

        final List<String> info = Lists.newArrayListWithExpectedSize(opponents.size());

        info.add("Commanders:");
        for (final CardView v : commanders) {
            info.add(Localizer.getInstance().getMessage("lblCommanderCastPlayer", CardTranslation.getTranslatedName(v.getName()), String.valueOf(getCommanderCast(v))));
        }

        // own commanders
        for (final CardView v : commanders) {
            final int damage = getCommanderDamage(v);
            if (damage > 0) {
                info.add(Localizer.getInstance().getMessage("lblNCommanderDamageFromOwnCommander", CardTranslation.getTranslatedName(v.getName()), String.valueOf(damage)));
            }
        }

        // opponents commanders
        for (final PlayerView p : opponents) {
            for (final CardView v : p.getCommanders()) {
                final int damage = getCommanderDamage(v);
                if (damage > 0) {
                    info.add(Localizer.getInstance().getMessage("lblNCommanderDamageFromPlayerCommander", p.toString(), CardTranslation.getTranslatedName(v.getName()), String.valueOf(damage)));
                }
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
    public int getCounters(CounterEnumType counterType) {
        return getCounters(CounterType.get(counterType));
    }
    void updateCounters(Player p) {
        set(TrackableProperty.Counters, p.getCounters());
    }

    public boolean getIsExtraTurn() {
        return get(TrackableProperty.IsExtraTurn);
    }

    public void setIsExtraTurn(final boolean val) {
        set(TrackableProperty.IsExtraTurn, val);
    }

    public boolean getHasLost() {
        if (get(TrackableProperty.HasLost) == null)
            return false;
        return get(TrackableProperty.HasLost);
    }

    public void setHasLost(final boolean val) {
        set(TrackableProperty.HasLost, val);
    }

    public int getAvatarLifeDifference() {
        return (int)get(TrackableProperty.AvatarLifeDifference);
    }
    public boolean wasAvatarLifeChanged() {
        if ((int)get(TrackableProperty.AvatarLifeDifference) == 0)
            return false;
        return (int)get(TrackableProperty.AvatarLifeDifference) != 0;
    }
    public void setAvatarLifeDifference(final int val) {
        set(TrackableProperty.AvatarLifeDifference, val);
    }

    public int getExtraTurnCount() {
        return get(TrackableProperty.ExtraTurnCount);
    }

    public void setExtraTurnCount(final int val) {
        set(TrackableProperty.ExtraTurnCount, val);
    }

    public boolean getHasPriority() {
        return get(TrackableProperty.HasPriority);
    }
    public void setHasPriority(final boolean val) {
        set(TrackableProperty.HasPriority, val);
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
        return hasUnlimitedHandSize() ? Localizer.getInstance().getMessage("lblUnlimited") : String.valueOf(getMaxHandSize());
    }

    public int getMaxLandPlay() {
        return get(TrackableProperty.MaxLandPlay);
    }
    void updateMaxLandPlay(Player p) {
        set(TrackableProperty.MaxLandPlay, p.getMaxLandPlays());
    }

    public boolean hasUnlimitedLandPlay() {
        return get(TrackableProperty.HasUnlimitedLandPlay);
    }
    void updateUnlimitedLandPlay(Player p) {
        set(TrackableProperty.HasUnlimitedLandPlay, p.getMaxLandPlaysInfinite());
    }

    public String getMaxLandString() {
        return hasUnlimitedLandPlay() ? "unlimited" : String.valueOf(getMaxLandPlay());
    }

    public int getNumLandThisTurn() {
        return get(TrackableProperty.NumLandThisTurn);
    }
    void updateNumLandThisTurn(Player p) {
        set(TrackableProperty.NumLandThisTurn, p.getLandsPlayedThisTurn());
    }

    public int getNumDrawnThisTurn() {
        return get(TrackableProperty.NumDrawnThisTurn);
    }
    void updateNumDrawnThisTurn(Player p) {
        set(TrackableProperty.NumDrawnThisTurn, p.getNumDrawnThisTurn());
    }

    public int getAdditionalVote() {
        return get(TrackableProperty.AdditionalVote);
    }
    public void updateAdditionalVote(Player p) {
        set(TrackableProperty.AdditionalVote, p.getAdditionalVotesAmount());
    }

    public int getOptionalAdditionalVote() {
        return get(TrackableProperty.OptionalAdditionalVote);
    }
    public void updateOptionalAdditionalVote(Player p) {
        set(TrackableProperty.OptionalAdditionalVote, p.getAdditionalOptionalVotesAmount());
    }

    public boolean getControlVote() {
        return get(TrackableProperty.ControlVotes);
    }
    public void updateControlVote(boolean val) {
        set(TrackableProperty.ControlVotes, val);
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
        return allKws;
    }
    public boolean hasKeyword(String keyword) {
        return getKeywords().contains(keyword);
    }
    void updateKeywords(Player p) {
        set(TrackableProperty.Keywords, ImmutableMultiset.copyOf(p.getKeywords().asStringList()));
    }

    public List<CardView> getCommanders() {
        return get(TrackableProperty.Commander);
    }
    void updateCommander(Player p) {
        set(TrackableProperty.Commander, CardView.getCollection(p.getCommanders()));
    }

    public int getCommanderDamage(CardView commander) {
        Map<Integer, Integer> map = get(TrackableProperty.CommanderDamage);
        if (map == null) { return 0; }
        Integer damage = map.get(commander.getId());
        return damage == null ? 0 : damage.intValue();
    }
    void updateCommanderDamage(Player p) {
        Map<Integer, Integer> map = Maps.newHashMap();
        for (Entry<Card, Integer> entry : p.getCommanderDamage()) {
            map.put(entry.getKey().getId(), entry.getValue());
        }
        set(TrackableProperty.CommanderDamage, map);
    }
    void updateMergedCommanderDamage(Card card, Card commander) {
        // Add commander damage to top card for card view panel info
        for (final PlayerView p : Iterables.concat(Collections.singleton(this), getOpponents())) {
            Map<Integer, Integer> map = p.get(TrackableProperty.CommanderDamage);
            if (map == null) continue;
            Integer damage = map.get(commander.getId());
            map.put(card.getId(), damage);
        }
    }

    public int getCommanderCast(CardView commander) {
        Map<Integer, Integer> map = get(TrackableProperty.CommanderCast);
        if (map == null) { return 0; }
        Integer damage = map.get(commander.getId());
        return damage == null ? 0 : damage.intValue();
    }

    void updateCommanderCast(Player p, Card c) {
        Map<Integer, Integer> map = get(TrackableProperty.CommanderCast);
        if (map == null) {
            map = Maps.newHashMap();
        }
        map.put(c.getId(), p.getCommanderCast(c));
        set(TrackableProperty.CommanderCast, map);
    }

    void updateMergedCommanderCast(Player p, Card target, Card commander) {
        Map<Integer, Integer> map = get(TrackableProperty.CommanderCast);
        if (map == null) {
            map = Maps.newHashMap();
        }
        map.put(target.getId(), p.getCommanderCast(commander));
        set(TrackableProperty.CommanderCast, map);
    }

    public PlayerView getMindSlaveMaster() {
        return get(TrackableProperty.MindSlaveMaster);
    }
    void updateMindSlaveMaster(Player p) {
        set(TrackableProperty.MindSlaveMaster, PlayerView.get(p.getControllingPlayer()));
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

    public FCollectionView<CardView> getSideboard() {
        return get(TrackableProperty.Sideboard);
    }
    public int getSideboardSize() {
        return getZoneSize(TrackableProperty.Sideboard);
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

    public boolean hasDelirium() {
        if (get(TrackableProperty.HasDelirium) == null)
            return false;
        return get(TrackableProperty.HasDelirium);
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
        case Sideboard:
            return TrackableProperty.Sideboard;
        default:
            return null; //other zones not represented
        }
    }
    void updateZone(PlayerZone zone) {
        TrackableProperty prop = getZoneProp(zone.getZoneType());
        if (prop == null) { return; }
        set(prop, CardView.getCollection(zone.getCards(false)));

        //update delirium
        if (ZoneType.Graveyard == zone.getZoneType())
            set(TrackableProperty.HasDelirium, getZoneTypes(TrackableProperty.Graveyard) >= 4);

        //update flashback zone when graveyard, library, or exile zones updated
        switch (zone.getZoneType()) {
        case Command:
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
        Integer count = null;
        try {
            count = getMana().get(color);
        }
        catch (Exception e) {
            e.printStackTrace();
            count = null;
        }
        return count != null ? count.intValue() : 0;
    }
    private Map<Byte, Integer> getMana() {
        return get(TrackableProperty.Mana);
    }
    void updateMana(Player p) {
        Map<Byte, Integer> mana = new HashMap<>();
        for (byte b : ManaAtom.MANATYPES) {
            mana.put(b, p.getManaPool().getAmountOfColor(b));
        }
        set(TrackableProperty.Mana, mana);
    }

    private List<String> getDetailsList() {
        final List<String> details = Lists.newArrayListWithCapacity(8);
        details.add(Localizer.getInstance().getMessage("lblLifeHas", String.valueOf(getLife())));

        Map<CounterType, Integer> counters = getCounters();
        if (counters != null) {
            for (Entry<CounterType, Integer> p : counters.entrySet()) {
                if (p.getValue() > 0) {
                    details.add(Localizer.getInstance().getMessage("lblTypeCounterHas", p.getKey().getName(), String.valueOf(p.getValue())));
                }
            }
        }

        details.add(Localizer.getInstance().getMessage("lblCardInHandHas", String.valueOf(getHandSize()), getMaxHandString()));
        details.add(Localizer.getInstance().getMessage("lblLandsPlayed", String.valueOf(getNumLandThisTurn()), this.getMaxLandString()));
        details.add(Localizer.getInstance().getMessage("lblCardDrawnThisTurnHas", String.valueOf(getNumDrawnThisTurn())));
        details.add(Localizer.getInstance().getMessage("lblDamagepreventionHas", String.valueOf(getPreventNextDamage())));

        int v = getAdditionalVote();
        if (v > 0) {
            details.add(Localizer.getInstance().getMessage("lblAdditionalVotes", String.valueOf(v)));
        }
        v = getOptionalAdditionalVote();
        if (v > 0) {
            details.add(Localizer.getInstance().getMessage("lblOptionalAdditionalVotes", String.valueOf(v)));
        }

        if (getControlVote()) {
            details.add(Localizer.getInstance().getMessage("lblControlsVote"));
        }

        if (getIsExtraTurn()) {
            details.add(Localizer.getInstance().getMessage("lblIsExtraTurn"));
        }
        details.add(Localizer.getInstance().getMessage("lblExtraTurnCountHas", String.valueOf(getExtraTurnCount())));
        final String keywords = Lang.joinHomogenous(getDisplayableKeywords());
        if (!keywords.isEmpty()) {
            details.add(keywords);
        }
        final FCollectionView<CardView> ante = getAnte();
        if (ante != null && !ante.isEmpty()) {
            details.add(Localizer.getInstance().getMessage("lblAntedHas", Lang.joinHomogenous(ante)));
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
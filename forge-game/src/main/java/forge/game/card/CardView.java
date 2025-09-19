package forge.game.card;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.game.Direction;
import forge.game.EvenOdd;
import forge.game.GameEntityView;
import forge.game.GameType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import forge.util.*;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CardView extends GameEntityView {
    private static final long serialVersionUID = -3624090829028979255L;

    public static CardView get(Card c) {
        return c == null ? null : c.getView();
    }
    public static CardStateView getState(Card c, CardStateName state) {
        if (c == null) { return null; }
        CardState s = c.getState(state);
        return s == null ? null : s.getView();
    }

    public static Map<CardStateView, CardState> getStateMap(Iterable<CardState> states) {
        Map<CardStateView, CardState> stateViewCache = Maps.newLinkedHashMap();
        for (CardState state : states) {
            stateViewCache.put(state.getView(), state);
        }
        return stateViewCache;
    }

    public CardView getBackup() {
        if (get(TrackableProperty.PaperCardBackup) == null)
            return null;
        return getCardForUi(get(TrackableProperty.PaperCardBackup));
    }

    public static CardView getCardForUi(IPaperCard pc) {
        return Card.getCardForUi(pc).getView();
    }

    public static TrackableCollection<CardView> getCollection(Iterable<Card> cards) {
        TrackableCollection<CardView> collection = new TrackableCollection<>();
        if (cards != null) {
            for (Card c : cards) {
                if (c != null && c.getRenderForUI()) { //only add cards that match their card for UI
                    collection.add(c.getView());
                }
            }
        }
        return collection;
    }

    public static boolean mayViewAny(Iterable<CardView> cards, Iterable<PlayerView> viewers) {
        if (cards == null) { return false; }

        for (CardView cv : cards) {
            if (cv.canBeShownToAny(viewers)) {
                return true;
            }
        }
        return false;
    }

    public CardView(final int id0, final Tracker tracker) {
        super(id0, tracker);
        set(TrackableProperty.CurrentState, new CardStateView(id0, CardStateName.Original, tracker));
    }
    public CardView(final int id0, final Tracker tracker, final String name0) {
        this(id0, tracker);
        getCurrentState().setName(name0);
        set(TrackableProperty.Name, name0);
        set(TrackableProperty.ChangedColorWords, new HashMap<String, String>());
        set(TrackableProperty.ChangedTypes, new HashMap<String, String>());
        set(TrackableProperty.Sickness, true);
    }
    public CardView(final int id0, final Tracker tracker, final String name0, final PlayerView ownerAndController, final String imageKey) {
        this(id0, tracker, name0);
        set(TrackableProperty.Owner, ownerAndController);
        set(TrackableProperty.Controller, ownerAndController);
        set(TrackableProperty.ImageKey, imageKey);
    }
    public PlayerView getOwner() {
        return get(TrackableProperty.Owner);
    }
    void updateOwner(Card c) {
        set(TrackableProperty.Owner, PlayerView.get(c.getOwner()));
    }

    public PlayerView getController() {
        return get(TrackableProperty.Controller);
    }
    void updateController(Card c) {
        set(TrackableProperty.Controller, PlayerView.get(c.getController()));
    }

    public ZoneType getZone() {
        return get(TrackableProperty.Zone);
    }
    void updateZone(Card c) {
        set(TrackableProperty.Zone, c.getZone() == null ? null : c.getZone().getZoneType());
    }
    public boolean isInZone(final Iterable<ZoneType> zones) {
        return Iterables.contains(zones, getZone());
    }

    public boolean isCloned() {
        return get(TrackableProperty.Cloned);
    }

    public boolean isFaceDown() {
        return get(TrackableProperty.Facedown);//  getCurrentState().getState() == CardStateName.FaceDown;
    }

    public boolean isForeTold() {
        return get(TrackableProperty.Foretold);
    }

    public boolean isFlipCard() {
        return get(TrackableProperty.FlipCard);
    }

    public boolean isFlipped() {
        return get(TrackableProperty.Flipped);
    }
    public boolean isSplitCard() {
        return get(TrackableProperty.SplitCard);
    }

    public boolean isDoubleFacedCard() {
        return get(TrackableProperty.DoubleFaced);
    }

    public boolean hasSecondaryState() {
        return get(TrackableProperty.Secondary);
    }

    public boolean isModalCard() {
        return get(TrackableProperty.Modal);
    }

    public boolean isRoom() {
        return get(TrackableProperty.Room);
    }

    public String getFacedownImageKey() {
        return get(TrackableProperty.FacedownImageKey);
    }

    /*
    public boolean isTransformed() {
        return getCurrentState().getState() == CardStateName.Transformed;
    }
    //*/

    public boolean isAttacking() {
        return get(TrackableProperty.Attacking);
    }
    void updateAttacking(Card c) {
        Combat combat = c.getGame().getCombat();
        set(TrackableProperty.Attacking, combat != null && combat.isAttacking(c));
    }

    public boolean isExertedThisTurn() {
        return get(TrackableProperty.ExertedThisTurn);
    }
    void updateExertedThisTurn(Card c, boolean exerted) {
        set(TrackableProperty.ExertedThisTurn, exerted);
    }

    public boolean isBlocking() {
        return get(TrackableProperty.Blocking);
    }
    void updateBlocking(Card c) {
        Combat combat = c.getGame().getCombat();
        set(TrackableProperty.Blocking, combat != null && combat.isBlocking(c));
    }

    public boolean isPhasedOut() {
        return get(TrackableProperty.PhasedOut);
    }
    void updatePhasedOut(Card c) {
        set(TrackableProperty.PhasedOut, c.isPhasedOut());
    }

    public boolean isFirstTurnControlled() {
        return get(TrackableProperty.Sickness);
    }
    public boolean hasSickness() {
        return isFirstTurnControlled() && !getCurrentState().hasHaste();
    }
    public boolean isSick() {
        return getZone() == ZoneType.Battlefield && getCurrentState().isCreature() && hasSickness();
    }
    void updateSickness(Card c) {
        set(TrackableProperty.Sickness, c.isFirstTurnControlled());
    }

    public boolean isTapped() {
        return get(TrackableProperty.Tapped);
    }
    void updateTapped(Card c) {
        set(TrackableProperty.Tapped, c.isTapped());
    }

    public GamePieceType getGamePieceType() {
        return get(TrackableProperty.GamePieceType);
    }
    void updateGamePieceType(Card c) {
        set(TrackableProperty.GamePieceType, c.getGamePieceType());
    }

    //Tracked separately from GamePieceType; a token card or a merged permanent with a token as the top is also considered a "token"
    public boolean isToken() {
        return get(TrackableProperty.Token);
    }
    void updateToken(Card c) {
        set(TrackableProperty.Token, c.isToken());
    }

    public boolean isImmutable() {
        return get(TrackableProperty.GamePieceType) == GamePieceType.EFFECT;
    }

    public boolean isEmblem() {
        return get(TrackableProperty.IsEmblem);
    }
    public void updateEmblem(Card c) {
        set(TrackableProperty.IsEmblem, c.isEmblem());
    }

    public boolean isBoon() {
        return get(TrackableProperty.IsBoon);
    }
    public void updateBoon(Card c) {
        set(TrackableProperty.IsBoon, c.isBoon());
    }

    public boolean canSpecialize() {
        return get(TrackableProperty.CanSpecialize);
    }
    public void updateSpecialize(Card c) {
        set(TrackableProperty.CanSpecialize, c.canSpecialize());
    }

    public boolean isTokenCard() { return get(TrackableProperty.TokenCard); }
    void updateTokenCard(Card c) { set(TrackableProperty.TokenCard, c.isTokenCard()); }

    public boolean isCommander() {
        return get(TrackableProperty.IsCommander);
    }
    void updateCommander(Card c) {
        boolean isCommander = c.isCommander();
        set(TrackableProperty.IsCommander, isCommander);
        if (isCommander) {
            if (c.getGame().getRules().hasAppliedVariant(GameType.Oathbreaker)) {
                //store alternate type for oathbreaker or signature spell for display in card text
                if (c.getPaperCard().getRules().canBeSignatureSpell()) {
                    set(TrackableProperty.CommanderAltType, "Signature Spell");
                } else {
                    set(TrackableProperty.CommanderAltType, "Oathbreaker");
                }
            } else {
                set(TrackableProperty.CommanderAltType, "Commander");
            }
        }
    }
    public String getCommanderType() {
        return get(TrackableProperty.CommanderAltType);
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
    public boolean hasSameCounters(CardView otherCard) {
        Map<CounterType, Integer> counters = getCounters();
        if (counters == null) {
            return otherCard.getCounters() == null;
        }
        return counters.equals(otherCard.getCounters());
    }
    public boolean hasSamePT(CardView otherCard) {
        if (getCurrentState().getPower() != otherCard.getCurrentState().getPower())
            return false;
        if (getCurrentState().getToughness() != otherCard.getCurrentState().getToughness())
            return false;
        return true;
    }
    void updateCounters(Card c) {
        set(TrackableProperty.Counters, c.getCounters());
        updateLethalDamage(c);
        CardStateView state = getCurrentState();
        state.updatePower(c);
        state.updateToughness(c);
        state.updateLoyalty(c);
        state.updateDefense(c);
    }

    public int getCrackOverlayInt() {
        if (get(TrackableProperty.CrackOverlay) == null)
            return 0;
        return get(TrackableProperty.CrackOverlay);
    }
    public int getDamage() {
        return get(TrackableProperty.Damage);
    }
    void updateDamage(Card c) {
        set(TrackableProperty.Damage, c.getDamage());
        updateLethalDamage(c);
        //get crackoverlay by level of damage light 0, medium 1, heavy 2, max 3
        int randCrackLevel = 0;
        if (c.getDamage() > 0) {
            switch (c.getDamage()) {
                case 1:
                case 2:
                    randCrackLevel = 0;
                    break;
                case 3:
                case 4:
                    randCrackLevel = 1;
                    break;
                case 5:
                case 6:
                    randCrackLevel = 2;
                    break;
                default:
                    randCrackLevel = 3;
                    break;
            }
        }
        set(TrackableProperty.CrackOverlay, randCrackLevel);
    }

    public int getAssignedDamage() {
        return get(TrackableProperty.AssignedDamage);
    }
    void updateAssignedDamage(Card c) {
        set(TrackableProperty.AssignedDamage, c.getTotalAssignedDamage());
        updateLethalDamage(c);
    }

    public int getLethalDamage() {
        return get(TrackableProperty.LethalDamage);
    }
    void updateLethalDamage(Card c) {
        set(TrackableProperty.LethalDamage, c.getLethalDamage());
    }

    public int getShieldCount() {
        return get(TrackableProperty.ShieldCount);
    }
    void updateShieldCount(Card c) {
        set(TrackableProperty.ShieldCount, c.getShieldCount());
    }

    public String getChosenType() {
        return get(TrackableProperty.ChosenType);
    }
    void updateChosenType(Card c) {
        set(TrackableProperty.ChosenType, c.getChosenType());
    }

    public String getChosenType2() {
        return get(TrackableProperty.ChosenType2);
    }
    void updateChosenType2(Card c) {
        set(TrackableProperty.ChosenType2, c.getChosenType2());
    }

    public List<String> getNotedTypes() {
        return get(TrackableProperty.NotedTypes);
    }
    void updateNotedTypes(Card c) {
        set(TrackableProperty.NotedTypes, c.getNotedTypes());
    }

    public String getChosenNumber() {
        return get(TrackableProperty.ChosenNumber);
    }
    void updateChosenNumber(Card c) {
        set(TrackableProperty.ChosenNumber, c.getChosenNumber().toString());
    }
    void clearChosenNumber() {
        set(TrackableProperty.ChosenNumber, "");
    }

    public List<String> getStoredRolls() {
        return get(TrackableProperty.StoredRolls);
    }
    void updateStoredRolls(Card c) {
        set(TrackableProperty.StoredRolls, c.getStoredRollsForView());
    }

    public List<String> getChosenColors() {
        return get(TrackableProperty.ChosenColors);
    }
    void updateChosenColors(Card c) {
        set(TrackableProperty.ChosenColors, c.getChosenColors());
    }
    public boolean hasPaperFoil() {
        return get(TrackableProperty.PaperFoil);
    }
    void updatePaperFoil(boolean v) {
        set(TrackableProperty.PaperFoil, v);
    }
    public ColorSet getMarkedColors() {
        return get(TrackableProperty.MarkedColors);
    }
    void updateMarkedColors(Card c) {
        set(TrackableProperty.MarkedColors, c.getMarkedColors());
    }
    public FCollectionView<CardView> getMergedCardsCollection() {
        return get(TrackableProperty.MergedCardsCollection);
    }

    public FCollectionView<CardView> getChosenCards() {
        return get(TrackableProperty.ChosenCards);
    }

    public PlayerView getChosenPlayer() {
        return get(TrackableProperty.ChosenPlayer);
    }
    void updateChosenPlayer(Card c) {
        set(TrackableProperty.ChosenPlayer, PlayerView.get(c.getChosenPlayer()));
    }
    public PlayerView getPromisedGift() {
        return get(TrackableProperty.PromisedGift);
    }
    void updatePromisedGift(Card c) {
        set(TrackableProperty.PromisedGift, PlayerView.get(c.getPromisedGift()));
    }
    public PlayerView getProtectingPlayer() {
        return get(TrackableProperty.ProtectingPlayer);
    }
    void updateProtectingPlayer(Card c) {
        set(TrackableProperty.ProtectingPlayer, PlayerView.get(c.getProtectingPlayer()));
    }

    public Direction getChosenDirection() {
        return get(TrackableProperty.ChosenDirection);
    }
    void updateChosenDirection(Card c) {
        set(TrackableProperty.ChosenDirection, c.getChosenDirection());
    }
    public EvenOdd getChosenEvenOdd() {
        return get(TrackableProperty.ChosenEvenOdd);
    }
    void updateChosenEvenOdd(Card c) {
        set(TrackableProperty.ChosenEvenOdd, c.getChosenEvenOdd());
    }

    public String getChosenMode() {
        return get(TrackableProperty.ChosenMode);
    }
    void updateChosenMode(Card c) {
        set(TrackableProperty.ChosenMode, c.getChosenMode());
    }

    public String getCurrentRoom() {
        return get(TrackableProperty.CurrentRoom);
    }
    void updateCurrentRoom(Card c) {
        set(TrackableProperty.CurrentRoom, c.getCurrentRoom());
        updateMarkerText(c);
    }

    public int getIntensity() {
        return get (TrackableProperty.Intensity);
    }
    void updateIntensity(Card c) {
        set(TrackableProperty.Intensity, c.getIntensity(true));
    }

    public boolean wasDestroyed() {
        if (get(TrackableProperty.WasDestroyed) == null)
            return false;
        return get(TrackableProperty.WasDestroyed);
    }
    void updateWasDestroyed(boolean value) {
        set(TrackableProperty.WasDestroyed, value);
    }

    public int getClassLevel() {
        return get(TrackableProperty.ClassLevel);
    }
    void updateClassLevel(Card c) {
        set(TrackableProperty.ClassLevel, c.getClassLevel());
        updateMarkerText(c);
    }

    public int getRingLevel() {
        return get(TrackableProperty.RingLevel);
    }
    void updateRingLevel(Card c) {
        Player p = c.getController();
        if (p != null && p.getTheRing() == c)
            set(TrackableProperty.RingLevel, p.getNumRingTemptedYou());
    }

    public String getOverlayText() {
        return get(TrackableProperty.OverlayText);
    }
    public List<String> getMarkerText() {
        return get(TrackableProperty.MarkerText);
    }
    void updateMarkerText(Card c) {
        List<String> markerItems = new ArrayList<>();
        if(c.getCurrentRoom() != null && !c.getCurrentRoom().isEmpty()) {
            markerItems.add("In Room:");
            markerItems.add(c.getCurrentRoom());
        }
        if(c.isClassCard() && c.isInZone(ZoneType.Battlefield)) {
            markerItems.add("CL:" + c.getClassLevel());
        }
        if(getRingLevel() > 0) {
            markerItems.add("RL:" + getRingLevel());
        }

        if(StringUtils.isNotEmpty(c.getOverlayText())) {
            set(TrackableProperty.OverlayText, c.getOverlayText());
            markerItems.add(c.getOverlayText());
        }
        else {
            //Overlay text is any custom string. It gets mixed in with the other marker lines, but it also needs its
            //own property so that it can display in the card detail text.
            set(TrackableProperty.OverlayText, null);
        }

        if(markerItems.isEmpty())
            set(TrackableProperty.MarkerText, null);
        else
            set(TrackableProperty.MarkerText, markerItems);
    }

    private String getRemembered() {
        return get(TrackableProperty.Remembered);
    }
    void updateRemembered(Card c) {
        if (c.getRemembered() == null || Iterables.isEmpty(c.getRemembered())) {
            set(TrackableProperty.Remembered, null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\r\nRemembered: \r\n");
        for (final Object o : c.getRemembered()) {
            if (o != null) {
                sb.append(o.toString());
                sb.append("\r\n");
            }
        }
        set(TrackableProperty.Remembered, sb.toString());
    }

    public String getSector() {
       return get(TrackableProperty.Sector);
    }
    void updateSector(Card c) {
        set(TrackableProperty.Sector, c.getSector());
    }

    public int getSprocket() {
        return get(TrackableProperty.Sprocket);
    }
    void updateSprocket(Card c) {
        set(TrackableProperty.Sprocket, c.getSprocket());
    }

    public List<String> getDraftAction() { return get(TrackableProperty.DraftAction); }
    void updateDraftAction(Card c) {
        set(TrackableProperty.DraftAction, c.getDraftActions());
    }

    public List<String> getNamedCard() {
        return get(TrackableProperty.NamedCard);
    }
    void updateNamedCard(Card c) {
        set(TrackableProperty.NamedCard, c.getNamedCards());
    }
    public boolean getMayPlayPlayers(PlayerView pv) {
        TrackableCollection<PlayerView> col = get(TrackableProperty.MayPlayPlayers);
        return col != null && col.indexOf(pv) != -1;
    }
    void setMayPlayPlayers(Iterable<Player> list) {
        if (Iterables.isEmpty(list)) {
            set(TrackableProperty.MayPlayPlayers, null);
        } else {
            set(TrackableProperty.MayPlayPlayers, PlayerView.getCollection(list));
        }
    }
    public boolean mayPlayerLook(PlayerView pv) {
        TrackableCollection<PlayerView> col = get(TrackableProperty.PlayerMayLook);
        // TODO don't use contains as it only queries the backing HashSet which is problematic for netplay because of unsynchronized player ids
        return col != null && col.indexOf(pv) != -1;
    }
    void setPlayerMayLook(Iterable<Player> list) {
        if (Iterables.isEmpty(list)) {
            set(TrackableProperty.PlayerMayLook, null);
        } else {
            set(TrackableProperty.PlayerMayLook, PlayerView.getCollection(list));
        }
    }

    public boolean canBeShownToAny(final Iterable<PlayerView> viewers) {
        if (viewers == null || Iterables.isEmpty(viewers)) { return true; }

        return IterableUtil.any(viewers, this::canBeShownTo);
    }

    public boolean canBeShownTo(final PlayerView viewer) {
        if (viewer == null) { return false; }

        ZoneType zone = getZone();
        if (zone == null) { return true; } //cards outside any zone are visible to all

        final PlayerView controller = getController();
        switch (zone) {
        case Ante:
        case Command:
        case Battlefield:
        case Graveyard:
        case Flashback:
        case Stack:
        case Junkyard:
            //cards in these zones are visible to all
            return true;
        case Exile:
        case Merged:
            //in exile, only face up cards and face down cards you can look at should be shown (since "exile face down" is a thing)
            if (!isFaceDown()) {
                return true;
            }
            break;
        case Hand:
            if (controller.equals(viewer)) {
                return true;
            }
            break;
        case Sideboard:
            //face-up cards in these zones are hidden to opponents unless they specify otherwise
            if (controller.isOpponentOf(viewer) && !mayPlayerLook(viewer)) {
                break;
            }
            return true;
        case Library:
        case PlanarDeck:
        case AttractionDeck:
        case ContraptionDeck:
            //cards in these zones are hidden to all unless they specify otherwise
            break;
        case SchemeDeck:
            // true for now, to actually see the Scheme cards (can't see deck anyway)
            return true;
        default:
            break;
        }

        // special viewing permissions for viewer
        if (mayPlayerLook(viewer)) {
            return true;
        }

        //if viewer is controlled by another player, also check if card can be shown to that player
        PlayerView mindSlaveMaster = controller.getMindSlaveMaster();
        if (mindSlaveMaster != null && mindSlaveMaster != controller && mindSlaveMaster == viewer) {
            return canBeShownTo(controller);
        }
        return false;
    }

    public boolean canFaceDownBeShownToAny(final Iterable<PlayerView> viewers) {
        if (viewers == null || Iterables.isEmpty(viewers)) { return true; }

        return IterableUtil.any(viewers, this::canFaceDownBeShownTo);
    }

    public boolean canFaceDownBeShownTo(final PlayerView viewer) {
        if (!isFaceDown()) {
            return true;
        }

        // special viewing permissions for viewer
        if (mayPlayerLook(viewer)) {
            return true;
        }
        //if viewer is controlled by another player, also check if face can be shown to that player
        final PlayerView mindSlaveMaster = getController().getMindSlaveMaster();
        if (mindSlaveMaster != null && mindSlaveMaster != getController() && mindSlaveMaster == viewer) {
            return canFaceDownBeShownTo(getController());
        }

        return isInZone(EnumSet.of(ZoneType.Battlefield, ZoneType.Stack, ZoneType.Sideboard)) && getController().equals(viewer);
    }

    public FCollectionView<CardView> getEncodedCards() {
        return get(TrackableProperty.EncodedCards);
    }

    public FCollectionView<CardView> getUntilLeavesBattlefield() {
        return get(TrackableProperty.UntilLeavesBattlefield);
    }

    public GameEntityView getEntityAttachedTo() {
        return get(TrackableProperty.EntityAttachedTo);
    }
    void updateAttachedTo(Card c) {
        set(TrackableProperty.EntityAttachedTo, GameEntityView.get(c.getEntityAttachedTo()));
    }

    public CardView getAttachedTo() {
        GameEntityView enchanting = getEntityAttachedTo();
        if (enchanting instanceof CardView) {
            return (CardView) enchanting;
        }
        return null;
    }
    public PlayerView getEnchantedPlayer() {
        GameEntityView enchanting = getEntityAttachedTo();
        if (enchanting instanceof PlayerView) {
            return (PlayerView) enchanting;
        }
        return null;
    }

    public FCollectionView<CardView> getGainControlTargets() {
        return get(TrackableProperty.GainControlTargets);
    }

    public CardView getCloneOrigin() {
        return get(TrackableProperty.CloneOrigin);
    }

    public CardView getExiledWith() {
        return get(TrackableProperty.ExiledWith);
    }

    public FCollectionView<CardView> getImprintedCards() {
        return get(TrackableProperty.ImprintedCards);
    }

    public FCollectionView<CardView> getExiledCards() {
        return get(TrackableProperty.ExiledCards);
    }

    public FCollectionView<CardView> getHauntedBy() {
        return get(TrackableProperty.HauntedBy);
    }

    public CardView getHaunting() {
        return get(TrackableProperty.Haunting);
    }

    public FCollectionView<CardView> getMustBlockCards() {
        return get(TrackableProperty.MustBlockCards);
    }
    void updateMustBlockCards(Card c) {
        setCards(null, c.getMustBlockCards(), TrackableProperty.MustBlockCards);
    }

    public CardView getPairedWith() {
        return get(TrackableProperty.PairedWith);
    }

    public Map<String, String> getChangedColorWords() {
        return get(TrackableProperty.ChangedColorWords);
    }
    void updateChangedColorWords(Card c) {
        set(TrackableProperty.ChangedColorWords, c.getChangedTextColorWords());
    }
    public Map<String, String> getChangedTypes() {
        return get(TrackableProperty.ChangedTypes);
    }
    void updateChangedTypes(Card c) {
        set(TrackableProperty.ChangedTypes, c.getChangedTextTypeWords());
    }

    void updateNonAbilityText(Card c) {
        set(TrackableProperty.NonAbilityText, c.getNonAbilityText());
    }

    public String getText() {
        return getText(getCurrentState(), null);
    }
    public String getText(CardStateView state, HashMap<String, String> translationsText) {
        final StringBuilder sb = new StringBuilder();
        String tname = "", toracle = "", taltname = "", taltoracle = "";

        // If we have translations, use them
        if (translationsText != null) {
            tname = translationsText.get("name");
            taltname = translationsText.get("altname");
            toracle = translationsText.get("oracle");
            taltoracle = translationsText.get("altoracle");
        }

        if (isSplitCard()) {
            tname = tname.isEmpty() ? getLeftSplitState().getName() : tname;
            taltname = taltname.isEmpty() ? getRightSplitState().getName() : taltname;
            if (getId() < 0) {
                toracle = toracle.isEmpty() ? getLeftSplitState().getOracleText() : toracle;
                taltoracle = taltoracle.isEmpty() ? getRightSplitState().getOracleText() : taltoracle;
            }
        } else {
            tname = tname.isEmpty() ? state.getName() : tname;
            if (getId() < 0) {
                toracle = toracle.isEmpty() ? state.getOracleText() : toracle;
            }
        }

        if (getId() < 0) {
            if (isSplitCard() && !toracle.isEmpty()) {
                sb.append("(").append(tname).append(") ");
                sb.append(toracle);
                sb.append("\r\n\r\n");
                sb.append("(").append(taltname).append(") ");
                sb.append(taltoracle);
            } else {
                sb.append(toracle);
            }
            return sb.toString();
        }

        final String rulesText = state.getRulesText();
        if (!rulesText.isEmpty()) {
            sb.append(rulesText).append("\r\n\r\n");
        }
        if (isCommander()) {
            sb.append(getOwner()).append("'s ").append(getCommanderType()).append("\r\n");
            sb.append(getOwner().getCommanderInfo(this)).append("\r\n");
        }

        if (isSplitCard() && !isFaceDown() && getZone() != ZoneType.Stack && getZone() != ZoneType.Battlefield) {
            sb.append("(").append(getLeftSplitState().getName()).append(") ");
            sb.append(getLeftSplitState().getAbilityText());
            sb.append("\r\n\r\n").append("(").append(getRightSplitState().getName()).append(") ");
            sb.append(getRightSplitState().getAbilityText());
        } else {
            sb.append(state.getAbilityText());
        }

        String nonAbilityText = get(TrackableProperty.NonAbilityText);
        if (!nonAbilityText.isEmpty()) {
            sb.append("\r\n \r\nNon ability features: \r\n");
            sb.append(nonAbilityText.replaceAll("CARDNAME", getName()));
        }

        Set<Integer> attractionLights = get(TrackableProperty.AttractionLights);
        if (attractionLights != null && !attractionLights.isEmpty()) {
            sb.append("\r\n\r\nLights: ");
            sb.append(StringUtils.join(attractionLights, ", "));
        }

        sb.append(getRemembered());

        Direction chosenDirection = getChosenDirection();
        if (chosenDirection != null) {
            sb.append("\r\n[Chosen direction: ");
            sb.append(chosenDirection);
            sb.append("]\r\n");
        }

        EvenOdd chosenEvenOdd = getChosenEvenOdd();
        if (chosenEvenOdd != null) {
            sb.append("\r\n[Chosen value: ");
            sb.append(chosenEvenOdd);
            sb.append("]\r\n");
        }

        CardView pairedWith = getPairedWith();
        if (pairedWith != null) {
            sb.append("\r\n \r\nPaired With: ").append(pairedWith);
            sb.append("\r\n");
        }

        if (getCanBlockAny()) {
            sb.append("\r\n\r\n");
            sb.append("CARDNAME can block any number of creatures.".replaceAll("CARDNAME", getName()));
            sb.append("\r\n");
        } else {
            int i = getBlockAdditional();
            if (i > 0) {
                sb.append("\r\n\r\n");
                sb.append("CARDNAME can block an additional ".replaceAll("CARDNAME", getName()));
                sb.append(i == 1 ? "creature" : Lang.nounWithNumeral(i, "creature"));
                sb.append(" each combat.");
                sb.append("\r\n");
            }
        }

        Set<String> cantHaveKeyword = this.getCantHaveKeyword();
        if (cantHaveKeyword != null && !cantHaveKeyword.isEmpty()) {
            sb.append("\r\n\r\n");
            for (String k : cantHaveKeyword) {
                sb.append("CARDNAME can't have or gain ".replaceAll("CARDNAME", getName()));
                sb.append(k);
                sb.append(".");
                sb.append("\r\n");
            }
        }

        String cloner = get(TrackableProperty.Cloner);
        if (!cloner.isEmpty()) {
            sb.append("\r\nCloned by: ").append(cloner);
        }

        String mergedCards = get(TrackableProperty.MergedCards);
        if (!mergedCards.isEmpty()) {
            sb.append("\r\n\r\nMerged Cards: ").append(mergedCards);
        }

        return sb.toString().trim()
            .replace("\\r", "\r")
            .replace("\\n", "\n");
    }

    public CardStateView getCurrentState() {
        return get(TrackableProperty.CurrentState);
    }

    public boolean hasAlternateState() {
        return getAlternateState() != null;
    }
    public CardStateView getAlternateState() {
        return get(TrackableProperty.AlternateState);
    }

    public boolean hasLeftSplitState() {
        return getLeftSplitState() != null;
    }
    public CardStateView getLeftSplitState() {
        return get(TrackableProperty.LeftSplitState);
    }

    public boolean hasRightSplitState() {
        return getRightSplitState() != null;
    }
    public CardStateView getRightSplitState() {
        return get(TrackableProperty.RightSplitState);
    }

    public boolean hasBackSide() {
        return get(TrackableProperty.HasBackSide);
    }
    public String getBackSideName() { return get(TrackableProperty.BackSideName); }

    CardStateView createAlternateState(final CardStateName state0) {
        return new CardStateView(getId(), state0, tracker);
    }

    public CardStateView getState(final boolean alternate0) {
        return alternate0 ? getAlternateState() : getCurrentState();
    }
    void updateBackSide(String stateName, boolean hasBackSide) {
        set(TrackableProperty.HasBackSide, hasBackSide);
        set(TrackableProperty.BackSideName, stateName);
    }
    public boolean needsUntapAnimation() {
        if (get(TrackableProperty.NeedsUntapAnimation) == null)
            return false;
        return get(TrackableProperty.NeedsUntapAnimation);
    }
    public void updateNeedsUntapAnimation(boolean value) {
        set(TrackableProperty.NeedsUntapAnimation, value);
    }
    public boolean needsTapAnimation() {
        if (get(TrackableProperty.NeedsTapAnimation) == null)
            return false;
        return get(TrackableProperty.NeedsTapAnimation);
    }
    public void updateNeedsTapAnimation(boolean value) {
        set(TrackableProperty.NeedsTapAnimation, value);
    }
    public boolean needsTransformAnimation() {
        if (get(TrackableProperty.NeedsTransformAnimation) == null)
            return false;
        return get(TrackableProperty.NeedsTransformAnimation);
    }
    public void updateNeedsTransformAnimation(boolean value) {
        set(TrackableProperty.NeedsTransformAnimation, value);
    }
    void updateState(Card c) {
        updateName(c);
        updateZoneText(c);
        updateDamage(c);
        updateSpecialize(c);
        updateRingLevel(c);
        updateMarkerText(c);

        if (c.getIntensity(false) > 0) {
            updateIntensity(c);
        }

        if (getBackup() == null && !c.isFaceDown() && (c.isDoubleFaced() || c.isFlipCard() || c.isAdventureCard() || c.isCloned())) {
            set(TrackableProperty.PaperCardBackup, c.getPaperCard());
        }

        boolean isSplitCard = c.isSplitCard();
        set(TrackableProperty.Cloned, c.isCloned());
        set(TrackableProperty.SplitCard, isSplitCard);
        set(TrackableProperty.FlipCard, c.isFlipCard());
        set(TrackableProperty.Flipped, c.getCurrentStateName() == CardStateName.Flipped);
        set(TrackableProperty.Facedown, c.isFaceDown());
        set(TrackableProperty.Foretold, c.isForetold());
        set(TrackableProperty.Secondary, c.hasState(CardStateName.Secondary));
        set(TrackableProperty.DoubleFaced, c.isDoubleFaced());
        set(TrackableProperty.Modal, c.isModal());
        set(TrackableProperty.Room, c.isRoom());
        set(TrackableProperty.FacedownImageKey, c.getFacedownImageKey());

        //backside
        if (c.getAlternateState() != null)
            updateBackSide(c.getAlternateState().getName(), c.isDoubleFaced());

        final Card cloner = c.getCloner();

        //CardStateView cloner = CardView.getState(c, CardStateName.Cloner);
        set(TrackableProperty.Cloner, cloner == null ? null : cloner.getName() + " (" + cloner.getId() + ")");

        CardCollection mergedCollection = new CardCollection();
        if (c.hasMergedCard()) {
            StringBuilder sb = new StringBuilder();
            CardCollectionView mergedCards = c.getMergedCards();
            for (int i = 1; i < mergedCards.size(); i++) {
                final Card card = mergedCards.get(i);
                if (i > 1) sb.append(", ");
                sb.append(card.getOriginalState(card.getCurrentStateName()).getName());
                sb.append(" (").append(card.getId()).append(")");
            }
            set(TrackableProperty.MergedCards, sb.toString());
            for (int i = 0; i < mergedCards.size(); i++) {
                final Card card = mergedCards.get(i);
                if (i == 0) { //get the original view of the top card
                    if (!card.isFaceDown())
                        mergedCollection.add(Card.getCardForUi(c.getPaperCard()));
                    else
                        mergedCollection.add(card);
                } else {
                    mergedCollection.add(card);
                }
            }
        } else {
            set(TrackableProperty.MergedCards, null);
        }
        updateMergeCollections(mergedCollection);

        CardState currentState = c.getCurrentState();
        if (isSplitCard) {
            set(TrackableProperty.LeftSplitState, c.getState(CardStateName.LeftSplit).getView());
            set(TrackableProperty.RightSplitState, c.getState(CardStateName.RightSplit).getView());

            // need to update ability text
            getLeftSplitState().updateAbilityText(c, c.getState(CardStateName.LeftSplit));
            getRightSplitState().updateAbilityText(c, c.getState(CardStateName.RightSplit));
        }

        CardStateView currentStateView = currentState.getView();
        if (getCurrentState() != currentStateView || c.hasPerpetual()) {
            set(TrackableProperty.CurrentState, currentStateView);
            currentStateView.updateName(currentState);
            currentStateView.updatePower(c); //ensure power, toughness, and loyalty updated when current state changes
            currentStateView.updateToughness(c);
            currentStateView.updateLoyalty(c);
            currentStateView.updateDefense(c);

            // update the color only while in Game
            if (c.getGame() != null) {
                if (c.hasPerpetual()) currentStateView.updateColors(c);
                else currentStateView.updateColors(currentState);
                currentStateView.updateHasChangeColors(!Iterables.isEmpty(c.getChangedCardColors()));
            }
        } else {
            currentStateView.updateLoyalty(currentState);
            currentStateView.updateDefense(currentState);
        }
        currentState.getView().updateKeywords(c, currentState); //update keywords even if state doesn't change
        currentState.getView().setOriginalColors(c); //set original Colors

        currentStateView.updateAttractionLights(currentState);
        currentStateView.updateHasPrintedPT((currentStateView.isVehicle() || currentStateView.isSpaceCraft()) && c.getRules() != null && c.getRules().hasPrintedPT());

        CardState alternateState = isSplitCard && isFaceDown() ? c.getState(CardStateName.RightSplit) : c.getAlternateState();

        if (isSplitCard && isFaceDown()) {
            // face-down (e.g. manifested) split cards should show the original face on their flip side
            alternateState = c.getState(CardStateName.Original);
        }

        if (c.isDoubleFaced() && isFaceDown()) //fixes facedown cards with backside...
            alternateState = c.getState(CardStateName.Original);

        if (alternateState == null) {
            set(TrackableProperty.AlternateState, null);
        } else {
            CardStateView alternateStateView = alternateState.getView();
            if (getAlternateState() != alternateStateView) {
                set(TrackableProperty.AlternateState, alternateStateView);
                alternateStateView.updateName(alternateState);
                alternateStateView.updatePower(c); //ensure power, toughness, and loyalty updated when current state changes
                alternateStateView.updateToughness(c);
                alternateStateView.updateLoyalty(c);
                alternateStateView.updateDefense(c);

                // update the color only while in Game
                if (c.getGame() != null) {
                    alternateStateView.updateColors(alternateState);
                }
            } else {
                alternateStateView.updateLoyalty(alternateState);
                alternateStateView.updateDefense(alternateState);
            }
            alternateState.getView().updateKeywords(c, alternateState);
        }
    }

    public int getHiddenId() {
        final Integer hiddenId = get(TrackableProperty.HiddenId);
        if (hiddenId == null) {
            return getId();
        }
        return hiddenId;
    }
    void updateHiddenId(final int hiddenId) {
        set(TrackableProperty.HiddenId, hiddenId);
    }

    int getBlockAdditional() {
        return get(TrackableProperty.BlockAdditional);
    }

    boolean getCanBlockAny() {
        return get(TrackableProperty.BlockAny);
    }

    void updateBlockAdditional(Card c) {
        set(TrackableProperty.BlockAdditional, c.canBlockAdditional());
        set(TrackableProperty.BlockAny, c.canBlockAny());
    }

    public boolean isRingBearer() {
        return get(TrackableProperty.IsRingBearer);
    }

    void updateRingBearer(Card c) {
        set(TrackableProperty.IsRingBearer, c.isRingBearer());
    }

    Set<String> getCantHaveKeyword() {
        return get(TrackableProperty.CantHaveKeyword);
    }

    void updateCantHaveKeyword(Card c) {
        Set<String> keywords = Sets.newTreeSet();
        for (Keyword k : c.getCantHaveKeyword()) {
            keywords.add(k.toString());
        }
        set(TrackableProperty.CantHaveKeyword, keywords);
    }

    private String zoneText = "";
    private String getZoneText() {
        return zoneText;
    }

    void updateZoneText(Card c) {
        if (c.getZone() != null && c.getZone().is(ZoneType.Sideboard) && c.getGame().getMaingame() != null) {
            Card parentCard = c.getOwner().getMappingMaingameCard(c);
            StringBuilder sb = new StringBuilder();
            int parentLevel = 0;
            while (parentCard != null) {
                parentLevel++;
                if (!parentCard.getZone().is(ZoneType.Sideboard)) {
                    sb.append('[');
                    if (parentCard.getGame().getMaingame() == null) {
                        sb.append(Localizer.getInstance().getMessage("lblMainGame"));
                    } else {
                        sb.append(Localizer.getInstance().getMessage("lblSubgame", parentLevel));
                    }
                    sb.append(": ");
                    sb.append(TextUtil.capitalize(parentCard.getZone().getZoneType().getTranslatedName()));
                    sb.append(']');
                    break;
                }
                parentCard = parentCard.getOwner().getMappingMaingameCard(parentCard);
            }
            zoneText = sb.toString();
        }
    }

    @Override
    public String toString() {
        String name = getName();
        String zone = getZoneText();
        if (getId() <= 0) { //if fake card, just return name
            return name;
        }

        if (name.isEmpty()) {
            CardStateView alternate = getAlternateState();
            if (alternate != null) {
                if (isFaceDown()) {
                    return "Face-down card (H" + getHiddenId() + ")";
                } else {
                    return getAlternateState().getName() + " (" + getId() + ")";
                }
            }
        }
        return (zone + ' ' + CardTranslation.getTranslatedName(name) + " (" + getId() + ")").trim();
    }

    public class CardStateView extends TrackableObject implements ITranslatable {
        private static final long serialVersionUID = 6673944200513430607L;

        private final CardStateName state;

        public CardStateView(final int id0, final CardStateName state0, final Tracker tracker) {
            super(id0, tracker);
            state = state0;
        }

        public String getDisplayId() {
            if (getState().equals(CardStateName.FaceDown)) {
                return "H" + getHiddenId();
            }
            final int id = getId();
            if (id > 0) {
                return String.valueOf(getId());
            }
            return StringUtils.EMPTY;
        }

        @Override
        public int hashCode() {
            return Objects.hash(getId(), state);
        }

        @Override
        public String toString() {
            return (getName() + " (" + getDisplayId() + ")").trim();
        }

        public CardView getCard() {
            return CardView.this;
        }

        public CardStateName getState() {
            return state;
        }

        public String getName() {
            return get(TrackableProperty.Name);
        }
        void updateName(CardState c) {
            Card card = c.getCard();
            setName(card.getName(c, false));

            if (CardView.this.getCurrentState() == this) {
                if (card != null) {
                    CardView.this.updateName(card);
                }
            }
        }
        private void setName(String name0) {
            set(TrackableProperty.Name, name0);
        }

        public ColorSet getColors() {
            return get(TrackableProperty.Colors);
        }
        public ColorSet getOriginalColors() {
            return get(TrackableProperty.OriginalColors);
        }
        public ColorSet getLeftSplitColors() {
            return get(TrackableProperty.LeftSplitColors);
        }
        public ColorSet getRightSplitColors() {
            return get(TrackableProperty.RightSplitColors);
        }
        void updateColors(Card c) {
            set(TrackableProperty.Colors, c.getColor());
        }
        void updateColors(CardState c) {
            set(TrackableProperty.Colors, c.getColor());
        }
        void setOriginalColors(Card c) {
            set(TrackableProperty.OriginalColors, c.getColor());
            if (c.isSplitCard()) {
                set(TrackableProperty.LeftSplitColors, c.getColor(c.getState(CardStateName.LeftSplit)));
                set(TrackableProperty.RightSplitColors, c.getColor(c.getState(CardStateName.RightSplit)));
            }
        }
        void updateHasChangeColors(boolean hasChangeColor) {
            set(TrackableProperty.HasChangedColors, hasChangeColor);
        }
        public boolean hasChangeColors() { return get(TrackableProperty.HasChangedColors); }
        public String getImageKey() {
            return getImageKey(null);
        }
        public String getImageKey(Iterable<PlayerView> viewers) {
            if (getState() == CardStateName.FaceDown) {
                return getCard().getFacedownImageKey();
            }
            if (canBeShownToAny(viewers)) {
                if (isCloned() && StaticData.instance().useSourceImageForClone()) {
                    return getBackup().getCurrentState().getImageKey(viewers);
                }
                return get(TrackableProperty.ImageKey);
            }
            return ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD);
        }
        /*
        * Use this for revealing purposes only
        * */
        public String getTrackableImageKey() {
            return get(TrackableProperty.ImageKey);
        }
        void updateImageKey(Card c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }
        void updateImageKey(CardState c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }

        public CardTypeView getType() {
            if (getState() != CardStateName.Original && isFaceDown() && !isInZone(EnumSet.of(ZoneType.Battlefield, ZoneType.Stack))) {
                return CardType.EMPTY;
            }
            return get(TrackableProperty.Type);
        }
        void updateType(CardState c) {
            CardTypeView type = c.getType();
            if (CardView.this.getCurrentState() == this) {
                Card card = c.getCard();
                if (card != null) {
                    type = type.getTypeWithChanges(card.getChangedCardTypes()); //TODO: find a better way to do this
                    updateRulesText(card.getRules(), type);
                }
            }
            set(TrackableProperty.Type, type);
        }

        public ManaCost getManaCost() {
            return get(TrackableProperty.ManaCost);
        }
        void updateManaCost(CardState c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }
        void updateManaCost(Card c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }

        public String getOracleText() {
            return get(TrackableProperty.OracleText);
        }
        void setOracleText(String oracleText) {
            set(TrackableProperty.OracleText, oracleText.replace("\\n", "\r\n\r\n").trim());
        }

        public String getFunctionalVariantName() {
            return get(TrackableProperty.FunctionalVariant);
        }
        void setFunctionalVariantName(String functionalVariant) {
            set(TrackableProperty.FunctionalVariant, functionalVariant);
        }

        public String getRulesText() {
            return get(TrackableProperty.RulesText);
        }
        void updateRulesText(CardRules rules, CardTypeView type) {
            String rulesText = null;

            if (type.isVanguard() && rules != null) {
                boolean decHand = rules.getHand() < 0;
                boolean decLife = rules.getLife() < 0;
                String handSize = Localizer.getInstance().getMessageorUseDefault("lblHandSize", "Hand Size")
                        + (!decHand ? ": +" : ": ") + rules.getHand();
                String startingLife = Localizer.getInstance().getMessageorUseDefault("lblStartingLife", "Starting Life")
                        + (!decLife ? ": +" : ": ") + rules.getLife();
                rulesText = handSize + "\r\n" + startingLife;
            }
            set(TrackableProperty.RulesText, rulesText);
        }

        public int getPower() {
            return get(TrackableProperty.Power);
        }
        void updatePower(Card c) {
            int num;
            if (hasPrintedPT() && !isCreature()) {
                // use printed value so user can still see it
                num = c.getCurrentPower();
            } else {
                num = c.getNetPower();
            }
            if (c.getCurrentState().getView() != this && c.getAlternateState() != null) {
                num = num - c.getBasePower() + c.getAlternateState().getBasePower();
            }
            set(TrackableProperty.Power, num);
        }
        void updatePower(CardState c) {
            Card card = c.getCard();
            if (card != null) {
                updatePower(card); //TODO: find a better way to do this
                return;
            }
            set(TrackableProperty.Power, c.getBasePower());
        }

        public int getToughness() {
            return get(TrackableProperty.Toughness);
        }
        void updateToughness(Card c) {
            int num;
            if (hasPrintedPT() && !isCreature()) {
                // use printed value so user can still see it
                num = c.getCurrentToughness();
            } else {
                num = c.getNetToughness();
            }
            if (c.getCurrentState().getView() != this && c.getAlternateState() != null) {
                num = num - c.getBaseToughness() + c.getAlternateState().getBaseToughness();
            }
            set(TrackableProperty.Toughness, num);
        }
        void updateToughness(CardState c) {
            Card card = c.getCard();
            if (card != null) {
                updateToughness(card); //TODO: find a better way to do this
                return;
            }
            set(TrackableProperty.Toughness, c.getBaseToughness());
        }

        public String getLoyalty() {
            return get(TrackableProperty.Loyalty);
        }
        void updateLoyalty(Card c) {
            if (c.isInZone(ZoneType.Battlefield)) {
                updateLoyalty(String.valueOf(c.getCurrentLoyalty()));
            } else {
                updateLoyalty(c.getCurrentState().getBaseLoyalty());
            }
        }
        void updateLoyalty(String loyalty) {
            set(TrackableProperty.Loyalty, loyalty);
        }
        void updateLoyalty(CardState c) {
            if (CardView.this.getCurrentState() == this) {
                Card card = c.getCard();
                if (card != null) {
                    if (card.isInZone(ZoneType.Battlefield)) {
                        updateLoyalty(card);
                    } else {
                        updateLoyalty(c.getBaseLoyalty());
                    }

                    return;
                }
            }
            set(TrackableProperty.Loyalty, "0"); //alternates don't need loyalty
        }

        public String getDefense() {
            return get(TrackableProperty.Defense);
        }
        void updateDefense(Card c) {
            if (c.isInZone(ZoneType.Battlefield)) {
                updateDefense(String.valueOf(c.getCurrentDefense()));
            } else {
                updateDefense(c.getCurrentState().getBaseDefense());
            }
        }
        void updateDefense(String defense) {
            set(TrackableProperty.Defense, defense);
        }
        void updateDefense(CardState c) {
            if (CardView.this.getCurrentState() == this) {
                Card card = c.getCard();
                if (card != null) {
                    if (card.isInZone(ZoneType.Battlefield)) {
                        updateDefense(card);
                    } else {
                        updateDefense(c.getBaseDefense());
                    }

                    return;
                }
            }
            updateDefense("0");
        }

        public Set<Integer> getAttractionLights() {
            return get(TrackableProperty.AttractionLights);
        }
        void updateAttractionLights(CardState c) {
            set(TrackableProperty.AttractionLights, c.getAttractionLights());
        }

        public boolean hasPrintedPT() {
            return get(TrackableProperty.HasPrintedPT);
        }
        void updateHasPrintedPT(boolean v) {
            set(TrackableProperty.HasPrintedPT, v);
        }

        public String getSetCode() {
            return get(TrackableProperty.SetCode);
        }
        void updateSetCode(CardState c) {
            set(TrackableProperty.SetCode, c.getSetCode());
        }

        public CardRarity getRarity() {
            return get(TrackableProperty.Rarity);
        }
        void updateRarity(CardState c) {
            set(TrackableProperty.Rarity, c.getRarity());
        }

        private int foilIndexOverride = -1;
        public int getFoilIndex() {
            if (foilIndexOverride >= 0) {
                return foilIndexOverride;
            }
            return get(TrackableProperty.FoilIndex);
        }
        void updateFoilIndex(Card c) {
            updateFoilIndex(c.getCurrentState());
        }
        void updateFoilIndex(CardState c) {
            set(TrackableProperty.FoilIndex, c.getFoil());
        }
        public void setFoilIndexOverride(int index0) {
            if (index0 < 0) {
                index0 = CardEdition.getRandomFoil(getSetCode());
            }
            foilIndexOverride = index0;
        }

        public String getKeywordKey() { return get(TrackableProperty.KeywordKey); }
        public String getProtectionKey() { return get(TrackableProperty.ProtectionKey); }
        public String getHexproofKey() { return get(TrackableProperty.HexproofKey); }
        public boolean hasAnnihilator() { return get(TrackableProperty.HasAnnihilator); }
        public boolean hasDeathtouch() { return get(TrackableProperty.HasDeathtouch); }
        public boolean hasToxic() { return get(TrackableProperty.HasToxic); }
        public boolean hasDevoid() { return get(TrackableProperty.HasDevoid); }
        public boolean hasDefender() { return get(TrackableProperty.HasDefender); }
        public boolean hasDivideDamage() { return get(TrackableProperty.HasDivideDamage); }
        public boolean hasDoubleStrike() { return get(TrackableProperty.HasDoubleStrike); }
        public boolean hasDoubleTeam() { return get(TrackableProperty.HasDoubleTeam); }
        public boolean hasExalted() { return get(TrackableProperty.HasExalted); }
        public boolean hasFirstStrike() { return get(TrackableProperty.HasFirstStrike); }
        public boolean hasFlying() { return get(TrackableProperty.HasFlying); }
        public boolean hasFear() { return get(TrackableProperty.HasFear); }
        public boolean hasHexproof() { return get(TrackableProperty.HasHexproof); }
        public boolean hasHorsemanship() { return get(TrackableProperty.HasHorsemanship); }
        public boolean hasWard() { return get(TrackableProperty.HasWard); }
        public boolean hasWither() { return get(TrackableProperty.HasWither); }
        public boolean hasIndestructible() { return get(TrackableProperty.HasIndestructible); }
        public boolean hasIntimidate() { return get(TrackableProperty.HasIntimidate); }
        public boolean hasLifelink() { return get(TrackableProperty.HasLifelink); }
        public boolean hasMenace() { return get(TrackableProperty.HasMenace); }
        public boolean hasReach() { return get(TrackableProperty.HasReach); }
        public boolean hasShadow() { return get(TrackableProperty.HasShadow); }
        public boolean hasShroud() { return get(TrackableProperty.HasShroud); }
        public boolean hasTrample() { return get(TrackableProperty.HasTrample); }
        public boolean hasVigilance() { return get(TrackableProperty.HasVigilance); }

        public boolean hasHaste() {
            return get(TrackableProperty.HasHaste);
        }
        public boolean hasInfect() {
            return get(TrackableProperty.HasInfect);
        }
        public boolean hasStorm() {
            return get(TrackableProperty.HasStorm);
        }
        public boolean hasLandwalk() {
            return get(TrackableProperty.HasLandwalk);
        }
        public boolean hasAftermath() {
            return get(TrackableProperty.HasAftermath);
        }

        public boolean origProduceAnyMana() {
            return get(TrackableProperty.OrigProduceAnyMana);
        }
        public boolean origProduceManaR() {
            return get(TrackableProperty.OrigProduceManaR);
        }
        public boolean origProduceManaG() {
            return get(TrackableProperty.OrigProduceManaG);
        }
        public boolean origProduceManaB() {
            return get(TrackableProperty.OrigProduceManaB);
        }
        public boolean origProduceManaU() {
            return get(TrackableProperty.OrigProduceManaU);
        }
        public boolean origProduceManaW() {
            return get(TrackableProperty.OrigProduceManaW);
        }
        public boolean origProduceManaC() {
            return get(TrackableProperty.OrigProduceManaC);
        }
        public int origCanProduceColoredMana() {
            return get(TrackableProperty.CountOrigProduceColoredMana);
        }
        public int countBasicLandTypes() {
            return get(TrackableProperty.CountBasicLandTypes);
        }

        public String getAbilityText() {
            return get(TrackableProperty.AbilityText);
        }
        void updateAbilityText(Card c, CardState state) {
            set(TrackableProperty.AbilityText, c.getAbilityText(state));
        }
        void updateKeywords(Card c, CardState state) {
            c.updateKeywordsCache(state);
            // deeper check for Idris
            set(TrackableProperty.HasAnnihilator, c.hasKeyword(Keyword.ANNIHILATOR, state) || state.getTriggers().anyMatch(t -> t.isKeyword(Keyword.ANNIHILATOR)));
            set(TrackableProperty.HasWard, c.hasKeyword(Keyword.WARD, state) || state.getTriggers().anyMatch(t -> t.isKeyword(Keyword.WARD)));
            set(TrackableProperty.HasDeathtouch, c.hasKeyword(Keyword.DEATHTOUCH, state));
            set(TrackableProperty.HasToxic, c.hasKeyword(Keyword.TOXIC, state));
            set(TrackableProperty.HasDevoid, c.hasKeyword(Keyword.DEVOID, state));
            set(TrackableProperty.HasDefender, c.hasKeyword(Keyword.DEFENDER, state));
            set(TrackableProperty.HasDivideDamage, c.hasKeyword("You may assign CARDNAME's combat damage divided as " +
                    "you choose among defending player and/or any number of creatures they control."));
            set(TrackableProperty.HasDoubleStrike, c.hasKeyword(Keyword.DOUBLE_STRIKE, state));
            set(TrackableProperty.HasExalted, c.hasKeyword(Keyword.EXALTED, state));
            set(TrackableProperty.HasFirstStrike, c.hasKeyword(Keyword.FIRST_STRIKE, state));
            set(TrackableProperty.HasFlying, c.hasKeyword(Keyword.FLYING, state));
            set(TrackableProperty.HasFear, c.hasKeyword(Keyword.FEAR, state));
            set(TrackableProperty.HasHexproof, c.hasKeyword(Keyword.HEXPROOF, state));
            set(TrackableProperty.HasHorsemanship, c.hasKeyword(Keyword.HORSEMANSHIP, state));
            set(TrackableProperty.HasWither, c.hasKeyword(Keyword.WITHER, state));
            set(TrackableProperty.HasIndestructible, c.hasKeyword(Keyword.INDESTRUCTIBLE, state));
            set(TrackableProperty.HasIntimidate, c.hasKeyword(Keyword.INTIMIDATE, state));
            set(TrackableProperty.HasLifelink, c.hasKeyword(Keyword.LIFELINK, state));
            set(TrackableProperty.HasMenace, c.hasKeyword(Keyword.MENACE, state));
            set(TrackableProperty.HasReach, c.hasKeyword(Keyword.REACH, state));
            set(TrackableProperty.HasShadow, c.hasKeyword(Keyword.SHADOW, state));
            set(TrackableProperty.HasShroud, c.hasKeyword(Keyword.SHROUD, state));
            set(TrackableProperty.HasTrample, c.hasKeyword(Keyword.TRAMPLE, state));
            set(TrackableProperty.HasVigilance, c.hasKeyword(Keyword.VIGILANCE, state));
            set(TrackableProperty.HasHaste, c.hasKeyword(Keyword.HASTE, state));
            set(TrackableProperty.HasInfect, c.hasKeyword(Keyword.INFECT, state));
            set(TrackableProperty.HasStorm, c.hasKeyword(Keyword.STORM, state));
            set(TrackableProperty.HasLandwalk, c.hasKeyword(Keyword.LANDWALK, state));
            set(TrackableProperty.HasAftermath, c.hasKeyword(Keyword.AFTERMATH, state));
            updateAbilityText(c, state);
            //set protectionKey for Icons
            set(TrackableProperty.ProtectionKey, c.getProtectionKey());
            //set hexproofKeys for Icons
            set(TrackableProperty.HexproofKey, c.getHexproofKey());
            //keywordkey
            set(TrackableProperty.KeywordKey, c.getKeywordKey());
            //update Trackable Mana Color for BG Colors
            updateManaColorBG(state);
        }
        void updateManaColorBG(CardState state) {
            boolean anyMana = false;
            boolean rMana = false;
            boolean gMana = false;
            boolean bMana = false;
            boolean uMana = false;
            boolean wMana = false;
            boolean cMana = false;
            int count = 0;
            int basicLandTypes = 0;
            for (SpellAbility sa : state.getManaAbilities()) {
                if (sa == null)
                    continue;
                for (AbilityManaPart mp : sa.getAllManaParts()) {
                    if (mp.isAnyMana()) {
                        anyMana = true;
                    }

                    String[] colorsProduced = mp.mana(sa).split(" ");

                    //todo improve this
                    for (final String s : colorsProduced) {
                        switch (s.toUpperCase()) {
                            case "R":
                                if (!rMana) {
                                    count += 1;
                                    rMana = true;
                                }
                                break;
                            case "G":
                                if (!gMana) {
                                    count += 1;
                                    gMana = true;
                                }
                                break;
                            case "B":
                                if (!bMana) {
                                    count += 1;
                                    bMana = true;
                                }
                                break;
                            case "U":
                                if (!uMana) {
                                    count += 1;
                                    uMana = true;
                                }
                                break;
                            case "W":
                                if (!wMana) {
                                    count += 1;
                                    wMana = true;
                                }
                                break;
                            case "C":
                                if (!cMana) {
                                    cMana = true;
                                }
                                break;
                        }
                    }
                }
            }
            if (isForest())
                basicLandTypes += 1;
            if (isMountain())
                basicLandTypes += 1;
            if (isSwamp())
                basicLandTypes += 1;
            if (isPlains())
                basicLandTypes += 1;
            if (isIsland())
                basicLandTypes += 1;
            set(TrackableProperty.CountBasicLandTypes, basicLandTypes);
            set(TrackableProperty.OrigProduceManaR, rMana);
            set(TrackableProperty.OrigProduceManaG, gMana);
            set(TrackableProperty.OrigProduceManaB, bMana);
            set(TrackableProperty.OrigProduceManaU, uMana);
            set(TrackableProperty.OrigProduceManaW, wMana);
            set(TrackableProperty.OrigProduceManaC, cMana);
            set(TrackableProperty.CountOrigProduceColoredMana, count);
            set(TrackableProperty.OrigProduceAnyMana, anyMana);
        }

        public boolean isBasicLand() {
            return getType().isBasicLand();
        }
        public boolean isCreature() {
            return getType().isCreature();
        }
        public boolean isLand() {
            return getType().isLand();
        }
        public boolean isPlane() {
            return getType().isPlane();
        }
        public boolean isPhenomenon() {
            return getType().isPhenomenon();
        }
        public boolean isPlaneswalker() {
            return getType().isPlaneswalker();
        }

        public boolean isBattle() {
            return getType().isBattle();
        }
        public boolean isMountain() {
            return getType().hasSubtype("Mountain");
        }
        public boolean isPlains() {
            return getType().hasSubtype("Plains");
        }
        public boolean isSwamp() {
            return getType().hasSubtype("Swamp");
        }
        public boolean isForest() {
            return getType().hasSubtype("Forest");
        }
        public boolean isIsland() {
            return getType().hasSubtype("Island");
        }
        public boolean isVehicle() {
            return getType().hasSubtype("Vehicle");
        }
        public boolean isArtifact() {
            return getType().isArtifact();
        }
        public boolean isEnchantment() {
            return getType().isEnchantment();
        }
        public boolean isSpaceCraft() {
            return getType().hasSubtype("Spacecraft");
        }
        public boolean isAttraction() {
            return getType().isAttraction();
        }
        public boolean isContraption() {
            return getType().isContraption();
        }

        @Override
        public String getTranslationKey() {
            String key = getName();
            String variant = getFunctionalVariantName();
            if(StringUtils.isNotEmpty(variant))
                key = key + " $" + variant;
            return key;
        }

        @Override
        public String getUntranslatedType() {
            return getType().toString();
        }

        @Override
        public String getUntranslatedOracle() {
            return getOracleText();
        }
    }

    //special methods for updating card and player properties as needed and returning the new collection
    Card setCard(Card oldCard, Card newCard, TrackableProperty key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    CardCollection setCards(CardCollection oldCards, CardCollection newCards, TrackableProperty key) {
        if (newCards == null || newCards.isEmpty()) { //avoid storing empty collections
            set(key, null);
            return null;
        }
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, TrackableProperty key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    CardCollection addCard(CardCollection oldCards, Card cardToAdd, TrackableProperty key) {
        if (cardToAdd == null) { return oldCards; }

        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        if (oldCards.add(cardToAdd)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                views = new TrackableCollection<>();
                views.add(cardToAdd.getView());
                set(key, views);
            }
            else if (views.add(cardToAdd.getView())) {
                flagAsChanged(key);
            }
        }
        return oldCards;
    }
    CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, TrackableProperty key) {
        if (cardsToAdd == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        boolean needFlagAsChanged = false;
        for (Card c : cardsToAdd) {
            if (c != null && oldCards.add(c)) {
                if (views == null) {
                    views = new TrackableCollection<>();
                    views.add(c.getView());
                    set(key, views);
                }
                else if (views.add(c.getView())) {
                    needFlagAsChanged = true;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection removeCard(CardCollection oldCards, Card cardToRemove, TrackableProperty key) {
        if (cardToRemove == null || oldCards == null) { return oldCards; }

        if (oldCards.remove(cardToRemove)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                set(key, null);
            } else if (views.remove(cardToRemove.getView())) {
                if (views.isEmpty()) {
                    set(key, null); //avoid keeping around an empty collection
                }
                else {
                    flagAsChanged(key);
                }
            }
            if (oldCards.isEmpty()) {
                oldCards = null; //avoid keeping around an empty collection
            }
        }
        return oldCards;
    }
    CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, TrackableProperty key) {
        if (cardsToRemove == null || oldCards == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        boolean needFlagAsChanged = false;
        for (Card c : cardsToRemove) {
            if (oldCards.remove(c)) {
                if (views == null) {
                    set(key, null);
                } else if (views.remove(c.getView())) {
                    if (views.isEmpty()) {
                        views = null;
                        set(key, null); //avoid keeping around an empty collection
                        needFlagAsChanged = false; //doesn't need to be flagged a second time
                    }
                    else {
                        needFlagAsChanged = true;
                    }
                }
                if (oldCards.isEmpty()) {
                    oldCards = null; //avoid keeping around an empty collection
                    break;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection clearCards(CardCollection oldCards, TrackableProperty key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }
    void updateMergeCollections(CardCollection cards) {
        TrackableCollection<CardView> views = get(TrackableProperty.MergedCardsCollection);
        boolean needFlagAsChanged = false;
        if (views == null) {
            views = new TrackableCollection<>();
            set(TrackableProperty.MergedCardsCollection, views);
        } else {
            if (!views.isEmpty())
                needFlagAsChanged = true;
            views.clear();
        }
        if (cards != null) {
            for (Card c : cards)
                if (views.add(c.getView()))
                    needFlagAsChanged = true;
        }
        if (needFlagAsChanged)
            flagAsChanged(TrackableProperty.MergedCardsCollection);
    }
}

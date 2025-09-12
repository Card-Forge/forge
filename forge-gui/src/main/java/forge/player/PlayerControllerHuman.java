package forge.player;

import com.google.common.collect.*;
import forge.LobbyPlayer;
import forge.StaticData;
import forge.ai.GameState;
import forge.ai.PlayerControllerAi;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.*;
import forge.game.card.CardView.CardStateView;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.event.GameEventPlayerStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.*;
import forge.game.player.actions.SelectCardAction;
import forge.game.player.actions.SelectPlayerAction;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementEffectView;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityView;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.PlayerZone;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.input.*;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.control.FControlGamePlayback;
import forge.gui.events.UiEventNextGameDecision;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SOptionPane;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.localinstance.achievements.AchievementCollection;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.trackable.TrackableCollection;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Sentry;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A prototype for player controller class
 * <p>
 * Handles phase skips for now.
 */
public class PlayerControllerHuman extends PlayerController implements IGameController {
    /**
     * Cards this player may look at right now, for example when searching a
     * library.
     */
    private boolean mayLookAtAllCards = false;
    private boolean disableAutoYields = false;

    private IGuiGame gui;

    protected final InputQueue inputQueue;
    protected final InputProxy inputProxy;

    private final Localizer localizer = Localizer.getInstance();

    protected Map<SpellAbilityView, SpellAbility> spellViewCache = null;

    public PlayerControllerHuman(final Game game0, final Player p, final LobbyPlayer lp) {
        super(game0, p, lp);
        inputProxy = new InputProxy(this);
        inputQueue = new InputQueue(game0.getView(), inputProxy);
    }

    public PlayerControllerHuman(final Player p, final LobbyPlayer lp, final PlayerControllerHuman owner) {
        super(owner.getGame(), p, lp);
        gui = owner.gui;
        inputProxy = owner.inputProxy;
        inputQueue = owner.getInputQueue();
    }

    public final IGuiGame getGui() {
        return gui;
    }

    public final void setGui(final IGuiGame gui) {
        this.gui = gui;
    }

    public final InputQueue getInputQueue() {
        return inputQueue;
    }

    public InputProxy getInputProxy() {
        return inputProxy;
    }

    public PlayerView getLocalPlayerView() {
        return player == null ? null : player.getView();
    }

    public boolean getDisableAutoYields() {
        return disableAutoYields;
    }
    public void setDisableAutoYields(final boolean disableAutoYields0) {
        disableAutoYields = disableAutoYields0;
    }

    @Override
    public boolean mayLookAtAllCards() {
        return mayLookAtAllCards;
    }
    /**
     * Set this to {@code true} to enable this player to see all cards any other
     * player can see.
     *
     * @param mayLookAtAllCards the mayLookAtAllCards to set
     */
    public void setMayLookAtAllCards(final boolean mayLookAtAllCards) {
        this.mayLookAtAllCards = mayLookAtAllCards;
    }

    private final ArrayList<Card> tempShownCards = new ArrayList<>();

    public <T> void tempShow(final Iterable<T> objects) {
        for (final T t : objects) {
            // assume you may see any card passed through here
            if (t instanceof Card) {
                tempShowCard((Card) t);
            } else if (t instanceof CardView) {
                tempShowCard(getCard((CardView) t));
            }
        }
    }

    private void tempShowCard(final Card c) {
        if (c == null) {
            return;
        }
        tempShownCards.add(c);
        c.addMayLookTemp(player);
    }

    @Override
    public void tempShowCards(final Iterable<Card> cards) {
        for (final Card c : cards) {
            tempShowCard(c);
        }
    }

    @Override
    public void endTempShowCards() {
        if (tempShownCards.isEmpty()) {
            return;
        }

        for (final Card c : tempShownCards) {
            c.removeMayLookTemp(player);
        }
        tempShownCards.clear();
    }

    /**
     * Uses GUI to learn which spell the player (human in our case) would like
     * to play
     */
    @Override
    public SpellAbility getAbilityToPlay(final Card hostCard, final List<SpellAbility> abilities,
                                         final ITriggerEvent triggerEvent) {
        // make sure another human player can't choose opponents cards just because he might see them
        if (triggerEvent != null && !hostCard.isInPlay() && !hostCard.getOwner().equals(player) &&
                !hostCard.getController().equals(player) &&
                // If player cast Shaman's Trance, they can play spells from any Graveyard (if other effects allow it to be cast)
                (!player.hasKeyword("Shaman's Trance") || !hostCard.isInZone(ZoneType.Graveyard))) {
            boolean noPermission = true;
            for (CardPlayOption o : hostCard.mayPlay(player)) {
                if (o.grantsZonePermissions()) {
                    noPermission = false;
                    break;
                }
            }
            for (SpellAbility sa : hostCard.getAllSpellAbilities()) {
                if (sa.hasParam("Activator")
                        && player.isValid(sa.getParam("Activator"), hostCard.getController(), hostCard, sa)) {
                    noPermission = false;
                    break;
                }
            }
            if (noPermission) {
                return null;
            }
        }
        //FIXME - on mobile gui it allows the card to cast from opponent hands issue #2127, investigate where the bug occurs before this method is called
        spellViewCache = SpellAbilityView.getMap(abilities);
        for (SpellAbility sa : abilities) {
            sa.getView().updateCanPlay(sa);
        }
        final SpellAbilityView resultView = getGui().getAbilityToPlay(CardView.get(hostCard),
                Lists.newArrayList(spellViewCache.keySet()), triggerEvent);
        return resultView == null ? null : spellViewCache.get(resultView);
    }

    @Override
    public void playSpellAbilityNoStack(final SpellAbility effectSA, final boolean canSetupTargets) {
        HumanPlay.playSpellAbilityNoStack(this, player, effectSA, !canSetupTargets);
    }

    @Override
    public List<PaperCard> sideboard(final Deck deck, final GameType gameType, String message) {
        CardPool sideboard = deck.get(DeckSection.Sideboard);
        if (sideboard == null) {
            // Use an empty cardpool instead of null for 75/0 sideboarding scenario.
            sideboard = new CardPool();
        }

        final CardPool main = deck.get(DeckSection.Main);

        final int mainSize = main.countAll();
        final int sbSize = sideboard.countAll();
        final int combinedDeckSize = mainSize + sbSize;

        final int deckMinSize = Math.min(mainSize, gameType.getDeckFormat().getMainRange().getMinimum());
        final Range<Integer> sbRange = gameType.getDeckFormat().getSideRange();
        // Limited doesn't have a sideboard max, so let the Main min take care of things.
        final int sbMax = sbRange == null ? combinedDeckSize : sbRange.getMaximum();

        List<PaperCard> newMain = null;

        // Skip sideboard loop if there are no sideboarding opportunities
        if (sbSize == 0 && mainSize == deckMinSize) {
            return null;
        }

        // conformance should not be checked here
        final boolean conform = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);
        do {
            if (newMain != null) {
                String errMsg;
                if (newMain.size() < deckMinSize) {
                    errMsg = TextUtil.concatNoSpace(localizer.getMessage("lblTooFewCardsMainDeck", String.valueOf(deckMinSize)));
                } else {
                    errMsg = TextUtil.concatNoSpace(localizer.getMessage("lblTooManyCardsSideboard", String.valueOf(sbMax)));
                }
                getGui().showErrorDialog(errMsg, localizer.getMessage("lblInvalidDeck"));
            }
            // Sideboard rules have changed for M14, just need to consider min
            // maindeck and max sideboard sizes
            // No longer need 1:1 sideboarding in non-limited formats
            List<PaperCard> resp = getGui().sideboard(sideboard, main, message);
            newMain = ObjectUtils.getIfNull(resp, main.toFlatList());
        } while (conform && (newMain.size() < deckMinSize || combinedDeckSize - newMain.size() > sbMax));

        return newMain;
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(final Card attacker, final CardCollectionView blockers, final CardCollectionView remaining,
                                                 final int damageDealt, final GameEntity defender, final boolean overrideOrder) {
        // Attacker is a poor name here, since the creature assigning damage
        // could just as easily be the blocker.
        final Map<Card, Integer> map = Maps.newHashMap();

        if ((attacker.hasKeyword(Keyword.TRAMPLE) && defender != null) || (blockers.size() > 1)
                || ((attacker.hasKeyword("You may assign CARDNAME's combat damage divided as you choose among " +
                "defending player and/or any number of creatures they control.")) && overrideOrder &&
                blockers.size() > 0) || (attacker.hasKeyword("Trample:Planeswalker") && defender instanceof Card)) {
            GameEntityViewMap<Card, CardView> gameCacheBlockers = GameEntityView.getMap(blockers);
            final CardView vAttacker = CardView.get(attacker);
            final GameEntityView vDefender = GameEntityView.get(defender);
            boolean maySkip = false;
            if (remaining != null && remaining.size() > 1 && attacker.isAttacking()) {
                maySkip = true;
            }
            final Map<CardView, Integer> result = getGui().assignCombatDamage(vAttacker, gameCacheBlockers.getTrackableKeys(), damageDealt,
                    vDefender, overrideOrder, maySkip);
            if (result == null) {
                return null;
            }
            for (final Entry<CardView, Integer> e : result.entrySet()) {
                if (gameCacheBlockers.containsKey(e.getKey())) {
                    map.put(gameCacheBlockers.get(e.getKey()), e.getValue());
                } else if (e.getKey() == null || e.getKey().getId() == -1) {
                    // null key or key with -1 means defender
                    map.put(null, e.getValue());
                }
            }
        } else {
            map.put(blockers.isEmpty() ? null : blockers.get(0), damageDealt);
        }
        return map;
    }

    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        final CardView vSource = CardView.get(effectSource);
        final Map<Object, Integer> vAffected = new HashMap<>(affected.size());
        for (Map.Entry<GameEntity, Integer> e : affected.entrySet()) {
            vAffected.put(GameEntityView.get(e.getKey()), e.getValue());
        }
        final Map<Object, Integer> vResult = getGui().assignGenericAmount(vSource, vAffected, shieldAmount, false,
                localizer.getMessage("lblShield"));
        Map<GameEntity, Integer> result = new HashMap<>();
        if (vResult != null) { //fix for netplay
            for (Map.Entry<GameEntity, Integer> e : affected.entrySet()) {
                if (vResult.containsKey(GameEntityView.get(e.getKey()))) {
                    result.put(e.getKey(), vResult.get(GameEntityView.get(e.getKey())));
                }
            }
        }
        return result;
    }

    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        final CardView vSource = CardView.get(sa.getHostCard());
        final Map<Object, Integer> vAffected = new LinkedHashMap<>(manaAmount);
        Integer maxAmount = different ? 1 : manaAmount;
        for (Byte color : colorSet) {
            vAffected.put(color, maxAmount);
        }
        final Map<Object, Integer> vResult = getGui().assignGenericAmount(vSource, vAffected, manaAmount, false,
                localizer.getMessage("lblMana").toLowerCase());
        Map<Byte, Integer> result = new HashMap<>();
        if (vResult != null) { //fix for netplay
            for (Byte color : colorSet) {
                if (vResult.containsKey(color)) {
                    result.put(color, vResult.get(color));
                }
            }
        }
        return result;
    }

    @Override
    public Integer announceRequirements(final SpellAbility ability, final String announce) {
        final Card host = ability.getHostCard();
        int max = Integer.MAX_VALUE;
        int xMin = 0;
        final boolean abXMin = ability.hasParam("XMin");
        Cost cost = ability.getPayCosts();

        if ("X".equals(announce)) {
            if (abXMin) xMin = Integer.parseInt(ability.getParam("XMin"));
            if (ability.hasParam("XMaxLimit")) {
                max = Math.min(max, AbilityUtils.calculateAmount(host, ability.getParam("XMaxLimit"), ability));
            }
            if (cost != null) {
                Integer costX = cost.getMaxForNonManaX(ability, player, false);
                if (costX != null && !player.getController().isFullControl(FullControlFlag.AllowPaymentStartWithMissingResources)) {
                    max = Math.min(max, costX);
                }
                if (cost.hasManaCost() && !abXMin) {
                    xMin = cost.getCostMana().getXMin();
                }
            }
        }
        final int min = xMin;

        if (ability.hasParam("AnnounceMax")) {
            max = Math.min(max, AbilityUtils.calculateAmount(host, ability.getParam("AnnounceMax"), ability));
        }

        if (ability.usesTargeting()) {
            // if announce is used as min targets, check what the max possible number would be
            if (announce.equals(ability.getTargetRestrictions().getMinTargets())) {
                max = Math.min(max, CardUtil.getValidCardsToTarget(ability).size());
            }
        }
        if (min > max) {
            return null;
        }

        String announceTitle = "X".equals(announce) ? ability.getParamOrDefault("XAnnounceTitle", announce) :
                ability.getParamOrDefault("AnnounceTitle", announce);
        if (cost.isMandatory()) {
            return chooseNumber(ability, localizer.getMessage("lblChooseAnnounceForCard", announceTitle,
                    CardTranslation.getTranslatedName(host.getName())), min, max);
        }
        if ("NumTimes".equals(announce)) {
            return getGui().getInteger(localizer.getMessage("lblHowManyTimesToPay", ability.getPayCosts().getTotalMana(),
                    CardTranslation.getTranslatedName(host.getName())), min, max, min + 9);
        }
        return getGui().getInteger(localizer.getMessage("lblChooseAnnounceForCard", announceTitle,
                CardTranslation.getTranslatedName(host.getName())), min, max, min + 9);
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(final SpellAbility sa, final int min, final int max,
                                                          final CardCollectionView valid, final String message) {
        return choosePermanentsTo(min, max, valid, message, localizer.getMessage("lblSacrifice").toLowerCase(), sa);
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(final SpellAbility sa, final int min, final int max,
                                                        final CardCollectionView valid, final String message) {
        return choosePermanentsTo(min, max, valid, message, localizer.getMessage("lblDestroy"), sa);
    }

    private CardCollectionView choosePermanentsTo(final int min, int max, final CardCollectionView valid,
                                                  final String message, final String action, final SpellAbility sa) {
        max = Math.min(max, valid.size());
        if (max <= 0) {
            return CardCollection.EMPTY;
        }

        String inpMessage = localizer.getMessage((min == 0 ? "lblSelectUpToNumTargetToAction" :
                "lblSelectNumTargetToAction"), message, action);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid, sa);
        inp.setMessage(inpMessage);
        inp.setCancelAllowed(min == 0);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    private boolean useSelectCardsInput(final FCollectionView<? extends GameEntity> sourceList, final SpellAbility sa) {
        //this can be used to stop zone select GUI when certain APIs would reveal illegal zone information
        //initially created for HeistEffect which showed library placement
        if (sa != null && ApiType.Heist.equals(sa.getApi())) return false;
        return useSelectCardsInput(sourceList);
    }

    private boolean useSelectCardsInput(final FCollectionView<? extends GameEntity> sourceList) {
        // can't use InputSelect from GUI thread (e.g., DevMode Tutor)
        if (FThreads.isGuiThread()) {
            return false;
        }

        // if UI_SELECT_FROM_CARD_DISPLAYS not set use InputSelect only for battlefield and player hand
        // if UI_SELECT_FROM_CARD_DISPLAYS set and using desktop GUI use InputSelect for any zone that can be shown
        for (final GameEntity c : sourceList) {
            if (c instanceof Player) {
                continue;
            }

            if (!(c instanceof Card)) {
                return false;
            }
            final Zone cz = ((Card) c).getZone();
            // Don't try to draw the UI point of a card if it doesn't exist in any zone.
            if (cz == null) {
                return false;
            }

            final boolean useUiPointAtCard =
                    (FModel.getPreferences().getPrefBoolean(FPref.UI_SELECT_FROM_CARD_DISPLAYS) && (!GuiBase.getInterface().isLibgdxPort())) ?
                            (cz.is(ZoneType.Battlefield) || cz.is(ZoneType.Hand) || cz.is(ZoneType.Library) ||
                                    cz.is(ZoneType.Graveyard) || cz.is(ZoneType.Exile) || cz.is(ZoneType.Flashback) ||
                                    cz.is(ZoneType.Command) || cz.is(ZoneType.Sideboard)) :
                            (cz.is(ZoneType.Hand, player) || cz.is(ZoneType.Battlefield));
            if (!useUiPointAtCard) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CardCollectionView chooseCardsForEffect(final CardCollectionView sourceList, final SpellAbility sa,
                                                   final String title, final int min, final int max, final boolean isOptional, Map<String, Object> params) {
        // If only one card to choose, use a dialog box.
        // Otherwise, use the order dialog to be able to grab multiple cards in one shot

        if (min == 1 && max == 1) {
            final Card singleChosen = chooseSingleEntityForEffect(sourceList, sa, title, isOptional, params);
            return singleChosen == null ? CardCollection.EMPTY : new CardCollection(singleChosen);
        }

        final CardCollection choices = new CardCollection();
        if (sourceList.isEmpty()) {
            return choices;
        }

        getGui().setPanelSelection(CardView.get(sa.getHostCard()));

        if (useSelectCardsInput(sourceList)) {
            tempShowCards(sourceList);
            final InputSelectCardsFromList sc = new InputSelectCardsFromList(this, min, max, sourceList, sa);
            sc.setMessage(title);
            sc.setCancelAllowed(isOptional);
            sc.showAndWait();
            endTempShowCards();
            return new CardCollection(sc.getSelected());
        }

        tempShowCards(sourceList);
        GameEntityViewMap<Card, CardView> gameCachechoose = GameEntityView.getMap(sourceList);
        List<CardView> views = getGui().many(title, localizer.getMessage("lblChosenCards"), min, max,
                gameCachechoose.getTrackableKeys(), CardView.get(sa.getHostCard()));
        endTempShowCards();
        gameCachechoose.addToList(views, choices);
        return choices;
    }

    /**
     * IDs of Contraptions that have been cranked previously, and will default to the "cranked" column next time their
     * sprocket is cranked.
     */
    private final Set<Integer> savedCrankedIDs = new HashSet<>();

    @Override
    public List<Card> chooseContraptionsToCrank(List<Card> contraptions) {
        if(contraptions.isEmpty())
            return contraptions;

        tempShowCards(contraptions);
        GameEntityViewMap<Card, CardView> gameCacheChoose = GameEntityView.getMap(contraptions);
        TrackableCollection<CardView> viewList = gameCacheChoose.getTrackableKeys();

        //Contraptions that were cranked previously will start in the cranked column when the dialog is shown.
        List<CardView> cranked = new ArrayList<>(), uncranked = new ArrayList<>();
        for(CardView c : viewList) {
            int id = c.getId();
            (savedCrankedIDs.contains(id) ? cranked : uncranked).add(c);
        }

        List<CardView> views = getGui().many(localizer.getMessage("lblChooseCrank"),
                localizer.getMessage("lblCranked"), -1, -1, uncranked, cranked, null);
        endTempShowCards();

        //If any were on the saved cranked list before but aren't cranked now, remove them from the saved list.
        cranked.stream().filter(v -> !views.contains(v)).map(CardView::getId).forEach(savedCrankedIDs::remove);
        //Add any that were cranked this time to the saved list.
        views.stream().map(CardView::getId).forEach(savedCrankedIDs::add);

        List<Card> choices = new CardCollection();
        gameCacheChoose.addToList(views, choices);

        return choices;
    }


    @Override
    public boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested) {
        // This is like a mini-announce X
        String title = String.format("%s trying to cast (%s) How much would you like to help pay for Assist? (Max: %s)", sa.getActivatingPlayer(), sa, max);
        int willPay = chooseNumber(sa, title, 0, max);

        if (willPay <= 0) {
            // Just because you choose not to help, doesn't mean we should cancel the spell
            return true;
        }

        ManaCost manaCost = ManaCost.get(willPay);
        ManaCostBeingPaid assistCost = new ManaCostBeingPaid(manaCost);

        InputPayMana inpPayment = new InputPayManaOfCostPayment(this, assistCost, sa, this.getPlayer(), null, true);
        inpPayment.setMessagePrefix("Paying for assist - ");
        inpPayment.showAndWait();

        if (inpPayment.isPaid()) {
            // Apply payments from assistCost to cost
            // If cost is canceled, how do we make sure mana gets undone?

            cost.decreaseGenericMana(willPay);
            return true;
        } else if (sa.getHostCard().getGame().EXPERIMENTAL_RESTORE_SNAPSHOT) {
            // Let's roll it back!
            return false;
        } else {
            System.out.println("Assist rollback may not work well without experimental restore snapshot enabled");
            return false;
        }
    }

    @Override
    public Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max) {
        return chooseSingleEntityForEffect(optionList, null, sa, title, true, null, null);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(final FCollectionView<T> optionList,
                                                                final DelayedReveal delayedReveal, final SpellAbility sa, final String title, final boolean isOptional,
                                                                final Player targetedPlayer, Map<String, Object> params) {
        // Human is supposed to read the message and understand from it what to choose
        if (optionList.isEmpty()) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(),
                        delayedReveal.getMessagePrefix());
            }
            return null;
        }
        if (!isOptional && optionList.size() == 1) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(),
                        delayedReveal.getMessagePrefix());
            }
            return Iterables.getFirst(optionList, null);
        }

        tempShow(optionList);
        if (delayedReveal != null) {
            tempShow(delayedReveal.getCards());
        }

        if (useSelectCardsInput(optionList, sa)) {
            final InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<>(this, isOptional ? 0 : 1, 1,
                    optionList, sa);
            input.setCancelAllowed(isOptional);
            input.setMessage(MessageUtil.formatMessage(title, player, targetedPlayer));
            input.showAndWait();
            endTempShowCards();
            return Iterables.getFirst(input.getSelected(), null);
        }

        GameEntityViewMap<T, GameEntityView> gameCacheChoose = GameEntityView.getMap(optionList);
        final GameEntityView result = getGui().chooseSingleEntityForEffect(title,
                gameCacheChoose.getTrackableKeys(), delayedReveal, isOptional);
        endTempShowCards();

        if (result == null || !gameCacheChoose.containsKey(result)) {
            return null;
        }
        return gameCacheChoose.get(result);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(final FCollectionView<T> optionList, final int min, final int max,
                                                                  final DelayedReveal delayedReveal, final SpellAbility sa, final String title, final Player targetedPlayer, Map<String, Object> params) {
        // useful details for debugging problems with the mass select logic
        Sentry.setExtra("Card", sa.getCardView().toString());
        Sentry.setExtra("SpellAbility", sa.toString());

        // Human is supposed to read the message and understand from it what to choose
        if (optionList.isEmpty()) {
            if (delayedReveal != null) {
                reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(),
                        delayedReveal.getMessagePrefix());
            }
            return Lists.newArrayList();
        }

        if (delayedReveal != null) {
            tempShow(delayedReveal.getCards());
        }

        tempShow(optionList);
        if (useSelectCardsInput(optionList)) {
            final InputSelectEntitiesFromList<T> input = new InputSelectEntitiesFromList<>(this, min, max, optionList,
                    sa);
            input.setCancelAllowed(min == 0);
            input.setMessage(MessageUtil.formatMessage(title, player, targetedPlayer));
            input.showAndWait();
            endTempShowCards();
            return (List<T>) input.getSelected();
        }

        GameEntityViewMap<T, GameEntityView> gameCacheEntity = GameEntityView.getMap(optionList);
        final List<GameEntityView> views = getGui().chooseEntitiesForEffect(title, gameCacheEntity.getTrackableKeys(), min, max, delayedReveal);
        endTempShowCards();

        List<T> results = Lists.newArrayList();

        if (views != null) {
            gameCacheEntity.addToList(views, results);
        }

        return results;
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final int min, final int max) {
        if (min >= max) {
            return min;
        }
        // todo check for X cost or any max value for optional costs like multikicker, etc to determine the correct max value,
        // fixes crash for word of command OutOfMemoryError when selecting a card with announce X or Multikicker since
        // it will build from 0 to Integer.MAX_VALUE...
        if (max == Integer.MAX_VALUE) {
            Integer choice = getGui().getInteger(title, min, max, 9);
            if (choice != null)
                return choice;
            else
                return 0;
        } else {
            final ImmutableList.Builder<Integer> choices = ImmutableList.builder();
            int size = max - min;
            for (int i = 0; i <= size; i++) {
                choices.add(i + min);
            }
            return getGui().one(title, choices.build());
        }
    }

    @Override
    public int chooseNumber(final SpellAbility sa, final String title, final List<Integer> choices,
                            final Player relatedPlayer) {
        return getGui().one(title, choices);
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(final List<SpellAbility> spells, final SpellAbility sa,
                                                   final String title, Map<String, Object> params) {
        if (spells.size() < 2) {
            return Iterables.getFirst(spells, null);
        }

        // Show the card that asked for this choice
        getGui().setCard(CardView.get(sa.getHostCard()));

        // create a mapping between a spell's view and the spell itself
        Map<SpellAbilityView, SpellAbility> spellViewCache = SpellAbilityView.getMap(spells);
        Object choice = getGui().one(title, Lists.newArrayList(spellViewCache.keySet()));

        // Human is supposed to read the message and understand from it what to choose
        return spellViewCache.get(choice);
    }

    @Override
    public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title, int num, Map<String, Object> params) {
        List<SpellAbility> result = Lists.newArrayList();
        // create a mapping between a spell's view and the spell itself
        Map<SpellAbilityView, SpellAbility> spellViewCache = SpellAbilityView.getMap(spells);

        if(sa.hasParam("ShowCurrentCard"))
        {
            Card current = Iterables.getFirst(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("ShowCurrentCard"), sa), null);
            if(current != null) {
                String promptCurrent = localizer.getMessage("lblCurrentCard") + ": " + current;
                title = title + "\n" + promptCurrent;
            }
        }

        //override generic
        List<SpellAbilityView> chosen = getGui().getChoices(title, num, num, Lists.newArrayList(spellViewCache.keySet()));

        for (SpellAbilityView view : chosen) {
            if (spellViewCache.containsKey(view)) {
                result.add(spellViewCache.get(view));
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#confirmAction(forge.gui.card.spellability.
     * SpellAbility, java.lang.String, java.lang.String)
     */
    @Override
    public boolean confirmAction(final SpellAbility sa, final PlayerActionConfirmMode mode, final String message,
                                 List<String> options, Card cardToShow, Map<String, Object> params) {
        // Another card should be displayed in the prompt on mouse over rather than the SA source
        if (cardToShow != null) {
            tempShowCard(cardToShow);
            boolean result = options.isEmpty() ? InputConfirm.confirm(this, cardToShow.getView(), sa, message)
                    : InputConfirm.confirm(this, cardToShow.getView(), message, true, options);
            endTempShowCards();
            return result;
        }

        // The general case: display the source of the SA in the prompt on mouse over
        return options.isEmpty() ? InputConfirm.confirm(this, sa, message) :
                InputConfirm.confirm(this, sa.getHostCard().getView(), sa, message, true, options);
    }

    @Override
    public boolean confirmBidAction(final SpellAbility sa, final PlayerActionConfirmMode bidlife, final String string,
                                    final int bid, final Player winner) {
        return InputConfirm.confirm(this, sa, string + " " + localizer.getMessage("lblHighestBidder") + " " + winner);
    }

    @Override
    public boolean confirmStaticApplication(final Card hostCard, PlayerActionConfirmMode mode, final String message, final String logic) {
        return InputConfirm.confirm(this, CardView.get(hostCard), message);
    }

    @Override
    public boolean confirmTrigger(final WrappedAbility wrapper) {
        final SpellAbility sa = wrapper.getWrappedAbility();
        final Trigger regtrig = wrapper.getTrigger();
        if (getGui().shouldAlwaysAcceptTrigger(regtrig.getId())) {
            return true;
        }
        if (getGui().shouldAlwaysDeclineTrigger(regtrig.getId())) {
            return false;
        }

        // triggers with costs can always be declined by not paying the cost
        if (sa.hasParam("Cost") && !sa.getParam("Cost").equals("0")) {
            return true;
        }

        final StringBuilder buildQuestion = new StringBuilder(localizer.getMessage("lblUseTriggeredAbilityOf") + " ");
        buildQuestion.append(regtrig.getHostCard().toString()).append("?");
        if (!FModel.getPreferences().getPrefBoolean(FPref.UI_COMPACT_PROMPT)
                && !FModel.getPreferences().getPrefBoolean(FPref.UI_DETAILED_SPELLDESC_IN_PROMPT)) {
            // append trigger description unless prompt is compact or detailed descriptions are on
            buildQuestion.append("\n(");
            buildQuestion.append(regtrig.toString());
            buildQuestion.append(")");
        }
        final Map<AbilityKey, Object> tos = sa.getTriggeringObjects();
        if (tos.containsKey(AbilityKey.Attacker)) {
            buildQuestion.append("\n").append(localizer.getMessage("lblAttacker")).append(": ").append(tos.get(AbilityKey.Attacker));
        }
        if (tos.containsKey(AbilityKey.Card)) {
            final Card card = (Card) tos.get(AbilityKey.Card);
            if (card != null && (card.getController() == player || getGame().getZoneOf(card) == null
                    || getGame().getZoneOf(card).getZoneType().isKnown())) {
                buildQuestion.append("\n").append(localizer.getMessage("lblTriggeredby")).append(": ").append(tos.get(AbilityKey.Card));
            }
        }
        if (GuiBase.getInterface().isLibgdxPort()) {
            CardView cardView;
            SpellAbilityView spellAbilityView = wrapper.getView();
            if (spellAbilityView != null) //updated view
                cardView = spellAbilityView.getHostCard();
            else
                cardView = wrapper.getCardView();
            return this.getGui().confirm(cardView, buildQuestion.toString().replaceAll("\n", " "));
        } else {
            final InputConfirm inp = new InputConfirm(this, buildQuestion.toString(), wrapper);
            inp.showAndWait();
            return inp.getResult();
        }
    }

    @Override
    public Player chooseStartingPlayer(final boolean isFirstGame) {
        String prompt = null;
        if (isFirstGame) {
            prompt = localizer.getMessage("lblYouHaveWonTheCoinToss", player.getName());
        } else {
            prompt = localizer.getMessage("lblYouLostTheLastGame", player.getName());
        }

        if (getGame().getPlayers().size() == 2) {
            prompt += "\n\n" + localizer.getMessage("lblWouldYouLiketoPlayorDraw");
            final InputConfirm inp = new InputConfirm(this, prompt, localizer.getMessage("lblPlay"), localizer.getMessage("lblDraw"));
            inp.showAndWait();
            return inp.getResult() ? this.player : this.player.getOpponents().get(0);
        }

        prompt += "\n\n" + localizer.getMessage("lblWhoWouldYouLiketoStartthisGame");
        final InputSelectEntitiesFromList<Player> input = new InputSelectEntitiesFromList<>(this, 1, 1, getGame().getPlayersInTurnOrder());
        input.setMessage(prompt);
        input.showAndWait();
        return input.getFirstSelected();
    }

    @Override
    public CardCollection orderBlockers(final Card attacker, final CardCollection blockers) {
        GameEntityViewMap<Card, CardView> gameCacheBlockers = GameEntityView.getMap(blockers);
        final CardView vAttacker = CardView.get(attacker);
        getGui().setPanelSelection(vAttacker);
        List<CardView> chosen = getGui().order(localizer.getMessage("lblChooseDamageOrderFor", CardTranslation.getTranslatedName(vAttacker.getName())), localizer.getMessage("lblDamagedFirst"),
                gameCacheBlockers.getTrackableKeys(), vAttacker);
        CardCollection chosenCards = new CardCollection();
        gameCacheBlockers.addToList(chosen, chosenCards);
        return chosenCards;
    }

    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        GameEntityViewMap<Card, CardView> gameCacheExert = GameEntityView.getMap(attackers);
        List<CardView> chosen = getGui().order(localizer.getMessage("lblExertAttackersConfirm"), localizer.getMessage("lblExerted"),
                0, gameCacheExert.size(), gameCacheExert.getTrackableKeys(), null, null, false);

        List<Card> chosenCards = new CardCollection();
        gameCacheExert.addToList(chosen, chosenCards);
        return chosenCards;
    }

    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        GameEntityViewMap<Card, CardView> gameCacheExert = GameEntityView.getMap(attackers);
        List<CardView> chosen = getGui().order(localizer.getMessage("lblEnlistAttackersConfirm"), localizer.getMessage("lblEnlisted"),
                0, gameCacheExert.size(), gameCacheExert.getTrackableKeys(), null, null, false);

        List<Card> chosenCards = new CardCollection();
        gameCacheExert.addToList(chosen, chosenCards);
        return chosenCards;
    }

    @Override
    public List<CostPart> orderCosts(List<CostPart> costs) {
        if (!isFullControl(FullControlFlag.ChooseCostOrder) || costs.size() < 2) {
            return costs;
        }
        List<CostPart> chosen = getGui().order(localizer.getMessage("lblOrderCosts"), localizer.getMessage("lblPayFirst"),
                0, 0, costs, null, null, false);
        return chosen;
    }

    @Override
    public CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers) {
        GameEntityViewMap<Card, CardView> gameCacheBlockers = GameEntityView.getMap(oldBlockers);
        final CardView vAttacker = CardView.get(attacker);
        getGui().setPanelSelection(vAttacker);
        List<CardView> chosen = getGui().insertInList(
                localizer.getMessage("lblChooseBlockerAfterWhichToPlaceAttackert", CardTranslation.getTranslatedName(vAttacker.getName())),
                CardView.get(blocker), CardView.getCollection(oldBlockers));
        CardCollection chosenCards = new CardCollection();
        gameCacheBlockers.addToList(chosen, chosenCards);
        return chosenCards;
    }

    @Override
    public CardCollection orderAttackers(final Card blocker, final CardCollection attackers) {
        GameEntityViewMap<Card, CardView> gameCacheAttackers = GameEntityView.getMap(attackers);
        final CardView vBlocker = CardView.get(blocker);
        getGui().setPanelSelection(vBlocker);
        List<CardView> chosen = getGui().order(localizer.getMessage("lblChooseDamageOrderFor", CardTranslation.getTranslatedName(vBlocker.getName())), localizer.getMessage("lblDamagedFirst"),
                CardView.getCollection(attackers), vBlocker);
        CardCollection chosenCards = new CardCollection();
        gameCacheAttackers.addToList(chosen, chosenCards);
        return chosenCards;
    }

    @Override
    public void reveal(final CardCollectionView cards, final ZoneType zone, final Player owner, String message, boolean addSuffix) {
        reveal(cards, zone, PlayerView.get(owner), message, addSuffix);
    }

    @Override
    public void reveal(final List<CardView> cards, final ZoneType zone, final PlayerView owner, String message, boolean addSuffix) {
        reveal(getCardList(cards), zone, owner, message, addSuffix);
    }

    protected void reveal(final CardCollectionView cards, final ZoneType zone, final PlayerView owner, String message, boolean addSuffix) {
        if (StringUtils.isBlank(message)) {
            message = localizer.getMessage("lblLookCardInPlayerZone", "{player's}", zone.getTranslatedName().toLowerCase());
        } else {
            if (addSuffix) message += " " + localizer.getMessage("lblPlayerZone", "{player's}", zone.getTranslatedName().toLowerCase());
        }
        final String fm = MessageUtil.formatMessage(message, getLocalPlayerView(), owner);
        if (!cards.isEmpty()) {
            tempShowCards(cards);
            TrackableCollection<CardView> collection = CardView.getCollection(cards);
            getGui().reveal(fm, collection);
            getGui().updateRevealedCards(collection);
            endTempShowCards();
        } else {
            getGui().message(MessageUtil.formatMessage(localizer.getMessage("lblThereNoCardInPlayerZone", "{player's}", zone.getTranslatedName().toLowerCase()),
                    getLocalPlayerView(), owner), fm);
        }
    }

    public List<Card> manipulateCardList(final String title, final Iterable<Card> cards, final Iterable<Card> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
        GameEntityViewMap<Card, CardView> gameCacheManipulate = GameEntityView.getMap(cards);
        gameCacheManipulate.putAll(manipulable);
        List<CardView> views = getGui().manipulateCardList(title, CardView.getCollection(cards), CardView.getCollection(manipulable), toTop, toBottom, toAnywhere);
        return gameCacheManipulate.addToList(views, new CardCollection());
    }

    public ImmutablePair<CardCollection, CardCollection> arrangeForMove(final String title, final FCollectionView<Card> cards, final List<Card> manipulable, final boolean topOK, final boolean bottomOK) {
        List<Card> result = manipulateCardList(title, cards, manipulable, topOK, bottomOK, false);
        CardCollection toBottom = new CardCollection();
        CardCollection toTop = new CardCollection();
        for (int i = 0; i < cards.size() && manipulable.contains(result.get(i)); i++) {
            toTop.add(result.get(i));
        }
        if (toTop.size() < cards.size()) { // the top isn't everything
            for (int i = result.size() - 1; i >= 0 && manipulable.contains(result.get(i)); i--) {
                toBottom.add(result.get(i));
            }
        }
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(final CardCollection topN) {
        CardCollection toBottom = null;
        CardCollection toTop = null;

        tempShowCards(topN);
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_SELECT_FROM_CARD_DISPLAYS) &&
                (!GuiBase.getInterface().isLibgdxPort()) && (!GuiBase.isNetworkplay())) { //prevent crash for desktop vs mobile port it will crash the netplay since mobile doesnt have manipulatecardlist, send the alternate below
            CardCollectionView cardList = player.getCardsIn(ZoneType.Library);
            ImmutablePair<CardCollection, CardCollection> result =
                    arrangeForMove(localizer.getMessage("lblMoveCardstoToporBbottomofLibrary"), cardList, topN, true, true);
            toTop = result.getLeft();
            toBottom = result.getRight();
        } else {
            if (topN.size() == 1) {
                if (willPutCardOnTop(topN.get(0))) {
                    toTop = topN;
                } else {
                    toBottom = topN;
                }
            } else {
                GameEntityViewMap<Card, CardView> cardCacheScry = GameEntityView.getMap(topN);

                toBottom = new CardCollection();
                List<CardView> views = getGui().many(localizer.getMessage("lblSelectCardsToBeOutOnTheBottomOfYourLibrary"),
                        localizer.getMessage("lblCardsToPutOnTheBottom"), -1, cardCacheScry.getTrackableKeys(), null);
                cardCacheScry.addToList(views, toBottom);

                topN.removeAll(toBottom);
                if (topN.isEmpty()) {
                    toTop = null;
                } else if (topN.size() == 1) {
                    toTop = topN;
                } else {
                    GameEntityViewMap<Card, CardView> cardCacheOrder = GameEntityView.getMap(topN);
                    toTop = new CardCollection();
                    views = getGui().order(localizer.getMessage("lblArrangeCardsToBePutOnTopOfYourLibrary"),
                            localizer.getMessage("lblTopOfLibrary"), cardCacheOrder.getTrackableKeys(), null);
                    cardCacheOrder.addToList(views, toTop);
                }
            }
        }
        endTempShowCards();
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(final CardCollection topN) {
        CardCollection toGrave = null;
        CardCollection toTop = null;

        tempShowCards(topN);
        if (topN.size() == 1) {
            final Card c = topN.getFirst();
            final CardView view = CardView.get(c);

            tempShowCard(c);
            getGui().setCard(view);
            boolean result = false;
            result = InputConfirm.confirm(this, view, localizer.getMessage("lblPutCardsOnTheTopLibraryOrGraveyard", CardTranslation.getTranslatedName(view.getName())),
                    true, ImmutableList.of(localizer.getMessage("lblLibrary"), localizer.getMessage("lblGraveyard")));
            if (result) {
                toTop = topN;
            } else {
                toGrave = topN;
            }
        } else {
            GameEntityViewMap<Card, CardView> gameCacheSurveil = GameEntityView.getMap(topN);
            toGrave = new CardCollection();
            List<CardView> views = getGui().many(localizer.getMessage("lblSelectCardsToBePutIntoTheGraveyard"),
                    localizer.getMessage("lblCardsToPutInTheGraveyard"), -1, gameCacheSurveil.getTrackableKeys(), null);
            gameCacheSurveil.addToList(views, toGrave);
            topN.removeAll(toGrave);
            if (topN.isEmpty()) {
                toTop = null;
            } else if (topN.size() == 1) {
                toTop = topN;
            } else {
                GameEntityViewMap<Card, CardView> cardCacheOrder = GameEntityView.getMap(topN);
                toTop = new CardCollection();
                views = getGui().order(localizer.getMessage("lblArrangeCardsToBePutOnTopOfYourLibrary"),
                        localizer.getMessage("lblTopOfLibrary"), cardCacheOrder.getTrackableKeys(), null);
                cardCacheOrder.addToList(views, toTop);
            }
        }
        endTempShowCards();
        return ImmutablePair.of(toTop, toGrave);
    }

    @Override
    public boolean willPutCardOnTop(final Card c) {
        final CardView view = CardView.get(c);

        tempShowCard(c);
        getGui().setCard(c.getView());

        boolean result = false;
        result = InputConfirm.confirm(this, view, localizer.getMessage("lblPutCardOnTopOrBottomLibrary", CardTranslation.getTranslatedName(view.getName())),
                true, ImmutableList.of(localizer.getMessage("lblTop"), localizer.getMessage("lblBottom")));

        endTempShowCards();
        return result;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(final CardCollectionView cards, final ZoneType destinationZone, final SpellAbility source) {
        if (source == null || source.getApi() != ApiType.ReorderZone) {
            if (destinationZone == ZoneType.Graveyard) {
                switch (FModel.getPreferences().getPref(FPref.UI_ALLOW_ORDER_GRAVEYARD_WHEN_NEEDED)) {
                    case ForgeConstants.GRAVEYARD_ORDERING_NEVER:
                        // No ordering is ever performed by the player except when done by effect (AF ReorderZone)
                        return cards;
                    case ForgeConstants.GRAVEYARD_ORDERING_OWN_CARDS:
                        // Order only if the relevant cards controlled by the player determine the potential necessity for it
                        if (!getGame().isGraveyardOrdered(player)) {
                            return cards;
                        }
                        break;
                    case ForgeConstants.GRAVEYARD_ORDERING_ALWAYS:
                        // Always order cards, no matter if there is a determined case for it or not
                        break;
                    default:
                        // By default, assume no special ordering necessary (but should not get here unless the preference file is borked)
                        return cards;
                }
            }
        }

        tempShowCards(cards);
        GameEntityViewMap<Card, CardView> gameCacheMove = GameEntityView.getMap(cards);
        List<CardView> choices = gameCacheMove.getTrackableKeys();

        boolean topOfDeck = destinationZone.isDeck()
                && (source == null
                    || !source.hasParam("LibraryPosition")
                    || AbilityUtils.calculateAmount(source.getHostCard(), source.getParam("LibraryPosition"), source) >= 0);

        switch (destinationZone) {
            case Library:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoLibrary"), localizer.getMessage(topOfDeck ? "lblClosestToTop" : "lblClosestToBottom"), choices, null);
                break;
            case Battlefield:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutOntoBattlefield"), localizer.getMessage("lblPutFirst"), choices, null);
                break;
            case Graveyard:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoGraveyard"), localizer.getMessage("lblClosestToBottom"), choices, null);
                break;
            case Exile:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoExile"), localizer.getMessage("lblPutFirst"), choices, null);
                break;
            case PlanarDeck:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoPlanarDeck"), localizer.getMessage(topOfDeck ? "lblClosestToTop" : "lblClosestToBottom"), choices, null);
                break;
            case SchemeDeck:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoSchemeDeck"), localizer.getMessage(topOfDeck ? "lblClosestToTop" : "lblClosestToBottom"), choices, null);
                break;
            case AttractionDeck:
            case ContraptionDeck:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCardsPutIntoExtraDeck"), localizer.getMessage(topOfDeck ? "lblClosestToTop" : "lblClosestToBottom"), choices, null);
            case Stack:
                choices = getGui().order(localizer.getMessage("lblChooseOrderCopiesCast"), localizer.getMessage("lblPutFirst"), choices, null);
                break;
            case None: //for when we want to order but don't really want to move the cards
                choices = getGui().order(localizer.getMessage("lblChooseOrderCards"), localizer.getMessage("lblPutFirst"), choices, null);
                break;
            default:
                System.out.println("ZoneType " + destinationZone + " - Not Ordered");
                endTempShowCards();
                return cards;
        }
        endTempShowCards();
        if(topOfDeck)
            Collections.reverse(choices);
        CardCollection result = new CardCollection();
        gameCacheMove.addToList(choices, result);
        return result;
    }

    @Override
    public CardCollectionView chooseCardsToDiscardFrom(final Player p, final SpellAbility sa,
                                                       final CardCollection valid, final int min, final int max) {
        boolean optional = min == 0;

        if (p != player) {
            tempShowCards(valid);
            GameEntityViewMap<Card, CardView> gameCacheDiscard = GameEntityView.getMap(valid);
            List<CardView> views = getGui().many(String.format(localizer.getMessage("lblChooseMinCardToDiscard"), optional ? max : min),
                    localizer.getMessage("lblDiscarded"), min, max, gameCacheDiscard.getTrackableKeys(), null);
            endTempShowCards();
            final CardCollection choices = new CardCollection();
            gameCacheDiscard.addToList(views, choices);
            return choices;
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid, sa);
        inp.setMessage(sa.hasParam("AnyNumber") ? localizer.getMessage("lblDiscardUpToNCards") : localizer.getMessage("lblDiscardNCards"));
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsToDelve(final int genericAmount, final CardCollection grave) {
        final int cardsInGrave = Math.min(genericAmount, grave.size());
        if (cardsInGrave == 0) {
            return CardCollection.EMPTY;
        }

        final CardCollection toExile = new CardCollection();
        final ImmutableList.Builder<Integer> cntChoice = ImmutableList.builder();
        for (int i = 0; i <= cardsInGrave; i++) {
            cntChoice.add(i);
        }
        final int chosenAmount = getGui().one(localizer.getMessage("lblDelveHowManyCards"), cntChoice.build());

        GameEntityViewMap<Card, CardView> gameCacheGrave = GameEntityView.getMap(grave);
        for (int i = 0; i < chosenAmount; i++) {
            String title = localizer.getMessage("lblExileWhichCard", String.valueOf(i + 1), String.valueOf(chosenAmount));
            final CardView nowChosen = getGui().oneOrNone(title, gameCacheGrave.getTrackableKeys());

            if (nowChosen == null || !gameCacheGrave.containsKey(nowChosen)) {
                // User canceled,abort delving.
                toExile.clear();
                break;
            }

            toExile.add(gameCacheGrave.remove(nowChosen));
        }
        return toExile;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#chooseCardsToDiscardUnlessType(int,
     * java.lang.String, forge.gui.card.spellability.SpellAbility)
     */
    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(final int num, final CardCollectionView hand,
                                                             final String uType, final SpellAbility sa) {
        String[] splitUTypes = uType.split(",");
        final InputSelectEntitiesFromList<Card> target = new InputSelectEntitiesFromList<Card>(this, num, num, hand, sa) {
            private static final long serialVersionUID = -5774108410928795591L;

            @Override
            protected boolean hasEnoughTargets() {
                for (final Card c : selected) {
                    if (c.isValid(splitUTypes, sa.getActivatingPlayer(), sa.getHostCard(), sa)) {
                        return true;
                    }
                }
                return super.hasEnoughTargets();
            }
        };
        int n = 1;
        StringBuilder promptType = new StringBuilder();
        for (String part : splitUTypes) {
            if (n == 1) {
                promptType.append(part.toLowerCase());
            } else {
                promptType.append(" or ").append(part.toLowerCase());
            }
            n++;
        }
        target.setMessage(localizer.getMessage("lblSelectNCardsToDiscardUnlessDiscarduType", promptType));
        target.showAndWait();
        return new CardCollection(target.getSelected());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#chooseManaFromPool(java.util.List)
     */
    @Override
    public Mana chooseManaFromPool(final List<Mana> manaChoices) {
        final List<String> options = Lists.newArrayList();
        for (int i = 0; i < manaChoices.size(); i++) {
            final Mana m = manaChoices.get(i);
            options.add(localizer.getMessage("lblNColorManaFromCard", String.valueOf(1 + i), MagicColor.toLongString(m.getColor()), CardTranslation.getTranslatedName(m.getSourceCard().getName())));
        }
        final String chosen = getGui().one(localizer.getMessage("lblPayManaFromManaPool"), options);
        final String idx = TextUtil.split(chosen, '.')[0];
        return manaChoices.get(Integer.parseInt(idx) - 1);
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.game.player.PlayerController#chooseSomeType(java.lang.String,
     * java.lang.String, java.util.List, java.util.List, java.lang.String)
     */
    @Override
    public String chooseSomeType(final String kindOfType, final SpellAbility sa, final Collection<String> validTypes, final boolean isOptional) {
        final List<String> types = Lists.newArrayList(validTypes);
        if (kindOfType.equals("Creature")) {
            sortCreatureTypes(types);
        }
        if (isOptional) {
            return getGui().oneOrNone(localizer.getMessage("lblChooseATargetType", kindOfType.toLowerCase()), types);
        }
        return getGui().one(localizer.getMessage("lblChooseATargetType", kindOfType.toLowerCase()), types);
    }

    // sort creature types such that those most prevalent in player's deck are
    // sorted to the top
    private void sortCreatureTypes(List<String> types) {
        // build map of creature types in player's main deck against the
        // occurrences of each
        Map<String, Integer> typesInDeck = Maps.newHashMap();

        for (Card c : player.getAllCards()) {
            // Changeling are all creature types, they are not interesting for
            // counting creature types
            if (c.hasKeyword(Keyword.CHANGELING)) {
                continue;
            }
            // same is true if it somehow has all creature types
            if (c.getType().hasAllCreatureTypes()) {
                continue;
            }
            // ignore cards that does enter the battlefield as clones
            boolean isClone = false;
            for (ReplacementEffect re : c.getReplacementEffects()) {
                if (re.getLayer() == ReplacementLayer.Copy) {
                    isClone = true;
                    break;
                }
            }
            if (isClone) {
                continue;
            }

            for (String type : c.getType().getCreatureTypes()) {
                Integer count = typesInDeck.getOrDefault(type, 0);
                typesInDeck.put(type, count + 1);
            }
            // also take into account abilities that generate tokens
            for (SpellAbility sa : c.getAllSpellAbilities()) {
                if (sa.getApi() != ApiType.Token) {
                    continue;
                }
                if (sa.hasParam("TokenScript")) {
                    sa.setActivatingPlayer(player);
                    for (String token : sa.getParam("TokenScript").split(",")) {
                        Card protoType = TokenInfo.getProtoType(token, sa, null);
                        for (String type : protoType.getType().getCreatureTypes()) {
                            Integer count = typesInDeck.getOrDefault(type, 0);
                            typesInDeck.put(type, count + 1);
                        }
                    }
                }
            }
            // same for Trigger that does make Tokens
            for (Trigger t : c.getTriggers()) {
                SpellAbility sa = t.ensureAbility();
                if (sa != null) {
                    if (sa.hasParam("TokenScript")) {
                        sa.setActivatingPlayer(player);
                        for (String token : sa.getParam("TokenScript").split(",")) {
                            Card protoType = TokenInfo.getProtoType(token, sa, null);
                            for (String type : protoType.getType().getCreatureTypes()) {
                                Integer count = typesInDeck.getOrDefault(type, 0);
                                typesInDeck.put(type, count + 1);
                            }
                        }
                    }
                }
            }
            // special rule for Fabricate and Servo
            if (c.hasKeyword(Keyword.FABRICATE)) {
                Integer count = typesInDeck.getOrDefault("Servo", 0);
                typesInDeck.put("Servo", count + 1);
            }
        }

        // pre sort
        Collections.sort(types);

        // create sorted list from map from least to most frequent
        List<Entry<String, Integer>> sortedList = Lists.newArrayList(typesInDeck.entrySet());
        sortedList.sort(Entry.comparingByValue());

        // loop through sorted list and move each type to the front of the
        // validTypes collection
        for (Entry<String, Integer> entry : sortedList) {
            String type = entry.getKey();
            if (types.remove(type)) { // ensure an invalid type isn't introduced
                types.add(0, type);
            }
        }
    }

    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        String prompt;
        if (assignee != null) {
            String creature = CardTranslation.getTranslatedName(assignee.getName()) + " (" + assignee.getId() + ")";
            prompt = Localizer.getInstance().getMessage("lblAssignSectorCreature", creature);
        } else {
            prompt = Localizer.getInstance().getMessage("lblChooseSectorEffect");
        }
        return getGui().one(prompt, sectors);
    }

    @Override
    public int chooseSprocket(Card assignee, boolean forceDifferent) {
        String cardName = CardTranslation.getTranslatedName(assignee.getName()) + " (" + assignee.getId() + ")";
        String prompt = Localizer.getInstance().getMessage("lblAssignSprocket", cardName);
        List<Integer> options = Lists.newArrayList(1, 2, 3);
        if(forceDifferent)
            options.remove(Integer.valueOf(assignee.getSprocket()));
        int crankedNextTurn = (player.getCrankCounter() % 3) + 1;
        getGui().setCard(assignee.getView());
        List<Integer> choices = getGui().getChoices(prompt, 1, 1, options, null, (sprocket) -> {
            //Add some info about each sprocket.
            StringBuilder label = new StringBuilder();
            label.append(sprocket);
            int currentCount = CardLists.count(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isContraptionOnSprocket(sprocket));
            if(currentCount > 0)
                label.append(' ').append(Localizer.getInstance().getMessage("lblAssignSprocketCurrentCount", currentCount));
            if(sprocket == crankedNextTurn)
                label.append(' ').append(Localizer.getInstance().getMessage("lblAssignSprocketNextTurn"));
            return label.toString();
        });
        assert choices.size() == 1;
        return choices.get(0);
    }

    @Override
    public PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls) {
        return getGui().one(Localizer.getInstance().getMessage("lblChooseRollIgnore"), rolls);
    }

    @Override
    public Integer chooseRollToIgnore(List<Integer> rolls) {
        return getGui().one(Localizer.getInstance().getMessage("lblChooseRollIgnore"), rolls);
    }

    @Override
    public List<Integer> chooseDiceToReroll(List<Integer> rolls) {
        return getGui().many(Localizer.getInstance().getMessage("lblChooseDiceToRerollTitle"),
                Localizer.getInstance().getMessage("lblChooseDiceToRerollCaption"),0, rolls.size(), rolls, null);
    }

    @Override
    public Integer chooseRollToModify(List<Integer> rolls) {
        return getGui().oneOrNone(Localizer.getInstance().getMessage("lblChooseRollToModify"), rolls);
    }

    @Override
    public RollDiceEffect.DieRollResult chooseRollToSwap(List<RollDiceEffect.DieRollResult> rolls) {
        return getGui().oneOrNone(Localizer.getInstance().getMessage("lblChooseRollToSwap"), rolls);
    }

    @Override
    public String chooseRollSwapValue(List<String> swapChoices, Integer currentResult, int power, int toughness) {
        return getGui().oneOrNone(Localizer.getInstance().getMessage("lblChooseSwapPT", currentResult, power, toughness), swapChoices);
    }

    @Override
    public Object vote(final SpellAbility sa, final String prompt, final List<Object> options,
                       final ListMultimap<Object, Player> votes, Player forPlayer, boolean optional) {
        if (sa.hasParam("Choices")) {
            return chooseSpellAbilitiesForEffect(Lists.newArrayList(IterableUtil.filter(options, SpellAbility.class)), sa, prompt, 1, null).get(0);
        }
        return chooseSingleEntityForEffect(new FCollection<>(IterableUtil.filter(options, GameEntity.class)), sa, prompt, optional, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#confirmReplacementEffect(forge.gui.card.
     * replacement.ReplacementEffect, forge.gui.card.spellability.SpellAbility,
     * java.lang.String)
     */
    @Override
    public boolean confirmReplacementEffect(final ReplacementEffect replacementEffect, final SpellAbility effectSA,
                                            GameEntity affected, final String question) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            CardView cardView;
            SpellAbilityView spellAbilityView = effectSA == null ? null : effectSA.getView();
            if (spellAbilityView != null) //updated view
                cardView = spellAbilityView.getHostCard();
            else //fallback
                cardView = effectSA == null ? null : effectSA.getCardView();
            return this.getGui().confirm(cardView, question.replaceAll("\n", " "));
        } else {
            final InputConfirm inp = new InputConfirm(this, question, effectSA);
            inp.showAndWait();
            return inp.getResult();
        }
    }

    @Override
    public boolean mulliganKeepHand(final Player mulliganingPlayer, int cardsToReturn) {
        // TODO we should be passing tuckCards into Confirmation Dialog
        final InputConfirmMulligan inp = new InputConfirmMulligan(this, player, mulliganingPlayer);
        inp.showAndWait();
        return inp.isKeepHand();
    }

    @Override
    public CardCollectionView londonMulliganReturnCards(final Player mulliganingPlayer, int cardsToReturn) {
        final InputLondonMulligan inp = new InputLondonMulligan(this, player, cardsToReturn);
        inp.showAndWait();
        return inp.getSelectedCards();
    }

    @Override
    public void declareAttackers(final Player attackingPlayer, final Combat combat) {
        if (mayAutoPass()) {
            if (CombatUtil.validateAttackers(combat)) {
                return; // don't prompt to declare attackers if user chose to
                // end the turn and not attacking is legal
            }
            // otherwise: cancel auto pass because of this unexpected attack
            autoPassCancel();
        }

        // This input should not modify combat object itself, but should return user choice
        final InputAttack inpAttack = new InputAttack(this, attackingPlayer, combat);
        inpAttack.showAndWait();
    }

    @Override
    public void declareBlockers(final Player defender, final Combat combat) {
        // This input should not modify combat object itself, but should return user choice
        final InputBlock inpBlock = new InputBlock(this, defender, combat);
        inpBlock.showAndWait();
        getGui().updateAutoPassPrompt();
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        final MagicStack stack = getGame().getStack();

        if (mayAutoPass()) {
            // avoid prompting for input if current phase is set to be
            // auto-passed instead posing a short delay if needed to
            // prevent the game jumping ahead too quick
            int delay = 0;
            if (stack.isEmpty()) {
                // make sure to briefly pause at phases you're not set up to skip
                if (!getGui().isUiSetToSkipPhase(getGame().getPhaseHandler().getPlayerTurn().getView(),
                        getGame().getPhaseHandler().getPhase())) {
                    delay = FControlGamePlayback.phasesDelay;
                }
            } else {
                // pause slightly longer for spells and abilities on the stack resolving
                delay = FControlGamePlayback.resolveDelay;
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        if (stack.isEmpty()) {
            if (getGui().isUiSetToSkipPhase(getGame().getPhaseHandler().getPlayerTurn().getView(),
                    getGame().getPhaseHandler().getPhase())) {
                return null; // avoid prompt for input if stack is empty and
                // player is set to skip the current phase
            }
        } else {
            final SpellAbility ability = stack.peekAbility();
            if (ability != null && ability.isAbility() && getGui().shouldAutoYield(ability.yieldKey())) {
                // avoid prompt for input if top ability of stack is set to auto-yield
                try {
                    Thread.sleep(FControlGamePlayback.resolveDelay);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        final InputPassPriority defaultInput = new InputPassPriority(this);
        defaultInput.showAndWait();
        return defaultInput.getChosenSa();
    }

    @Override
    public boolean playChosenSpellAbility(final SpellAbility chosenSa) {
        return HumanPlay.playSpellAbility(this, player, chosenSa);
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(final int nDiscard) {
        final int max = player.getMaxHandSize();

        if (GuiBase.getInterface().isLibgdxPort()) {
            tempShowCards(player.getCardsIn(ZoneType.Hand));
            GameEntityViewMap<Card, CardView> gameCacheDiscard = GameEntityView.getMap(player.getCardsIn(ZoneType.Hand));
            List<CardView> views = getGui().many(String.format(localizer.getMessage("lblChooseMinCardToDiscard"), nDiscard),
                    localizer.getMessage("lblDiscarded"), nDiscard, nDiscard, gameCacheDiscard.getTrackableKeys(), null);
            endTempShowCards();
            final CardCollection choices = new CardCollection();
            gameCacheDiscard.addToList(views, choices);
            return choices;
        }

        @SuppressWarnings("serial") final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, nDiscard, nDiscard,
                player.getZone(ZoneType.Hand).getCards()) {
            @Override
            protected boolean allowAwaitNextInput() {
                return true; // prevent Cleanup message getting stuck during
                // opponent's next turn
            }
        };
        final String message = localizer.getMessage("lblCleanupPhase") + "\n"
                + localizer.getMessage("lblSelectCardsToDiscardHandDownMaximum", String.valueOf(nDiscard), String.valueOf(max));
        inp.setMessage(message);
        inp.setCancelAllowed(false);
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, final CardCollectionView valid) {
        max = Math.min(max, valid.size());
        min = Math.min(min, max);
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(this, min, max, valid);
        inp.setMessage(localizer.getMessage("lblChooseWhichCardstoReveal"));
        inp.showAndWait();
        return new CardCollection(inp.getSelected());
    }

    @Override
    public boolean payCombatCost(final Card c, final Cost cost, final SpellAbility sa, final String prompt) {
        if (cost.isOnlyManaCost() && cost.getTotalMana().isZero() && isFullControl(FullControlFlag.NoFreeCombatCostHandling)) {
            return true;
        }
        return HumanPlay.payCostDuringAbilityResolve(this, player, c, cost, sa, prompt);
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(final List<SpellAbility> usableFromOpeningHand) {
        final CardCollection srcCards = new CardCollection();
        for (final SpellAbility sa : usableFromOpeningHand) {
            srcCards.add(sa.getHostCard());
        }
        final List<SpellAbility> result = Lists.newArrayList();
        if (srcCards.isEmpty()) {
            return result;
        }
        GameEntityViewMap<Card, CardView> gameCacheOpenHand = GameEntityView.getMap(srcCards);

        final List<CardView> chosen = getGui().many(localizer.getMessage("lblChooseCardsActivateOpeningHandandOrder"),
                localizer.getMessage("lblActivateFirst"), -1, CardView.getCollection(srcCards), null);
        for (final CardView view : chosen) {
            if (!gameCacheOpenHand.containsKey(view)) {
                continue;
            }
            final Card c = gameCacheOpenHand.get(view);
            for (final SpellAbility sa : usableFromOpeningHand) {
                if (sa.getHostCard() == c) {
                    result.add(sa);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public PlayerZone chooseStartingHand(List<PlayerZone> zones) {
        // Create new zone objects in the UI temporarily.
        // Spawn a new input dialog, it works by selecting a card in the zone you want and clicking OK
        // The card will then extract the PlayerZone via the card that is chosen and return it to this function
        // Which will then return the PlayerZone to the caller
        player.updateZoneForView(player.getZone(ZoneType.Hand));
        final InputChooseStartingHand inp = new InputChooseStartingHand(this, player);
        inp.showAndWait();
        return inp.getSelectedHand();
    }

    @Override
    public boolean chooseBinary(final SpellAbility sa, final String question, final BinaryChoiceType kindOfChoice,
                                final Boolean defaultVal) {
        final List<String> labels;
        switch (kindOfChoice) {
            case HeadsOrTails:
                labels = ImmutableList.of(localizer.getMessage("lblHeads"), localizer.getMessage("lblTails"));
                break;
            case TapOrUntap:
                labels = ImmutableList.of(StringUtils.capitalize(localizer.getMessage("lblTap")),
                        localizer.getMessage("lblUntap"));
                break;
            case OddsOrEvens:
                labels = ImmutableList.of(localizer.getMessage("lblOdds"), localizer.getMessage("lblEvens"));
                break;
            case UntapOrLeaveTapped:
                labels = ImmutableList.of(localizer.getMessage("lblUntap"), localizer.getMessage("lblLeaveTapped"));
                break;
            case PlayOrDraw:
                labels = ImmutableList.of(localizer.getMessage("lblPlay"), localizer.getMessage("lblDraw"));
                break;
            case LeftOrRight:
                labels = ImmutableList.of(localizer.getMessage("lblLeft"), localizer.getMessage("lblRight"));
                break;
            case AddOrRemove:
                labels = ImmutableList.of(localizer.getMessage("lblAddCounter"), localizer.getMessage("lblRemoveCounter"));
                break;
            case IncreaseOrDecrease:
                labels = ImmutableList.of(localizer.getMessage("lblIncrease"), localizer.getMessage("lblDecrease"));
                break;
            default:
                labels = ImmutableList.copyOf(kindOfChoice.toString().split("Or"));
        }

        return InputConfirm.confirm(this, sa, question, defaultVal == null || defaultVal, labels);
    }

    @Override
    public boolean chooseFlipResult(final SpellAbility sa, final Player flipper, final boolean[] results,
                                    final boolean call) {
        final String[] labelsSrc = call ? new String[]{localizer.getMessage("lblHeads"), localizer.getMessage("lblTails")}
                : new String[]{localizer.getMessage("lblWinTheFlip"), localizer.getMessage("lblLoseTheFlip")};
        final List<String> sortedResults = new ArrayList<String>();
        for (boolean result : results) {
            sortedResults.add(labelsSrc[result ? 0 : 1]);
        }

        Collections.sort(sortedResults);
        if (!call) {
            Collections.reverse(sortedResults);
        }
        return getGui().one(sa.getHostCard().getName() + " - " + localizer.getMessage("lblChooseAResult"), sortedResults).equals(labelsSrc[0]);
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(final SpellAbility saSpellskite,
                                                                    final List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        if (allTargets.size() < 2) {
            return Iterables.getFirst(allTargets, null);
        }

        final List<Pair<SpellAbilityStackInstance, GameObject>> chosen = getGui()
                .getChoices(saSpellskite.getHostCard().getName(), 1, 1, allTargets, null, new FnTargetToString());
        return Iterables.getFirst(chosen, null);
    }

    private final static class FnTargetToString
            implements Function<Pair<SpellAbilityStackInstance, GameObject>, String>, Serializable {
        private static final long serialVersionUID = -4779137632302777802L;

        @Override
        public String apply(final Pair<SpellAbilityStackInstance, GameObject> targ) {
            return targ.getRight().toString() + " - " + targ.getLeft().getStackDescription();
        }
    }

    @Override
    public void notifyOfValue(final SpellAbility sa, final GameObject realtedTarget, final String value) {
        final String message = MessageUtil.formatNotificationMessage(sa, player, realtedTarget, value);
        if (sa != null && sa.isManaAbility()) {
            getGame().getGameLog().add(GameLogEntryType.LAND, message);
        } else {
            if (sa != null && sa.getHostCard() != null && GuiBase.getInterface().isLibgdxPort()) {
                CardView cardView;
                IPaperCard iPaperCard = sa.getHostCard().getPaperCard();
                if (iPaperCard != null)
                    cardView = CardView.getCardForUi(iPaperCard);
                else
                    cardView = sa.getHostCard().getView();
                getGui().confirm(cardView, message, ImmutableList.of(localizer.getMessage("lblOK")));
            } else {
                getGui().message(message, sa == null || sa.getHostCard() == null ? "" : CardView.get(sa.getHostCard()).toString());
            }
        }
    }

    // end of not related candidates for move.

    /*
     * (non-Javadoc)
     *
     * @see forge.game.player.PlayerController#chooseModeForAbility(forge.gui.card.
     * spellability.SpellAbility, java.util.List, int, int)
     */
    @Override
    public List<AbilitySub> chooseModeForAbility(final SpellAbility sa, List<AbilitySub> possible, final int min, final int num,
                                                 boolean allowRepeat) {
        boolean trackerFrozen = getGame().getTracker().isFrozen();
        if (trackerFrozen) {
            // The view tracker needs to be unfrozen to update the SpellAbilityViews at this point, or it may crash
            getGame().getTracker().unfreeze();
        }
        Map<SpellAbilityView, AbilitySub> spellViewCache = SpellAbilityView.getMap(possible);
        if (trackerFrozen) {
            getGame().getTracker().freeze(); // refreeze if the tracker was frozen prior to this update
        }
        final String modeTitle = localizer.getMessage("lblPlayerActivatedCardChooseMode", sa.getActivatingPlayer().toString(), CardTranslation.getTranslatedName(sa.getHostCard().getName()));
        final List<AbilitySub> chosen = Lists.newArrayListWithCapacity(num);
        int chosenPawprint = 0;
        for (int i = 0; i < num; i++) {
            if (sa.hasParam("Pawprint")) {
                final int tmpPaw = chosenPawprint;
                spellViewCache.values().removeIf(ab -> Integer.parseInt(ab.getParam("Pawprint")) > num - tmpPaw);
            }
            final List<SpellAbilityView> choices = Lists.newArrayList(spellViewCache.keySet());

            SpellAbilityView a;
            if (i < min) {
                a = getGui().one(modeTitle, choices);
            } else {
                a = getGui().oneOrNone(modeTitle, choices);
            }
            if (a == null) {
                break;
            }

            AbilitySub sp = spellViewCache.get(a);
            if (!allowRepeat) {
                spellViewCache.remove(a);
            }
            if (sp.hasParam("Pawprint")) {
                chosenPawprint += AbilityUtils.calculateAmount(sp.getHostCard(), sp.getParam("Pawprint"), sp);
            }
            chosen.add(sp);
        }
        return chosen;
    }

    @Override
    public List<String> chooseColors(final String message, final SpellAbility sa, final int min, final int max,
                                     List<String> options) {
        List<MagicColor.Color> enumOptions = options.stream().map(s -> MagicColor.Color.fromByte(MagicColor.fromName(s))).collect(Collectors.toList());
        List<MagicColor.Color> enumChoices = getGui().getChoices(message, min, max, enumOptions);
        return enumChoices.stream().map(MagicColor.Color::getName).collect(Collectors.toList());
    }

    @Override
    public MagicColor.Color chooseColor(final String message, final SpellAbility sa, final ColorSet colors) {
        final int cntColors = colors.countColors();
        switch (cntColors) {
            case 0:
                return null;
            case 1:
                return MagicColor.Color.fromByte(colors.getColor());
            default:
                return chooseColorCommon(message, sa == null ? null : sa.getHostCard(), colors, false);
        }
    }

    @Override
    public MagicColor.Color chooseColorAllowColorless(final String message, final Card c, final ColorSet colors) {
        final int cntColors = 1 + colors.countColors();
        switch (cntColors) {
            case 1:
                return MagicColor.Color.COLORLESS;
            default:
                return chooseColorCommon(message, c, colors, true);
        }
    }

    private MagicColor.Color chooseColorCommon(final String message, final Card c, final ColorSet colors,
                                   final boolean withColorless) {
        List<MagicColor.Color> options = Lists.newArrayList(colors.toEnumSet());
        if (withColorless && colors.countColors() > 0) {
            options.add(MagicColor.Color.COLORLESS);
        }

        if (options.size() > 2) {
            return getGui().one(message, options);
        }

        boolean confirmed = false;
        confirmed = InputConfirm.confirm(this, CardView.get(c), message, true,
                options.stream().map(MagicColor.Color::toString).collect(Collectors.toList()));
        final int idxChosen = confirmed ? 0 : 1;
        return options.get(idxChosen);
    }

    @Override
    public ICardFace chooseSingleCardFace(final SpellAbility sa, final String message, final Predicate<ICardFace> cpp,
                                          final String name) {
        List<CardFaceView> choices = FModel.getMagicDb().getCommonCards().streamAllFaces()
                .filter(cpp)
                .map(cardFace -> new CardFaceView(CardTranslation.getTranslatedName(cardFace.getName()), cardFace.getName()))
                .sorted()
                .collect(Collectors.toList());
        CardFaceView cardFaceView = getGui().one(message, choices);
        return StaticData.instance().getCommonCards().getFaceByName(cardFaceView.getOracleName());
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message) {
        return getGui().one(message, faces);
    }

    @Override
    public CounterType chooseCounterType(final List<CounterType> options, final SpellAbility sa, final String prompt,
                                         Map<String, Object> params) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return getGui().one(prompt, options);
    }

    @Override
    public CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params) {
        if (states.size() <= 1) {
            return Iterables.getFirst(states, null);
        }
        Map<CardStateView, CardState> cache = CardView.getStateMap(states);
        CardStateView chosen = getGui().one(message, Lists.newArrayList(cache.keySet()));
        return cache.get(chosen);
    }

    @Override
    public String chooseKeywordForPump(final List<String> options, final SpellAbility sa, final String prompt, final Card tgtCard) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return getGui().one(prompt, options);
    }

    @Override
    public boolean confirmPayment(final CostPart costPart, final String question, SpellAbility sa) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            CardView cardView;
            try {
                cardView = CardView.getCardForUi(ImageUtil.getPaperCardFromImageKey(sa.getView().getHostCard().getCurrentState().getTrackableImageKey()));
            } catch (Exception e) {
                SpellAbilityView spellAbilityView = sa.getView();
                if (spellAbilityView != null) //updated view
                    cardView = spellAbilityView.getHostCard();
                else //fallback
                    cardView = sa.getCardView();
            }
            return this.getGui().confirm(cardView, question.replaceAll("\n", " "));
        } else {
            final InputConfirm inp = new InputConfirm(this, question, sa);
            inp.showAndWait();
            return inp.getResult();
        }
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(final List<ReplacementEffect> possibleReplacers) {
        final ReplacementEffect first = possibleReplacers.get(0);
        if (possibleReplacers.size() == 1) {
            return first;
        }
        final List<String> res = possibleReplacers.stream().map(ReplacementEffect::toString).collect(Collectors.toList());
        final String firstStr = res.get(0);
        final String prompt = localizer.getMessage("lblChooseFirstApplyReplacementEffect");
        for (int i = 1; i < res.size(); i++) {
            // prompt user if there are multiple different options
            if (!res.get(i).equals(firstStr)) {
                if (!GuiBase.isNetworkplay()) //non network game don't need serialization
                    return getGui().one(prompt, possibleReplacers);
                ReplacementEffectView rev = getGui().one(prompt, possibleReplacers.stream().map(ReplacementEffect::getView).collect(Collectors.toList()));
                return possibleReplacers.stream().filter(re -> re.getId() == rev.getId()).findAny().orElse(first);
            }
        }
        // return first option without prompting if all options are the same
        return first;
    }

    @Override
    public StaticAbility chooseSingleStaticAbility(final String prompt, final List<StaticAbility> possibleStatics) {
        final StaticAbility first = possibleStatics.get(0);
        if (possibleStatics.size() == 1 || !isFullControl(FullControlFlag.ChooseCostOrder)) {
            return first;
        }
        final List<String> sts = possibleStatics.stream().map(StaticAbility::toString).collect(Collectors.toList());
        final String firstStr = sts.get(0);
        for (int i = 1; i < sts.size(); i++) {
            // prompt user if there are multiple different options
            if (!sts.get(i).equals(firstStr)) {
                if (!GuiBase.isNetworkplay()) //non network game don't need serialization
                    return getGui().one(prompt, possibleStatics);
                StaticAbilityView stv = getGui().one(prompt, possibleStatics.stream().map(StaticAbility::getView).collect(Collectors.toList()));
                return possibleStatics.stream().filter(st -> st.getId() == stv.getId()).findAny().orElse(first);
            }
        }
        // return first option without prompting if all options are the same
        return first;
    }

    @Override
    public String chooseProtectionType(final String string, final SpellAbility sa, final List<String> choices) {
        return getGui().one(string, choices);
    }

    @Override
    public boolean payCostToPreventEffect(final Cost cost, final SpellAbility sa, final boolean alreadyPaid, final FCollectionView<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        String prompt = null;
        if (sa.isKeyword(Keyword.ECHO)) {
            prompt = Localizer.getInstance().getMessage("lblPayEcho");
        } else if (sa.isKeyword(Keyword.CUMULATIVE_UPKEEP)) {
            prompt = "Cumulative upkeep for " + sa.getHostCard();
        }
        return HumanPlay.payCostDuringAbilityResolve(this, player, sa.getHostCard(), cost, sa, prompt);
    }

    @Override
    public boolean payCostDuringRoll(final Cost cost, final SpellAbility sa, final FCollectionView<Player> allPayers) {
        // if it's paid by the AI already the human can pay, but it won't change anything
        return HumanPlay.payCostDuringAbilityResolve(this, player, sa.getHostCard(), cost, sa, null);
    }

    // stores saved order for different sets of SpellAbilities
    private final Map<String, List<Integer>> orderedSALookup = Maps.newHashMap();

    @Override
    public void orderAndPlaySimultaneousSa(final List<SpellAbility> activePlayerSAs) {
        List<SpellAbility> orderedSAs = activePlayerSAs;
        if (activePlayerSAs.size() > 1) {
            final String firstStr = activePlayerSAs.get(0).toString();
            boolean needPrompt = !activePlayerSAs.get(0).isTrigger();

            // for the purpose of pre-ordering, no need for extra granularity
            int idxAdditionalInfo = firstStr.indexOf(" [");
            StringBuilder saLookupKey = new StringBuilder(idxAdditionalInfo > 0 ? firstStr.substring(0, idxAdditionalInfo - 1) : firstStr);

            char delim = (char) 5;
            for (int i = 1; i < activePlayerSAs.size(); i++) {
                SpellAbility currentSa = activePlayerSAs.get(i);
                String saStr = currentSa.toString();

                // if current SA isn't a trigger and it uses Targeting, try to show prompt
                if (currentSa.isTrigger()) {
                    needPrompt |= currentSa.getTrigger().hasParam("OrderDuplicates");
                } else if (currentSa.usesTargeting()) {
                    needPrompt = true;
                }
                if (!needPrompt && !saStr.equals(firstStr)) {
                    // prompt by default unless all abilities are the same
                    needPrompt = true;
                }

                saLookupKey.append(delim).append(saStr);
                idxAdditionalInfo = saLookupKey.indexOf(" [");
                if (idxAdditionalInfo > 0) {
                    saLookupKey = new StringBuilder(saLookupKey.substring(0, idxAdditionalInfo - 1));
                }
            }
            if (needPrompt) {
                List<Integer> savedOrder = orderedSALookup.get(saLookupKey.toString());
                List<SpellAbilityView> orderedSAVs = Lists.newArrayList();

                // create a mapping between a spell's view and the spell itself
                Map<SpellAbilityView, SpellAbility> spellViewCache = SpellAbilityView.getMap(orderedSAs);

                if (savedOrder != null) {
                    orderedSAVs = Lists.newArrayList();
                    for (Integer index : savedOrder) {
                        orderedSAVs.add(activePlayerSAs.get(index).getView());
                    }
                } else {
                    for (SpellAbility spellAbility : orderedSAs) {
                        orderedSAVs.add(spellAbility.getView());
                    }
                }
                if (savedOrder != null) {
                    boolean preselect = FModel.getPreferences()
                            .getPrefBoolean(FPref.UI_PRESELECT_PREVIOUS_ABILITY_ORDER);
                    orderedSAVs = getGui().order(localizer.getMessage("lblReorderSimultaneousAbilities"), localizer.getMessage("lblResolveFirst"), 0, 0,
                            preselect ? Lists.newArrayList() : orderedSAVs,
                            preselect ? orderedSAVs : Lists.newArrayList(), null, false);
                } else {
                    orderedSAVs = getGui().order(localizer.getMessage("lblSelectOrderForSimultaneousAbilities"), localizer.getMessage("lblResolveFirst"), orderedSAVs,
                            null);
                }
                orderedSAs = Lists.newArrayList();
                for (SpellAbilityView spellAbilityView : orderedSAVs) {
                    orderedSAs.add(spellViewCache.get(spellAbilityView));
                }
                // save order to avoid needing to prompt a second time to order
                // the same abilities
                savedOrder = Lists.newArrayListWithCapacity(activePlayerSAs.size());
                for (SpellAbility sa : orderedSAs) {
                    savedOrder.add(activePlayerSAs.indexOf(sa));
                }
                orderedSALookup.put(saLookupKey.toString(), savedOrder);
            }
        }
        for (int i = orderedSAs.size() - 1; i >= 0; i--) {
            final SpellAbility next = orderedSAs.get(i);
            if (next.isTrigger() && !next.isCopied()) {
                HumanPlay.playSpellAbility(this, player, next);
            } else {
                if (next.isCopied()) {
                    if (next.isSpell()) {
                        // copied spell always add to stack
                        if (!next.getHostCard().isInZone(ZoneType.Stack)) {
                            next.setHostCard(player.getGame().getAction().moveToStack(next.getHostCard(), next));
                        } else {
                            player.getGame().getStackZone().add(next.getHostCard());
                        }
                    }
                    // TODO check if static abilities needs to be run for things affecting the copy?
                    if (next.isMayChooseNewTargets()) {
                        next.setupNewTargets(player);
                    }
                }
                player.getGame().getStack().add(next);
            }
        }
    }

    @Override
    public boolean playTrigger(final Card host, final WrappedAbility wrapperAbility, final boolean isMandatory) {
        return HumanPlay.playSpellAbilityNoStack(this, player, wrapperAbility);
    }

    @Override
    public boolean playSaFromPlayEffect(final SpellAbility tgtSA) {
        return HumanPlay.playSpellAbility(this, player, tgtSA);
    }

    @Override
    public boolean chooseTargetsFor(final SpellAbility currentAbility) {
        final TargetSelection select = new TargetSelection(this, currentAbility);
        boolean canFilterMustTarget = true;

        // Can't filter MustTarget if any parent ability is also targeting
        SpellAbility checkSA = currentAbility.getParent();
        while (checkSA != null) {
            if (checkSA.usesTargeting()) {
                canFilterMustTarget = false;
                break;
            }
            checkSA = checkSA.getParent();
        }
        // Can't filter MustTarget is any SubAbility is also targeting
        checkSA = currentAbility.getSubAbility();
        while (checkSA != null) {
            if (checkSA.usesTargeting()) {
                canFilterMustTarget = false;
                break;
            }
            checkSA = checkSA.getSubAbility();
        }

        boolean result = select.chooseTargets(null, null, null, false, canFilterMustTarget);

        final Iterable<GameEntity> targets = currentAbility.getTargets().getTargetEntities();
        final int size = Iterables.size(targets);
        int amount = currentAbility.getStillToDivide();

        // assign divided as you choose values
        if (result && size > 0 && amount > 0) {
            if (currentAbility.hasParam("DividedUpTo")) {
                amount = chooseNumber(currentAbility, localizer.getMessage("lblHowMany"), size, amount);
            }
            if (size == 1) {
                currentAbility.addDividedAllocation(Iterables.get(targets, 0), amount);
            } else if (size == amount) {
                for (GameEntity e : targets) {
                    currentAbility.addDividedAllocation(e, 1);
                }
            } else if (amount == 0) {
                for (GameEntity e : targets) {
                    currentAbility.addDividedAllocation(e, 0);
                }
            } else if (size > amount) {
                return false;
            } else {
                String label = "lblDamage";
                if (currentAbility.getApi() == ApiType.PreventDamage) {
                    label = "lblShield";
                } else if (currentAbility.getApi() == ApiType.PutCounter) {
                    label = "lblCounters";
                }
                label = localizer.getMessage(label).toLowerCase();
                final CardView vSource = CardView.get(currentAbility.getHostCard());
                final Map<Object, Integer> vTargets = new HashMap<>(size);
                for (GameEntity e : targets) {
                    vTargets.put(GameEntityView.get(e), amount);
                }
                final Map<Object, Integer> vResult = getGui().assignGenericAmount(vSource, vTargets, amount, true, label);
                for (GameEntity e : targets) {
                    currentAbility.addDividedAllocation(e, vResult.get(GameEntityView.get(e)));
                }
                if (currentAbility.getStillToDivide() > 0) {
                    return false;
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.game.player.PlayerController#chooseTargets(forge.gui.card.spellability.
     * SpellAbility, forge.gui.card.spellability.SpellAbilityStackInstance)
     */
    @Override
    public TargetChoices chooseNewTargetsFor(final SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        final SpellAbility sa = ability.isWrapper() ? ((WrappedAbility) ability).getWrappedAbility() : ability;
        if (!sa.usesTargeting()) {
            return null;
        }
        final TargetChoices oldTarget = sa.getTargets();
        final TargetSelection select = new TargetSelection(this, sa);
        sa.clearTargets();
        if (select.chooseTargets(oldTarget.size(), sa.isDividedAsYouChoose() ? Lists.newArrayList(oldTarget.getDividedValues()) : null, filter, optional, false)) {
            return sa.getTargets();
        } else {
            sa.setTargets(oldTarget);
            // Return old target, since we had to reset them above
            return null;
        }
    }

    @Override
    public boolean chooseCardsPile(final SpellAbility sa, final CardCollectionView pile1,
                                   final CardCollectionView pile2, final String faceUp) {
        final String p1Str = TextUtil.concatNoSpace("-- Pile 1 (", String.valueOf(pile1.size()), " cards) --");
        final String p2Str = TextUtil.concatNoSpace("-- Pile 2 (", String.valueOf(pile2.size()), " cards) --");

        /*
         * if (faceUp.equals("True")) { final List<String> possibleValues =
         * ImmutableList.of(p1Str , p2Str); return
         * getGui().confirm(CardView.get(sa.getHostCard()), "Choose a Pile",
         * possibleValues); }
         */

        final List<CardView> cards = Lists.newArrayListWithCapacity(pile1.size() + pile2.size() + 2);
        final CardView pileView1 = new CardView(Integer.MIN_VALUE, null, p1Str);

        cards.add(pileView1);
        if (faceUp.equals("False")) {
            tempShowCards(pile1);
            cards.addAll(CardView.getCollection(pile1));
        }

        final CardView pileView2 = new CardView(Integer.MIN_VALUE + 1, null, p2Str);
        cards.add(pileView2);
        if (!faceUp.equals("True")) {
            tempShowCards(pile2);
            cards.addAll(CardView.getCollection(pile2));
        }

        // make sure Pile 1 or Pile 2 is clicked on
        boolean result;
        while (true) {
            final CardView chosen = getGui().one(localizer.getMessage("lblChooseaPile"), cards);
            if (chosen.equals(pileView1)) {
                result = true;
                break;
            }
            if (chosen.equals(pileView2)) {
                result = false;
                break;
            }
        }

        endTempShowCards();
        return result;
    }

    @Override
    public void revealAnte(final String message, final Multimap<Player, PaperCard> removedAnteCards) {
        for (final Player p : removedAnteCards.keySet()) {
            getGui().reveal(localizer.getMessage("lblActionFromPlayerDeck", message, Lang.getInstance().getPossessedObject(MessageUtil.mayBeYou(player, p), "")),
                    ImmutableList.copyOf(removedAnteCards.get(p)));
        }
    }

    @Override
    public void revealAISkipCards(final String message, final Map<Player, Map<DeckSection, List<? extends PaperCard>>> unplayable) {
        if (GuiBase.getInterface().isLibgdxPort()) {
            //restore old functionality for mobile version since list of card names can't be zoomed to display the cards
            for (Player p : unplayable.keySet()) {
                final Map<DeckSection, List<? extends PaperCard>> removedUnplayableCards = unplayable.get(p);
                final List<PaperCard> labels = new ArrayList<>();
                for (final DeckSection s : new TreeSet<>(removedUnplayableCards.keySet())) {
                    if (DeckSection.Sideboard.equals(s))
                        continue;
                    labels.addAll(removedUnplayableCards.get(s));
                }
                if (!labels.isEmpty())
                    getGui().reveal(localizer.getMessage("lblActionFromPlayerDeck", message, Lang.getInstance().getPossessedObject(MessageUtil.mayBeYou(player, p), "")),
                            ImmutableList.copyOf(labels));
            }
            return;
        }
        for (Player p : unplayable.keySet()) {
            final Map<DeckSection, List<? extends PaperCard>> removedUnplayableCards = unplayable.get(p);
            final List<Object> labels = new ArrayList<>();
            for (final DeckSection s : new TreeSet<>(removedUnplayableCards.keySet())) {
                labels.add("=== " + s.getLocalizedName() + " ===");
                labels.addAll(removedUnplayableCards.get(s));
            }
            getGui().reveal(localizer.getMessage("lblActionFromPlayerDeck", message, Lang.getInstance().getPossessedObject(MessageUtil.mayBeYou(player, p), "")),
                    ImmutableList.copyOf(labels));
        }
    }

    @Override
    public void revealUnsupported(final Map<Player, List<PaperCard>> unsupported) {
        for (final Player p : unsupported.keySet()) {
            List<PaperCard> removed = unsupported.get(p);
            if (removed == null || removed.isEmpty())
                continue;
            getGui().getChoices(localizer.getMessage("lblActionFromPlayerDeck", localizer.getMessage("lblRemoved"), Lang.getInstance().getPossessedObject(MessageUtil.mayBeYou(player, p), "")), -1, -1, ImmutableList.copyOf(removed));
        }
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(final List<PaperCard> losses) {
        return getGui().many(localizer.getMessage("lblSelectCardstoAddtoYourDeck"), localizer.getMessage("lblAddTheseToMyDeck"), 0, losses.size(), losses, null);
    }

    @Override
    public boolean payManaCost(final ManaCost toPay, final CostPartMana costPartMana, final SpellAbility sa,
                               final String prompt, ManaConversionMatrix matrix, final boolean effect) {
        return HumanPlay.payManaCost(this, toPay, costPartMana, sa, player, prompt, matrix, effect);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(final SpellAbility sa, final ManaCost manaCost,
                                                                     final CardCollectionView untappedCards, boolean improvise) {
        final InputSelectCardsForConvokeOrImprovise inp = new InputSelectCardsForConvokeOrImprovise(this, player,
                manaCost, untappedCards, improvise, sa);
        inp.showAndWait();
        return inp.getConvokeMap();
    }

    @Override
    public String chooseCardName(final SpellAbility sa, final Predicate<ICardFace> cpp, final String valid,
                                 final String message) {
        while (true) {
            final ICardFace cardFace = chooseSingleCardFace(sa, message, cpp, sa.getHostCard().getName());
            final PaperCard cp = FModel.getMagicDb().getCommonCards().getCard(cardFace.getName());
            // the Card instance for test needs a game to be tested
            final Card instanceForPlayer = Card.fromPaperCard(cp, player);
            // TODO need the valid check be done against the CardFace?
            if (instanceForPlayer.isValid(valid, sa.getHostCard().getController(), sa.getHostCard(), sa)) {
                // it need to return name for card face
                return cardFace.getName();
            }
        }
    }

    @Override
    public Card chooseSingleCardForZoneChange(final ZoneType destination, final List<ZoneType> origin,
                                              final SpellAbility sa, final CardCollection fetchList, final DelayedReveal delayedReveal,
                                              final String selectPrompt, final boolean isOptional, final Player decider) {
        return chooseSingleEntityForEffect(fetchList, delayedReveal, sa, selectPrompt, isOptional, decider, null);
    }

    public List<Card> chooseCardsForZoneChange(final ZoneType destination, final List<ZoneType> origin,
                                               final SpellAbility sa, final CardCollection fetchList, final int min, final int max, final DelayedReveal delayedReveal,
                                               final String selectPrompt, final Player decider) {
        return chooseEntitiesForEffect(fetchList, min, max, delayedReveal, sa, selectPrompt, decider, null);
    }

    @Override
    public boolean isGuiPlayer() {
        return lobbyPlayer == GamePlayerUtil.getGuiPlayer();
    }

    public void updateAchievements() {
        AchievementCollection.updateAll(this);
    }

    public boolean canUndoLastAction() {
        if (!getGame().stack.canUndo(player)) {
            return false;
        }
        final Player priorityPlayer = getGame().getPhaseHandler().getPriorityPlayer();
        return priorityPlayer != null && priorityPlayer == player;
    }

    @Override
    public void undoLastAction() {
        tryUndoLastAction();
    }

    public boolean tryUndoLastAction() {
        if (!canUndoLastAction()) {
            return false;
        }

        if (getGame().getStack().undo()) {
            final Input currentInput = inputQueue.getInput();
            if (currentInput instanceof InputPassPriority) {
                // ensure prompt updated if needed
                currentInput.showMessageInitial();
            }
            return true;
        }
        return false;
    }

    @Override
    public void selectButtonOk() {
        inputProxy.selectButtonOK();
    }

    @Override
    public void selectButtonCancel() {
        inputProxy.selectButtonCancel();
    }

    public void confirm() {
        if (inputQueue.getInput() instanceof InputConfirm) {
            selectButtonOk();
        }
    }

    @Override
    public void passPriority() {
        passPriority(false);
    }

    @Override
    public void passPriorityUntilEndOfTurn() {
        passPriority(true);
    }

    private void passPriority(final boolean passUntilEndOfTurn) {
        final Input inp = inputProxy.getInput();
        if (inp instanceof InputPassPriority) {
            if (passUntilEndOfTurn) {
                autoPassUntilEndOfTurn();
            }
            inp.selectButtonOK();
        } else {
            FThreads.invokeInEdtNowOrLater(() -> {
                // getGui().message("Cannot pass priority at this time.");
            });
        }
    }

    @Override
    public void useMana(final byte mana) {
        final Input input = inputQueue.getInput();
        if (input instanceof InputPayMana) {
            ((InputPayMana) input).useManaFromPool(mana);
        }
    }

    @Override
    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        // TODO Also record input type and wait for that input to be present before sending select player
        macros().addRememberedAction(new SelectPlayerAction(playerView));

        inputProxy.selectPlayer(playerView, triggerEvent);
    }

    @Override
    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect,
                              final ITriggerEvent triggerEvent) {
        macros().addRememberedAction(new SelectCardAction(cardView));

        return inputProxy.selectCard(cardView, otherCardViewsToSelect, triggerEvent);
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
        if (spellViewCache == null || spellViewCache.isEmpty()) {
            return;
        }
        inputProxy.selectAbility(spellViewCache.get(sa));
    }

    @Override
    public void alphaStrike() {
        inputProxy.alphaStrike();
    }

    @Override
    public void resetAtEndOfTurn() {
        // Not used by the human controller
    }

    // Dev Mode cheat functions
    private boolean canPlayUnlimitedLands;

    @Override
    public boolean canPlayUnlimitedLands() {
        return canPlayUnlimitedLands;
    }

    private IDevModeCheats cheats;

    @Override
    public IDevModeCheats cheat() {
        if (cheats == null) {
            cheats = new DevModeCheats();
            // TODO: In Network game, inform other players that this player is cheating
        }
        return cheats;
    }

    public boolean hasCheated() {
        return cheats != null;
    }

    public class DevModeCheats implements IDevModeCheats {
        private CardFaceView lastAdded;
        private ZoneType lastAddedZone;
        private Player lastAddedPlayer;
        private SpellAbility lastAddedSA;
        private boolean lastTrigs;
        private boolean lastSummoningSickness;
        private boolean lastTopOfTheLibrary;

        private DevModeCheats() {
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#setCanPlayUnlimitedLands(boolean)
         */
        @Override
        public void setCanPlayUnlimitedLands(final boolean canPlayUnlimitedLands0) {
            canPlayUnlimitedLands = canPlayUnlimitedLands0;
            getGame().fireEvent(new GameEventPlayerStatsChanged(player, false));
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#setViewAllCards(boolean)
         */
        @Override
        public void setViewAllCards(final boolean canViewAll) {
            mayLookAtAllCards = canViewAll;
            for (final Player p : getGame().getPlayers()) {
                getGui().updateCards(CardView.getCollection(p.getAllCards()));
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#generateMana()
         */
        @Override
        public void generateMana() {
            final Player pPriority = getGame().getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message(localizer.getMessage("lblNoPlayerHasPriorityCannotAddedManaToPool"));
                return;
            }

            final Card dummy = new Card(-777777, getGame());
            dummy.setOwner(pPriority);
            final Map<String, String> produced = Maps.newHashMap();
            produced.put("Produced", "W W W W W W W U U U U U U U B B B B B B B G G G G G G G R R R R R R R 7");
            final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
            getGame().getAction().invoke(() -> abMana.produceMana(null));
        }

        @Override
        public void rollbackPhase() {
            final Player pPriority = getGame().getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message(localizer.getMessage("lblNoPlayerPriorityGameStateCannotBeSetup"));
                return;
            }
            if (getGui().getGamestate() != null)
                getGui().getGamestate().applyToGame(getGame());
        }

        private GameState createGameStateObject() {
            return new GameState() {
                @Override
                public IPaperCard getPaperCard(final String cardName, final String setCode, final int artID) {
                    return FModel.getMagicDb().getCommonCards().getCard(cardName, setCode, artID);
                }
            };
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#dumpGameState()
         */
        @Override
        public void dumpGameState() {
            final GameState state = createGameStateObject();
            try {
                state.initFromGame(getGame());
                final File f = GuiBase.getInterface().getSaveFile(new File(ForgeConstants.USER_GAMES_DIR, "state.txt"));
                if (f != null
                        && (!f.exists() || getGui().showConfirmDialog(localizer.getMessage("lblOverwriteExistFileConfirm"), localizer.getMessage("lblFileExists")))) {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
                        bw.write(state.toString());
                    }
                }
            } catch (final Exception e) {
                String err = e.getClass().getName();
                if (e.getMessage() != null) {
                    err += ": " + e.getMessage();
                }
                getGui().showErrorDialog(err);
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#setupGameState()
         */
        @Override
        public void setupGameState() {
            final File gamesDir = new File(ForgeConstants.USER_GAMES_DIR);
            if (!gamesDir.exists()) {
                // if the directory does not exist, try to create it
                gamesDir.mkdir();
            }

            final String filename = GuiBase.getInterface().showFileDialog(localizer.getMessage("lblSelectGameStateFile"),
                    ForgeConstants.USER_GAMES_DIR);
            if (filename == null) {
                return;
            }

            final GameState state = createGameStateObject();
            try {
                final FileInputStream fstream = new FileInputStream(filename);
                state.parse(fstream);
                fstream.close();
            } catch (final FileNotFoundException fnfe) {
                SOptionPane.showErrorDialog(localizer.getMessage("lblFileNotFound") + ": " + filename);
                return;
            } catch (final Exception e) {
                SOptionPane.showErrorDialog(localizer.getMessage("lblErrorLoadingBattleSetupFile"));
                return;
            }

            final Player pPriority = getGame().getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message(localizer.getMessage("lblNoPlayerPriorityGameStateCannotBeSetup"));
                return;
            }
            state.applyToGame(getGame());
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#tutorForCard()
         */
        @Override
        public void tutorForCard() {
            final Player pPriority = getGame().getPhaseHandler().getPriorityPlayer();
            if (pPriority == null) {
                getGui().message(localizer.getMessage("lblNoPlayerPriorityDeckCantBeTutoredFrom"));
                return;
            }

            final CardCollection lib = (CardCollection) pPriority.getCardsIn(ZoneType.Library);
            final List<ZoneType> origin = Lists.newArrayList();
            origin.add(ZoneType.Library);
            final SpellAbility sa = new SpellAbility.EmptySa(new Card(-1, getGame()));
            final Card card = chooseSingleCardForZoneChange(ZoneType.Hand, origin, sa, lib, null, localizer.getMessage("lblChooseaCard"), true,
                    pPriority);
            if (card == null) {
                return;
            }

            getGame().getAction().invoke(() -> getGame().getAction().moveToHand(card, null));
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCountersToPermanent()
         */
        @Override
        public void addCountersToPermanent() {
            modifyCountersOnPermanent(false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#removeCountersToPermanent()
         */
        @Override
        public void removeCountersFromPermanent() {
            modifyCountersOnPermanent(true);
        }

        public void modifyCountersOnPermanent(boolean subtract) {
            final String titleMsg = subtract ? localizer.getMessage("lblRemoveCountersFromWhichCard") : localizer.getMessage("lblAddCountersToWhichCard");

            GameEntityViewMap<Card, CardView> gameCacheCounters = GameEntityView.getMap(getGame().getCardsIn(ZoneType.Battlefield));

            final CardView cv = getGui().oneOrNone(titleMsg, gameCacheCounters.getTrackableKeys());
            if (cv == null || !gameCacheCounters.containsKey(cv)) {
                return;
            }
            final Card card = gameCacheCounters.get(cv);

            final ImmutableList<CounterType> counters = subtract ? ImmutableList.copyOf(card.getCounters().keySet())
                    : ImmutableList.copyOf(CounterEnumType.values);

            final CounterType counter = getGui().oneOrNone(localizer.getMessage("lblWhichTypeofCounter"), counters);
            if (counter == null) {
                return;
            }

            final Integer count = getGui().getInteger(localizer.getMessage("lblHowManyCounters"), 1, Integer.MAX_VALUE, 10);
            if (count == null) {
                return;
            }

            if (subtract) {
                card.subtractCounter(counter, count, null);
            } else {
                card.addCounterInternal(counter, count, card.getController(), false, null, null);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#tapPermanents()
         */
        @Override
        public void tapPermanents() {
            getGame().getAction().invoke(() -> {
                final CardCollectionView untapped = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield),
                        CardPredicates.UNTAPPED);
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0,
                        Integer.MAX_VALUE, untapped);
                inp.setCancelAllowed(true);
                inp.setMessage(localizer.getMessage("lblChoosePermanentstoTap"));
                inp.showAndWait();
                if (!inp.hasCancelled()) {
                    CardCollection tapped = new CardCollection();
                    for (final Card c : inp.getSelected()) {
                        if (c.tap(true, null, null)) tapped.add(c);
                    }
                    if (!tapped.isEmpty()) {
                        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                        runParams.put(AbilityKey.Cards, tapped);
                        getGame().getTriggerHandler().runTrigger(TriggerType.TapAll, runParams, false);
                    }
                }
            });
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#untapPermanents()
         */
        @Override
        public void untapPermanents() {
            getGame().getAction().invoke(() -> {
                final CardCollectionView tapped = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield),
                        CardPredicates.TAPPED);
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(PlayerControllerHuman.this, 0,
                        Integer.MAX_VALUE, tapped);
                inp.setCancelAllowed(true);
                inp.setMessage(localizer.getMessage("lblChoosePermanentstoUntap"));
                inp.showAndWait();
                if (!inp.hasCancelled()) {
                    CardCollection untapped = new CardCollection();
                    for (final Card c : inp.getSelected()) {
                        if (c.untap()) untapped.add(c);
                    }
                    if (!untapped.isEmpty()) {
                        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                        final Map<Player, CardCollection> map = Maps.newHashMap();
                        map.put(getPlayer(), untapped);
                        runParams.put(AbilityKey.Map, map);
                        getGame().getTriggerHandler().runTrigger(TriggerType.UntapAll, runParams, false);
                    }
                }
            });
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#setPlayerLife()
         */
        @Override
        public void setPlayerLife() {
            GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());

            final PlayerView pv = getGui().oneOrNone(localizer.getMessage("lblSetLifeforWhichPlayer"), gameCachePlayer.getTrackableKeys());
            if (pv == null || !gameCachePlayer.containsKey(pv)) {
                return;
            }
            final Player player = gameCachePlayer.get(pv);

            final Integer life = getGui().getInteger(localizer.getMessage("lblSetLifetoWhat"), 0);
            if (life == null) {
                return;
            }

            player.setLife(life, null);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#winGame()
         */
        @Override
        public void winGame() {
            final Input input = inputQueue.getInput();
            if (!(input instanceof InputPassPriority)) {
                getGui().message(localizer.getMessage("lblYouMustHavePrioritytoUseThisFeature"), localizer.getMessage("lblWinGame"));
                return;
            }

            // set life of all other players to 0
            final LobbyPlayer guiPlayer = getLobbyPlayer();
            final FCollectionView<Player> players = getGame().getPlayers();
            for (final Player player : players) {
                if (player.getLobbyPlayer() != guiPlayer) {
                    player.setLife(0, null);
                }
            }

            // pass priority so that causes gui player to win
            input.selectButtonOK();
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToHand()
         */
        @Override
        public void addCardToHand() {
            addCardToZone(ZoneType.Hand, false, false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToBattlefield()
         */
        @Override
        public void addCardToBattlefield() {
            addCardToZone(ZoneType.Battlefield, false, true);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToLibrary()
         */
        @Override
        public void addCardToLibrary() {
            addCardToZone(ZoneType.Library, false, false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToGraveyard()
         */
        @Override
        public void addCardToGraveyard() {
            addCardToZone(ZoneType.Graveyard, false, false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToExile()
         */
        @Override
        public void addCardToExile() {
            addCardToZone(ZoneType.Exile, false, false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#addCardToExile()
         */
        @Override
        public void castASpell() {
            addCardToZone(ZoneType.Battlefield, false, false);
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#repeatLastAddition()
         */
        @Override
        public void repeatLastAddition() {
            if (lastAdded == null) {
                return;
            }
            addCardToZone(null, true, lastTrigs);
        }

        private void addCardToZone(ZoneType zone, final boolean repeatLast, final boolean noTriggers) {
            final ZoneType targetZone = repeatLast ? lastAddedZone : zone;
            String message = null;
            if (targetZone != ZoneType.Battlefield) {
                message = localizer.getMessage("lblPutCardInWhichPlayerZone", targetZone.getTranslatedName().toLowerCase());
            } else {
                if (noTriggers) {
                    message = localizer.getMessage("lblPutCardInWhichPlayerBattlefield");
                } else {
                    message = localizer.getMessage("lblPutCardInWhichPlayerPlayOrStack");
                }
            }

            Player pOld = lastAddedPlayer;
            if (repeatLast) {
                if (pOld == null) {
                    return;
                }
            } else {
                GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());
                PlayerView pv = getGui().oneOrNone(message, gameCachePlayer.getTrackableKeys());
                if (pv == null || !gameCachePlayer.containsKey(pv)) {
                    return;
                }
                pOld = gameCachePlayer.get(pv);
            }
            final Player p = pOld;


            final CardDb carddb = FModel.getMagicDb().getCommonCards();
            final List<ICardFace> faces = Lists.newArrayList(carddb.getAllFaces());

            List<CardFaceView> choices = new ArrayList<>();
            CardFaceView cardFaceView;
            for (ICardFace cardFace : faces) {
                cardFaceView = new CardFaceView(CardTranslation.getTranslatedName(cardFace.getName()), cardFace.getName());
                choices.add(cardFaceView);
            }
            Collections.sort(choices);

            // use standard forge's list selection dialog
            final CardFaceView f = repeatLast ? lastAdded : getGui().oneOrNone(localizer.getMessage("lblNameTheCard"), choices);
            if (f == null) {
                return;
            }

            PaperCard c = carddb.getUniqueByName(f.getOracleName());
            final Card forgeCard = Card.fromPaperCard(c, p);
            forgeCard.setGameTimestamp(getGame().getNextTimestamp());

            PaperCard finalC = c;
            getGame().getAction().invoke(() -> {
                if (targetZone == ZoneType.Battlefield) {
                    if (!forgeCard.getName().equals(f.getName())) {
                        if (forgeCard.getRules().getSplitType().equals(CardSplitType.Specialize)) {
                            for (Map.Entry<CardStateName, ICardFace> e : forgeCard.getRules().getSpecializeParts().entrySet()) {
                                if (f.getName().equals(e.getValue().getName())) {
                                    forgeCard.changeToState(e.getKey());
                                    break;
                                }
                            }
                        } else {
                            forgeCard.changeToState(forgeCard.getRules().getSplitType().getChangedStateName());
                            if (forgeCard.getCurrentStateName().equals(CardStateName.Backside)) {
                                forgeCard.setBackSide(true);
                            }
                        }
                    }

                    if (noTriggers) {
                        if (forgeCard.isPermanent() && !forgeCard.isAura()) {
                            if (forgeCard.isCreature()) {
                                if (!repeatLast) {
                                    if (forgeCard.hasKeyword(Keyword.HASTE)) {
                                        lastSummoningSickness = true;
                                    } else {
                                        lastSummoningSickness = getGui().confirm(forgeCard.getView(),
                                                localizer.getMessage("lblCardShouldBeSummoningSicknessConfirm", CardTranslation.getTranslatedName(forgeCard.getName())));
                                    }
                                }
                            }
                            getGame().getAction().moveTo(targetZone, forgeCard, null, AbilityKey.newMap());
                            if (forgeCard.isCreature()) {
                                forgeCard.setSickness(lastSummoningSickness);
                            }
                        } else {
                            getGui().message(localizer.getMessage("lblChosenCardNotPermanentorCantExistIndependentlyontheBattleground"), localizer.getMessage("lblError"));
                            return;
                        }
                    } else {
                        if (finalC.getRules().getType().isLand()) {
                            // this is needed to ensure land abilities fire
                            getGame().getAction().moveToHand(forgeCard, null);
                            getGame().getAction().moveToPlay(forgeCard, null, null);
                            // ensure triggered abilities fire
                            getGame().getTriggerHandler().runWaitingTriggers();
                        } else {
                            final FCollectionView<SpellAbility> choices1 = forgeCard.getBasicSpells();
                            if (choices1.isEmpty()) {
                                return; // when would it happen?
                            }

                            final SpellAbility sa;
                            if (choices1.size() == 1) {
                                sa = choices1.iterator().next();
                            } else {
                                sa = repeatLast ? lastAddedSA : getGui().oneOrNone(localizer.getMessage("lblChoose"), (FCollection<SpellAbility>) choices1);
                            }
                            if (sa == null) {
                                return; // happens if cancelled
                            }

                            lastAddedSA = sa;

                            // this is really needed (for rollbacks at least)
                            getGame().getAction().moveToHand(forgeCard, null);
                            // Human player is choosing targets for an ability
                            // controlled by chosen player.
                            sa.setActivatingPlayer(p);
                            sa.setCastFromPlayEffect(true);
                            HumanPlay.playSaWithoutPayingManaCost(PlayerControllerHuman.this, sa, true);
                        }
                        // playSa could fire some triggers
                        getGame().getStack().addAllTriggeredAbilitiesToStack();
                    }
                } else if (targetZone == ZoneType.Library) {
                    if (!repeatLast) {
                        lastTopOfTheLibrary = getGui().confirm(forgeCard.getView(), localizer.getMessage("lblCardShouldBeAddedToLibraryTopOrBottom", CardTranslation.getTranslatedName(forgeCard.getName())),
                                true, Arrays.asList(localizer.getMessage("lblTop"), localizer.getMessage("lblBottom")));
                    }
                    if (lastTopOfTheLibrary) {
                        getGame().getAction().moveToLibrary(forgeCard, null);
                    } else {
                        getGame().getAction().moveToBottomOfLibrary(forgeCard, null);
                    }
                } else {
                    getGame().getAction().moveTo(targetZone, forgeCard, null, AbilityKey.newMap());
                }

                lastAdded = f;
                lastAddedZone = targetZone;
                lastAddedPlayer = p;
                lastTrigs = noTriggers;
            });
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#exileCardsFromHand()
         */
        @Override
        public void exileCardsFromHand() {
            GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());

            final PlayerView pv = getGui().oneOrNone(localizer.getMessage("lblExileCardsFromPlayerHandConfirm"),
                    gameCachePlayer.getTrackableKeys());
            if (pv == null || !gameCachePlayer.containsKey(pv)) {
                return;
            }
            Player p = gameCachePlayer.get(pv);

            CardCollectionView inHand = p.getCardsIn(ZoneType.Hand);
            GameEntityViewMap<Card, CardView> gameCacheExile = GameEntityView.getMap(inHand);

            List<CardView> views = getGui().many(localizer.getMessage("lblChooseCardsExile"), localizer.getMessage("lblDiscarded"), 0, inHand.size(),
                    gameCacheExile.getTrackableKeys(), null);

            final CardCollection selection = new CardCollection();
            gameCacheExile.addToList(views, selection);

            for (Card c : selection) {
                if (c == null) {
                    continue;
                }
                if (getGame().getAction().moveTo(ZoneType.Exile, c, null, AbilityKey.newMap()) != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(p).append(" exiles ").append(c).append(" due to Dev Cheats.");
                    getGame().getGameLog().add(GameLogEntryType.DISCARD, sb.toString());
                } else {
                    getGame().getGameLog().add(GameLogEntryType.INFORMATION, "DISCARD CHEAT ERROR");
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#exileCardsFromBattlefield()
         */
        @Override
        public void exileCardsFromBattlefield() {
            GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());

            final PlayerView pv = getGui().oneOrNone(localizer.getMessage("lblExileCardsFromPlayerBattlefieldConfirm"),
                    gameCachePlayer.getTrackableKeys());
            if (pv == null || !gameCachePlayer.containsKey(pv)) {
                return;
            }
            Player p = gameCachePlayer.get(pv);

            CardCollectionView otb = p.getCardsIn(ZoneType.Battlefield);
            GameEntityViewMap<Card, CardView> gameCacheExile = GameEntityView.getMap(otb);

            List<CardView> views = getGui().many(localizer.getMessage("lblChooseCardsExile"), localizer.getMessage("lblDiscarded"), 0, otb.size(),
                    gameCacheExile.getTrackableKeys(), null);

            final CardCollection selection = new CardCollection();
            gameCacheExile.addToList(views, selection);

            for (Card c : selection) {
                if (c == null) {
                    continue;
                }
                if (getGame().getAction().moveTo(ZoneType.Exile, c, null, AbilityKey.newMap()) != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(p).append(" exiles ").append(c).append(" due to Dev Cheats.");
                    getGame().getGameLog().add(GameLogEntryType.ZONE_CHANGE, sb.toString());
                } else {
                    getGame().getGameLog().add(GameLogEntryType.INFORMATION, "EXILE FROM PLAY CHEAT ERROR");
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#removeCardsFromGame()
         */
        @Override
        public void removeCardsFromGame() {
            GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());

            final PlayerView pv = getGui().oneOrNone(localizer.getMessage("lblRemoveCardBelongingWitchPlayer"),
                    gameCachePlayer.getTrackableKeys());
            if (pv == null || !gameCachePlayer.containsKey(pv)) {
                return;
            }
            Player p = gameCachePlayer.get(pv);

            final String zone = getGui().one(localizer.getMessage("lblRemoveCardFromWhichZone"),
                    Arrays.asList("Hand", "Battlefield", "Library", "Graveyard", "Exile"));

            CardCollectionView cards = p.getCardsIn(ZoneType.smartValueOf(zone));
            GameEntityViewMap<Card, CardView> gameCacheExile = GameEntityView.getMap(cards);
            List<CardView> views = getGui().many(localizer.getMessage("lblChooseCardsRemoveFromGame"), localizer.getMessage("lblRemoved"), 0, cards.size(),
                    gameCacheExile.getTrackableKeys(), null);

            final CardCollection selection = new CardCollection();
            gameCacheExile.addToList(views, selection);

            for (Card c : selection) {
                if (c == null) {
                    continue;
                }
                c.getGame().getAction().ceaseToExist(c, true);

                StringBuilder sb = new StringBuilder();
                sb.append(p).append(" removes ").append(c).append(" from game due to Dev Cheats.");
                getGame().getGameLog().add(GameLogEntryType.ZONE_CHANGE, sb.toString());
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#riggedPlanarRoll()
         */
        @Override
        public void riggedPlanarRoll() {
            GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(getGame().getPlayers());

            final PlayerView pv = getGui().oneOrNone(localizer.getMessage("lblWhichPlayerShouldRoll"), gameCachePlayer.getTrackableKeys());
            if (pv == null || !gameCachePlayer.containsKey(pv)) {
                return;
            }
            final Player player = gameCachePlayer.get(pv);

            final PlanarDice res = getGui().oneOrNone(localizer.getMessage("lblChooseResult"), PlanarDice.values);
            if (res == null) {
                return;
            }

            System.out.println("Rigging planar dice roll: " + res.toString());

            getGame().getAction().invoke(() -> PlanarDice.roll(player, res));
        }

        /*
         * (non-Javadoc)
         *
         * @see forge.player.IDevModeCheats#planeswalkTo()
         */
        @Override
        public void planeswalkTo() {
            if (!getGame().getRules().hasAppliedVariant(GameType.Planechase)) {
                return;
            }
            final Player p = getGame().getPhaseHandler().getPlayerTurn();

            final List<PaperCard> allPlanars = Lists.newArrayList();
            for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                if (c.getRules().getType().isPlane() || c.getRules().getType().isPhenomenon()) {
                    allPlanars.add(c);
                }
            }
            Collections.sort(allPlanars);

            // use standard forge's list selection dialog
            final IPaperCard c = getGui().oneOrNone(localizer.getMessage("lblNameTheCard"), allPlanars);
            if (c == null) {
                return;
            }
            final Card forgeCard = Card.fromPaperCard(c, p);

            getGame().getAction().invoke(() -> {
                getGame().getAction().changeZone(null, p.getZone(ZoneType.PlanarDeck), forgeCard, 0, null);
                PlanarDice.roll(p, PlanarDice.Planeswalk);
            });
        }

        public void askAI(boolean useSimulation) {
            PlayerControllerAi ai = new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer());
            ai.setUseSimulation(useSimulation);
            player.runWithController(() -> {
                List<SpellAbility> sas = ai.chooseSpellAbilityToPlay();
                SpellAbility chosen = sas == null ? null : sas.get(0);
                getGui().message(chosen == null ? "AI doesn't want to play anything right now" : chosen.getHostCard().toString(), "AI Play Suggestion");
            }, ai);
        }
    }

    private IMacroSystem macros;

    @Override
    public IMacroSystem macros() {
        if (macros == null) {
            //macros = new BasicMacroSystem(this);
            macros = new RecordActionsMacroSystem(this);
        }
        return macros;
    }

    @Override
    public void concede() {
        if (player != null) {
            player.concede();
            getGame().getAction().checkGameOverCondition();
        }
    }

    public boolean mayAutoPass() {
        return getGui().mayAutoPass(getLocalPlayerView());
    }

    public void autoPassUntilEndOfTurn() {
        getGui().autoPassUntilEndOfTurn(getLocalPlayerView());
    }

    @Override
    public void autoPassCancel() {
        if (getGui() == null) {
            return;
        }

        getGui().autoPassCancel(getLocalPlayerView());
    }

    @Override
    public void awaitNextInput() {
        getGui().awaitNextInput();
    }

    @Override
    public void cancelAwaitNextInput() {
        getGui().cancelAwaitNextInput();
    }

    @Override
    public void resetInputs() {
        final Input inp = inputProxy.getInput();
        if (inp != null) {
            inp.selectButtonCancel();
        }
    }

    @Override
    public void nextGameDecision(final NextGameDecision decision) {
        gameView.getMatch().fireEvent(new UiEventNextGameDecision(this, decision));
    }

    @Override
    public String getActivateDescription(final CardView card) {
        return getInputProxy().getActivateAction(card);
    }

    @Override
    public void reorderHand(final CardView card, final int index) {
        final PlayerZone hand = player.getZone(ZoneType.Hand);
        hand.reorder(getCard(card), index);
        player.updateZoneForView(hand);
    }

    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        ICardFace face = chooseSingleCardFace(sa, faces, message);
        return face == null ? "" : face.getName();
    }

    @Override
    public Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message) {
        PaperCard dungeon = getGui().one(message, dungeonCards);
        return Card.fromPaperCard(dungeon, player);
    }

    @Override
    public List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        GameEntityViewMap<Card, CardView> gameCacheSplice = GameEntityView.getMap(cards);

        List<CardView> chosen = getGui().many(
                localizer.getMessage("lblChooseCardstoSpliceonto"),
                localizer.getMessage("lblChosenCards"),
                0,
                gameCacheSplice.size(),
                gameCacheSplice.getTrackableKeys(),
                sa.getHostCard().getView()
        );

        List<Card> chosenCards = new CardCollection();
        gameCacheSplice.addToList(chosen, chosenCards);
        return chosenCards;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.game.player.PlayerController#chooseOptionalCosts(forge.game.
     * spellability.SpellAbility, java.util.List)
     */
    @Override
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen, List<OptionalCostValue> optionalCost) {
        return getGui().many(localizer.getMessage("lblChooseOptionalCosts"), localizer.getMessage("lblOptionalCosts"), 0, optionalCost.size(),
                optionalCost, choosen.getHostCard().getView());
    }

    @Override
    public boolean confirmMulliganScry(Player p) {
        return InputConfirm.confirm(this, (SpellAbility) null, localizer.getMessage("lblDoYouWanttoScry"));
    }

    @Override
    public int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max) {
        if (max <= 0) {
            return 0;
        }
        if (max == 1) {
            return InputConfirm.confirm(this, sa, prompt) ? 1 : 0;
        }

        Integer v = getGui().getInteger(prompt, 0, max, 9);
        return v == null ? 0 : v;
    }

    @Override
    public int chooseNumberForCostReduction(final SpellAbility sa, final int min, final int max) {
        if (isFullControl(FullControlFlag.ChooseCostReductionOrderAndVariableAmount)) {
            return chooseNumber(sa, localizer.getMessage("lblChooseAmountCostReduction"), min, max);
        }
        return max;
    }

    @Override
    public CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional) {
        CardCollection result = new CardCollection();
        for (Map.Entry<String, CardCollection> e : validMap.entrySet()) {
            result.addAll(chooseCardsForEffect(e.getValue(), sa, title + " (" + e.getKey() + ")", 0, 1, isOptional, null));
        }
        return result;
    }

    public Card getCard(final CardView cardView) {
        return getGame().findByView(cardView);
    }

    public CardCollection getCardList(Iterable<CardView> cardViews) {
        CardCollection result = new CardCollection();
        for (CardView cardView : cardViews) {
            final Card c = this.getCard(cardView);
            if (c != null) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public boolean isOrderedZone() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_ORDER_HAND);
    }

}

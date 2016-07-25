package forge.ai;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import forge.LobbyPlayer;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.ability.ProtectAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
import forge.util.MyRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * A prototype for player controller class
 * 
 * Handles phase skips for now.
 */
public class PlayerControllerAi extends PlayerController {
    private final AiController brains;

    public PlayerControllerAi(Game game, Player p, LobbyPlayer lp) {
        super(game, p, lp);

        brains = new AiController(p, game);
    }

    public void allowCheatShuffle(boolean value) {
        brains.allowCheatShuffle(value);
    }

    public void setUseSimulation(boolean value) {
        brains.setUseSimulation(value);
    }

    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (abilities.size() == 0) {
            return null;
        } else {
            return abilities.get(0);
        }
    }

    public AiController getAi() {
        return brains;
    }

    @Override
    public boolean isAI() {
        return true;
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType) {
        // AI does not know how to sideboard
        return null;
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, int damageDealt,
            GameEntity defender, boolean overrideOrder) {
        return ComputerUtilCombat.distributeAIDamage(attacker, blockers, damageDealt, defender, overrideOrder);
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero) {
        // For now, these "announcements" are made within the AI classes of the
        // appropriate SA effects
        if (ability.getApi() != null) {
            switch (ability.getApi()) {
            case ChooseNumber:
                return ability.getActivatingPlayer().isOpponentOf(player) ? 0
                        : ComputerUtilMana.determineLeftoverMana(ability, player);
            case BidLife:
                return 0;
            default:
                return null;
            }
        }
        return null; // return incorrect value to indicate that
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max,
            CardCollectionView validTargets, String message) {
        return ComputerUtil.choosePermanentsToSacrifice(player, validTargets, max, sa, false, min == 0);
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max,
            CardCollectionView validTargets, String message) {
        return ComputerUtil.choosePermanentsToSacrifice(player, validTargets, max, sa, true, min == 0);
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title,
            int min, int max, boolean isOptional) {
        return brains.chooseCardsForEffect(sourceList, sa, min, max, isOptional);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
            DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(),
                    delayedReveal.getMessagePrefix());
        }
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseSingleEntity(player, sa, (FCollection<T>) optionList, isOptional,
                targetedPlayer);
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(java.util.List<SpellAbility> spells, SpellAbility sa, String title) {
        ApiType api = sa.getApi();
        if (null == api) {
            throw new InvalidParameterException("SA is not api-based, this is not supported yet");
        }
        return SpellApiToAi.Converter.get(api).chooseSingleSpellAbility(player, sa, spells);
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return getAi().confirmAction(sa, mode, message);
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode mode, String string, int bid,
            Player winner) {
        return getAi().confirmBidAction(sa, mode, string, bid, winner);
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
        return getAi().confirmStaticApplication(hostCard, affected, logic, message);
    }

    @Override
    public boolean confirmTrigger(SpellAbility sa, Trigger regtrig, Map<String, String> triggerParams,
            boolean isMandatory) {
        if (sa.getHostCard().getName().equals("Deathmist Raptor")) {
            return true;
        }
        if (triggerParams.containsKey("DelayedTrigger") || isMandatory) {
            // TODO: The only card with an optional delayed trigger is Shirei,
            // Shizo's Caretaker,
            // needs to be expanded when a more difficult cards comes up
            return true;
        }
        // Store/replace target choices more properly to get this SA cleared.
        TargetChoices tc = null;
        TargetChoices subtc = null;
        boolean storeChoices = sa.getTargetRestrictions() != null;
        final SpellAbility sub = sa.getSubAbility();
        boolean storeSubChoices = sub != null && sub.getTargetRestrictions() != null;
        boolean ret = true;

        if (storeChoices) {
            tc = sa.getTargets();
            sa.resetTargets();
        }
        if (storeSubChoices) {
            subtc = sub.getTargets();
            sub.resetTargets();
        }
        // There is no way this doTrigger here will have the same target as
        // stored above
        // So it's possible it's making a different decision here than will
        // actually happen
        if (!brains.doTrigger(sa, false)) {
            ret = false;
        }
        if (storeChoices) {
            sa.resetTargets();
            sa.setTargets(tc);
        }
        if (storeSubChoices) {
            sub.resetTargets();
            sub.setTargets(subtc);
        }

        return ret;
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstGame) {
        return this.player; // AI is brave :)
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        return AiBlockController.orderBlockers(attacker, blockers);
    }

    @Override
    public CardCollection orderBlocker(Card attacker, Card blocker, CardCollection oldBlockers) {
        return AiBlockController.orderBlocker(attacker, blocker, oldBlockers);
    };

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        return AiBlockController.orderAttackers(blocker, attackers);
    }

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix) {
        // We don't know how to reveal cards to AI
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix) {
        // We don't know how to reveal cards to AI
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        CardCollection toBottom = new CardCollection();
        CardCollection toTop = new CardCollection();

        for (Card c : topN) {
            if (ComputerUtil.scryWillMoveCardToBottomOfLibrary(player, c)) {
                toBottom.add(c);
            } else {
                toTop.add(c);
            }
        }

        // put the rest on top in random order
        Collections.shuffle(toTop);
        return ImmutablePair.of(toTop, toBottom);
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        return true; // AI does not know what will happen next (another clash or
                     // that would become his topdeck)
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone) {
        // TODO Add logic for AI ordering here
        return cards;
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(Player p, SpellAbility sa, CardCollection validCards, int min,
            int max) {
        if (p == player) {
            return brains.getCardsToDiscard(min, max, validCards, sa);
        }

        boolean isTargetFriendly = !p.isOpponentOf(player);

        return isTargetFriendly ? ComputerUtil.getCardsToDiscardFromFriend(player, p, sa, validCards, min, max)
                : ComputerUtil.getCardsToDiscardFromOpponent(player, p, sa, validCards, min, max);
    }

    @Override
    public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChooseNewTargets) {
        // Ai is known to set targets in doTrigger, so if it cannot choose new
        // targets, we won't call canPlays
        if (mayChooseNewTargets) {
            if (copySA instanceof Spell) {
                Spell spell = (Spell) copySA;
                ((PlayerControllerAi) player.getController()).getAi().canPlayFromEffectAI(spell, true, true);
            } else {
                getAi().canPlaySa(copySA);
            }
        }
        ComputerUtil.playSpellAbilityForFree(player, copySA);
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean canSetupTargets) {
        if (canSetupTargets)
            brains.doTrigger(effectSA, true); // first parameter does not
                                              // matter, since return value
                                              // won't be used
        ComputerUtil.playNoStack(player, effectSA, game);
    }

    @Override
    public void playMiracle(SpellAbility miracle, Card card) {
        getAi().chooseAndPlaySa(false, false, miracle);
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        return getAi().chooseCardsToDelve(genericAmount, grave);
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability) {
        // AI currently can't do this. But when it can it will need to be based
        // on Ability API
        return null;
    }

    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int num, CardCollectionView hand, String uType,
            SpellAbility sa) {
        final CardCollectionView cardsOfType = CardLists.getType(hand, uType);
        if (!cardsOfType.isEmpty()) {
            Card toDiscard = Aggregates.itemWithMin(cardsOfType, CardPredicates.Accessors.fnGetCmc);
            return new CardCollection(toDiscard);
        }
        return getAi().getCardsToDiscard(num, (String[]) null, sa);
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        return manaChoices.get(0); // no brains used
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes,
            boolean isOptional) {
        String chosen = ComputerUtil.chooseSomeType(player, kindOfType, sa.getParam("AILogic"), invalidTypes);
        if (StringUtils.isBlank(chosen) && !validTypes.isEmpty()) {
            chosen = validTypes.get(0);
            Log.warn("AI has no idea how to choose " + kindOfType + ", defaulting to 1st element: chosen");
        }
        game.getAction().nofityOfValue(sa, null, "Computer picked: " + chosen, player);
        return chosen;
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes) {
        return ComputerUtil.vote(player, options, sa, votes);
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA,
            String question) {
        return brains.aiShouldRun(replacementEffect, effectSA);
    }

    @Override
    public CardCollectionView getCardsToMulligan(Player firstPlayer) {
        if (!ComputerUtil.wantMulligan(player)) {
            return null;
        }

        return player.getCardsIn(ZoneType.Hand);
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        brains.declareAttackers(attacker, combat);
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        brains.declareBlockersFor(defender, combat);
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        return brains.chooseSpellAbilityToPlay();
    }

    @Override
    public void playChosenSpellAbility(SpellAbility sa) {
        // System.out.println("Playing sa: " + sa);
        if (sa == sa.getHostCard().getGame().PLAY_LAND_SURROGATE) {
            player.playLand(sa.getHostCard(), false);
        } else {
            ComputerUtil.handlePlayingSpellAbility(player, sa, game);
        }
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        return brains.getCardsToDiscard(numDiscard, (String[]) null, null);
    }

    @Override
    public CardCollection chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        int numCardsToReveal = Math.min(max, valid.size());
        return numCardsToReveal == 0 ? new CardCollection() : (CardCollection) valid.subList(0, numCardsToReveal);
    }

    @Override
    public boolean payManaOptional(Card c, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
        final Ability ability = new AbilityStatic(c, cost, null) {
            @Override
            public void resolve() {
            }
        };
        ability.setActivatingPlayer(c.getController());

        if (ComputerUtilCost.canPayCost(ability, c.getController())) {
            ComputerUtil.playNoStack(c.getController(), ability, game);
            return true;
        }
        return false;
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        // AI would play everything. But limits to one copy of (Leyline of
        // Singularity) and (Gemstone Caverns)
        return brains.chooseSaToActivateFromOpeningHand(usableFromOpeningHand);
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        return brains.chooseNumber(sa, title, min, max);
    }

    public int chooseNumber(SpellAbility sa, String title, List<Integer> options, Player relatedPlayer) {
        return brains.chooseNumber(sa, title, options, relatedPlayer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.game.player.PlayerController#chooseFlipResult(forge.Card,
     * forge.game.player.Player, java.lang.String[], boolean)
     */
    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        if (call) {
            // Win if possible
            boolean result = false;
            for (boolean s : results) {
                if (s) {
                    result = s;
                    break;
                }
            }
            return result;
        } else {
            // heads or tails, AI doesn't know which is better now
            int i = MyRandom.getRandom().nextInt(results.length);
            return results[i];
        }
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility saSrc,
            List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        // TODO Teach AI how to use Spellskite
        return allTargets.get(0);
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        // AI should take into consideration creature types, numbers and other
        // information (mostly choices) arriving through this channel
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        switch (kindOfChoice) {
        case TapOrUntap:
            return true;
        case UntapOrLeaveTapped:
            return defaultVal != null && defaultVal.booleanValue();
        case UntapTimeVault:
            return false; // TODO Should AI skip his turn for time vault?
        case LeftOrRight:
            return brains.chooseDirection(sa);
        default:
            return MyRandom.getRandom().nextBoolean();
        }
    }

    @Override
    public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options,
            Map<String, Card> choiceMap) {
        int i = MyRandom.getRandom().nextInt(options.size());
        return choiceMap.get(options.get(i));
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num, boolean allowRepeat) {
        /**
         * Called when CharmEffect resolves for the AI to select its choices.
         * The list of chosen options (sa.getChosenList()) should be set by
         * CharmAi.canPlayAi() for cast spells while CharmAi.doTrigger() deals
         * with triggers. The logic in CharmAi should only be called once to
         * account for probabilistic choices that may result in different
         * results in subsequent calls.
         */
        if (sa.getChosenList() == null) {
            getAi().doTrigger(sa, true);
        }
        return sa.getChosenList();
    }

    @Override
    public Pair<CounterType, String> chooseAndRemoveOrPutCounter(Card cardWithCounter) {
        if (!cardWithCounter.hasCounters()) {
            System.out.println(
                    "chooseCounterType was reached with a card with no counters on it. Consider filtering this card out earlier");
            return null;
        }

        final Player controller = cardWithCounter.getController();
        final List<Player> enemies = player.getOpponents();
        final List<Player> allies = player.getAllies();
        allies.add(player);

        List<CounterType> countersToIncrease = new ArrayList<CounterType>();
        List<CounterType> countersToDecrease = new ArrayList<CounterType>();

        for (final CounterType counter : cardWithCounter.getCounters().keySet()) {
            if ((!ComputerUtil.isNegativeCounter(counter, cardWithCounter) && allies.contains(controller))
                    || (ComputerUtil.isNegativeCounter(counter, cardWithCounter) && enemies.contains(controller))) {
                countersToIncrease.add(counter);
            } else {
                countersToDecrease.add(counter);
            }
        }

        if (!countersToIncrease.isEmpty()) {
            int random = MyRandom.getRandom().nextInt(countersToIncrease.size());
            return new ImmutablePair<CounterType, String>(countersToIncrease.get(random), "Put");
        } else if (!countersToDecrease.isEmpty()) {
            int random = MyRandom.getRandom().nextInt(countersToDecrease.size());
            return new ImmutablePair<CounterType, String>(countersToDecrease.get(random), "Remove");
        }

        // shouldn't reach here but just in case, remove random counter
        List<CounterType> countersOnCard = new ArrayList<CounterType>();
        int random = MyRandom.getRandom().nextInt(countersOnCard.size());
        return new ImmutablePair<CounterType, String>(countersOnCard.get(random), "Remove");
    }

    @Override
    public byte chooseColorAllowColorless(String message, Card card, ColorSet colors) {
        final String c = ComputerUtilCard.getMostProminentColor(player.getCardsIn(ZoneType.Hand));
        byte chosenColorMask = MagicColor.fromName(c);
        if ((colors.getColor() & chosenColorMask) != 0) {
            return chosenColorMask;
        } else {
            return Iterables.getFirst(colors, (byte) 0);
        }
    }

    @Override
    public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
        // You may switch on sa.getApi() here and use sa.getParam("AILogic")
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        if (sa.getApi() == ApiType.Mana) {
            hand = CardCollection.combine(hand, player.getCardsIn(ZoneType.Stack));
        }
        final String c = ComputerUtilCard.getMostProminentColor(hand);
        byte chosenColorMask = MagicColor.fromName(c);

        if ((colors.getColor() & chosenColorMask) != 0) {
            return chosenColorMask;
        } else {
            return Iterables.getFirst(colors, MagicColor.WHITE);
        }
    }

    @Override
    public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
        throw new UnsupportedOperationException("Should not be called for AI"); // or
                                                                                // implement
                                                                                // it
                                                                                // if
                                                                                // you
                                                                                // know
                                                                                // how
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        return ComputerUtilCard.chooseColor(sa, min, max, options);
    }

    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt) {
        // may write a smarter AI if you need to (with calls to AI-clas for
        // given API ability)

        // TODO: ArsenalNut (06 Feb 12)computer needs
        // better logic to pick a counter type and probably
        // an initial target
        // find first nonzero counter on target
        return Iterables.getFirst(options, null);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String prompt) {
        return brains.confirmPayment(costPart); // AI is expected to know what
                                                // it is paying for at the
                                                // moment (otherwise add another
                                                // parameter to this method)
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt, List<ReplacementEffect> possibleReplacers,
            Map<String, Object> runParams) {
        // AI logic for choosing which replacement effect to apply
        // happens here.
        return possibleReplacers.get(0);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        String choice = choices.get(0);
        SpellAbility hostsa = null; // for Protect sub-ability
        if (game.stack.size() > 1) {
            for (SpellAbilityStackInstance si : game.getStack()) {
                SpellAbility spell = si.getSpellAbility(true);
                if (sa != spell && sa.getHostCard() != spell.getHostCard()) {
                    String s = ProtectAi.toProtectFrom(spell.getHostCard(), sa);
                    if (s != null) {
                        return s;
                    }
                    break;
                }
            }
        }
        final Combat combat = game.getCombat();
        if (combat != null) {
            if (game.stack.size() == 1) {
                SpellAbility topstack = game.stack.peekAbility();
                if (topstack.getSubAbility() == sa) {
                    hostsa = topstack;
                }
            }
            Card toSave = hostsa == null ? sa.getTargetCard() : hostsa.getTargetCard();
            CardCollection threats = null;
            if (toSave != null) {
                if (combat.isBlocked(toSave)) {
                    threats = combat.getBlockers(toSave);
                }
                if (combat.isBlocking(toSave)) {
                    threats = combat.getAttackersBlockedBy(toSave);
                }
            }
            if (threats != null && !threats.isEmpty()) {
                ComputerUtilCard.sortByEvaluateCreature(threats);
                String s = ProtectAi.toProtectFrom(threats.get(0), sa);
                if (s != null) {
                    return s;
                }
            }
        }
        final PhaseHandler ph = game.getPhaseHandler();
        if (ph.getPlayerTurn() == sa.getActivatingPlayer() && ph.getPhase() == PhaseType.MAIN1
                && sa.getTargetCard() != null) {
            AiAttackController aiAtk = new AiAttackController(sa.getActivatingPlayer(), sa.getTargetCard());
            String s = aiAtk.toProtectAttacker(sa);
            if (s != null) {
                return s;
            }
        }
        final String logic = sa.getParam("AILogic");
        if (logic == null || logic.equals("MostProminentHumanCreatures")) {
            CardCollection list = new CardCollection();
            for (Player opp : player.getOpponents()) {
                list.addAll(opp.getCreaturesInPlay());
            }
            if (list.isEmpty()) {
                list = CardLists.filterControlledBy(game.getCardsInGame(), player.getOpponents());
            }
            if (!list.isEmpty()) {
                choice = ComputerUtilCard.getMostProminentColor(list);
            }
        }
        return choice;
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid,
            FCollectionView<Player> allPayers) {
        final Card source = sa.getHostCard();
        final Ability emptyAbility = new AbilityStatic(source, cost, sa.getTargetRestrictions()) {
            @Override
            public void resolve() {
            }
        };
        emptyAbility.setActivatingPlayer(player);
        if (ComputerUtilCost.willPayUnlessCost(sa, player, cost, alreadyPaid, allPayers)
                && ComputerUtilCost.canPayCost(emptyAbility, player)) {
            ComputerUtil.playNoStack(player, emptyAbility, game); // AI needs
                                                                  // something
                                                                  // to resolve
                                                                  // to pay that
                                                                  // cost
            return true;
        }
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        for (final SpellAbility sa : activePlayerSAs) {
            prepareSingleSa(sa.getHostCard(), sa, true);
            ComputerUtil.playStack(sa, player, game);
        }
    }

    private void prepareSingleSa(final Card host, final SpellAbility sa, boolean isMandatory) {
        if (sa.hasParam("TargetingPlayer")) {
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(host, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            targetingPlayer.getController().chooseTargetsFor(sa);
        } else {
            brains.doTrigger(sa, isMandatory);
        }
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        prepareSingleSa(host, wrapperAbility, isMandatory);
        ComputerUtil.playNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, game);
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean optional = tgtSA.hasParam("Optional");
        boolean noManaCost = tgtSA.hasParam("WithoutManaCost");
        if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
            Spell spell = (Spell) tgtSA;
            if (brains.canPlayFromEffectAI(spell, !optional, noManaCost) == AiPlayDecision.WillPlay || !optional) {
                if (noManaCost) {
                    ComputerUtil.playSpellAbilityWithoutPayingManaCost(player, tgtSA, game);
                } else {
                    ComputerUtil.playStack(tgtSA, player, game);
                }
            } else
                return false; // didn't play spell
        }
        return true;
    }

    @Override
    public Map<GameEntity, CounterType> chooseProliferation() {
        return brains.chooseProliferation();
    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        return brains.doTrigger(currentAbility, true);
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2,
            boolean faceUp) {
        if (!faceUp) {
            // AI will choose the first pile if it is larger or the same
            // TODO Improve this to be slightly more random to not be so
            // predictable
            return pile1.size() >= pile2.size();
        } else {
            boolean allCreatures = Iterables.all(Iterables.concat(pile1, pile2), CardPredicates.Presets.CREATURES);
            int cmc1 = allCreatures ? ComputerUtilCard.evaluateCreatureList(pile1)
                    : ComputerUtilCard.evaluatePermanentList(pile1);
            int cmc2 = allCreatures ? ComputerUtilCard.evaluateCreatureList(pile2)
                    : ComputerUtilCard.evaluatePermanentList(pile2);
            System.out.println("value:" + cmc1 + " " + cmc2);

            // for now, this assumes that the outcome will be bad
            // TODO: This should really have a ChooseLogic param to
            // figure this out
            return "Worst".equals(sa.getParam("AILogic")) ^ (cmc1 >= cmc2);
        }
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // Ai won't understand that anyway
    }

    @Override
    public Collection<? extends PaperCard> complainCardsCantPlayWell(Deck myDeck) {
        return brains.complainCardsCantPlayWell(myDeck);
    }

    @Override
    public CardCollectionView cheatShuffle(CardCollectionView list) {
        return brains.getBooleanProperty(AiProps.CHEAT_WITH_MANA_ON_SHUFFLE) ? brains.cheatShuffle(list) : list;
    }

    @Override
    public CardShields chooseRegenerationShield(Card c) {
        return Iterables.getFirst(c.getShields(), null);
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        // TODO AI takes all by default
        return losses;
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa,
            String prompt /* ai needs hints as well */, boolean isActivatedSa) {
        // TODO Auto-generated method stub
        ManaCostBeingPaid cost = isActivatedSa ? ComputerUtilMana.calculateManaCost(sa, false, 0)
                : new ManaCostBeingPaid(toPay);
        return ComputerUtilMana.payManaCost(cost, sa, player);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvoke(SpellAbility sa, ManaCost manaCost,
            CardCollectionView untappedCreats0) {
        final Player ai = sa.getActivatingPlayer();
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        // Filter out mana sources that will interfere with payManaCost()
        CardCollection untappedCreats = CardLists.filter(untappedCreats0, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getManaAbilities().isEmpty();
            }
        });

        // Only convoke after attackers have been declared
        if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            return new HashMap<Card, ManaCostShard>();
        }

        // Do not convoke potential blockers until after opponent's attack
        final CardCollectionView blockers = ComputerUtilCard.getLikelyBlockers(ai, null);
        if ((ph.isPlayerTurn(ai) && ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN))
                || (!ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
            untappedCreats.removeAll((List<?>) blockers);
            // Add threatened creatures
            if (!ai.getGame().getStack().isEmpty()) {
                final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), null);
                for (Card c : blockers) {
                    if (objects.contains(c)) {
                        untappedCreats.add(c);
                    }
                }
            }
        }
        return ComputerUtilMana.getConvokeFromList(manaCost, untappedCreats);
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<PaperCard> cpp, String valid, String message) {
        if (sa.hasParam("AILogic")) {
            final String logic = sa.getParam("AILogic");
            if (logic.equals("MostProminentInComputerDeck")) {
                return ComputerUtilCard.getMostProminentCardName(player.getCardsIn(ZoneType.Library));
            } else if (logic.equals("MostProminentInHumanDeck")) {
                return ComputerUtilCard.getMostProminentCardName(player.getOpponent().getCardsIn(ZoneType.Library));
            } else if (logic.equals("MostProminentCreatureInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(player.getCardsIn(ZoneType.Library), "Creature",
                        player, sa.getHostCard());
                return ComputerUtilCard.getMostProminentCardName(cards);
            } else if (logic.equals("BestCreatureInComputerDeck")) {
                return ComputerUtilCard.getBestCreatureAI(player.getCardsIn(ZoneType.Library)).getName();
            } else if (logic.equals("RandomInComputerDeck")) {
                return Aggregates.random(player.getCardsIn(ZoneType.Library)).getName();
            } else if (logic.equals("MostProminentSpellInComputerDeck")) {
                CardCollectionView cards = CardLists.getValidCards(player.getCardsIn(ZoneType.Library),
                        "Card.Instant,Card.Sorcery", player, sa.getHostCard());
                return ComputerUtilCard.getMostProminentCardName(cards);
            }
        } else {
            CardCollectionView list = CardLists.filterControlledBy(game.getCardsInGame(), player.getOpponent());
            list = CardLists.filter(list, Predicates.not(Presets.LANDS));
            if (!list.isEmpty()) {
                return list.get(0).getName();
            }
        }
        return "Morphling";
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
            CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional,
            Player decider) {

        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(),
                    delayedReveal.getMessagePrefix());
        }
        return ChangeZoneAi.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player, decider);
    }

    @Override
    public void resetAtEndOfTurn() {
        // TODO - if card memory is ever used to remember something for longer
        // than a turn, make sure it's not reset here.
        getAi().getCardMemory().clearAllRemembered();
    }

    @Override
    public void autoPassCancel() {
        // Do nothing
    }

    @Override
    public void awaitNextInput() {
        // Do nothing
    }

    @Override
    public void cancelAwaitNextInput() {
        // Do nothing
    }
}

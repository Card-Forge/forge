package forge.gamesimulationtests.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import forge.LobbyPlayer;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.ability.DrawAi;
import forge.ai.ability.GameLossAi;
import forge.ai.ability.GameWinAi;
import forge.card.ColorSet;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.*;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.RollDiceEffect;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.Mana;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationHandler;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.*;
import forge.item.PaperCard;
import forge.player.HumanPlay;
import forge.util.Aggregates;
import forge.util.ITriggerEvent;
import forge.util.MyRandom;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;

/**
 * Default harmless implementation for tests.
 * Test-specific behaviour can easily be added by mocking (parts of) this class.
 *
 * Note that the current PlayerController implementations seem to be responsible for handling some game logic,
 * and even aside from that, they are theoretically capable of making illegal choices (which are then not blocked by the real game logic).
 * Test cases that need to override the default behaviour of this class should make sure to do so in a way that does not invalidate their correctness.
 */
public class PlayerControllerForTests extends PlayerController {
    private PlayerActions playerActions;

    public PlayerControllerForTests(Game game, Player player, LobbyPlayer lobbyPlayer) {
        super(game, player, lobbyPlayer);
    }

    public void setPlayerActions(PlayerActions playerActions) {
        this.playerActions = playerActions;
    }

    public PlayerActions getPlayerActions() {
        return playerActions;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets) {
        //TODO: eventually (when the real code is refactored) this should be handled normally...
        if (effectSA.getDescription().equals("At the beginning of your upkeep, if you have exactly 1 life, you win the game.")) {//test_104_2b_effect_may_state_that_player_wins_the_game
            HumanPlay.playSpellAbilityNoStack(null, player, effectSA, !mayChoseNewTargets);
            return;
        }
        SpellAbilityAi sai = SpellApiToAi.Converter.get(effectSA.getApi());
        if (
                (effectSA.getHostCard().getName().equals("Nefarious Lich") && sai instanceof DrawAi) ||
                (effectSA.getHostCard().getName().equals("Laboratory Maniac") && sai instanceof GameWinAi) ||
                (effectSA.getHostCard().getName().equals("Nefarious Lich") && sai instanceof ChangeZoneAi) ||
                (effectSA.getHostCard().getName().equals("Near-Death Experience") && sai instanceof  GameWinAi) ||
                (effectSA.getHostCard().getName().equals("Final Fortune") && sai instanceof GameLossAi)
        ) {//test_104_3f_if_a_player_would_win_and_lose_simultaneously_he_loses
            HumanPlay.playSpellAbilityNoStack(null, player, effectSA, !mayChoseNewTargets);
            return;
        }
        throw new IllegalStateException("Callers of this method currently assume that it performs extra functionality!");
    }

    @Override
    public List<PaperCard> sideboard(Deck deck, GameType gameType, String message) {
        return null; // refused to side
    }

    @Override
    public Map<Card, Integer> assignCombatDamage(Card attacker, CardCollectionView blockers, CardCollectionView remaining, int damageDealt, GameEntity defender, boolean overrideOrder) {
        if (blockers.size() == 1 && damageDealt == 2 && (
                (attacker.getName().equals("Grizzly Bears") && blockers.get(0).getName().equals("Ajani's Sunstriker")) ||
                (attacker.getName().equals("Ajani's Sunstriker") && blockers.get(0).getName().equals("Grizzly Bears"))
        )) {//test_104_3b_player_with_less_than_zero_life_loses_the_game_only_when_a_player_receives_priority_variant_with_combat
            Map<Card, Integer> result = new HashMap<>();
            result.put(blockers.get(0), damageDealt);
            return result;
        }
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public Map<GameEntity, Integer> divideShield(Card effectSource, Map<GameEntity, Integer> affected, int shieldAmount) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public Map<Byte, Integer> specifyManaCombo(SpellAbility sa, ColorSet colorSet, int manaAmount, boolean different) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public Integer announceRequirements(SpellAbility ability, String announce) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public CardCollectionView choosePermanentsToSacrifice(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return chooseItems(validTargets, min);
    }

    @Override
    public CardCollectionView choosePermanentsToDestroy(SpellAbility sa, int min, int max, CardCollectionView validTargets, String message) {
        return chooseItems(validTargets, min);
    }

    @Override
    public TargetChoices chooseNewTargetsFor(SpellAbility ability, Predicate<GameObject> filter, boolean optional) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
        return chooseItem(allTargets);
    }

    @Override
    public CardCollectionView chooseCardsForEffect(CardCollectionView sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional, Map<String, Object> params) {
        return chooseItems(sourceList, max);
    }

    @Override
    public List<Card> chooseContraptionsToCrank(List<Card> contraptions) {
        return contraptions;
    }

    @Override
    public boolean helpPayForAssistSpell(ManaCostBeingPaid cost, SpellAbility sa, int max, int requested) {
        // For now, don't change anything for assists in tests
        // "True" here means don't rewind spell
        return true;
    }

    @Override
    public Player choosePlayerToAssistPayment(FCollectionView<Player> optionList, SpellAbility sa, String title, int max) {
        return Iterables.getFirst(optionList, null);
    }

    @Override
    public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList, DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
        }
        return chooseItem(optionList);
    }

    @Override
    public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title,
            Map<String, Object> params) {
        return chooseItem(spells);
    }

    @Override
    public <T extends GameEntity> List<T> chooseEntitiesForEffect(FCollectionView<T> optionList, int min, int max, DelayedReveal delayedReveal, SpellAbility sa, String title, Player relatedPlayer, Map<String, Object> params) {
        // this isn't used
        return null;
    }

    @Override
    public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message, List<String> options, Card cardToShow, Map<String, Object> params) {
        return true;
    }

    @Override
    public boolean confirmBidAction(SpellAbility sa, PlayerActionConfirmMode bidlife, String string, int bid, Player winner) {
        return false;
    }

    @Override
    public boolean confirmStaticApplication(Card hostCard, PlayerActionConfirmMode mode, String message, String logic) {
        return true;
    }

    @Override
    public boolean confirmTrigger(WrappedAbility wrapper) {
        return true;
    }

    @Override
    public Player chooseStartingPlayer(boolean isFirstgame) {
        return this.player;
    }

    @Override
    public CardCollection orderBlockers(Card attacker, CardCollection blockers) {
        return blockers;
    }

    @Override
    public List<Card> exertAttackers(List<Card> attackers) {
        return Lists.newArrayList(attackers);
    }

    @Override
    public List<Card> enlistAttackers(List<Card> attackers) {
        return Lists.newArrayList();
    }

    @Override
    public CardCollection orderBlocker(final Card attacker, final Card blocker, final CardCollection oldBlockers) {
        final CardCollection allBlockers = new CardCollection(oldBlockers);
        allBlockers.add(blocker);
        return allBlockers;
    }

    @Override
    public CardCollection orderAttackers(Card blocker, CardCollection attackers) {
        return attackers;
    }

    @Override
    public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix, boolean addSuffix) {
        //nothing needs to be done here
    }

    @Override
    public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String messagePrefix, boolean addSuffix) {
        //nothing needs to be done here
    }

    @Override
    public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
        //nothing needs to be done here
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForScry(CardCollection topN) {
        return ImmutablePair.of(topN, null);
    }

    @Override
    public ImmutablePair<CardCollection, CardCollection> arrangeForSurveil(CardCollection topN) {
        return ImmutablePair.of(topN, null);
    }

    @Override
    public boolean willPutCardOnTop(Card c) {
        return false;
    }

    @Override
    public CardCollectionView orderMoveToZoneList(CardCollectionView cards, ZoneType destinationZone, SpellAbility source) {
        return cards;
    }

    @Override
    public CardCollection chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, CardCollection validCards, int min, int max) {
        return chooseItems(validCards, min);
    }

    @Override
    public CardCollectionView chooseCardsToDelve(int genericAmount, CardCollection grave) {
        return CardCollection.EMPTY;
    }

    @Override
    public CardCollectionView chooseCardsToRevealFromHand(int min, int max, CardCollectionView valid) {
        return chooseItems(valid, min);
    }

    @Override
    public CardCollectionView chooseCardsToDiscardUnlessType(int min, CardCollectionView hand, String param, SpellAbility sa) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
        return usableFromOpeningHand;
    }

    @Override
    public PlayerZone chooseStartingHand(List<PlayerZone> zones) {
        return zones.get(0);
    }

    @Override
    public Mana chooseManaFromPool(List<Mana> manaChoices) {
        return chooseItem(manaChoices);
    }

    @Override
    public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, GameEntity affected, String question) {
        return true;
    }

    @Override
    public CardCollectionView londonMulliganReturnCards(final Player mulliganingPlayer, int cardsToReturn) {
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        return hand;
    }

    @Override
    public boolean mulliganKeepHand(Player firstPlayer, int cardsToReturn) {
        return true;
    }

    @Override
    public void declareAttackers(Player attacker, Combat combat) {
        //Doing nothing is safe in most cases, but not all (creatures that must attack etc).  TODO: introduce checks?
        if (playerActions == null) {
            return;
        }
        DeclareAttackersAction declareAttackers = playerActions.getNextActionIfApplicable(player, getGame(), DeclareAttackersAction.class);
        if (declareAttackers == null) {
            return;
        }

        //TODO: check that the chosen attack configuration is legal?  (Including creatures that did not attack but should)
        //TODO: check that the chosen attack configuration was a complete match to what was requested?
        //TODO: banding (don't really care at the moment...)

        for (Map.Entry<CardSpecification, PlayerSpecification> playerAttackAssignment : declareAttackers.getPlayerAttackAssignments().entrySet()) {
            Player defender = getPlayerBeingAttacked(getGame(), player, playerAttackAssignment.getValue());
            attack(combat, playerAttackAssignment.getKey(), defender);
        }
        for (Map.Entry<CardSpecification, CardSpecification> planeswalkerAttackAssignment: declareAttackers.getPlaneswalkerAttackAssignments().entrySet()) {
            Card defender = CardSpecificationHandler.INSTANCE.find(getGame().getCardsInGame(), planeswalkerAttackAssignment.getKey());
            attack(combat, planeswalkerAttackAssignment.getKey(), defender);
        }

        if (!CombatUtil.validateAttackers(combat)) {
            throw new IllegalStateException("Illegal attack declaration!");
        }
    }

    private Player getPlayerBeingAttacked(Game game, Player attacker, PlayerSpecification defenderSpecification) {
        if (defenderSpecification != null) {
            return PlayerSpecificationHandler.INSTANCE.find(getGame().getPlayers(), defenderSpecification);
        }
        if (getGame().getPlayers().size() != 2) {
            throw new IllegalStateException("Can't use implicit defender specification in this situation!");
        }
        for (Player player : getGame().getPlayers()) {
            if (!attacker.equals(player)) {
                return player;
            }
        }
        throw new IllegalStateException("Couldn't find implicit defender!");
    }

    private void attack(Combat combat, CardSpecification attackerSpecification, GameEntity defender) {
        Card attacker = CardSpecificationHandler.INSTANCE.find(combat.getAttackingPlayer().getCreaturesInPlay(), attackerSpecification);
        if (!CombatUtil.canAttack(attacker, defender)) {
            throw new IllegalStateException(attacker + " can't attack " + defender);
        }
        combat.addAttacker(attacker, defender);
    }

    @Override
    public void declareBlockers(Player defender, Combat combat) {
        //Doing nothing is safe in most cases, but not all (creatures that must block, attackers that must be blocked etc).  TODO: legality checks?
        if (playerActions == null) {
            return;
        }
        DeclareBlockersAction declareBlockers = playerActions.getNextActionIfApplicable(player, getGame(), DeclareBlockersAction.class);
        if (declareBlockers == null) {
            return;
        }

        //TODO: check that the chosen block configuration is 100% legal?
        //TODO: check that the chosen block configuration was a 100% match to what was requested?
        //TODO: where do damage assignment orders get handled?

        for (Map.Entry<CardSpecification, Collection<CardSpecification>> blockingAssignment : declareBlockers.getBlockingAssignments().asMap().entrySet()) {
            Card attacker = CardSpecificationHandler.INSTANCE.find(combat.getAttackers(), blockingAssignment.getKey());
            for (CardSpecification blockerSpecification : blockingAssignment.getValue()) {
                Card blocker = CardSpecificationHandler.INSTANCE.find(getGame(), blockerSpecification);
                if (!CombatUtil.canBlock(attacker, blocker)) {
                    throw new IllegalStateException(blocker + " can't block " + blocker);
                }
                combat.addBlocker(attacker, blocker);
            }
        }
        String blockValidation = CombatUtil.validateBlocks(combat, player);
        if (blockValidation != null) {
            throw new IllegalStateException(blockValidation);
        }
    }

    @Override
    public List<SpellAbility> chooseSpellAbilityToPlay() {
        //TODO: This method has to return the spellability chosen by player
        // It should not play the sa right from here. The code has been left as it is to quickly adapt to changed playercontroller interface
        if (playerActions != null) {
            CastSpellFromHandAction castSpellFromHand = playerActions.getNextActionIfApplicable(player, getGame(), CastSpellFromHandAction.class);
            if (castSpellFromHand != null) {
                castSpellFromHand.castSpellFromHand(player, getGame());
            }

            ActivateAbilityAction activateAbilityAction = playerActions.getNextActionIfApplicable(player, getGame(), ActivateAbilityAction.class);
            if (activateAbilityAction != null) {
                activateAbilityAction.activateAbility(player, getGame());
            }
        }
        return null;
    }

    @Override
    public CardCollection chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
        return chooseItems(player.getZone(ZoneType.Hand).getCards(), numDiscard);
    }

    @Override
    public boolean payCombatCost(Card card, Cost cost, SpellAbility sa, String prompt) {
        throw new IllegalStateException("Callers of this method currently assume that it performs extra functionality!");
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, int min, int max) {
        return min;
    }

    @Override
    public boolean chooseBinary(SpellAbility sa, String question, BinaryChoiceType kindOfChoice, Boolean defaultVal) {
        return true;
    }

    @Override
    public boolean chooseFlipResult(SpellAbility sa, Player flipper, boolean[] results, boolean call) {
        return true;
    }

    @Override
    public List<AbilitySub> chooseModeForAbility(SpellAbility sa, List<AbilitySub> possible, int min, int num, boolean allowRepeat) {
        throw new IllegalStateException("Erring on the side of caution here...");
    }

    @Override
    public MagicColor.Color chooseColor(String message, SpellAbility sa, ColorSet colors) {
        if (colors.countColors()==0) {
            return null;
        }
        return Iterables.getFirst(colors.toEnumSet(), MagicColor.Color.WHITE);
    }

    @Override
    public MagicColor.Color chooseColorAllowColorless(String message, Card card, ColorSet colors) {
        return Iterables.getFirst(colors.toEnumSet(), MagicColor.Color.COLORLESS);
    }

    private CardCollection chooseItems(CardCollectionView items, int amount) {
        if (items == null || items.isEmpty()) {
            return new CardCollection(items);
        }
        return (CardCollection)items.subList(0, Math.max(amount, items.size()));
    }

    private <T> T chooseItem(Iterable<T> items) {
        if (items == null) {
            return null;
        }
        return Iterables.getFirst(items, null);
    }

    @Override
    public SpellAbility getAbilityToPlay(Card hostCard, List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        // Isn't this a method invocation loop? --elcnesh
        return getAbilityToPlay(hostCard, abilities);
    }

    @Override
    public String chooseSomeType(String kindOfType, SpellAbility sa, Collection<String> validTypes, boolean isOptional) {
        return chooseItem(validTypes);
    }

    @Override
    public String chooseSector(Card assignee, String ai, List<String> sectors) {
        return chooseItem(sectors);
    }

    @Override
    public int chooseSprocket(Card assignee, boolean forceDifferent) {
        return forceDifferent && assignee.getSprocket() == 1 ? 2 : 1;
    }

    @Override
    public PlanarDice choosePDRollToIgnore(List<PlanarDice> rolls) {
        return Aggregates.random(rolls);
    }

    @Override
    public Integer chooseRollToIgnore(List<Integer> rolls) {
        return Aggregates.random(rolls);
    }

    @Override
    public List<Integer> chooseDiceToReroll(List<Integer> rolls) {
        return new ArrayList<>();
    }

    @Override
    public Integer chooseRollToModify(List<Integer> rolls) {
        return Aggregates.random(rolls);
    }

    @Override
    public RollDiceEffect.DieRollResult chooseRollToSwap(List<RollDiceEffect.DieRollResult> rolls) {
        return Aggregates.random(rolls);
    }

    @Override
    public String chooseRollSwapValue(List<String> swapChoices, Integer currentResult, int power, int toughness) {
        return Aggregates.random(swapChoices);
    }

    @Override
    public Object vote(SpellAbility sa, String prompt, List<Object> options, ListMultimap<Object, Player> votes, Player forPlayer, boolean optional) {
        return chooseItem(options);
    }

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        throw new UnsupportedOperationException("No idea how a test player controller would choose colors");
    }

    @Override
    public CounterType chooseCounterType(List<CounterType> options, SpellAbility sa, String prompt, Map<String, Object> params) {
        return Iterables.getFirst(options, CounterEnumType.P1P1);
    }

    @Override
    public String chooseKeywordForPump(final List<String> options, final SpellAbility sa, final String prompt, final Card tgtCard) {
        if (options.size() <= 1) {
            return Iterables.getFirst(options, null);
        }
        return Aggregates.random(options);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String string, SpellAbility ability) {
        return true;
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(List<ReplacementEffect> possibleReplacers) {
        // TODO Auto-generated method stub
        return Iterables.getFirst(possibleReplacers, null);
    }

    @Override
    public StaticAbility chooseSingleStaticAbility(String prompt, List<StaticAbility> possibleStatics) {
        // TODO Auto-generated method stub
        return Iterables.getFirst(possibleStatics, null);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return choices.get(0);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, FCollectionView<Player> allPayers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean payCostDuringRoll(final Cost cost, final SpellAbility sa, final FCollectionView<Player> allPayers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        for (final SpellAbility sa : activePlayerSAs) {
            prepareSingleSa(sa.getHostCard(),sa,true);
            ComputerUtil.playStack(sa, player, getGame());
        }
    }

    private void prepareSingleSa(final Card host, final SpellAbility sa, boolean isMandatory){
        if (sa.hasParam("TargetingPlayer")) {
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(host, sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(targetingPlayer);
            targetingPlayer.getController().chooseTargetsFor(sa);
        } else {
            // this code is no longer possible!
            // sa.doTrigger(isMandatory, player);
        }
    }

    @Override
    public boolean playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        prepareSingleSa(host, wrapperAbility, isMandatory);
        return ComputerUtil.playNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, getGame(), true);
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        boolean optional = !tgtSA.getPayCosts().isMandatory();
        boolean noManaCost = tgtSA.hasParam("WithoutManaCost");
        if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
            Spell spell = (Spell) tgtSA;
            // if (spell.canPlayFromEffectAI(player, !optional, noManaCost) || !optional) {  -- could not save this part
            if (spell.canPlay() || !optional) {
                ComputerUtil.playStack(tgtSA, player, getGame());
            } else
                return false; // didn't play spell
        }
        return true;

    }

    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        // no longer possible to run AI's methods on SpellAbility
        // return currentAbility.doTrigger(true, player);
        return false;
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, CardCollectionView pile1, CardCollectionView pile2, String faceUp) {
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // test this!
    }

    @Override
    public void revealAISkipCards(final String message, final Map<Player, Map<DeckSection, List<? extends PaperCard>>> unplayable) {
        // TODO test this!
    }

    @Override
    public void revealUnsupported(Map<Player, List<PaperCard>> unsupported) {
        // test this!
    }

    @Override
    public List<PaperCard> chooseCardsYouWonToAddToDeck(List<PaperCard> losses) {
        // TODO Auto-generated method stub
        return losses;
    }

    @Override
    public int chooseNumber(SpellAbility sa, String title, List<Integer> values, Player relatedPlayer) {
        // TODO Auto-generated method stub
        return Iterables.getFirst(values, 0);
    }

    @Override
    public boolean payManaCost(ManaCost toPay, CostPartMana costPartMana, SpellAbility sa, String prompt /* ai needs hints as well */, ManaConversionMatrix matrix, boolean effect) {
        // TODO Auto-generated method stub
        ManaCostBeingPaid cost = new ManaCostBeingPaid(toPay);
        return ComputerUtilMana.payManaCost(cost, sa, player, effect);
    }

    @Override
    public Map<Card, ManaCostShard> chooseCardsForConvokeOrImprovise(SpellAbility sa, ManaCost manaCost,
                                                                     CardCollectionView untappedCards, boolean improvise) {
        // TODO: AI to choose a creature to tap would go here
        // Probably along with deciding how many creatures to tap
        return new HashMap<>();
    }

    @Override
    public boolean playChosenSpellAbility(SpellAbility sa) {
        // TODO Play abilities from here
        return true;
    }

    @Override
    public Card chooseSingleCardForZoneChange(ZoneType destination,
            List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, DelayedReveal delayedReveal,
            String selectPrompt, boolean isOptional, Player decider) {

        if (delayedReveal != null) {
            reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
        }
        return ChangeZoneAi.chooseCardToHiddenOriginChangeZone(destination, origin, sa, fetchList, player, decider);
    }

    @Override
    public List<Card> chooseCardsForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa, CardCollection fetchList, int min, int max, DelayedReveal delayedReveal, String selectPrompt, Player decider) {
        // this isn't used
        return null;
    }

    @Override
    public void resetAtEndOfTurn() {
        // Not used by the controller for tests
    }

    @Override
    public void autoPassCancel() {
        // Not used by the controller for tests
    }

    @Override
    public void awaitNextInput() {
        // Not used by the controller for tests
    }
    @Override
    public void cancelAwaitNextInput() {
        // Not used by the controller for tests
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, String message, Predicate<ICardFace> cpp, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String chooseCardName(SpellAbility sa, Predicate<ICardFace> cpp, String valid, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String chooseCardName(SpellAbility sa, List<ICardFace> faces, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICardFace chooseSingleCardFace(SpellAbility sa, List<ICardFace> faces, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CardState chooseSingleCardState(SpellAbility sa, List<CardState> states, String message, Map<String, Object> params) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Card chooseDungeon(Player player, List<PaperCard> dungeonCards, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Card> chooseCardsForSplice(SpellAbility sa, List<Card> cards) {
        return Lists.newArrayList();
    }

    @Override
    public List<OptionalCostValue> chooseOptionalCosts(SpellAbility choosen,
            List<OptionalCostValue> optionalCostValues) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean confirmMulliganScry(Player p) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int chooseNumberForKeywordCost(SpellAbility sa, Cost cost, KeywordInterface keyword, String prompt, int max) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int chooseNumberForCostReduction(final SpellAbility sa, final int min, final int max) {
        return max;
    }

    @Override
    public CardCollection chooseCardsForEffectMultiple(Map<String, CardCollection> validMap, SpellAbility sa, String title, boolean isOptional) {
        // TODO Auto-generated method stub
        return new CardCollection();
    }

	@Override
	public List<SpellAbility> chooseSpellAbilitiesForEffect(List<SpellAbility> spells, SpellAbility sa, String title,
			int num, Map<String, Object> params) {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public List<CostPart> orderCosts(List<CostPart> costs) {
        return costs;
    }
}

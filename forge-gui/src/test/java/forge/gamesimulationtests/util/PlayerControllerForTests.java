package forge.gamesimulationtests.util;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import forge.ai.ComputerUtil;
import forge.ai.ability.ChangeZoneAi;
import forge.ai.ability.DrawAi;
import forge.ai.ability.GameWinAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.mana.Mana;
import forge.gui.player.HumanPlay;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerController;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationHandler;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.ActivateAbilityAction;
import forge.gamesimulationtests.util.playeractions.CastSpellFromHandAction;
import forge.gamesimulationtests.util.playeractions.DeclareAttackersAction;
import forge.gamesimulationtests.util.playeractions.DeclareBlockersAction;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import forge.item.PaperCard;
import forge.util.MyRandom;

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

	public Game getGame() {
		return game;
	}

	@Override
	public void playSpellAbilityForFree(SpellAbility copySA, boolean mayChoseNewTargets) {
		throw new IllegalStateException("Callers of this method currently assume that it performs extra functionality!");
	}

	@Override
	public void playSpellAbilityNoStack(SpellAbility effectSA, boolean mayChoseNewTargets) {
		//TODO: eventually (when the real code is refactored) this should be handled normally...
		if (effectSA.getDescription().equals("At the beginning of your upkeep, if you have exactly 1 life, you win the game.")) {//test_104_2b_effect_may_state_that_player_wins_the_game
			HumanPlay.playSpellAbilityNoStack(player, effectSA, !mayChoseNewTargets);
			return;
		}
		if (
				(effectSA.getSourceCard().getName().equals("Nefarious Lich") && effectSA.getApi().getAi() instanceof DrawAi) ||
				(effectSA.getSourceCard().getName().equals("Laboratory Maniac") && effectSA.getApi().getAi() instanceof GameWinAi) ||
				(effectSA.getSourceCard().getName().equals("Nefarious Lich") && effectSA.getApi().getAi() instanceof ChangeZoneAi)
		) {//test_104_3f_if_a_player_would_win_and_lose_simultaneously_he_loses
			HumanPlay.playSpellAbilityNoStack(player, effectSA, !mayChoseNewTargets);
			return;
		}
		throw new IllegalStateException("Callers of this method currently assume that it performs extra functionality!");
	}

	@Override
	public Deck sideboard(Deck deck, GameType gameType) {
		return deck;
	}

	@Override
	public Map<Card, Integer> assignCombatDamage(Card attacker, List<Card> blockers, int damageDealt, GameEntity defender, boolean overrideOrder) {
		if (blockers.size() == 1 && damageDealt == 2 && (
				(attacker.getName().equals("Grizzly Bears") && blockers.get(0).getName().equals("Ajani's Sunstriker")) ||
				(attacker.getName().equals("Ajani's Sunstriker") && blockers.get(0).getName().equals("Grizzly Bears"))
		)) {//test_104_3b_player_with_less_than_zero_life_loses_the_game_only_when_a_player_receives_priority_variant_with_combat
			Map<Card, Integer> result = new HashMap<Card, Integer>();
			result.put(blockers.get(0), damageDealt);
			return result;
		}
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public Integer announceRequirements(SpellAbility ability, String announce, boolean allowZero) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public List<Card> choosePermanentsToSacrifice(SpellAbility sa, int min, int max, List<Card> validTargets, String message) {
		return chooseItems(validTargets, min);
	}

	@Override
	public List<Card> choosePermanentsToDestroy(SpellAbility sa, int min, int max, List<Card> validTargets, String message) {
		return chooseItems(validTargets, min);
	}

	@Override
	public TargetChoices chooseNewTargetsFor(SpellAbility ability) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public Pair<SpellAbilityStackInstance, GameObject> chooseTarget(SpellAbility sa, List<Pair<SpellAbilityStackInstance, GameObject>> allTargets) {
		return chooseItem(allTargets);
	}

	@Override
	public List<Card> chooseCardsForEffect(List<Card> sourceList, SpellAbility sa, String title, int min, int max, boolean isOptional) {
		return chooseItems(sourceList, max);
	}

	@Override
	public <T extends GameEntity> T chooseSingleEntityForEffect(Collection<T> sourceList, SpellAbility sa, String title, boolean isOptional, Player targetedPlayer) {
		return chooseItem(sourceList);
	}

	@Override
	public SpellAbility chooseSingleSpellForEffect(List<SpellAbility> spells, SpellAbility sa, String title) {
		return chooseItem(spells);
	}

	@Override
	public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message) {
		return true;
	}

	@Override
	public boolean confirmStaticApplication(Card hostCard, GameEntity affected, String logic, String message) {
		return true;
	}

	@Override
    public boolean confirmTrigger(SpellAbility sa, Trigger regtrig, Map<String, String> triggerParams, boolean isMandatory) {
        return true;
    }

    @Override
    public boolean getWillPlayOnFirstTurn(boolean isFirstGame) {
        return true;
    }

	@Override
	public List<Card> orderBlockers(Card attacker, List<Card> blockers) {
		return blockers;
	}

	@Override
	public List<Card> orderAttackers(Card blocker, List<Card> attackers) {
		return attackers;
	}

	@Override
	public void reveal(String string, Collection<Card> cards, ZoneType zone, Player owner) {
		//nothing needs to be done here
	}

	@Override
	public void notifyOfValue(SpellAbility saSource, GameObject realtedTarget, String value) {
		//nothing needs to be done here
	}

	@Override
	public ImmutablePair<List<Card>, List<Card>> arrangeForScry(List<Card> topN) {
		return ImmutablePair.of(topN, null);
	}

	@Override
	public boolean willPutCardOnTop(Card c) {
		return false;
	}

	@Override
	public List<Card> orderMoveToZoneList(List<Card> cards, ZoneType destinationZone) {
		return cards;
	}

	@Override
	public List<Card> chooseCardsToDiscardFrom(Player playerDiscard, SpellAbility sa, List<Card> validCards, int min, int max) {
		return chooseItems(validCards, min);
	}

	@Override
	public Card chooseCardToDredge(List<Card> dredgers) {
		return null;
	}

	@Override
	public void playMiracle(SpellAbility miracle, Card card) {
		throw new IllegalStateException("Callers of this method currently assume that it performs extra functionality!");
	}

	@Override
	public List<Card> chooseCardsToDelve(int colorLessAmount, List<Card> grave) {
		return Collections.<Card>emptyList();
	}

	@Override
	public List<Card> chooseCardsToRevealFromHand(int min, int max, List<Card> valid) {
		return chooseItems(valid, min);
	}

	@Override
	public List<Card> chooseCardsToDiscardUnlessType(int min, List<Card> hand, String param, SpellAbility sa) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public List<SpellAbility> chooseSaToActivateFromOpeningHand(List<SpellAbility> usableFromOpeningHand) {
		return usableFromOpeningHand;
	}

	@Override
	public Mana chooseManaFromPool(List<Mana> manaChoices) {
		return chooseItem(manaChoices);
	}

	@Override
	public Pair<CounterType, String> chooseAndRemoveOrPutCounter(Card cardWithCounter) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA, String question) {
		return true;
	}

	@Override
	public List<Card> getCardsToMulligan(boolean isCommander, Player firstPlayer) {
		return null;
	}

	@Override
	public void declareAttackers(Player attacker, Combat combat) {
		//Doing nothing is safe in most cases, but not all (creatures that must attack etc).  TODO: introduce checks?
		if (playerActions == null) {
			return;
		}
		DeclareAttackersAction declareAttackers = playerActions.getNextActionIfApplicable(player, game, DeclareAttackersAction.class);
		if (declareAttackers == null) {
			return;
		}

		//TODO: check that the chosen attack configuration is legal?  (Including creatures that did not attack but should)
		//TODO: check that the chosen attack configuration was a complete match to what was requested?
		//TODO: banding (don't really care at the moment...)

		for (Map.Entry<CardSpecification, PlayerSpecification> playerAttackAssignment : declareAttackers.getPlayerAttackAssignments().entrySet()) {
			Player defender = getPlayerBeingAttacked(game, player, playerAttackAssignment.getValue());
			attack(combat, playerAttackAssignment.getKey(), defender);
		}
		for (Map.Entry<CardSpecification, CardSpecification> planeswalkerAttackAssignment: declareAttackers.getPlaneswalkerAttackAssignments().entrySet()) {
			Card defender = CardSpecificationHandler.INSTANCE.find(game.getCardsInGame(), planeswalkerAttackAssignment.getKey());
			attack(combat, planeswalkerAttackAssignment.getKey(), defender);
		}
	}

	private Player getPlayerBeingAttacked(Game game, Player attacker, PlayerSpecification defenderSpecification) {
		if (defenderSpecification != null) {
			return PlayerSpecificationHandler.INSTANCE.find(game.getPlayers(), defenderSpecification);
		}
		if (game.getPlayers().size() != 2) {
			throw new IllegalStateException("Can't use implicit defender specification in this situation!");
		}
		for (Player player : game.getPlayers()) {
			if (!attacker.equals(player)) {
				return player;
			}
		}
		throw new IllegalStateException("Couldn't find implicit defender!");
	}

	private void attack(Combat combat, CardSpecification attackerSpecification, GameEntity defender) {
		Card attacker = CardSpecificationHandler.INSTANCE.find(combat.getAttackingPlayer().getCreaturesInPlay(), attackerSpecification);
		if (!CombatUtil.canAttack(attacker, defender, combat)) {
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
		DeclareBlockersAction declareBlockers = playerActions.getNextActionIfApplicable(player, game, DeclareBlockersAction.class);
		if (declareBlockers == null) {
			return;
		}

		//TODO: check that the chosen block configuration is 100% legal?
		//TODO: check that the chosen block configuration was a 100% match to what was requested?
		//TODO: where do damage assignment orders get handled?

		for (Map.Entry<CardSpecification, Collection<CardSpecification>> blockingAssignment : declareBlockers.getBlockingAssignments().asMap().entrySet()) {
			Card attacker = CardSpecificationHandler.INSTANCE.find(combat.getAttackers(), blockingAssignment.getKey());
			for (CardSpecification blockerSpecification : blockingAssignment.getValue()) {
				Card blocker = CardSpecificationHandler.INSTANCE.find(game, blockerSpecification);
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
	public void takePriority() {
		//TODO: just about everything...
		if (playerActions != null) {
			CastSpellFromHandAction castSpellFromHand = playerActions.getNextActionIfApplicable(player, game, CastSpellFromHandAction.class);
			if (castSpellFromHand != null) {
				castSpellFromHand.castSpellFromHand(player, game);
			}

			ActivateAbilityAction activateAbilityAction = playerActions.getNextActionIfApplicable(player, game, ActivateAbilityAction.class);
			if (activateAbilityAction != null) {
				activateAbilityAction.activateAbility(player, game);
			}
		}
	}

	@Override
	public List<Card> chooseCardsToDiscardToMaximumHandSize(int numDiscard) {
		return chooseItems(player.getZone(ZoneType.Hand).getCards(), numDiscard);
	}

	@Override
	public boolean payManaOptional(Card card, Cost cost, SpellAbility sa, String prompt, ManaPaymentPurpose purpose) {
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
	public Card chooseProtectionShield(GameEntity entityBeingDamaged, List<String> options, Map<String, Card> choiceMap) {
		return choiceMap.get(options.get(0));
	}

	@Override
	public List<AbilitySub> chooseModeForAbility(SpellAbility sa, int min, int num) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

	@Override
	public byte chooseColor(String message, SpellAbility sa, ColorSet colors) {
		return Iterables.getFirst(colors, MagicColor.WHITE);
	}

	private <T> List<T> chooseItems(Collection<T> items, int amount) {
		if (items == null || items.isEmpty()) {
			return new ArrayList<T>(items);
		}
		return new ArrayList<T>(items).subList(0, Math.max(amount, items.size()));
	}

	private <T> T chooseItem(Collection<T> items) {
		if (items == null || items.isEmpty()) {
			return null;
		}
		return Iterables.getFirst(items, null);
	}

	@Override
	public SpellAbility getAbilityToPlay(List<SpellAbility> abilities, MouseEvent triggerEvent) {
		return getAbilityToPlay(abilities);
	}


	@Override
	public String chooseSomeType(String kindOfType, SpellAbility sa, List<String> validTypes, List<String> invalidTypes, boolean isOptional) {
		return chooseItem(validTypes);
	}

	@Override
	public PaperCard chooseSinglePaperCard(SpellAbility sa, String message, Predicate<PaperCard> cpp, String name) {
		throw new IllegalStateException("Erring on the side of caution here...");
	}

    @Override
    public List<String> chooseColors(String message, SpellAbility sa, int min, int max, List<String> options) {
        throw new UnsupportedOperationException("No idea how a test player controller would choose colors");
    }

    @Override
    public CounterType chooseCounterType(Collection<CounterType> options, SpellAbility sa, String prompt) {
        return Iterables.getFirst(options, CounterType.P1P1);
    }

    @Override
    public boolean confirmPayment(CostPart costPart, String string) {
        return true;
    }

    @Override
    public ReplacementEffect chooseSingleReplacementEffect(String prompt,
            List<ReplacementEffect> possibleReplacers,
            HashMap<String, Object> runParams) {
        // TODO Auto-generated method stub
        return Iterables.getFirst(possibleReplacers, null);
    }

    @Override
    public String chooseProtectionType(String string, SpellAbility sa, List<String> choices) {
        return choices.get(0);
    }

    @Override
    public boolean payCostToPreventEffect(Cost cost, SpellAbility sa, boolean alreadyPaid, List<Player> allPayers) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void orderAndPlaySimultaneousSa(List<SpellAbility> activePlayerSAs) {
        for (final SpellAbility sa : activePlayerSAs) {
            prepareSingleSa(sa.getSourceCard(),sa,true);
            ComputerUtil.playStack(sa, player, game);
        }
    }
    
    private void prepareSingleSa(final Card host, final SpellAbility sa, boolean isMandatory){
        if (sa.hasParam("TargetingPlayer")) {
            Player targetingPlayer = AbilityUtils.getDefinedPlayers(host, sa.getParam("TargetingPlayer"), sa).get(0);
            targetingPlayer.getController().chooseTargetsFor(sa);
        } else {
            sa.doTrigger(isMandatory, player);
        }
    }

    @Override
    public void playTrigger(Card host, WrappedAbility wrapperAbility, boolean isMandatory) {
        prepareSingleSa(host, wrapperAbility, isMandatory);
        ComputerUtil.playNoStack(wrapperAbility.getActivatingPlayer(), wrapperAbility, game);
    }

    @Override
    public boolean playSaFromPlayEffect(SpellAbility tgtSA) {
        // TODO Auto-generated method stub
        boolean optional = tgtSA.hasParam("Optional");
        boolean noManaCost = tgtSA.hasParam("WithoutManaCost");
        if (tgtSA instanceof Spell) { // Isn't it ALWAYS a spell?
            Spell spell = (Spell) tgtSA;
            if (spell.canPlayFromEffectAI(player, !optional, noManaCost) || !optional) {
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
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean chooseTargetsFor(SpellAbility currentAbility) {
        return currentAbility.doTrigger(true, player);
    }

    @Override
    public boolean chooseCardsPile(SpellAbility sa, List<Card> pile1, List<Card> pile2, boolean faceUp) {
        return MyRandom.getRandom().nextBoolean();
    }

    @Override
    public void revealAnte(String message, Multimap<Player, PaperCard> removedAnteCards) {
        // test this!
    }
}

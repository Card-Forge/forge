package forge.gamesimulationtests.util;

import forge.deck.Deck;
import forge.game.*;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.event.GameEventGameFinished;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.card.CardSpecification;
import forge.gamesimulationtests.util.card.CardSpecificationHandler;
import forge.gamesimulationtests.util.gamestate.GameStateSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecification;
import forge.gamesimulationtests.util.player.PlayerSpecificationBuilder;
import forge.gamesimulationtests.util.player.PlayerSpecificationHandler;
import forge.gamesimulationtests.util.playeractions.PlayerActions;
import forge.gamesimulationtests.util.playeractions.ActivateAbilityAction;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GameWrapper {
	private final List<PlayerSpecification> players;
	private final GameStateSpecification initialGameStateSpecification;
	private final PlayerActions playerActions;
	private final GameLog gameLog;
	private Game game;

	public GameWrapper(GameStateSpecification initialGameStateSpecification, PlayerActions playerActions) {
		this(initialGameStateSpecification, playerActions,
				Arrays.asList(PlayerSpecification.PLAYER_1, PlayerSpecification.PLAYER_2));
	}

	public GameWrapper(GameStateSpecification initialGameStateSpecification, PlayerActions playerActions,
			List<PlayerSpecification> players) {
		this.initialGameStateSpecification = initialGameStateSpecification;
		this.playerActions = playerActions;
		this.players = players;

		gameLog = new GameLog();
	}

	/**
	 * This start method attempts to start from the specified game state. That
	 * requires a bit of ugly hackery, possibly breaking after harmless refactorings
	 * or improvements to real code, and always casting doubt upon the veracity of
	 * test results. To somewhat minimize those concerns, starting with stuff on the
	 * stack and/or combat in progress is pretty much out of the question. Note that
	 * if you use this option, regular startup is ignored (using player deck,
	 * shuffling, drawing hand, mulligan, ...)
	 */
	public void runGame() {
		List<RegisteredPlayer> registeredPlayers = new ArrayList<>();
		for (PlayerSpecification player : players) {
			RegisteredPlayer registeredPlayer = new RegisteredPlayer(new Deck(player.getName()));
			LobbyPlayerForTests lobbyPlayer = new LobbyPlayerForTests(player.getName(), playerActions);
			registeredPlayer.setPlayer(lobbyPlayer);
			registeredPlayers.add(registeredPlayer);
		}

		GameRules rules = new GameRules(GameType.Constructed);
		rules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE));
		rules.setMatchAnteRarity(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE_MATCH_RARITY));
		rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
		rules.setOrderCombatants(FModel.getPreferences().getPrefBoolean(FPref.LEGACY_ORDER_COMBATANTS));
		rules.setUseGrayText(FModel.getPreferences().getPrefBoolean(FPref.UI_GRAY_INACTIVE_TEXT));
		Match match = new Match(rules, registeredPlayers, "Test");
		game = match.createGame();

		// GameNew.newGame( game, false, false ) does a bit of internal setup, then
		// prepares libraries etc
		Trigger.resetIDs();
		TriggerHandler trigHandler = game.getTriggerHandler();
		trigHandler.clearDelayedTrigger();

		// instead of preparing libraries the normal way, we'll distribute cards across
		// the specified zones
		if (initialGameStateSpecification != null && !initialGameStateSpecification.getCards().isEmpty()) {
			for (CardSpecification card : initialGameStateSpecification.getCards()) {
				PaperCard paperCard = CardDatabaseHelper.getCard(card.getName());

				PlayerSpecification owner = card.getOwner();
				PlayerSpecification controller = card.getController();
				if (owner == null) {
					owner = controller;
				}
				if (controller == null) {
					controller = owner;
				}

				if (owner == null) {
					throw new IllegalStateException("Cards must specify owner for game state specification");
				}
				Player actualOwner = PlayerSpecificationHandler.INSTANCE.find(game, owner);
				if (controller == null) {
					throw new IllegalStateException("Cards must specify controller for game state specification");
				}
				Player actualController = PlayerSpecificationHandler.INSTANCE.find(game, controller);

				ZoneType zoneType = card.getZoneType();
				if (zoneType == null) {
					throw new IllegalStateException("Cards must specify zone for game state specification");
				}

				Card actualCard = Card.fromPaperCard(paperCard, actualOwner);
				actualController.getZone(zoneType).add(actualCard);

				if (card.getTarget() != null) {
					Card target = CardSpecificationHandler.INSTANCE.find(game, card.getTarget());
					if (actualCard.isAttachment()) {
						if (target.canBeAttached(actualCard, null)) {
							actualCard.attachToEntity(target, null);
						} else {
							throw new IllegalStateException(actualCard + " can't attach to " + target);
						}
					} else {
						throw new IllegalStateException("Don't know how to make " + actualCard + " target anything");
					}
				}
            for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if ("Outpost Siege".equals(c.getName())) {
                    c.setChosenType("Khans");
                }
            }
            game.getAction().checkStaticAbilities();
			}
		}

		// may need to tweak players a bit too
		if (initialGameStateSpecification != null && !initialGameStateSpecification.getPlayerFacts().isEmpty()) {
			for (final PlayerSpecification playerFact : initialGameStateSpecification.getPlayerFacts()) {
				final PlayerSpecification basePlayerSpec = new PlayerSpecificationBuilder(playerFact.getName()).build();
				Player player = PlayerSpecificationHandler.INSTANCE.find(game, basePlayerSpec);

				if (playerFact.getLife() != null) {
					player.setLife(playerFact.getLife(), null);
				}

				if (playerFact.getPoison() != null) {
					player.setPoisonCounters(playerFact.getPoison(), null);
				}
			}
		}

		// game.getAction().startGame( null ) determines starting player, draws starting
		// hands, handles mulligans, and initiates the first turn
		// skip drawing initial hand and mulliganing
		game.setAge(GameStage.Play);
		game.getTriggerHandler().runTrigger(TriggerType.NewGame, AbilityKey.newMap(), false);

		// first player in the list starts, no coin toss etc
		game.getPhaseHandler().startFirstTurn(game.getPlayers().get(0));
        if (playerActions != null) {
            Player p1 = PlayerSpecificationHandler.INSTANCE.find(game, new PlayerSpecificationBuilder(PlayerSpecification.PLAYER_1.getName()).build());
            Player p2 = PlayerSpecificationHandler.INSTANCE.find(game, new PlayerSpecificationBuilder(PlayerSpecification.PLAYER_2.getName()).build());
            game.getUpkeep().executeUntil(p1);
            game.getUpkeep().executeAt();
            playerActions.getNextActionIfApplicable(p1, game, ActivateAbilityAction.class);
            playerActions.getNextActionIfApplicable(p2, game, ActivateAbilityAction.class);
        }
        game.fireEvent(new GameEventGameFinished());
	}

	public PlayerActions getPlayerActions() {
		return playerActions;
	}

	public Game getGame() {
		return game;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("Game log : \r\n");
		List<GameLogEntry> gameLogEntries = gameLog.getLogEntries(GameLogEntryType.PHASE);
		Collections.reverse(gameLogEntries);
		for (GameLogEntry gameLogEntry : gameLogEntries) {
			sb.append(gameLogEntry.toString()).append("\r\n");
		}

		return sb.toString();
	}
}

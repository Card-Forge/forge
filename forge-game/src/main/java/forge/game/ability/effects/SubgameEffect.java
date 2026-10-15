package forge.game.ability.effects;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.GameOutcome;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventDayTimeChanged;
import forge.game.event.GameEventSubgameEnd;
import forge.game.event.GameEventSubgameStart;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

public class SubgameEffect extends SpellAbilityEffect {

    private Game createSubGame(Game maingame, int startingLife) {
        List<RegisteredPlayer> players = Lists.newArrayList();

        // Add remaining players to subgame
        for (Player p : maingame.getPlayers()) {
            players.add(p.getRegisteredPlayer());
        }

        return new Game(players, maingame.getRules(), maingame.getMatch(), maingame, startingLife);
    }

    private Card createSubgameCard(final Card original, final Player player, final Map<Player, Player> playerMapping) {
        Card card = Card.fromPaperCard(original.getPaperCard(), player);
        List<Player> owners = Lists.newArrayList();
        for (Player owner : original.getOwners()) {
            Player subgameOwner = playerMapping.get(owner);
            if (subgameOwner != null) {
                owners.add(subgameOwner);
            }
        }
        card.setOwners(owners.isEmpty() ? List.of(player) : owners);
        return card;
    }

    private void setCardsInZone(Player player, final ZoneType zoneType, final CardCollectionView oldCards,
            boolean addMapping, final Map<Card, Card> cards, final Map<Player, Player> playerMapping) {
        PlayerZone zone = player.getZone(zoneType);
        List<Card> newCards = Lists.newArrayList(zone.getCards());
        for (final Card card : oldCards) {
            if (card.isToken() || card.isCopiedSpell()) continue;
            Card newCard = cards.get(card);
            if (newCard == null) {
                newCard = createSubgameCard(card, player, playerMapping);
                cards.put(card, newCard);
                newCards.add(newCard);
            }
            if (addMapping) {
                // Build mapping between maingame cards and subgame cards,
                // so when subgame pick a card from maingame (like Wish effects),
                // The maingame card will also be moved.
                // (Will be move to Subgame zone, which will be added back to library after subgame ends.)
                for (Player owner : newCard.getOwners()) {
                    owner.addMaingameCardMapping(newCard, card);
                }
            }
        }
        zone.setCards(newCards);
    }

    private void initVariantsZonesSubgame(final Game subgame, final Player maingamePlayer, final Player player,
            final Map<Card, Card> cards, final Map<Player, Player> playerMapping) {
        PlayerZone com = player.getZone(ZoneType.Command);
        RegisteredPlayer registeredPlayer = player.getRegisteredPlayer();

        // Vanguard
        if (registeredPlayer.getVanguardAvatars() != null) {
            for(PaperCard avatar:registeredPlayer.getVanguardAvatars()) {
                com.add(Card.fromPaperCard(avatar, player));
            }
        }

        // Commander
        final CardCollectionView commandCards = maingamePlayer.getCardsIn(ZoneType.Command);
        for (final Card card : commandCards) {
            if (card.isCommander()) {
                Card cmd = cards.get(card);
                if (cmd != null) {
                    continue;
                }
                cmd = createSubgameCard(card, player, playerMapping);
                cards.put(card, cmd);
                player.initCommanderColor(cmd);
                com.add(cmd);
                player.addCommander(cmd);
            }
        }

        // Conspiracies
        // 720.2 doesn't mention Conspiracy cards so I guess they don't move
    }

    private void prepareAllZonesSubgame(final Game maingame, final Game subgame) {
        final FCollectionView<Player> players = subgame.getPlayers();
        final FCollectionView<Player> maingamePlayers = maingame.getPlayers();
        final Map<Card, Card> cards = new IdentityHashMap<>();
        final Map<Player, Player> playerMapping = new IdentityHashMap<>();
        for (int i = 0; i < players.size(); i++) {
            playerMapping.put(maingamePlayers.get(i), players.get(i));
        }
        final List<ZoneType> outsideZones = Arrays.asList(ZoneType.Hand, ZoneType.Battlefield,
                ZoneType.Graveyard, ZoneType.Exile, ZoneType.Stack, ZoneType.Sideboard, ZoneType.Ante, ZoneType.Merged);

        for (int i = 0; i < players.size(); i++) {
            final Player player = players.get(i);
            final Player maingamePlayer = maingamePlayers.get(i);

            // Library
            setCardsInZone(player, ZoneType.Library, maingamePlayer.getCardsIn(ZoneType.Library), false, cards, playerMapping);

            // Sideboard
            // 720.4
            final CardCollectionView outsideCards = maingame.getCardsInOwnedBy(outsideZones, maingamePlayer);
            if (!outsideCards.isEmpty()) {
                setCardsInZone(player, ZoneType.Sideboard, outsideCards, true, cards, playerMapping);
                // Update card view so it shows the origin zone in text.
                for (Card c : player.getCardsIn(ZoneType.Sideboard)) {
                    c.updateStateForView();
                }

                player.assignCompanion(subgame, player.getController());
            }

            // Schemes
            setCardsInZone(player, ZoneType.SchemeDeck, maingamePlayer.getCardsIn(ZoneType.SchemeDeck), false, cards, playerMapping);

            // Planes
            setCardsInZone(player, ZoneType.PlanarDeck, maingamePlayer.getCardsIn(ZoneType.PlanarDeck), false, cards, playerMapping);

            // Attractions
            setCardsInZone(player, ZoneType.AttractionDeck, maingamePlayer.getCardsIn(ZoneType.AttractionDeck), false, cards, playerMapping);

            // Contraptions
            setCardsInZone(player, ZoneType.ContraptionDeck, maingamePlayer.getCardsIn(ZoneType.ContraptionDeck), false, cards, playerMapping);

            // Vanguard and Commanders
            initVariantsZonesSubgame(subgame, maingamePlayer, player, cards, playerMapping);

            player.shuffle(null);
            player.getZone(ZoneType.SchemeDeck).shuffle();
            player.getZone(ZoneType.PlanarDeck).shuffle();
            player.getZone(ZoneType.AttractionDeck).shuffle();
            player.getZone(ZoneType.ContraptionDeck).shuffle();
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card hostCard = sa.getHostCard();
        final Game maingame = hostCard.getGame();

        int startingLife = -1;
        if (sa.hasParam("StartingLife")) {
            startingLife = Integer.parseInt(sa.getParam("StartingLife"));
        }
        Game subgame = createSubGame(maingame, startingLife);

        String startMessage = Localizer.getInstance().getMessage("lblSubgameStart", hostCard.getTranslatedName());
        maingame.getMatch().fireEvent(new GameEventSubgameStart(subgame, startMessage));

        prepareAllZonesSubgame(maingame, subgame);
        subgame.getAction().startGame(null, null);
        subgame.clearCaches();

        // Find out winners and losers
        final GameOutcome outcome = subgame.getOutcome();
        List<Player> winPlayers = Lists.newArrayList();
        List<Player> notWinPlayers = Lists.newArrayList();
        StringBuilder sbWinners = new StringBuilder();
        StringBuilder sbLosers = new StringBuilder();
        for (Player p : maingame.getPlayers()) {
            if (outcome.isWinner(p.getRegisteredPlayer())) {
                if (!winPlayers.isEmpty()) {
                    sbWinners.append(", ");
                }
                sbWinners.append(p.getName());
                winPlayers.add(p);
            } else {
                if (!notWinPlayers.isEmpty()) {
                    sbLosers.append(", ");
                }
                sbLosers.append(p.getName());
                notWinPlayers.add(p);
            }
        }

        if (sa.hasParam("RememberPlayers")) {
            final String param = sa.getParam("RememberPlayers");
            if (param.equals("Win")) {
                hostCard.addRemembered(winPlayers);
            } else if (param.equals("NotWin")) {
                hostCard.addRemembered(notWinPlayers);
            }
        }

        String endMessage = outcome.isDraw() ? Localizer.getInstance().getMessage("lblSubgameEndDraw") :
                Localizer.getInstance().getMessage("lblSubgameEnd", sbWinners.toString(), sbLosers.toString());
        maingame.getMatch().fireEvent(new GameEventSubgameEnd(maingame, endMessage));
        if (maingame.getDayTime() != null) {
            maingame.fireEvent(new GameEventDayTimeChanged(maingame.getDayTime()));
        }
        // only mobile seems to need this
        for (Player p : maingame.getPlayers()) {
            maingame.fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Hand, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Graveyard, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Exile, p, EventValueChangeType.ComplexUpdate, null));
            maingame.fireEvent(new GameEventZone(ZoneType.Command, p, EventValueChangeType.ComplexUpdate, null));
        }

        // Setup maingame library
        final FCollectionView<Player> subgamePlayers = subgame.getRegisteredPlayers();
        final FCollectionView<Player> players = maingame.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            final Player subgamePlayer = subgamePlayers.get(i);
            final Player player = players.get(i);

            // All cards moved to Subgame Zone will be put into library when subgame ends.
            // 720.5
            final CardCollectionView movedCards = player.getCardsIn(ZoneType.Subgame);
            PlayerZone library = player.getZone(ZoneType.Library);
            for (final Card card : movedCards) {
                library.add(card);
            }
            player.getZone(ZoneType.Subgame).removeAllCards(true);

            // Move commander if it is no longer in subgame's commander zone
            // 720.5c
            List<Card> subgameCommanders = Lists.newArrayList();
            List<Card> movedCommanders = Lists.newArrayList();
            for (final Card card : subgamePlayer.getCardsIn(ZoneType.Command)) {
                if (card.isCommander()) {
                    subgameCommanders.add(card);
                }
            }
            for (final Card card : player.getCardsIn(ZoneType.Command)) {
                if (card.isCommander()) {
                    boolean isInSubgameCommand = false;
                    for (final Card subCard : subgameCommanders) {
                        if (card.getName().equals(subCard.getName())) {
                            isInSubgameCommand = true;
                        }
                    }
                    if (!isInSubgameCommand) {
                        movedCommanders.add(card);
                    }
                }
            }
            for (final Card card : movedCommanders) {
                maingame.getAction().moveTo(ZoneType.Library, card, null, AbilityKey.newMap());
            }

            player.shuffle(sa);
            player.getZone(ZoneType.SchemeDeck).shuffle();
            player.getZone(ZoneType.PlanarDeck).shuffle();
            player.getZone(ZoneType.AttractionDeck).shuffle();
            player.getZone(ZoneType.ContraptionDeck).shuffle();
        }
    }

}

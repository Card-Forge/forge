package forge.ai;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.*;
import forge.game.card.token.TokenInfo;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventCombatChanged;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

public abstract class GameState {
    private static final Map<ZoneType, String> ZONES = new HashMap<>();
    static {
        ZONES.put(ZoneType.Battlefield, "battlefield");
        ZONES.put(ZoneType.Hand, "hand");
        ZONES.put(ZoneType.Graveyard, "graveyard");
        ZONES.put(ZoneType.Library, "library");
        ZONES.put(ZoneType.Exile, "exile");
        ZONES.put(ZoneType.Command, "command");
        ZONES.put(ZoneType.Sideboard, "sideboard");
    }

    static class PlayerState {
        private int life = -1;
        private String counters = "";
        private String manaPool = "";
        private String persistentMana = "";
        private int landsPlayed = 0;
        private int landsPlayedLastTurn = 0;
        private int numRingTemptedYou = 0;
        private String precast = null;
        private String putOnStack = null;
        private final Map<ZoneType, String> cardTexts = new EnumMap<>(ZoneType.class);
    }
    private final List<PlayerState> playerStates = new ArrayList<>();

    private boolean puzzleCreatorState = false;

    private final Map<Integer, Card> idToCard = new HashMap<>();
    private final Map<Card, Integer> cardToAttachId = new HashMap<>();
    private final Map<Card, Player> cardToEnchantPlayerId = new HashMap<>();
    private final Map<Card, Integer> markedDamage = new HashMap<>();
    private final Map<Card, List<String>> cardToChosenClrs = new HashMap<>();
    private final Map<Card, CardCollection> cardToChosenCards = new HashMap<>();
    private final Map<Card, String> cardToChosenType = new HashMap<>();
    private final Map<Card, String> cardToChosenType2 = new HashMap<>();
    private final Map<Card, List<String>> cardToRememberedId = new HashMap<>();
    private final Map<Card, List<String>> cardToImprintedId = new HashMap<>();
    private final Map<Card, List<String>> cardToMergedCards = new HashMap<>();
    private final Map<Card, List<String>> cardToNamedCard = new HashMap<>();
    private final Map<Card, String> cardToExiledWithId = new HashMap<>();
    private final Map<Card, Card> cardAttackMap = new HashMap<>();

    private final Map<Card, String> cardToScript = new HashMap<>();

    private final Map<String, String> abilityString = new HashMap<>();

    private final Set<Card> cardsReferencedByID = new HashSet<>();
    private final Set<Card> cardsWithoutETBTrigs = new HashSet<>();

    private String tChangePlayer = "NONE";
    private String tChangePhase = "NONE";

    private String tAdvancePhase = "NONE";

    private int turn = 1;

    private boolean removeSummoningSickness = false;

    // Targeting for precast spells in a game state (mostly used by Puzzle Mode game states)
    private final int TARGET_NONE = -1; // untargeted spell (e.g. Joraga Invocation)
    private final int TARGET_HUMAN = -2;
    private final int TARGET_AI = -3;

    public GameState() {
    }

    public abstract IPaperCard getPaperCard(String cardName, String setCode, int artID);

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (puzzleCreatorState) {
            // append basic puzzle metadata if we're dumping from the puzzle creator screen
            sb.append("[metadata]\n");
            sb.append("Name:New Puzzle\n");
            sb.append("URL:https://www.cardforge.org\n");
            sb.append("Goal:Win\n");
            sb.append("Turns:1\n");
            sb.append("Difficulty:Easy\n");
            sb.append("Description:Win this turn.\n");
            sb.append("[state]\n");
        }

        sb.append(TextUtil.concatNoSpace("turn=", String.valueOf(turn), "\n"));
        sb.append(TextUtil.concatNoSpace("activeplayer=", tChangePlayer, "\n"));
        sb.append(TextUtil.concatNoSpace("activephase=", tChangePhase, "\n"));

        int playerIndex = 0;
        for (PlayerState p : playerStates) {
            String prefix = "p" + playerIndex++;
            sb.append(TextUtil.concatNoSpace(prefix + "life=", String.valueOf(p.life), "\n"));
            sb.append(TextUtil.concatNoSpace(prefix + "landsplayed=", String.valueOf(p.landsPlayed), "\n"));
            sb.append(TextUtil.concatNoSpace(prefix + "landsplayedlastturn=", String.valueOf(p.landsPlayedLastTurn), "\n"));
            sb.append(TextUtil.concatNoSpace(prefix + "numringtemptedyou=", String.valueOf(p.numRingTemptedYou), "\n"));
            if (!p.counters.isEmpty()) {
                sb.append(TextUtil.concatNoSpace(prefix + "counters=", p.counters, "\n"));
            }
            if (!p.manaPool.isEmpty()) {
                sb.append(TextUtil.concatNoSpace(prefix + "manapool=", p.manaPool, "\n"));
            }
            if (!p.persistentMana.isEmpty()) {
                sb.append(TextUtil.concatNoSpace(prefix + "persistentmana=", p.persistentMana, "\n"));
            }
            appendCards(p.cardTexts, prefix, sb);
        }
        return sb.toString();
    }

    private void appendCards(Map<ZoneType, String> cardTexts, String categoryPrefix, StringBuilder sb) {
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            sb.append(TextUtil.concatNoSpace(categoryPrefix, ZONES.get(kv.getKey()), "=", kv.getValue(), "\n"));
        }
    }

    public void initFromGame(Game game) {
        playerStates.clear();
        for (Player player : game.getPlayers()) {
            PlayerState p = new PlayerState();
            p.life = player.getLife();
            p.landsPlayed = player.getLandsPlayedThisTurn();
            p.landsPlayedLastTurn = player.getLandsPlayedLastTurn();
            p.counters = countersToString(player.getCounters());
            p.manaPool = processManaPool(player.getManaPool());
            p.numRingTemptedYou = player.getNumRingTemptedYou();
            playerStates.add(p);
        }

        tChangePlayer = "p" + game.getPlayers().indexOf(game.getPhaseHandler().getPlayerTurn());
        tChangePhase = game.getPhaseHandler().getPhase().toString();
        turn = game.getPhaseHandler().getTurn();

        // Mark the cards that need their ID remembered for various reasons
        cardsReferencedByID.clear();
        for (ZoneType zone : ZONES.keySet()) {
            for (Card card : game.getCardsIncludePhasingIn(zone)) {
                if (card.getExiledWith() != null) {
                    // Remember the ID of the card that exiled this card
                    cardsReferencedByID.add(card.getExiledWith());
                }
                if (zone == ZoneType.Battlefield) {
                    if (!card.getAllAttachedCards().isEmpty()) {
                        // Remember the ID of cards that have attachments
                        cardsReferencedByID.add(card);
                    }
                }
                for (Object o : card.getRemembered()) {
                    // Remember the IDs of remembered cards
                    if (o instanceof Card) {
                        cardsReferencedByID.add((Card)o);
                    }
                }
                for (Card i : card.getImprintedCards()) {
                    // Remember the IDs of imprinted cards
                    cardsReferencedByID.add(i);
                }
                for (Card i : card.getChosenCards()) {
                    // Remember the IDs of chosen cards
                    cardsReferencedByID.add(i);
                }
                if (game.getCombat() != null && game.getCombat().isAttacking(card)) {
                    // Remember the IDs of attacked planeswalkers
                    GameEntity def = game.getCombat().getDefenderByAttacker(card);
                    if (def instanceof Card) {
                        cardsReferencedByID.add((Card)def);
                    }
                }
            }
        }

        for (ZoneType zone : ZONES.keySet()) {
            // Init texts to empty, so that restoring will clear the state
            // if the zone had no cards in it (e.g. empty hand).
            for (PlayerState p : playerStates) {
                p.cardTexts.put(zone, "");
            }
            for (Card card : game.getCardsIncludePhasingIn(zone)) {
                if (card.getName().equals("Puzzle Goal") && card.getOracleText().contains("New Puzzle")) {
                    puzzleCreatorState = true;
                }
                if (card instanceof DetachedCardEffect) {
                    continue;
                }
                int playerIndex = game.getPlayers().indexOf(card.getController());
                addCard(zone, playerStates.get(playerIndex).cardTexts, card);
            }
        }
    }

    private String getPlayerString(Player p) {
        return "P" + p.getGame().getPlayers().indexOf(p);
    }

    private Player parsePlayerString(Game game, String str) {
        if (str.equalsIgnoreCase("HUMAN")) {
            return game.getPlayers().get(0);
        } else if (str.equalsIgnoreCase("AI")) {
            return game.getPlayers().get(1);
        } else if (str.startsWith("P") && Character.isDigit(str.charAt(1))) {
            return game.getPlayers().get(Integer.parseInt(String.valueOf(str.charAt(1))));
        } else {
            return game.getPlayers().get(0);
        }
    }

    private void addCard(ZoneType zoneType, Map<ZoneType, String> cardTexts, Card c) {
        StringBuilder newText = new StringBuilder(cardTexts.get(zoneType));
        if (newText.length() > 0) {
            newText.append(";");
        }
        if (c.isToken()) {
            newText.append("t:").append(new TokenInfo(c));
        } else {
            if (c.getPaperCard() == null) {
                return;
            }

            if (c.hasMergedCard()) {
                // we have to go by the current top card name here
                newText.append(c.getTopMergedCard().getPaperCard().getName()).append("|Set:")
                        .append(c.getTopMergedCard().getPaperCard().getEdition()).append("|Art:")
                        .append(c.getTopMergedCard().getPaperCard().getArtIndex());
            } else {
                newText.append(c.getPaperCard().getName()).append("|Set:").append(c.getPaperCard().getEdition())
                        .append("|Art:").append(c.getPaperCard().getArtIndex());
            }
        }
        if (c.isCommander()) {
            newText.append("|IsCommander");
        }
        if (c.isRingBearer()) {
            newText.append("|IsRingBearer");
        }

        if (cardsReferencedByID.contains(c)) {
            newText.append("|Id:").append(c.getId());
        }

        if (zoneType == ZoneType.Battlefield) {
            if (c.getOwner() != c.getController()) {
                newText.append("|Owner:").append(getPlayerString(c.getOwner()));
            }
            if (c.isTapped()) {
                newText.append("|Tapped");
            }
            if (c.isSick()) {
                newText.append("|SummonSick");
            }
            if (c.isRenowned()) {
                newText.append("|Renowned");
            }
            if (c.isSolved()) {
                newText.append("|Solved");
            }
            if (c.isSuspected()) {
                newText.append("|Suspected");
            }
            if (c.isMonstrous()) {
                newText.append("|Monstrous");
            }
            if (c.isPhasedOut()) {
                newText.append("|PhasedOut:");
                newText.append(getPlayerString(c.getPhasedOut()));
            }
            if (c.isFaceDown()) {
                newText.append("|FaceDown");
                if (c.isManifested()) {
                    newText.append(":Manifested");
                }
                if (c.isCloaked()) {
                    newText.append(":Cloaked");
                }
            }
            if (c.getCurrentStateName().equals(CardStateName.Transformed)) {
                newText.append("|Transformed");
            } else if (c.getCurrentStateName().equals(CardStateName.Flipped)) {
                newText.append("|Flipped");
            } else if (c.getCurrentStateName().equals(CardStateName.Meld)) {
                newText.append("|Meld");
                if (c.getMeldedWith() != null) {
                    newText.append(":");
                    newText.append(c.getMeldedWith().getName());
                }
            } else if (c.getCurrentStateName().equals(CardStateName.Modal)) {
                newText.append("|Modal");
            }

            if (c.getPlayerAttachedTo() != null) {
                newText.append("|EnchantingPlayer:");
                newText.append(getPlayerString(c.getPlayerAttachedTo()));
            } else if (c.isAttachedToEntity()) {
                newText.append("|AttachedTo:").append(c.getEntityAttachedTo().getId());
            }

            if (c.getDamage() > 0) {
                newText.append("|Damage:").append(c.getDamage());
            }

            if (c.hasChosenColor()) {
                newText.append("|ChosenColor:").append(TextUtil.join(c.getChosenColors(), ","));
            }
            if (c.hasChosenType()) {
                newText.append("|ChosenType:").append(c.getChosenType());
            }
            if (c.hasChosenType2()) {
                newText.append("|ChosenType2:").append(c.getChosenType2());
            }
            if (!c.getNamedCard().isEmpty()) {
                newText.append("|NamedCard:").append(c.getNamedCard());
            }

            List<String> chosenCardIds = Lists.newArrayList();
            for (Card obj : c.getChosenCards()) {
                chosenCardIds.add(String.valueOf(obj.getId()));
            }
            if (!chosenCardIds.isEmpty()) {
                newText.append("|ChosenCards:").append(TextUtil.join(chosenCardIds, ","));
            }

            List<String> rememberedCardIds = Lists.newArrayList();
            for (Object obj : c.getRemembered()) {
                if (obj instanceof Card) {
                    int id = ((Card)obj).getId();
                    rememberedCardIds.add(String.valueOf(id));
                }
            }
            if (!rememberedCardIds.isEmpty()) {
                newText.append("|RememberedCards:").append(TextUtil.join(rememberedCardIds, ","));
            }

            List<String> imprintedCardIds = Lists.newArrayList();
            for (Card impr : c.getImprintedCards()) {
                int id = impr.getId();
                imprintedCardIds.add(String.valueOf(id));
            }
            if (!imprintedCardIds.isEmpty()) {
                newText.append("|Imprinting:").append(TextUtil.join(imprintedCardIds, ","));
            }

            if (c.hasMergedCard()) {
                List<String> mergedCardNames = new ArrayList<>();
                for (Card merged : c.getMergedCards()) {
                    if (c.getTopMergedCard() == merged) {
                        continue;
                    }
                    mergedCardNames.add(merged.getPaperCard().getName().replace(",", "^"));
                }
                newText.append("|MergedCards:").append(TextUtil.join(mergedCardNames, ","));
            }
        }

        if (zoneType == ZoneType.Exile) {
            if (c.getExiledWith() != null) {
                newText.append("|ExiledWith:").append(c.getExiledWith().getId());
            }
            if (c.isFaceDown()) {
                newText.append("|FaceDown"); // Exiled face down
            }
            if (c.isAdventureCard() && c.getZone().is(ZoneType.Exile)) {
                // TODO: this will basically default all exiled cards with Adventure to being "On Adventure".
                // Need to figure out a better way to detect if it's actually on adventure.
                newText.append("|OnAdventure");
            }
            if (c.isForetold()) {
                newText.append("|Foretold");
                if (c.enteredThisTurn()) {
                    newText.append("|ForetoldThisTurn");
                }
            }
        }

        if (zoneType == ZoneType.Battlefield || zoneType == ZoneType.Exile) {
            // A card can have counters on the battlefield and in exile (e.g. exiled by Mairsil, the Pretender)
            Map<CounterType, Integer> counters = c.getCounters();
            if (!counters.isEmpty()) {
                newText.append("|Counters:");
                newText.append(countersToString(counters));
            }
        }

        if (c.getGame().getCombat() != null) {
            if (c.getGame().getCombat().isAttacking(c)) {
                newText.append("|Attacking");
                GameEntity def = c.getGame().getCombat().getDefenderByAttacker(c);
                if (def instanceof Card) {
                    newText.append(":").append(def.getId());
                }
            }
        }

        cardTexts.put(zoneType, newText.toString());
    }

    private String countersToString(Map<CounterType, Integer> counters) {
        boolean first = true;
        StringBuilder counterString = new StringBuilder();

        for (Entry<CounterType, Integer> kv : counters.entrySet()) {
            if (!first) {
                counterString.append(",");
            }

            first = false;
            counterString.append(TextUtil.concatNoSpace(kv.getKey().toString(), "=", String.valueOf(kv.getValue())));
        }
        return counterString.toString();
    }

    private String[] splitLine(String line) {
        if (line.charAt(0) == '#') {
            return null;
        }
        final String[] tempData = line.split("=", 2);
        if (tempData.length >= 2) {
            return tempData;
        }
        if (tempData.length == 1 && line.endsWith("=")) {
            // Empty value.
            return new String[] {tempData[0], ""};
        }
        return null;
    }

    public void parse(InputStream in) throws Exception {
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        parse(br.lines());
    }

    public void parse(List<String> lines) {
        parse(lines.stream());
    }

    public void parse(Stream<String> lines) {
        playerStates.clear();
        lines.forEach(this::parseLine);
    }


    private PlayerState getPlayerState(int index) {
        while (index >= playerStates.size()) {
            playerStates.add(new PlayerState());
        }
        return playerStates.get(index);
    }

    private PlayerState getPlayerState(String key) {
        if (key.startsWith("human")) {
            return getPlayerState(0);
        } else if (key.startsWith("ai")) {
            return getPlayerState(1);
        } else if (key.startsWith("p") && Character.isDigit(key.charAt(1))) {
            return getPlayerState(Integer.parseInt(String.valueOf(key.charAt(1))));
        } else {
            System.err.println("Unknown player state key: " + key);
            return new PlayerState();
        }
    }

    protected void parseLine(String line) {
        String[] keyValue = splitLine(line);
        if (keyValue == null) return;

        final String categoryName = keyValue[0].toLowerCase();
        final String categoryValue = keyValue[1];

        if (categoryName.startsWith("active")) {
            if (categoryName.endsWith("player"))
                tChangePlayer = categoryValue.trim().toLowerCase();
            else if (categoryName.endsWith("phase"))
                tChangePhase = categoryValue.trim().toUpperCase();
            else if (categoryName.endsWith("phaseadvance"))
                tAdvancePhase = categoryValue.trim().toUpperCase();
            return;
        }

        if (categoryName.equals("turn")) {
            turn = Integer.parseInt(categoryValue);
        } else if (categoryName.equals("removesummoningsickness")) {
            removeSummoningSickness = categoryValue.equalsIgnoreCase("true");
        } else if (categoryName.endsWith("life")) {
            getPlayerState(categoryName).life = Integer.parseInt(categoryValue);
        } else if (categoryName.endsWith("counters")) {
            getPlayerState(categoryName).counters = categoryValue;
        } else if (categoryName.endsWith("landsplayed")) {
            getPlayerState(categoryName).landsPlayed = Integer.parseInt(categoryValue);
        } else if (categoryName.endsWith("landsplayedlastturn")) {
            getPlayerState(categoryName).landsPlayedLastTurn = Integer.parseInt(categoryValue);
        } else if (categoryName.endsWith("numringtemptedyou")) {
            getPlayerState(categoryName).numRingTemptedYou = Integer.parseInt(categoryValue);
        } else if (categoryName.endsWith("play") || categoryName.endsWith("battlefield")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Battlefield, categoryValue);
        } else if (categoryName.endsWith("hand")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Hand, categoryValue);
        } else if (categoryName.endsWith("graveyard")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Graveyard, categoryValue);
        } else if (categoryName.endsWith("library")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Library, categoryValue);
        } else if (categoryName.endsWith("exile")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Exile, categoryValue);
        } else if (categoryName.endsWith("command")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Command, categoryValue);
        } else if (categoryName.endsWith("sideboard")) {
            getPlayerState(categoryName).cardTexts.put(ZoneType.Sideboard, categoryValue);
        } else if (categoryName.startsWith("ability")) {
            abilityString.put(categoryName.substring("ability".length()), categoryValue);
        } else if (categoryName.endsWith("precast")) {
            getPlayerState(categoryName).precast = categoryValue;
        } else if (categoryName.endsWith("putonstack")) {
            getPlayerState(categoryName).putOnStack = categoryValue;
        } else if (categoryName.endsWith("manapool")) {
            getPlayerState(categoryName).manaPool = categoryValue;
        } else if (categoryName.endsWith("persistentmana")) {
            getPlayerState(categoryName).persistentMana = categoryValue;
        } else {
            System.err.println("Unknown key: " + categoryName);
        }
    }

    public void applyToGame(final Game game) {
        game.getAction().invoke(() -> applyGameOnThread(game));
    }

    protected void applyGameOnThread(final Game game) {
        if (game.getPlayers().size() != playerStates.size()) {
            throw new RuntimeException("Non-matching number of players, (" +
                game.getPlayers().size() + " vs. " + playerStates.size() + ")");
        }

        idToCard.clear();
        cardToAttachId.clear();
        cardToEnchantPlayerId.clear();
        cardToRememberedId.clear();
        cardToExiledWithId.clear();
        cardToImprintedId.clear();
        markedDamage.clear();
        cardToChosenClrs.clear();
        cardToChosenCards.clear();
        cardToChosenType.clear();
        cardToChosenType2.clear();
        cardToMergedCards.clear();
        cardToScript.clear();
        cardAttackMap.clear();

        int playerTurn = playerStates.indexOf(getPlayerState(tChangePlayer));
        Player newPlayerTurn = game.getPlayers().get(playerTurn);
        PhaseType newPhase = tChangePhase.equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);
        PhaseType advPhase = tAdvancePhase.equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tAdvancePhase);

        // Set stack to resolving so things won't trigger/effects be checked right away
        game.getStack().setResolving(true);

        game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn, turn);

        game.getTriggerHandler().setSuppressAllTriggers(true);

        for (int i = 0; i < playerStates.size(); i++) {
            setupPlayerState(game.getPlayers().get(i), playerStates.get(i));
        }
        handleCardAttachments();
        handleChosenEntities();
        handleRememberedEntities();
        handleMergedCards();
        handleScriptExecution(game);
        handlePrecastSpells(game);
        handleMarkedDamage();

        game.getTriggerHandler().setSuppressAllTriggers(false);

        // SAs added to stack cause triggers to fire, as if the relevant SAs were cast
        handleAddSAsToStack(game);

        // Combat only works for 1v1 matches for now (which are the only matches dev mode supports anyway)
        // Note: triggers may fire during combat declarations ("whenever X attacks, ...", etc.)
        if (newPhase == PhaseType.COMBAT_DECLARE_ATTACKERS || newPhase == PhaseType.COMBAT_DECLARE_BLOCKERS) {
            boolean toDeclareBlockers = newPhase == PhaseType.COMBAT_DECLARE_BLOCKERS;
            if (newPlayerTurn != null) {
                handleCombat(game, newPlayerTurn, newPlayerTurn.getSingleOpponent(), toDeclareBlockers);
            }
        }

        game.getStack().setResolving(false);

        // Advance to a certain phase, activating all triggered abilities
        if (advPhase != null) {
            game.getPhaseHandler().devAdvanceToPhase(advPhase);
        }

        if (removeSummoningSickness) {
            for (Card card : game.getCardsInGame()) {
                card.setSickness(false);
            }
        }

        game.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated

        // prevent interactions with objects from old state
        game.copyLastState();

        // Store snapshot for restoring
        game.stashGameState();

        // Set negative or zero life after state effects if need be, important for some puzzles that rely on
        // pre-setting negative life (e.g. PS_NEO4).
        for (int i = 0; i < playerStates.size(); i++) {
            int life = playerStates.get(i).life;
            if (life <= 0) {
                game.getPlayers().get(i).setLife(life, null);
            }
        }
    }

    private String processManaPool(ManaPool manaPool) {
        StringBuilder mana = new StringBuilder();
        for (final byte c : ManaAtom.MANATYPES) {
            int amount = manaPool.getAmountOfColor(c);
            for (int i = 0; i < amount; i++) {
                mana.append(MagicColor.toShortString(c)).append(" ");
            }
        }

        return mana.toString().trim();
    }

    private void updateManaPool(Player p, String manaDef, boolean clearPool, boolean persistent) {
        Game game = p.getGame();
        if (clearPool) {
            p.getManaPool().clearPool(false);
        }

        if (!manaDef.isEmpty()) {
            final Card dummy = new Card(-777777, game);
            dummy.setOwner(p);
            final Map<String, String> produced = Maps.newHashMap();
            produced.put("Produced", manaDef);
            if (persistent) {
                produced.put("PersistentMana", "True");
            }
            final AbilityManaPart abMana = new AbilityManaPart(dummy, produced);
            game.getAction().invoke(() -> abMana.produceMana(null));
        }
    }

    private void handleCombat(final Game game, final Player attackingPlayer, final Player defendingPlayer, final boolean toDeclareBlockers) {
        // First we need to ensure that all attackers are declared in the Declare Attackers step,
        // even if proceeding straight to Declare Blockers
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, attackingPlayer, turn);

        if (game.getPhaseHandler().getCombat() == null) {
            game.getPhaseHandler().setCombat(new Combat(attackingPlayer));
            game.updateCombatForView();
        }

        Combat combat = game.getPhaseHandler().getCombat();
        for (Entry<Card, Card> attackMap : cardAttackMap.entrySet()) {
            Card attacker = attackMap.getKey();
            Card attacked = attackMap.getValue();

            combat.addAttacker(attacker, attacked == null ? defendingPlayer : attacked);
        }

        // Run the necessary combat events and triggers to set things up correctly as if the
        // attack was actually declared by the attacking player
        Multimap<GameEntity, Card> attackersMap = ArrayListMultimap.create();
        for (GameEntity ge : combat.getDefenders()) {
            attackersMap.putAll(ge, combat.getAttackersOf(ge));
        }
        game.fireEvent(new GameEventAttackersDeclared(attackingPlayer, attackersMap));

        for (final Card c : combat.getAttackers()) {
            CombatUtil.checkDeclaredAttacker(game, c, combat, false);
        }

        game.updateCombatForView();
        game.fireEvent(new GameEventCombatChanged());

        // Gracefully proceed to Declare Blockers, giving priority to the defending player,
        // but only if the stack is empty (otherwise the game will crash).
        game.getStack().addAllTriggeredAbilitiesToStack();
        if (toDeclareBlockers && game.getStack().isEmpty()) {
            game.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_DECLARE_BLOCKERS);
        }
    }

    private void handleRememberedEntities() {
        // Remembered: X
        for (Entry<Card, List<String>> rememberedEnts : cardToRememberedId.entrySet()) {
            Card c = rememberedEnts.getKey();
            List<String> ids = rememberedEnts.getValue();

            for (String id : ids) {
                Card tgt = idToCard.get(Integer.parseInt(id));
                c.addRemembered(tgt);
            }
        }

        // Imprinting: X
        for (Entry<Card, List<String>> imprintedCards : cardToImprintedId.entrySet()) {
            Card c = imprintedCards.getKey();
            List<String> ids = imprintedCards.getValue();

            for (String id : ids) {
                Card tgt = idToCard.get(Integer.parseInt(id));
                c.addImprintedCard(tgt);
            }
        }

        // Exiled with X
        for (Entry<Card, String> rememberedEnts : cardToExiledWithId.entrySet()) {
            Card c = rememberedEnts.getKey();
            String id = rememberedEnts.getValue();

            Card exiledWith = idToCard.get(Integer.parseInt(id));
            if (exiledWith != null) {
                exiledWith.addExiledCard(c);
                c.setExiledWith(exiledWith);
                c.setExiledBy(exiledWith.getController());
            }
        }
    }

    private int parseTargetInScript(final String tgtDef) {
        int tgtID;
        if (tgtDef.equalsIgnoreCase("human")) {
            tgtID = TARGET_HUMAN;
        } else if (tgtDef.equalsIgnoreCase("ai")) {
            tgtID = TARGET_AI;
        } else {
            tgtID = Integer.parseInt(tgtDef);
        }

        return tgtID;
    }

    private void handleScriptedTargetingForSA(final Game game, final SpellAbility sa, int tgtID) {
        Player human = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        if (tgtID != TARGET_NONE) {
            switch (tgtID) {
                case TARGET_HUMAN:
                    sa.getTargets().add(human);
                    break;
                case TARGET_AI:
                    sa.getTargets().add(ai);
                    break;
                default:
                    sa.getTargets().add(idToCard.get(tgtID));
                    break;
            }
        }

        if (sa.hasParam("RememberTargets")) {
            sa.getHostCard().addRemembered(sa.getTargets());
        }
    }

    private void handleScriptExecution(final Game game) {
        for (Entry<Card, String> scriptPtr : cardToScript.entrySet()) {
            Card c = scriptPtr.getKey();
            String sPtr = scriptPtr.getValue();

            executeScript(game, c, sPtr);
        }
    }

    private void executeScript(Game game, Card c, String sPtr) {
        executeScript(game, c, sPtr, false);
    }
    private void executeScript(Game game, Card c, String sPtr, boolean putOnStack) {
        int tgtID = TARGET_NONE;
        if (sPtr.contains("->")) {
            String tgtDef = sPtr.substring(sPtr.lastIndexOf("->") + 2);

            tgtID = parseTargetInScript(tgtDef);
            sPtr = sPtr.substring(0, sPtr.lastIndexOf("->"));
        }

        SpellAbility sa = null;
        if (StringUtils.isNumeric(sPtr)) {
            int numSA = Integer.parseInt(sPtr);
            if (c.getSpellAbilities().size() >= numSA) {
                sa = c.getSpellAbilities().get(numSA);
            } else {
                System.err.println("ERROR: Unable to find SA with index " + numSA + " on card " + c + " to execute!");
            }
        } else {
            // Special handling for keyworded abilities
            if (sPtr.startsWith("KW#")) {
                String kwName = sPtr.substring(3);
                FCollectionView<SpellAbility> saList = c.getSpellAbilities();

                if (kwName.equals("Awaken") || kwName.equals("AwakenOnly")) {
                    // AwakenOnly only creates the Awaken effect, while Awaken precasts the whole spell with Awaken
                    for (SpellAbility ab : saList) {
                        if (ab.getDescription().startsWith("Awaken")) {
                            ab.setActivatingPlayer(c.getController());
                            // target for Awaken is set in its first subability
                            handleScriptedTargetingForSA(game, ab.getSubAbility(), tgtID);
                            sa = kwName.equals("AwakenOnly") ? ab.getSubAbility() : ab;
                        }
                    }
                    if (sa == null) {
                        System.err.println("ERROR: Could not locate keyworded ability Awaken in card " + c + " to execute!");
                        return;
                    }
                }
            } else {
                // SVar-based script execution
                String svarValue = "";

                if (sPtr.startsWith("CustomScript:")) {
                    // A custom line defined in the game state file
                    svarValue = sPtr.substring(sPtr.indexOf(":") + 1);
                } else {
                    // A SVar from the card script file
                    if (!c.hasSVar(sPtr)) {
                        System.err.println("ERROR: Unable to find SVar " + sPtr + " on card " + c + " + to execute!");
                        return;
                    }

                    svarValue = c.getSVar(sPtr);

                    if (tgtID != TARGET_NONE && svarValue.contains("| Defined$")) {
                        // We want a specific target, so try to undefine a predefined target if possible
                        svarValue = TextUtil.fastReplace(svarValue, "| Defined$", "| Undefined$");
                        if (tgtID == TARGET_HUMAN || tgtID == TARGET_AI) {
                            svarValue += " | ValidTgts$ Player";
                        } else {
                            svarValue += " | ValidTgts$ Card";
                        }
                    }
                }

                sa = AbilityFactory.getAbility(svarValue, c);
                if (sa == null) {
                    System.err.println("ERROR: Unable to generate ability for SVar " + svarValue);
                }
            }
        }

        if (sa != null) {
            sa.setActivatingPlayer(c.getController());
        }
        handleScriptedTargetingForSA(game, sa, tgtID);

        if (putOnStack) {
            game.getStack().addAndUnfreeze(sa);
        } else {
            sa.resolve();

            // resolve subabilities
            SpellAbility subSa = sa.getSubAbility();
            while (subSa != null) {
                subSa.resolve();
                subSa = subSa.getSubAbility();
            }
        }
    }

    private void handlePrecastSpells(final Game game) {
        for (int i = 0; i < playerStates.size(); i++) {
            if (playerStates.get(i).precast != null) {
                String[] spellList = TextUtil.split(playerStates.get(i).precast, ';');
                for (String spell : spellList) {
                    precastSpellFromCard(spell, game.getPlayers().get(i), game);
                }
            }
        }
    }

    private void handleAddSAsToStack(final Game game) {
        for (int i = 0; i < playerStates.size(); i++) {
            if (playerStates.get(i).putOnStack != null) {
                String[] spellList = TextUtil.split(playerStates.get(i).putOnStack, ';');
                for (String spell : spellList) {
                    precastSpellFromCard(spell, game.getPlayers().get(i), game, true);
                }
            }
        }
    }

    private void precastSpellFromCard(String spellDef, final Player activator, final Game game) {
        precastSpellFromCard(spellDef, activator, game, false);
    }
    private void precastSpellFromCard(String spellDef, final Player activator, final Game game, final boolean putOnStack) {
        int tgtID = TARGET_NONE;
        String scriptID = "";

        if (spellDef.contains(":")) {
            // targeting via -> will be handled in executeScript
            scriptID = spellDef.substring(spellDef.indexOf(":") + 1).trim();
            spellDef = spellDef.substring(0, spellDef.indexOf(":")).trim();
        } else if (spellDef.contains("->")) {
            String tgtDef = spellDef.substring(spellDef.indexOf("->") + 2).trim();
            tgtID = parseTargetInScript(tgtDef);
            spellDef = spellDef.substring(0, spellDef.indexOf("->")).trim();
        }

        Card c = null;

        if (StringUtils.isNumeric(spellDef)) {
            // Precast from a specific host
            c = idToCard.get(Integer.parseInt(spellDef));
            if (c == null) {
                System.err.println("ERROR: Could not find a card with ID " + spellDef + " to precast!");
                return;
            }
        } else {
            // Precast from a card by name
            PaperCard pc = StaticData.instance().getCommonCards().getCard(spellDef);

            if (pc == null) {
                System.err.println("ERROR: Could not find a card with name " + spellDef + " to precast!");
                return;
            }

            c = Card.fromPaperCard(pc, activator);
        }

        SpellAbility sa = null;

        if (!scriptID.isEmpty()) {
            executeScript(game, c, scriptID, putOnStack);
            return;
        }

        if (!c.getName().equals(spellDef) && c.hasAlternateState() && spellDef.equals(c.getAlternateState().getName())) {
            sa = c.getAlternateState().getFirstSpellAbility();
        } else {
            sa = c.getFirstSpellAbility();
        }

        sa.setActivatingPlayer(activator);

        handleScriptedTargetingForSA(game, sa, tgtID);

        if (putOnStack) {
            game.getStack().addAndUnfreeze(sa);
        } else {
            sa.resolve();
        }
    }

    private void handleMarkedDamage() {
        for (Entry<Card, Integer> entry : markedDamage.entrySet()) {
            Card c = entry.getKey();
            Integer dmg = entry.getValue();

            c.setDamage(dmg);
        }
    }

    private void handleChosenEntities() {
        // TODO: the AI still gets to choose something (and the notification box pops up) before the
        // choice is overwritten here. Somehow improve this so that there is at least no notification
        // about the choice that will be force-changed anyway.

        // Chosen colors
        for (Entry<Card, List<String>> entry : cardToChosenClrs.entrySet()) {
            Card c = entry.getKey();
            List<String> colors = entry.getValue();

            c.setChosenColors(colors);
        }

        // Chosen type
        for (Entry<Card, String> entry : cardToChosenType.entrySet()) {
            Card c = entry.getKey();
            c.setChosenType(entry.getValue());
        }

        // Chosen type 2
        for (Entry<Card, String> entry : cardToChosenType2.entrySet()) {
            Card c = entry.getKey();
            c.setChosenType2(entry.getValue());
        }

        // Named card
        for (Entry<Card, List<String>> entry : cardToNamedCard.entrySet()) {
            Card c = entry.getKey();
            for (String s : entry.getValue()) {
                c.addNamedCard(s);
            }
        }

        // Chosen cards
        for (Entry<Card, CardCollection> entry : cardToChosenCards.entrySet()) {
            Card c = entry.getKey();
            c.setChosenCards(entry.getValue());
        }
    }

    private void handleCardAttachments() {
        // Unattach all permanents first
        for (Entry<Card, Integer> entry : cardToAttachId.entrySet()) {
            Card attachedTo = idToCard.get(entry.getValue());
            attachedTo.unAttachAllCards();
        }

        // Attach permanents by ID
        for (Entry<Card, Integer> entry : cardToAttachId.entrySet()) {
            Card attachedTo = idToCard.get(entry.getValue());
            Card attacher = entry.getKey();
            if (attacher.isAttachment()) {
                attacher.attachToEntity(attachedTo, null, true);
            }
        }

        // Enchant players
        for (Entry<Card, Player> entry : cardToEnchantPlayerId.entrySet()) {
            entry.getKey().attachToEntity(entry.getValue(), null);
        }
    }

    private void handleMergedCards() {
        for (Entry<Card, List<String>> entry : cardToMergedCards.entrySet()) {
            Card mergedTo = entry.getKey();
            for (String mergedCardName : entry.getValue()) {
                Card c;
                PaperCard pc = StaticData.instance().getCommonCards().getCard(mergedCardName. replace("^", ","));
                if (pc == null) {
                    System.err.println("ERROR: Tried to create a non-existent card named " + mergedCardName + " (as a merged card) when loading game state!");
                    continue;
                }

                c = Card.fromPaperCard(pc, mergedTo.getOwner());
                emulateMergeViaMutate(mergedTo, c);
            }
        }
    }

    private void emulateMergeViaMutate(Card top, Card bottom) {
        if (top == null || bottom == null) {
            System.err.println("ERROR: Tried to call emulateMergeViaMutate with a null card!");
            return;
        }

        Game game = top.getGame();

        bottom.setMergedToCard(top);
        if (!top.hasMergedCard()) {
            top.addMergedCard(top);
        }
        top.addMergedCard(bottom);

        top.removeMutatedStates();

        final long ts = game.getNextTimestamp();
        top.setMutatedTimestamp(ts);
        if (top.getCurrentStateName() != CardStateName.FaceDown) {
            final CardCloneStates mutatedStates = CardFactory.getMutatedCloneStates(top, null/*FIXME*/);
            top.addCloneState(mutatedStates, ts);
        }
        bottom.setTapped(top.isTapped());
        bottom.setFlipped(top.isFlipped());
        top.setTimesMutated(top.getTimesMutated() + 1);
        top.updateTokenView();

        // TODO: Merged commanders aren't supported yet
    }

    private void applyCountersToGameEntity(GameEntity entity, String counterString) {
        entity.setCounters(Maps.newHashMap());
        String[] allCounterStrings = counterString.split(",");
        for (final String counterPair : allCounterStrings) {
            String[] pair = counterPair.split("=", 2);
            entity.addCounterInternal(CounterType.getType(pair[0]), Integer.parseInt(pair[1]), null, false, null, null);
        }
    }

    private void setupPlayerState(final Player p, final PlayerState state) {
        // Lock check static as we setup player state

        // Clear all zones first, this ensures that any lingering cards and effects (e.g. in command zone) get cleared up
        // before setting up a new state
        for (ZoneType zt : ZONES.keySet()) {
            p.getZone(zt).removeAllCards(true);
        }

        p.setCommanders(Lists.newArrayList());
        p.clearTheRing();

        Map<ZoneType, CardCollectionView> playerCards = new EnumMap<>(ZoneType.class);
        for (Entry<ZoneType, String> kv : state.cardTexts.entrySet()) {
            String value = kv.getValue();
            playerCards.put(kv.getKey(), processCardsForZone(value.isEmpty() ? new String[0] : value.split(";"), p));
        }

        if (state.life >= 0) p.setLife(state.life, null);
        p.setLandsPlayedThisTurn(state.landsPlayed);
        p.setLandsPlayedLastTurn(state.landsPlayedLastTurn);
        p.setNumRingTemptedYou(state.numRingTemptedYou);

        p.clearPaidForSA();

        for (Entry<ZoneType, CardCollectionView> kv : playerCards.entrySet()) {
            PlayerZone zone = p.getZone(kv.getKey());
            if (kv.getKey() == ZoneType.Battlefield) {
                List<Card> cards = new ArrayList<>();
                for (final Card c : kv.getValue()) {
                    if (c.isToken()) {
                        cards.add(c);
                    }
                }
                zone.setCards(cards);
                for (final Card c : kv.getValue()) {
                    if (c.isToken()) {
                        continue;
                    }
                    boolean tapped = c.isTapped();
                    boolean sickness = c.hasSickness();
                    Map<CounterType, Integer> counters = c.getCounters();
                    // Note: Not clearCounters() since we want to keep the counters var as-is.
                    c.setCounters(Maps.newHashMap());
                    if (c.isAura()) {
                        // dummy "enchanting" to indicate that the card will be force-attached elsewhere
                        // (will be overridden later, so the actual value shouldn't matter)

                        //FIXME it shouldn't be able to attach itself
                        c.setEntityAttachedTo(new CardCopyService(c).copyCard(true));
                    }

                    if (cardsWithoutETBTrigs.contains(c)) {
                        p.getGame().getAction().moveTo(ZoneType.Battlefield, c, null, null);
                    } else {
                        p.getZone(ZoneType.Hand).add(c);
                        p.getGame().getAction().moveToPlay(c, null, null);
                    }

                    c.setTapped(tapped);
                    c.setSickness(sickness);
                    c.setCounters(counters);
                }
            } else {
                zone.setCards(kv.getValue());
            }
        }
        for (Card cmd : p.getCommanders()) {
            p.getZone(ZoneType.Command).add(Player.createCommanderEffect(p.getGame(), cmd));
        }

        updateManaPool(p, state.manaPool, true, false);
        updateManaPool(p, state.persistentMana, false, true);

        if (!state.counters.isEmpty()) {
            applyCountersToGameEntity(p, state.counters);
        }
        if (state.numRingTemptedYou > 0) {
            //setup all levels
            for (int i = 1; i <= state.numRingTemptedYou; i++) {
                if (i > 4)
                    break;
                p.setRingLevel(i);
            }
        }
    }

    /**
     * <p>
     * processCardsForZone.
     * </p>
     *
     * @param data
     *            an array of {@link java.lang.String} objects.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link CardCollectionView} object.
     */
    private CardCollectionView processCardsForZone(final String[] data, final Player player) {
        final CardCollection cl = new CardCollection();
        for (final String element : data) {
            final String[] cardinfo = element.trim().split("\\|");

            String setCode = null;
            for (final String info : cardinfo) {
                if (info.startsWith("Set:")) {
                    setCode = info.substring(info.indexOf(':') + 1);
                    break;
                }
            }

            int artID = -1;
            for (final String info : cardinfo) {
                if (info.startsWith("Art:")) {
                    try {
                        artID = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                    } catch (Exception e) {
                        break;
                    }
                    break;
                }
            }

            Card c;
            boolean hasSetCurSet = false;
            if (cardinfo[0].startsWith("t:")) {
                // TODO Make sure Game State conversion works with new tokens
                String tokenStr = cardinfo[0].substring(2);
                c = new TokenInfo(tokenStr).makeOneToken(player);
            } else if (cardinfo[0].startsWith("T:")) {
                String tokenStr = cardinfo[0].substring(2);
                PaperToken token = StaticData.instance().getAllTokens().getToken(tokenStr,
                        setCode != null ? setCode : CardEdition.UNKNOWN.getName());
                if (token == null) {
                    System.err.println("ERROR: Tried to create a non-existent token named " + cardinfo[0] + " when loading game state!");
                    continue;
                }
                c = CardFactory.getCard(token, player, player.getGame());
            } else {
                PaperCard pc = StaticData.instance().getCommonCards().getCard(cardinfo[0], setCode, artID);
                if (pc == null) {
                    System.err.println("ERROR: Tried to create a non-existent card named " + cardinfo[0] + " (set: " + (setCode == null ? "any" : setCode) + ") when loading game state!");
                    continue;
                }

                c = Card.fromPaperCard(pc, player);
                if (setCode != null) {
                    hasSetCurSet = true;
                }
            }
            c.setSickness(false);

            for (final String info : cardinfo) {
                if (info.startsWith("Tapped")) {
                    c.tap(false, null, null);
                } else if (info.startsWith("Renowned")) {
                    c.setRenowned(true);
                } else if (info.startsWith("Solved")) {
                    c.setSolved(true);
                } else if (info.startsWith("Saddled")) {
                    c.setSaddled(true);
                } else if (info.startsWith("Suspected")) {
                    c.setSuspected(true);
                } else if (info.startsWith("Monstrous")) {
                    c.setMonstrous(true);
                } else if (info.startsWith("PhasedOut")) {
                    String tgt = info.substring(info.indexOf(':') + 1);
                    c.setPhasedOut(parsePlayerString(player.getGame(), tgt));
                } else if (info.startsWith("Counters:")) {
                    applyCountersToGameEntity(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("SummonSick")) {
                    c.setSickness(true);
                } else if (info.startsWith("FaceDown")) {
                    c.turnFaceDown(true);
                    if (info.endsWith("Manifested")) {
                        c.setManifested(true);
                    }
                    if (info.endsWith("Cloaked")) {
                        c.setCloaked(true);
                    }
                } else if (info.startsWith("Transformed")) {
                    c.setState(CardStateName.Transformed, true);
                    c.setBackSide(true);
                } else if (info.startsWith("Flipped")) {
                    c.setState(CardStateName.Flipped, true);
                } else if (info.startsWith("Meld")) {
                    if (info.indexOf(':') > 0) {
                        String meldCardName = info.substring(info.indexOf(':') + 1).replace("^", ",");
                        Card meldTarget;
                        PaperCard pc = StaticData.instance().getCommonCards().getCard(meldCardName);
                        if (pc == null) {
                            System.err.println("ERROR: Tried to create a non-existent card named " + meldCardName + " (as a MeldedWith card) when loading game state!");
                            continue;
                        }
                        meldTarget = Card.fromPaperCard(pc, c.getOwner());
                        c.setMeldedWith(meldTarget);
                    }
                    c.setState(CardStateName.Meld, true);
                    c.setBackSide(true);
                } else if (info.startsWith("Modal")) {
                    c.setState(CardStateName.Modal, true);
                    c.setBackSide(true);
                }
                else if (info.startsWith("OnAdventure")) {
                    String abAdventure = "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Play | ForgetOnMoved$ Exile | Duration$ Permanent | ConditionDefined$ Self | ConditionPresent$ Card.nonCopiedSpell";
                    SpellAbility saAdventure = AbilityFactory.getAbility(abAdventure, c);
                    StringBuilder sbPlay = new StringBuilder();
                    sbPlay.append("Mode$ Continuous | MayPlay$ True | EffectZone$ Command | Affected$ Card.IsRemembered+nonAdventure");
                    sbPlay.append(" | AffectedZone$ Exile | Description$ You may cast the card.");
                    saAdventure.setSVar("Play", sbPlay.toString());
                    saAdventure.setActivatingPlayer(c.getOwner());
                    saAdventure.resolve();
                    c.setExiledWith(c); // This seems to be the way it's set up internally. Potentially not needed here?
                    c.setExiledBy(c.getController());
                } else if (info.startsWith("IsCommander")) {
                    c.setCommander(true);
                    List<Card> cmd = Lists.newArrayList(player.getCommanders());
                    cmd.add(c);
                    player.setCommanders(cmd);
                } else if (info.startsWith("IsRingBearer")) {
                    c.setRingBearer(true);
                    player.setRingBearer(c);
                } else if (info.startsWith("Id:")) {
                    int id = Integer.parseInt(info.substring(3));
                    idToCard.put(id, c);
                } else if (info.startsWith("Attaching:") /*deprecated*/ || info.startsWith("AttachedTo:")) {
                    int id = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                    cardToAttachId.put(c, id);
                } else if (info.startsWith("EnchantingPlayer:")) {
                    String tgt = info.substring(info.indexOf(':') + 1);
                    cardToEnchantPlayerId.put(c, parsePlayerString(player.getGame(), tgt));
                } else if (info.startsWith("Owner:")) {
                    String owner = info.substring(info.indexOf(':') + 1);
                    Player controller = c.getController();
                    c.setOwner(parsePlayerString(player.getGame(), owner));
                    c.setController(controller, c.getGame().getNextTimestamp());
                } else if (info.startsWith("Ability:")) {
                    String abString = info.substring(info.indexOf(':') + 1).toLowerCase();
                    c.addSpellAbility(AbilityFactory.getAbility(abilityString.get(abString), c));
                } else if (info.startsWith("Damage:")) {
                    int dmg = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                    markedDamage.put(c, dmg);
                } else if (info.startsWith("ChosenColor:")) {
                    cardToChosenClrs.put(c, Arrays.asList(info.substring(info.indexOf(':') + 1).split(",")));
                } else if (info.startsWith("ChosenType:")) {
                    cardToChosenType.put(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("ChosenType2:")) {
                    cardToChosenType2.put(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("ChosenCards:")) {
                    CardCollection chosen = new CardCollection();
                    String[] idlist = info.substring(info.indexOf(':') + 1).split(",");
                    for (String id : idlist) {
                        chosen.add(idToCard.get(Integer.parseInt(id)));
                    }
                    cardToChosenCards.put(c, chosen);
                } else if (info.startsWith("MergedCards:")) {
                    List<String> cardNames = Arrays.asList(info.substring(info.indexOf(':') + 1).split(","));
                    cardToMergedCards.put(c, cardNames);
                } else if (info.startsWith("NamedCard:")) {
                    List<String> cardNames = Arrays.asList(info.substring(info.indexOf(':') + 1).split(","));
                    cardToNamedCard.put(c, cardNames);
                } else if (info.startsWith("ExecuteScript:")) {
                    cardToScript.put(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("RememberedCards:")) {
                    cardToRememberedId.put(c, Arrays.asList(info.substring(info.indexOf(':') + 1).split(",")));
                } else if (info.startsWith("Imprinting:")) {
                    cardToImprintedId.put(c, Arrays.asList(info.substring(info.indexOf(':') + 1).split(",")));
                } else if (info.startsWith("ExiledWith:")) {
                    cardToExiledWithId.put(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("Attacking")) {
                    if (info.contains(":")) {
                        int id = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                        cardAttackMap.put(c, idToCard.get(id));
                    } else {
                        cardAttackMap.put(c, null);
                    }
                } else if (info.equals("NoETBTrigs")) {
                    cardsWithoutETBTrigs.add(c);
                } else if (info.equals("Foretold")) {
                    c.setForetold(true);
                    c.turnFaceDown(true);
                    c.addMayLookTemp(c.getOwner());
                } else if (info.equals("ForetoldThisTurn")) {
                    c.setTurnInZone(turn);
                } else if (info.equals("IsToken")) {
                    c.setToken(true);
                }
            }

            if (!hasSetCurSet && !c.isToken()) {
                c.setSetCode(c.getMostRecentSet());
            }

            cl.add(c);
        }
        return cl;
    }
}

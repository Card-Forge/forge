package forge.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.*;
import org.apache.commons.lang3.StringUtils;

import forge.StaticData;
import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityFactory;
import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.Card;
import forge.game.card.CardCloneStates;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CounterType;
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
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

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

    private int humanLife = -1;
    private int computerLife = -1;
    private String humanCounters = "";
    private String computerCounters = "";
    private String humanManaPool = "";
    private String computerManaPool = "";
    private String humanPersistentMana = "";
    private String computerPersistentMana = "";
    private int humanLandsPlayed = 0;
    private int computerLandsPlayed = 0;
    private int humanLandsPlayedLastTurn = 0;
    private int computerLandsPlayedLastTurn = 0;

    private boolean puzzleCreatorState = false;

    private final Map<ZoneType, String> humanCardTexts = new EnumMap<>(ZoneType.class);
    private final Map<ZoneType, String> aiCardTexts = new EnumMap<>(ZoneType.class);

    private final Map<Integer, Card> idToCard = new HashMap<>();
    private final Map<Card, Integer> cardToAttachId = new HashMap<>();
    private final Map<Card, Integer> cardToEnchantPlayerId = new HashMap<>();
    private final Map<Card, Integer> markedDamage = new HashMap<>();
    private final Map<Card, List<String>> cardToChosenClrs = new HashMap<>();
    private final Map<Card, CardCollection> cardToChosenCards = new HashMap<>();
    private final Map<Card, String> cardToChosenType = new HashMap<>();
    private final Map<Card, String> cardToChosenType2 = new HashMap<>();
    private final Map<Card, List<String>> cardToRememberedId = new HashMap<>();
    private final Map<Card, List<String>> cardToImprintedId = new HashMap<>();
    private final Map<Card, List<String>> cardToMergedCards = new HashMap<>();
    private final Map<Card, String> cardToNamedCard = new HashMap<>();
    private final Map<Card, String> cardToNamedCard2 = new HashMap<>();
    private final Map<Card, String> cardToExiledWithId = new HashMap<>();
    private final Map<Card, Card> cardAttackMap = new HashMap<>();

    private final Map<Card, String> cardToScript = new HashMap<>();

    private final Map<String, String> abilityString = new HashMap<>();

    private final Set<Card> cardsReferencedByID = new HashSet<>();
    private final Set<Card> cardsWithoutETBTrigs = new HashSet<>();

    private String tChangePlayer = "NONE";
    private String tChangePhase = "NONE";

    private String tAdvancePhase = "NONE";

    private String precastHuman = null;
    private String precastAI = null;

    private String putOnStackHuman = null;
    private String putOnStackAI = null;

    private int turn = 1;

    private boolean removeSummoningSickness = false;

    // Targeting for precast spells in a game state (mostly used by Puzzle Mode game states)
    private final int TARGET_NONE = -1; // untargeted spell (e.g. Joraga Invocation)
    private final int TARGET_HUMAN = -2;
    private final int TARGET_AI = -3;

    public GameState() {
    }

    public abstract IPaperCard getPaperCard(String cardName);

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

        sb.append(TextUtil.concatNoSpace("humanlife=", String.valueOf(humanLife), "\n"));
        sb.append(TextUtil.concatNoSpace("ailife=", String.valueOf(computerLife), "\n"));
        sb.append(TextUtil.concatNoSpace("humanlandsplayed=", String.valueOf(humanLandsPlayed), "\n"));
        sb.append(TextUtil.concatNoSpace("ailandsplayed=", String.valueOf(computerLandsPlayed), "\n"));
        sb.append(TextUtil.concatNoSpace("humanlandsplayedlastturn=", String.valueOf(humanLandsPlayedLastTurn), "\n"));
        sb.append(TextUtil.concatNoSpace("ailandsplayedlastturn=", String.valueOf(computerLandsPlayedLastTurn), "\n"));
        sb.append(TextUtil.concatNoSpace("turn=", String.valueOf(turn), "\n"));

        if (!humanCounters.isEmpty()) {
            sb.append(TextUtil.concatNoSpace("humancounters=", humanCounters, "\n"));
        }
        if (!computerCounters.isEmpty()) {
            sb.append(TextUtil.concatNoSpace("aicounters=", computerCounters, "\n"));
        }

        if (!humanManaPool.isEmpty()) {
            sb.append(TextUtil.concatNoSpace("humanmanapool=", humanManaPool, "\n"));
        }
        if (!computerManaPool.isEmpty()) {
            sb.append(TextUtil.concatNoSpace("aimanapool=", humanManaPool, "\n"));
        }

        sb.append(TextUtil.concatNoSpace("activeplayer=", tChangePlayer, "\n"));
        sb.append(TextUtil.concatNoSpace("activephase=", tChangePhase, "\n"));
        appendCards(humanCardTexts, "human", sb);
        appendCards(aiCardTexts, "ai", sb);
        return sb.toString();
    }

    private void appendCards(Map<ZoneType, String> cardTexts, String categoryPrefix, StringBuilder sb) {
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            sb.append(TextUtil.concatNoSpace(categoryPrefix, ZONES.get(kv.getKey()), "=", kv.getValue(), "\n"));
        }
    }

    public void initFromGame(Game game) throws Exception {
        FCollectionView<Player> players = game.getPlayers();
        // Can only serialize a two player game with one AI and one human.
        if (players.size() != 2) {
            throw new Exception("Game not supported");
        }
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        if (!human.getController().isGuiPlayer() || !ai.getController().isAI()) {
            throw new Exception("Game not supported");
        }
        humanLife = human.getLife();
        computerLife = ai.getLife();
        humanLandsPlayed = human.getLandsPlayedThisTurn();
        computerLandsPlayed = ai.getLandsPlayedThisTurn();
        humanLandsPlayedLastTurn = human.getLandsPlayedLastTurn();
        computerLandsPlayedLastTurn = ai.getLandsPlayedLastTurn();
        humanCounters = countersToString(human.getCounters());
        computerCounters = countersToString(ai.getCounters());
        humanManaPool = processManaPool(human.getManaPool());
        computerManaPool = processManaPool(ai.getManaPool());

        tChangePlayer = game.getPhaseHandler().getPlayerTurn() == ai ? "ai" : "human";
        tChangePhase = game.getPhaseHandler().getPhase().toString();
        turn = game.getPhaseHandler().getTurn();
        aiCardTexts.clear();
        humanCardTexts.clear();

        // Mark the cards that need their ID remembered for various reasons
        cardsReferencedByID.clear();
        for (ZoneType zone : ZONES.keySet()) {
            for (Card card : game.getCardsIn(zone)) {
                if (card.getExiledWith() != null) {
                    // Remember the ID of the card that exiled this card
                    cardsReferencedByID.add(card.getExiledWith());
                }
                if (zone == ZoneType.Battlefield) {
                    if (!card.getAttachedCards().isEmpty()) {
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
            aiCardTexts.put(zone, "");
            humanCardTexts.put(zone, "");
            for (Card card : game.getCardsIn(zone)) {
                if (card.getName().equals("Puzzle Goal") && card.getOracleText().contains("New Puzzle")) {
                    puzzleCreatorState = true;
                }
                if (card instanceof DetachedCardEffect) {
                    continue;
                }
                addCard(zone, card.getController() == ai ? aiCardTexts : humanCardTexts, card);
            }
        }
    }

    private void addCard(ZoneType zoneType, Map<ZoneType, String> cardTexts, Card c) {
        StringBuilder newText = new StringBuilder(cardTexts.get(zoneType));
        if (newText.length() > 0) {
            newText.append(";");
        }
        if (c.isToken()) {
            newText.append("t:").append(new TokenInfo(c).toString());
        } else {
            if (c.getPaperCard() == null) {
                return;
            }

            if (!c.getMergedCards().isEmpty()) {
                // we have to go by the current top card name here
                newText.append(c.getTopMergedCard().getPaperCard().getName());
            } else {
                newText.append(c.getPaperCard().getName());
            }
        }
        if (c.isCommander()) {
            newText.append("|IsCommander");
        }

        if (cardsReferencedByID.contains(c)) {
            newText.append("|Id:").append(c.getId());
        }

        if (zoneType == ZoneType.Battlefield) {
            if (c.getOwner() != c.getController()) {
                // TODO: Handle more than 2-player games.
                newText.append("|Owner:" + (c.getOwner().isAI() ?  "AI" : "Human"));
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
            if (c.isMonstrous()) {
                newText.append("|Monstrous");
            }
            if (c.isPhasedOut()) {
                newText.append("|PhasedOut");
            }
            if (c.isFaceDown()) {
                newText.append("|FaceDown");
                if (c.isManifested()) {
                    newText.append(":Manifested");
                }
            }
            if (c.getCurrentStateName().equals(CardStateName.Transformed)) {
                newText.append("|Transformed");
            } else if (c.getCurrentStateName().equals(CardStateName.Flipped)) {
                newText.append("|Flipped");
            } else if (c.getCurrentStateName().equals(CardStateName.Meld)) {
                newText.append("|Meld");
            } else if (c.getCurrentStateName().equals(CardStateName.Modal)) {
                newText.append("|Modal");
            }

            if (c.getPlayerAttachedTo() != null) {
                // TODO: improve this for game states with more than two players
                newText.append("|EnchantingPlayer:");
                Player p = c.getPlayerAttachedTo();
                newText.append(p.getController().isAI() ? "AI" : "HUMAN");
            } else if (c.isAttachedToEntity()) {
                newText.append("|AttachedTo:").append(c.getEntityAttachedTo().getId());
            }

            if (c.getDamage() > 0) {
                newText.append("|Damage:").append(c.getDamage());
            }

            if (!c.getChosenColor().isEmpty()) {
                newText.append("|ChosenColor:").append(TextUtil.join(c.getChosenColors(), ","));
            }
            if (!c.getChosenType().isEmpty()) {
                newText.append("|ChosenType:").append(c.getChosenType());
            }
            if (!c.getChosenType2().isEmpty()) {
                newText.append("|ChosenType2:").append(c.getChosenType2());
            }
            if (!c.getNamedCard().isEmpty()) {
                newText.append("|NamedCard:").append(c.getNamedCard());
            }
            if (!c.getNamedCard2().isEmpty()) {
                newText.append("|NamedCard2:").append(c.getNamedCard2());
            }

            List<String> chosenCardIds = Lists.newArrayList();
            for (Object obj : c.getChosenCards()) {
                if (obj instanceof Card) {
                    int id = ((Card)obj).getId();
                    chosenCardIds.add(String.valueOf(id));
                }
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

            if (!c.getMergedCards().isEmpty()) {
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
            }
            if (c.isForetoldThisTurn()) {
                newText.append("|ForetoldThisTurn");
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

        String line;
        while ((line = br.readLine()) != null) {
            parseLine(line);
        }
    }

    public void parse(List<String> lines) {
        for (String line : lines) {
            parseLine(line);
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

        boolean isHuman = categoryName.startsWith("human");

        if (categoryName.equals("turn")) {
            turn = Integer.parseInt(categoryValue);
        }

        else if (categoryName.equals("removesummoningsickness")) {
            removeSummoningSickness = categoryValue.equalsIgnoreCase("true");
        }

        else if (categoryName.endsWith("life")) {
            if (isHuman)
                humanLife = Integer.parseInt(categoryValue);
            else
                computerLife = Integer.parseInt(categoryValue);
        }

        else if (categoryName.endsWith("counters")) {
            if (isHuman)
                humanCounters = categoryValue;
            else
                computerCounters = categoryValue;
        }

        else if (categoryName.endsWith("landsplayed")) {
            if (isHuman)
                humanLandsPlayed = Integer.parseInt(categoryValue);
            else
                computerLandsPlayed = Integer.parseInt(categoryValue);
        }

        else if (categoryName.endsWith("landsplayedlastturn")) {
            if (isHuman)
                humanLandsPlayedLastTurn = Integer.parseInt(categoryValue);
            else
                computerLandsPlayedLastTurn = Integer.parseInt(categoryValue);
        }

        else if (categoryName.endsWith("play") || categoryName.endsWith("battlefield")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Battlefield, categoryValue);
            else
                aiCardTexts.put(ZoneType.Battlefield, categoryValue);
        }

        else if (categoryName.endsWith("hand")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Hand, categoryValue);
            else
                aiCardTexts.put(ZoneType.Hand, categoryValue);
        }

        else if (categoryName.endsWith("graveyard")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Graveyard, categoryValue);
            else
                aiCardTexts.put(ZoneType.Graveyard, categoryValue);
        }

        else if (categoryName.endsWith("library")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Library, categoryValue);
            else
                aiCardTexts.put(ZoneType.Library, categoryValue);
        }

        else if (categoryName.endsWith("exile")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Exile, categoryValue);
            else
                aiCardTexts.put(ZoneType.Exile, categoryValue);
        }

        else if (categoryName.endsWith("command")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Command, categoryValue);
            else
                aiCardTexts.put(ZoneType.Command, categoryValue);
        }

        else if (categoryName.endsWith("sideboard")) {
            if (isHuman)
                humanCardTexts.put(ZoneType.Sideboard, categoryValue);
            else
                aiCardTexts.put(ZoneType.Sideboard, categoryValue);
        }

        else if (categoryName.startsWith("ability")) {
            abilityString.put(categoryName.substring("ability".length()), categoryValue);
        }

        else if (categoryName.endsWith("precast")) {
            if (isHuman)
                precastHuman = categoryValue;
            else
                precastAI = categoryValue;
        }

        else if (categoryName.endsWith("putonstack")) {
            if (isHuman)
                putOnStackHuman = categoryValue;
            else
                putOnStackAI = categoryValue;
        }

        else if (categoryName.endsWith("manapool")) {
            if (isHuman)
                humanManaPool = categoryValue;
            else
                computerManaPool = categoryValue;
        }

        else if (categoryName.endsWith("persistentmana")) {
            if (isHuman)
                humanPersistentMana = categoryValue;
            else
                computerPersistentMana = categoryValue;
        }

        else {
            System.out.println("Unknown key: " + categoryName);
        }
    }

    public void applyToGame(final Game game) {
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                applyGameOnThread(game);
            }
        });
    }

    protected void applyGameOnThread(final Game game) {
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);

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

        Player newPlayerTurn = tChangePlayer.equalsIgnoreCase("human") ? human : tChangePlayer.equalsIgnoreCase("ai") ? ai : null;
        PhaseType newPhase = tChangePhase.equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);
        PhaseType advPhase = tAdvancePhase.equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tAdvancePhase);

        // Set stack to resolving so things won't trigger/effects be checked right away
        game.getStack().setResolving(true);

        updateManaPool(human, humanManaPool, true, false);
        updateManaPool(ai, computerManaPool, true, false);
        updateManaPool(human, humanPersistentMana, false, true);
        updateManaPool(ai, computerPersistentMana, false, true);

        if (!humanCounters.isEmpty()) {
            applyCountersToGameEntity(human, humanCounters);
        }
        if (!computerCounters.isEmpty()) {
            applyCountersToGameEntity(ai, computerCounters);
        }

        game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn, turn);

        game.getTriggerHandler().setSuppressAllTriggers(true);

        setupPlayerState(humanLife, humanCardTexts, human, humanLandsPlayed, humanLandsPlayedLastTurn);
        setupPlayerState(computerLife, aiCardTexts, ai, computerLandsPlayed, computerLandsPlayedLastTurn);

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
    }

    private String processManaPool(ManaPool manaPool) {
        StringBuilder mana = new StringBuilder();
        for (final byte c : MagicColor.WUBRGC) {
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
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    abMana.produceMana(null);
                }
            });
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
            c.setExiledWith(exiledWith);
            c.setExiledBy(exiledWith.getController());
        }
    }

    private int parseTargetInScript(final String tgtDef) {
        int tgtID = TARGET_NONE;

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
            for (final GameObject o : sa.getTargets()) {
                sa.getHostCard().addRemembered(o);
            }
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
                            ab.getSubAbility().setActivatingPlayer(c.getController());
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
        Player human = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        if (precastHuman != null) {
            String[] spellList = TextUtil.split(precastHuman, ';');
            for (String spell : spellList) {
                precastSpellFromCard(spell, human, game);
            }
        }
        if (precastAI != null) {
            String[] spellList = TextUtil.split(precastAI, ';');
            for (String spell : spellList) {
                precastSpellFromCard(spell, ai, game);
            }
        }
    }

    private void handleAddSAsToStack(final Game game) {
        Player human = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        if (putOnStackHuman != null) {
            String[] spellList = TextUtil.split(putOnStackHuman, ';');
            for (String spell : spellList) {
                precastSpellFromCard(spell, human, game, true);
            }
        }
        if (putOnStackAI != null) {
            String[] spellList = TextUtil.split(putOnStackAI, ';');
            for (String spell : spellList) {
                precastSpellFromCard(spell, ai, game, true);
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

        sa = c.getFirstSpellAbility();
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
        for (Entry<Card, String> entry : cardToNamedCard.entrySet()) {
            Card c = entry.getKey();
            c.setNamedCard(entry.getValue());
        }

        // Named card 2
        for (Entry<Card,String> entry : cardToNamedCard2.entrySet()) {
            Card c = entry.getKey();
            c.setNamedCard2(entry.getValue());
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
                attacher.attachToEntity(attachedTo);
            }
        }

        // Enchant players by ID
        for (Entry<Card, Integer> entry : cardToEnchantPlayerId.entrySet()) {
            // TODO: improve this for game states with more than two players
            Card attacher = entry.getKey();
            Game game = attacher.getGame();
            Player attachedTo = entry.getValue() == TARGET_AI ? game.getPlayers().get(1) : game.getPlayers().get(0);

            attacher.attachToEntity(attachedTo);
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

        if (top.getMutatedTimestamp() != -1) {
            top.removeCloneState(top.getMutatedTimestamp());
        }

        final Long ts = game.getNextTimestamp();
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
            entity.addCounter(CounterType.getType(pair[0]), Integer.parseInt(pair[1]), null, null, false, false, null);
        }
    }

    private void setupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p, final int landsPlayed, final int landsPlayedLastTurn) {
        // Lock check static as we setup player state

        // Clear all zones first, this ensures that any lingering cards and effects (e.g. in command zone) get cleared up
        // before setting up a new state
        for (ZoneType zt : ZONES.keySet()) {
            p.getZone(zt).removeAllCards(true);
        }

        Map<ZoneType, CardCollectionView> playerCards = new EnumMap<>(ZoneType.class);
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            String value = kv.getValue();
            playerCards.put(kv.getKey(), processCardsForZone(value.isEmpty() ? new String[0] : value.split(";"), p));
        }

        if (life >= 0) p.setLife(life, null);
        p.setLandsPlayedThisTurn(landsPlayed);
        p.setLandsPlayedLastTurn(landsPlayedLastTurn);

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
                        c.setEntityAttachedTo(CardFactory.copyCard(c, true));
                    }

                    if (cardsWithoutETBTrigs.contains(c)) {
                        p.getGame().getAction().moveTo(ZoneType.Battlefield, c, null);
                    } else {
                        p.getZone(ZoneType.Hand).add(c);
                        p.getGame().getAction().moveToPlay(c, null);
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
            
            Card c;
            boolean hasSetCurSet = false;
            if (cardinfo[0].startsWith("t:")) {
                // TODO Make sure Game State conversion works with new tokens
                String tokenStr = cardinfo[0].substring(2);
                c = new TokenInfo(tokenStr).makeOneToken(player);
            } else {
                PaperCard pc = StaticData.instance().getCommonCards().getCard(cardinfo[0], setCode);
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
                    c.tap();
                } else if (info.startsWith("Renowned")) {
                    c.setRenowned(true);
                } else if (info.startsWith("Monstrous")) {
                    c.setMonstrous(true);
                } else if (info.startsWith("PhasedOut")) {
                    c.setPhasedOut(true);
                } else if (info.startsWith("Counters:")) {
                    applyCountersToGameEntity(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("SummonSick")) {
                    c.setSickness(true);
                } else if (info.startsWith("FaceDown")) {
                    c.turnFaceDown(true);
                    if (info.endsWith("Manifested")) {
                        c.setManifested(true);
                    }
                } else if (info.startsWith("Transformed")) {
                    c.setState(CardStateName.Transformed, true);
                } else if (info.startsWith("Flipped")) {
                    c.setState(CardStateName.Flipped, true);
                } else if (info.startsWith("Meld")) {
                    c.setState(CardStateName.Meld, true);
                } else if (info.startsWith("Modal")) {
                    c.setState(CardStateName.Modal, true);
                } 
                else if (info.startsWith("OnAdventure")) {
                    String abAdventure = "DB$ Effect | RememberObjects$ Self | StaticAbilities$ Play | ExileOnMoved$ Exile | Duration$ Permanent | ConditionDefined$ Self | ConditionPresent$ Card.nonCopiedSpell";
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
                    player.setCommanders(Lists.newArrayList(c));
                } else if (info.startsWith("Id:")) {
                    int id = Integer.parseInt(info.substring(3));
                    idToCard.put(id, c);
                } else if (info.startsWith("Attaching:") /*deprecated*/ || info.startsWith("AttachedTo:")) {
                    int id = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                    cardToAttachId.put(c, id);
                } else if (info.startsWith("EnchantingPlayer:")) {
                    // TODO: improve this for game states with more than two players
                    String tgt = info.substring(info.indexOf(':') + 1);
                    cardToEnchantPlayerId.put(c, tgt.equalsIgnoreCase("AI") ? TARGET_AI : TARGET_HUMAN);
                } else if (info.startsWith("Owner:")) {
                    // TODO: improve this for game states with more than two players
                    Player human = player.getGame().getPlayers().get(0);
                    Player ai = player.getGame().getPlayers().get(1);
                    String owner = info.substring(info.indexOf(':') + 1);
                    Player controller = c.getController();
                    c.setOwner(owner.equalsIgnoreCase("AI") ? ai : human);
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
                    cardToNamedCard.put(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("NamedCard2:")) {
                    cardToNamedCard2.put(c, info.substring(info.indexOf(':') + 1));
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
                    c.setForetoldThisTurn(true);
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

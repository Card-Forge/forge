package forge.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import forge.StaticData;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityFactory;
import forge.game.ability.effects.DetachedCardEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventCombatChanged;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.StringUtils;

public abstract class GameState {
    private static final Map<ZoneType, String> ZONES = new HashMap<ZoneType, String>();
    static {
        ZONES.put(ZoneType.Battlefield, "battlefield");
        ZONES.put(ZoneType.Hand, "hand");
        ZONES.put(ZoneType.Graveyard, "graveyard");
        ZONES.put(ZoneType.Library, "library");
        ZONES.put(ZoneType.Exile, "exile");
        ZONES.put(ZoneType.Command, "command");
    }

    private int humanLife = -1;
    private int computerLife = -1;
    private String humanCounters = "";
    private String computerCounters = "";

    private boolean puzzleCreatorState = false;

    private final Map<ZoneType, String> humanCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
    private final Map<ZoneType, String> aiCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);

    private final Map<Integer, Card> idToCard = new HashMap<>();
    private final Map<Card, Integer> cardToAttachId = new HashMap<>();
    private final Map<Card, Integer> markedDamage = new HashMap<>();
    private final Map<Card, List<String>> cardToChosenClrs = new HashMap<>();
    private final Map<Card, String> cardToChosenType = new HashMap<>();
    private final Map<Card, List<String>> cardToRememberedId = new HashMap<>();
    private final Map<Card, List<String>> cardToImprintedId = new HashMap<>();
    private final Map<Card, String> cardToExiledWithId = new HashMap<>();
    private final Map<Card, Card> cardAttackMap = new HashMap<>();

    private final Map<Card, String> cardToScript = new HashMap<>();

    private final Map<String, String> abilityString = new HashMap<>();

    private final Set<Card> cardsReferencedByID = new HashSet<>();

    private String tChangePlayer = "NONE";
    private String tChangePhase = "NONE";

    private String precastHuman = null;
    private String precastAI = null;

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

        sb.append(String.format("humanlife=%d\n", humanLife));
        sb.append(String.format("ailife=%d\n", computerLife));

        if (!humanCounters.isEmpty()) {
            sb.append(String.format("humancounters=%s\n", humanCounters));
        }
        if (!computerCounters.isEmpty()) {
            sb.append(String.format("aicounters=%s\n", computerCounters));
        }

        sb.append(String.format("activeplayer=%s\n", tChangePlayer));
        sb.append(String.format("activephase=%s\n", tChangePhase));
        appendCards(humanCardTexts, "human", sb);
        appendCards(aiCardTexts, "ai", sb);
        return sb.toString();
    }

    private void appendCards(Map<ZoneType, String> cardTexts, String categoryPrefix, StringBuilder sb) {
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            sb.append(String.format("%s%s=%s\n", categoryPrefix, ZONES.get(kv.getKey()), kv.getValue()));
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
        humanCounters = countersToString(human.getCounters());
        computerCounters = countersToString(ai.getCounters());

        tChangePlayer = game.getPhaseHandler().getPlayerTurn() == ai ? "ai" : "human";
        tChangePhase = game.getPhaseHandler().getPhase().toString();
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
                    if (!card.getEnchantedBy(false).isEmpty()
                            || !card.getEquippedBy(false).isEmpty()
                            || !card.getFortifiedBy(false).isEmpty()) {
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
                addCard(zone, card.getOwner() == ai ? aiCardTexts : humanCardTexts, card);
            }
        }
    }

    private void addCard(ZoneType zoneType, Map<ZoneType, String> cardTexts, Card c) {
        StringBuilder newText = new StringBuilder(cardTexts.get(zoneType));
        if (newText.length() > 0) {
            newText.append(";");
        }
        if (c.isToken()) {
            newText.append("t:" + new CardFactory.TokenInfo(c).toString());
        } else {
            if (c.getPaperCard() == null) {
                return;
            }
            newText.append(c.getPaperCard().getName());
        }
        if (c.isCommander()) {
            newText.append("|IsCommander");
        }

        if (cardsReferencedByID.contains(c)) {
            newText.append("|Id:").append(c.getId());
        }

        if (zoneType == ZoneType.Battlefield) {
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
                newText.append("|Monstrous:");
                newText.append(c.getMonstrosityNum());
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
            }
            if (c.getEquipping() != null) {
                newText.append("|Attaching:").append(c.getEquipping().getId());
            } else if (c.getFortifying() != null) {
                newText.append("|Attaching:").append(c.getFortifying().getId());
            } else if (c.getEnchantingCard() != null) {
                newText.append("|Attaching:").append(c.getEnchantingCard().getId());
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
        }

        if (zoneType == ZoneType.Exile) {
            if (c.getExiledWith() != null) {
                newText.append("|ExiledWith:").append(c.getExiledWith().getId());
            }
            if (c.isFaceDown()) {
                newText.append("|FaceDown"); // Exiled face down
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
                    newText.append(":" + def.getId());
                }
            }
        }

        cardTexts.put(zoneType, newText.toString());
    }

    private String countersToString(Map<CounterType, Integer> counters) {
        boolean first = true;
        StringBuilder counterString = new StringBuilder();

        for(Entry<CounterType, Integer> kv : counters.entrySet()) {
            if (!first) {
                counterString.append(",");
            }

            first = false;
            counterString.append(String.format("%s=%d", kv.getKey().toString(), kv.getValue()));
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
        for(String line : lines) {
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
            if (categoryName.endsWith("phase"))
                tChangePhase = categoryValue.trim().toUpperCase();
            return;
        }

        boolean isHuman = categoryName.startsWith("human");

        if (categoryName.endsWith("life")) {
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

        else if (categoryName.startsWith("ability")) {
            abilityString.put(categoryName.substring("ability".length()), categoryValue);
        }

        else if (categoryName.endsWith("precast")) {
            if (isHuman)
                precastHuman = categoryValue;
            else
                precastAI = categoryValue;
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
        cardToRememberedId.clear();
        cardToExiledWithId.clear();
        markedDamage.clear();
        cardToChosenClrs.clear();
        cardToChosenType.clear();
        cardToScript.clear();
        cardAttackMap.clear();

        Player newPlayerTurn = tChangePlayer.equals("human") ? human : tChangePlayer.equals("ai") ? ai : null;
        PhaseType newPhase = tChangePhase.equals("none") ? null : PhaseType.smartValueOf(tChangePhase);

        // Set stack to resolving so things won't trigger/effects be checked right away
        game.getStack().setResolving(true);

        if (!humanCounters.isEmpty()) {
            applyCountersToGameEntity(human, humanCounters);
        }
        if (!computerCounters.isEmpty()) {
            applyCountersToGameEntity(ai, computerCounters);
        }

        game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn);

        game.getTriggerHandler().setSuppressAllTriggers(true);

        setupPlayerState(humanLife, humanCardTexts, human);
        setupPlayerState(computerLife, aiCardTexts, ai);

        handleCardAttachments();
        handleChosenEntities();
        handleRememberedEntities();
        handleScriptExecution(game);
        handlePrecastSpells(game);
        handleMarkedDamage();

        game.getTriggerHandler().setSuppressAllTriggers(false);

        // Combat only works for 1v1 matches for now (which are the only matches dev mode supports anyway)
        // Note: triggers may fire during combat declarations ("whenever X attacks, ...", etc.)
        if (newPhase == PhaseType.COMBAT_DECLARE_ATTACKERS || newPhase == PhaseType.COMBAT_DECLARE_BLOCKERS) {
            boolean toDeclareBlockers = newPhase == PhaseType.COMBAT_DECLARE_BLOCKERS;
            handleCombat(game, newPlayerTurn, newPlayerTurn.getSingleOpponent(), toDeclareBlockers);
        }

        game.getStack().setResolving(false);

        game.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
    }

    private void handleCombat(final Game game, final Player attackingPlayer, final Player defendingPlayer, final boolean toDeclareBlockers) {
        // First we need to ensure that all attackers are declared in the Declare Attackers step,
        // even if proceeding straight to Declare Blockers
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, attackingPlayer);

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

        if (!combat.getAttackers().isEmpty()) {
            List<GameEntity> attackedTarget = Lists.newArrayList();
            for (final Card c : combat.getAttackers()) {
                attackedTarget.add(combat.getDefenderByAttacker(c));
            }
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("Attackers", combat.getAttackers());
            runParams.put("AttackingPlayer", combat.getAttackingPlayer());
            runParams.put("AttackedTarget", attackedTarget);
            game.getTriggerHandler().runTrigger(TriggerType.AttackersDeclared, runParams, false);
        }

        for (final Card c : combat.getAttackers()) {
            CombatUtil.checkDeclaredAttacker(game, c, combat);
        }

        game.getTriggerHandler().resetActiveTriggers();
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
    }

    private void handleScriptExecution(final Game game) {
        for (Entry<Card, String> scriptPtr : cardToScript.entrySet()) {
            Card c = scriptPtr.getKey();
            String sPtr = scriptPtr.getValue();

            executeScript(game, c, sPtr);
        }
    }

    private void executeScript(Game game, Card c, String sPtr) {
        int tgtID = TARGET_NONE;
        if (sPtr.contains("->")) {
            String tgtDef = sPtr.substring(sPtr.indexOf("->") + 2);

            tgtID = parseTargetInScript(tgtDef);
            sPtr = sPtr.substring(0, sPtr.indexOf("->"));
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
                if (!c.hasSVar(sPtr)) {
                    System.err.println("ERROR: Unable to find SVar " + sPtr + " on card " + c + " + to execute!");
                    return;
                }

                String svarValue = c.getSVar(sPtr);
                sa = AbilityFactory.getAbility(svarValue, c);
                if (sa == null) {
                    System.err.println("ERROR: Unable to generate ability for SVar " + svarValue);
                }
            }
        }

        sa.setActivatingPlayer(c.getController());
        handleScriptedTargetingForSA(game, sa, tgtID);

        sa.resolve();

        // resolve subabilities
        SpellAbility subSa = sa.getSubAbility();
        while (subSa != null) {
            subSa.resolve();
            subSa = subSa.getSubAbility();
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

    private void precastSpellFromCard(String spellDef, final Player activator, final Game game) {
        int tgtID = TARGET_NONE;
        String scriptID = "";

        if (spellDef.contains(":")) {
            // targeting via -> will be handled in executeScript
            scriptID = spellDef.substring(spellDef.indexOf(":") + 1);
            spellDef = spellDef.substring(0, spellDef.indexOf(":"));
        } else if (spellDef.contains("->")) {
            String tgtDef = spellDef.substring(spellDef.indexOf("->") + 2);
            tgtID = parseTargetInScript(tgtDef);
            spellDef = spellDef.substring(0, spellDef.indexOf("->"));
        }

        PaperCard pc = StaticData.instance().getCommonCards().getCard(spellDef);

        if (pc == null) {
            System.err.println("ERROR: Could not find a card with name " + spellDef + " to precast!");
            return;
        }

        Card c = Card.fromPaperCard(pc, activator);
        SpellAbility sa = null;

        if (!scriptID.isEmpty()) {
            executeScript(game, c, scriptID);
            return;
        }

        sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(activator);

        handleScriptedTargetingForSA(game, sa, tgtID);

        sa.resolve();
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
    }

    private void handleCardAttachments() {
        // Unattach all permanents first
        for(Entry<Card, Integer> entry : cardToAttachId.entrySet()) {
            Card attachedTo = idToCard.get(entry.getValue());

            attachedTo.unEnchantAllCards();
            attachedTo.unEquipAllCards();
            for (Card c : attachedTo.getFortifiedBy(true)) {
                attachedTo.unFortifyCard(c);
            }
        }

        // Attach permanents by ID
        for(Entry<Card, Integer> entry : cardToAttachId.entrySet()) {
            Card attachedTo = idToCard.get(entry.getValue());
            Card attacher = entry.getKey();

            if (attacher.isEquipment()) {
                attacher.equipCard(attachedTo);
            } else if (attacher.isAura()) {
                attacher.enchantEntity(attachedTo);
            } else if (attacher.isFortified()) {
                attacher.fortifyCard(attachedTo);
            }
        }
    }

    private void applyCountersToGameEntity(GameEntity entity, String counterString) {
        entity.setCounters(Maps.<CounterType, Integer>newEnumMap(CounterType.class));
        String[] allCounterStrings = counterString.split(",");
        for (final String counterPair : allCounterStrings) {
            String[] pair = counterPair.split("=", 2);
            entity.addCounter(CounterType.valueOf(pair[0]), Integer.parseInt(pair[1]), null, false, false);
        }
    }

    private void setupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p) {
        // Lock check static as we setup player state

        Map<ZoneType, CardCollectionView> playerCards = new EnumMap<ZoneType, CardCollectionView>(ZoneType.class);
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            String value = kv.getValue();
            playerCards.put(kv.getKey(), processCardsForZone(value.isEmpty() ? new String[0] : value.split(";"), p));
        }

        if (life >= 0) p.setLife(life, null);
        for (Entry<ZoneType, CardCollectionView> kv : playerCards.entrySet()) {
            PlayerZone zone = p.getZone(kv.getKey());
            if (kv.getKey() == ZoneType.Battlefield) {
                List<Card> cards = new ArrayList<Card>();
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
                    // Note: Not clearCounters() since we want to keep the counters
                    // var as-is.
                    c.setCounters(Maps.<CounterType, Integer>newEnumMap(CounterType.class));
                    p.getZone(ZoneType.Hand).add(c);
                    if (c.isAura()) {
                        // dummy "enchanting" to indicate that the card will be force-attached elsewhere
                        // (will be overridden later, so the actual value shouldn't matter)
                        c.setEnchanting(c);

                        p.getGame().getAction().moveToPlay(c, null);
                    } else {
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
                String tokenStr = cardinfo[0].substring(2);
                c = CardFactory.makeOneToken(CardFactory.TokenInfo.fromString(tokenStr), player);
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
                } else if (info.startsWith("Monstrous:")) {
                    c.setMonstrous(true);
                    c.setMonstrosityNum(Integer.parseInt(info.substring((info.indexOf(':') + 1))));
                } else if (info.startsWith("PhasedOut")) {
                    c.setPhasedOut(true);
                } else if (info.startsWith("Counters:")) {
                    applyCountersToGameEntity(c, info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("SummonSick")) {
                    c.setSickness(true);
                } else if (info.startsWith("FaceDown")) {
                    c.setState(CardStateName.FaceDown, true);
                    if (info.endsWith("Manifested")) {
                        c.setManifested(true);
                    }
                } else if (info.startsWith("Transformed")) {
                    c.setState(CardStateName.Transformed, true);
                } else if (info.startsWith("Flipped")) {
                    c.setState(CardStateName.Flipped, true);
                } else if (info.startsWith("Meld")) {
                    c.setState(CardStateName.Meld, true);
                } else if (info.startsWith("IsCommander")) {
                    // TODO: This doesn't seem to properly restore the ability to play the commander. Why?
                    c.setCommander(true);
                    player.setCommanders(Lists.newArrayList(c));
                    player.getZone(ZoneType.Command).add(Player.createCommanderEffect(player.getGame(), c));
                } else if (info.startsWith("Id:")) {
                    int id = Integer.parseInt(info.substring(3));
                    idToCard.put(id, c);
                } else if (info.startsWith("Attaching:")) {
                    int id = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                    cardToAttachId.put(c, id);
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

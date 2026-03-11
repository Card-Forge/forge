package forge.research.observation;

import java.util.Map;

import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.keyword.KeywordInterface;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.research.proto.AttackerInfo;
import forge.research.proto.CardState;
import forge.research.proto.CombatState;
import forge.research.proto.CounterState;
import forge.research.proto.GameInfo;
import forge.research.proto.ManaPool.Builder;
import forge.research.proto.Observation;
import forge.research.proto.PlayerState;
import forge.research.proto.StackEntry;

/**
 * Builds protobuf Observation messages from the current game state.
 */
public class ObservationBuilder {

    private final CardRegistry cardRegistry = CardRegistry.getInstance();

    public Observation buildObservation(Game game, Player agent, Player opponent) {
        Observation.Builder obs = Observation.newBuilder();
        obs.setGameInfo(buildGameInfo(game, agent, opponent));
        obs.setAgentPlayer(buildPlayerState(agent, true));
        obs.setOpponentPlayer(buildPlayerState(opponent, false));

        for (SpellAbilityStackInstance si : game.getStack()) {
            obs.addStack(buildStackEntry(si, agent));
        }

        PhaseHandler ph = game.getPhaseHandler();
        if (ph.inCombat()) {
            obs.setCombat(buildCombat(ph.getCombat(), agent));
        }

        return obs.build();
    }

    private GameInfo buildGameInfo(Game game, Player agent, Player opponent) {
        PhaseHandler ph = game.getPhaseHandler();
        GameInfo.Builder info = GameInfo.newBuilder();
        info.setTurn(ph.getTurn());
        info.setPhase(phaseToInt(ph.getPhase()));
        Player active = ph.getPlayerTurn();
        Player priority = ph.getPriorityPlayer();
        info.setActivePlayerIndex(active != null ? active.getId() : -1);
        info.setPriorityPlayerIndex(priority != null ? priority.getId() : -1);
        info.setAgentMulliganCount(agent.getStats().getMulliganCount());
        info.setOpponentMulliganCount(opponent.getStats().getMulliganCount());
        return info.build();
    }

    private PlayerState buildPlayerState(Player p, boolean includeHand) {
        PlayerState.Builder state = PlayerState.newBuilder();
        state.setLife(p.getLife());
        state.setPoisonCounters(p.getPoisonCounters());
        state.setHandSize(p.getCardsIn(ZoneType.Hand).size());
        state.setLibrarySize(p.getCardsIn(ZoneType.Library).size());
        state.setLandsPlayed(p.getLandsPlayedThisTurn());
        state.setMaxLands(p.getMaxLandPlays());
        state.setManaPool(buildManaPool(p.getManaPool()));

        if (includeHand) {
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                state.addHand(buildCardState(c, p));
            }
        }

        for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
            state.addBattlefield(buildCardState(c, p));
        }

        for (Card c : p.getCardsIn(ZoneType.Graveyard)) {
            state.addGraveyard(buildCardState(c, p));
        }

        for (Card c : p.getCardsIn(ZoneType.Exile)) {
            state.addExile(buildCardState(c, p));
        }

        return state.build();
    }

    private forge.research.proto.ManaPool buildManaPool(ManaPool pool) {
        Builder mp = forge.research.proto.ManaPool.newBuilder();
        mp.setWhite(pool.getAmountOfColor((byte) ManaAtom.WHITE));
        mp.setBlue(pool.getAmountOfColor((byte) ManaAtom.BLUE));
        mp.setBlack(pool.getAmountOfColor((byte) ManaAtom.BLACK));
        mp.setRed(pool.getAmountOfColor((byte) ManaAtom.RED));
        mp.setGreen(pool.getAmountOfColor((byte) ManaAtom.GREEN));
        mp.setColorless(pool.getAmountOfColor((byte) ManaAtom.COLORLESS));
        return mp.build();
    }

    private CardState buildCardState(Card c, Player agent) {
        CardState.Builder cs = CardState.newBuilder();
        cs.setCardId(c.getId());
        cs.setName(c.getName());
        cs.setNameId(cardRegistry.getNameId(c.getName()));

        if (c.isCreature()) {
            cs.setPower(c.getNetPower());
            cs.setToughness(c.getNetToughness());
        }

        cs.setCmc(c.getCMC());
        cs.setTapped(c.isTapped());
        cs.setSummoningSick(c.hasSickness());
        cs.setColorsBitmask(c.getColor().getColor());
        cs.setDamage(c.getDamage());
        cs.setLoyalty(c.getCurrentLoyalty());

        Player controller = c.getController();
        cs.setControllerIndex(controller != null ? controller.getId() : -1);

        Player owner = c.getOwner();
        cs.setOwnerIndex(owner != null ? owner.getId() : -1);

        Game game = c.getGame();
        boolean attacking = false;
        boolean blocking = false;
        if (game != null && game.getCombat() != null) {
            attacking = game.getCombat().isAttacking(c);
            blocking = game.getCombat().isBlocking(c);
        }
        cs.setAttacking(attacking);
        cs.setBlocking(blocking);

        // Counters
        Map<CounterType, Integer> counters = c.getCounters();
        int totalCounters = 0;
        for (Map.Entry<CounterType, Integer> entry : counters.entrySet()) {
            totalCounters += entry.getValue();
            cs.addCounters(CounterState.newBuilder()
                    .setType(entry.getKey().getName())
                    .setCount(entry.getValue())
                    .build());
        }
        cs.setCounterCount(totalCounters);

        // Keywords
        java.util.List<KeywordInterface> keywords = c.getKeywords();
        cs.setKeywordCount(keywords.size());
        for (KeywordInterface kw : keywords) {
            cs.addKeywords(kw.getOriginal());
        }

        // Types
        for (forge.card.CardType.CoreType ct : c.getType().getCoreTypes()) {
            cs.addTypes(ct.name());
        }
        for (String st : c.getType().getSubtypes()) {
            cs.addTypes(st);
        }

        return cs.build();
    }

    private StackEntry buildStackEntry(SpellAbilityStackInstance si, Player agent) {
        StackEntry.Builder entry = StackEntry.newBuilder();
        Card source = si.getSourceCard();
        entry.setSourceCardId(source.getId());
        entry.setSourceCardName(source.getName());
        entry.setDescription(si.getStackDescription());
        Player controller = si.getActivatingPlayer();
        entry.setControllerIndex(controller != null ? controller.getId() : -1);
        return entry.build();
    }

    private CombatState buildCombat(Combat combat, Player agent) {
        CombatState.Builder cs = CombatState.newBuilder();
        if (combat != null) {
            for (Card attacker : combat.getAttackers()) {
                AttackerInfo.Builder ai = AttackerInfo.newBuilder();
                ai.setCardId(attacker.getId());
                GameEntity defender = combat.getDefenderByAttacker(attacker);
                if (defender instanceof Player) {
                    ai.setDefendingPlayerIndex(((Player) defender).getId());
                }
                CardCollectionView blockers = combat.getBlockers(attacker);
                if (blockers != null) {
                    for (Card blocker : blockers) {
                        ai.addBlockerCardIds(blocker.getId());
                    }
                }
                cs.addAttackers(ai.build());
            }
        }
        return cs.build();
    }

    private static int phaseToInt(PhaseType phase) {
        if (phase == null) {
            return -1;
        }
        return phase.ordinal();
    }
}

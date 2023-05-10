package forge.game.ability.effects;

import java.util.Map;

import forge.game.GameAction;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ClashEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getHostCard().getName() + " - Clash with an opponent.";
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Player player = source.getController();
        final Player opponent = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(player.getOpponents(), sa, Localizer.getInstance().getMessage("lblChooseOpponent"), null);
        final Player winner = clashWithOpponent(sa, opponent);

        if (player.equals(winner)) {
            SpellAbility sub = sa.getAdditionalAbility("WinSubAbility");
            if (sub != null) {
                AbilityUtils.resolve(sub);
            }
        } else {
            SpellAbility sub = sa.getAdditionalAbility("OtherwiseSubAbility");
            if (sub != null) {
                AbilityUtils.resolve(sub);
            }
        }

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(player);
        runParams.put(AbilityKey.Won, player.equals(winner) ? "True" : "False");
        source.getGame().getTriggerHandler().runTrigger(TriggerType.Clashed, runParams, false);
        final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromPlayer(opponent);
        runParams2.put(AbilityKey.Won, opponent.equals(winner) ? "True" : "False");
        source.getGame().getTriggerHandler().runTrigger(TriggerType.Clashed, runParams2, false);
    }

    /**
     * <p>
     * clashWithOpponent.
     * </p>
     *
     * @return a boolean.
     */
    private static Player clashWithOpponent(final SpellAbility sa, Player opponent) {
        /*
         * Each clashing player reveals the top card of his or her library, then
         * puts that card on the top or bottom. A player wins if his or her card
         * had a higher mana cost.
         *
         * Clash you win or win you don't. There is no tie.
         */
        final Card source = sa.getHostCard();
        final Player player = source.getController();
        final ZoneType lib = ZoneType.Library;

        if (sa.hasParam("RememberClasher")) {
            source.addRemembered(opponent);
        }

        final PlayerZone pLib = player.getZone(lib);
        final PlayerZone oLib = opponent.getZone(lib);

        if ((pLib.size() == 0) && (oLib.size() == 0)) {
            return null;
        }

        final StringBuilder reveal = new StringBuilder();
        reveal.append("OVERRIDE "); //will return substring with the original message parsed here..
        Card pCard = null;
        Card oCard = null;

        if (pLib.size() > 0) {
            pCard = pLib.get(0);
        }
        if (oLib.size() > 0) {
            oCard = oLib.get(0);
        }

        int pCMC = 0;
        int oCMC = 0;

        if (!pLib.isEmpty()) {
            pCMC = pCard.getCMC();

            reveal.append(player).append(" " + Localizer.getInstance().getMessage("lblReveals") + ": ").append(pCard.getName()).append(". " + Localizer.getInstance().getMessage("lblCMC") + "= ").append(pCMC);
            reveal.append("\n");
            clashMoveToTopOrBottom(player, pCard, sa);
        }
        else {
            pCMC = -1;
        }
        if (!oLib.isEmpty()) {
            oCMC = oCard.getCMC();

            reveal.append(opponent).append(" " + Localizer.getInstance().getMessage("lblReveals") + ": ").append(oCard.getName()).append(". " + Localizer.getInstance().getMessage("lblCMC") + "= ").append(oCMC);
            reveal.append("\n");
            clashMoveToTopOrBottom(opponent, oCard, sa);
        }
        else {
            oCMC = -1;
        }
        final CardCollection toReveal = new CardCollection();
        if (pCard != null)
            toReveal.add(pCard);
        if (oCard != null)
            toReveal.add(oCard);

        // no winner, still show the revealed cards rather than do nothing
        if (pCMC == oCMC) {
            reveal.append(Localizer.getInstance().getMessage("lblNoWinner"));
            player.getGame().getAction().revealTo(toReveal, player.getGame().getPlayers(), reveal.toString());
            return null;
        }

        reveal.append(pCMC > oCMC ? player + " " + Localizer.getInstance().getMessage("lblWinsClash") + "." : opponent + " " + Localizer.getInstance().getMessage("lblWinsClash") + ".");
        player.getGame().getAction().revealTo(toReveal, player.getGame().getPlayers(), reveal.toString());
        return pCMC > oCMC ? player : opponent;
    }

    private static void clashMoveToTopOrBottom(final Player p, final Card c, final SpellAbility sa) {
        final GameAction action = p.getGame().getAction();
        final boolean putOnTop = p.getController().willPutCardOnTop(c);
        final String location = putOnTop ? "top" : "bottom";
        final String clashOutcome = p.getName() + " clashed and put " + c.getName() + " to the " + location + " of library.";

        if (putOnTop) {
            action.moveToLibrary(c, sa);
        } else {
            action.moveToBottomOfLibrary(c, sa);
        }
        p.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, clashOutcome);
    }
}

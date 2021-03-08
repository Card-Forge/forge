package forge.game.ability.effects;

import forge.game.GameAction;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

import java.util.Map;

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
        final boolean victory = clashWithOpponent(sa);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, sa.getHostCard().getController());

        if (victory) {

            AbilitySub sub = sa.getAdditionalAbility("WinSubAbility");
            if (sub != null) {
                AbilityUtils.resolve(sub);
            }

            runParams.put(AbilityKey.Won, "True");
        } else {
            AbilitySub sub = sa.getAdditionalAbility("OtherwiseSubAbility");
            if (sub != null) {
                AbilityUtils.resolve(sub);
            }

            runParams.put(AbilityKey.Won, "False");
        }

        
        sa.getHostCard().getGame().getTriggerHandler().runTrigger(TriggerType.Clashed, runParams, false);
    }

    /**
     * <p>
     * clashWithOpponent.
     * </p>
     *
     * @return a boolean.
     */
    private static boolean clashWithOpponent(final SpellAbility sa) {
        /*
         * Each clashing player reveals the top card of his or her library, then
         * puts that card on the top or bottom. A player wins if his or her card
         * had a higher mana cost.
         * 
         * Clash you win or win you don't. There is no tie.
         */
        final Card source = sa.getHostCard();
        final Player player = source.getController();
        final Player opponent = sa.getActivatingPlayer().getController().chooseSingleEntityForEffect(player.getOpponents(), sa, Localizer.getInstance().getMessage("lblChooseOpponent"), null);
        final ZoneType lib = ZoneType.Library;

        if (sa.hasParam("RememberClasher")) {
            source.addRemembered(opponent);
        }
    
        final PlayerZone pLib = player.getZone(lib);
        final PlayerZone oLib = opponent.getZone(lib);
    
        final StringBuilder reveal = new StringBuilder();
    
        Card pCard = null;
        Card oCard = null;
    
        if (pLib.size() > 0) {
            pCard = pLib.get(0);
        }
        if (oLib.size() > 0) {
            oCard = oLib.get(0);
        }
    
        if ((pLib.size() == 0) && (oLib.size() == 0)) {
            return false;
        } else if (pLib.isEmpty()) {
            clashMoveToTopOrBottom(opponent, oCard, sa);
            return false;
        } else if (oLib.isEmpty()) {
            clashMoveToTopOrBottom(player, pCard, sa);
            return true;
        } else {
            final int pCMC = pCard.getCMC();
            final int oCMC = oCard.getCMC();
            
            // TODO: Split cards will return two CMC values, so both players may become winners of clash
            
            reveal.append(player).append(" " + Localizer.getInstance().getMessage("lblReveals") + ": ").append(pCard.getName()).append(". " + Localizer.getInstance().getMessage("lblCMC") + "= ").append(pCMC);
            reveal.append("\r\n");
            reveal.append(opponent).append(" " + Localizer.getInstance().getMessage("lblReveals") + ": ").append(oCard.getName()).append(". " + Localizer.getInstance().getMessage("lblCMC") + "= ").append(oCMC);
            reveal.append("\r\n\r\n");
            reveal.append(player).append(pCMC > oCMC ? " " + Localizer.getInstance().getMessage("lblWinsClash") + "." : " " + Localizer.getInstance().getMessage("lblLosesClash") + ".");
            
            player.getGame().getAction().notifyOfValue(sa, source, reveal.toString(), null);
            clashMoveToTopOrBottom(player, pCard, sa);
            clashMoveToTopOrBottom(opponent, oCard, sa);
            return pCMC > oCMC;
        }
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

package forge.game;

import java.util.List;
import forge.Card;
import forge.FThreads;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.HumanPlaySpellAbility;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameActionPlay {
    
    private final GameState game;
    

    public GameActionPlay(final GameState game0) {
        game = game0;
    }
    
    public final void playCardWithoutManaCost(final Card c, Player player) {
        final List<SpellAbility> choices = c.getBasicSpells();
        // TODO add Buyback, Kicker, ... , spells here

        SpellAbility sa = player.getController().getAbilityToPlay(choices);

        if (sa == null) {
            return;
        }

        sa.setActivatingPlayer(player);
        this.playSpellAbilityWithoutPayingManaCost(sa);
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbilityWithoutPayingManaCost(final SpellAbility sa) {
        FThreads.checkEDT("GameActionPlay.playSpellAbilityWithoutPayingManaCost", false);
        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);

        if (sa.getPayCosts() != null) {
            if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
                CharmEffect.makeChoices(sa);
            }
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, true, false);
        } else {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;

            game.getStack().add(sa, x);
        }
    }
}

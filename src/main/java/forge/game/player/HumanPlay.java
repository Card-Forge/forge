package forge.game.player;

import java.util.List;

import forge.Card;
import forge.FThreads;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.Ability;
import forge.card.spellability.HumanPlaySpellAbility;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.InputPayManaBase;
import forge.control.input.InputPayManaSimple;
import forge.game.GameActionUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class HumanPlay {

    /**
     * TODO: Write javadoc for Constructor.
     */
    public HumanPlay() {
        // TODO Auto-generated constructor stub
    }

    /**
     * TODO: Write javadoc for this method.
     * @param card
     * @param ab
     */
    public static void playSpellAbility(Player p, Card c, SpellAbility ab) {
        if (ab == Ability.PLAY_LAND_SURROGATE)
            p.playLand(c);
        else {
            HumanPlay.playSpellAbility(p, ab);
        }
        p.getGame().getPhaseHandler().setPriority(p);
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final static void playSpellAbility(Player p, SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
        sa.setActivatingPlayer(p);
    
        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);
    
        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }
    
        sa = chooseOptionalAdditionalCosts(p, sa);
    
        if (sa == null) {
            return;
        }
    
        // Need to check PayCosts, and Ability + All SubAbilities for Target
        boolean newAbility = sa.getPayCosts() != null;
        SpellAbility ability = sa;
        while ((ability != null) && !newAbility) {
            final Target tgt = ability.getTarget();
    
            newAbility |= tgt != null;
            ability = ability.getSubAbility();
        }
    
        // System.out.println("Playing:" + sa.getDescription() + " of " + sa.getSourceCard() +  " new = " + newAbility);
        if (newAbility) {
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost("0", sa.isAbility()), sa);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa);
            }
    
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, false, false);
        } else {
            if (payManaCostIfNeeded(p, sa)) {
                if (sa.isSpell() && !source.isCopiedSpell()) {
                    sa.setSourceCard(p.getGame().getAction().moveToStack(source));
                }
                p.getGame().getStack().add(sa);
            } 
        }
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param activator 
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    static SpellAbility chooseOptionalAdditionalCosts(Player p, final SpellAbility original) {
        //final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final List<SpellAbility> abilities = GameActionUtil.getOptionalCosts(original);
        
        if (!original.isSpell()) {
            return original;
        }
    
        return p.getController().getAbilityToPlay(abilities);
    }

    private static boolean payManaCostIfNeeded(final Player p, final SpellAbility sa) {
        final ManaCostBeingPaid manaCost; 
        if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
            manaCost = new ManaCostBeingPaid(ManaCost.ZERO);
        } else {
            manaCost = new ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
            manaCost.applySpellCostChange(sa);
        }
    
        boolean isPaid = manaCost.isPaid();
    
        if( !isPaid ) {
            InputPayManaBase inputPay = new InputPayManaSimple(p.getGame(), sa, manaCost);
            FThreads.setInputAndWait(inputPay);
            isPaid = inputPay.isPaid();
        }
        return isPaid;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param humanPlayer
     * @param c
     */
    public static final void playCardWithoutPayingManaCost(Player player, Card c) {
        final List<SpellAbility> choices = c.getBasicSpells();
        // TODO add Buyback, Kicker, ... , spells here
    
        SpellAbility sa = player.getController().getAbilityToPlay(choices);
    
        if (sa != null) {
            sa.setActivatingPlayer(player);
            playSaWithoutPayingManaCost(player, sa);
        }
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final Player player, final SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
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
                    sa.setSourceCard(player.getGame().getAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;
    
            player.getGame().getStack().add(sa, x);
        }
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param skipTargeting
     *            a boolean.
     */
    public final static void playSpellAbilityNoStack(final Player player, final SpellAbility sa) {
        playSpellAbilityNoStack(player, sa, false);
    }

    public final static void playSpellAbilityNoStack(final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);
    
        if (sa.getPayCosts() != null) {
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, new CostPayment(sa.getPayCosts(), sa));
            
            req.fillRequirements(useOldTargets, false, true);
        } else {
            if (payManaCostIfNeeded(player, sa)) {
                AbilityUtils.resolve(sa, false);
            }
    
        }
    }

}

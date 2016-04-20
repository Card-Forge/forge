package forge.game.ability.effects;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.Lang;

import java.util.List;

import com.google.common.collect.Iterables;

public class AttachEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        if (sa.getHostCard().isAura() && sa.isSpell()) {

            final Player ap = sa.getActivatingPlayer();
            // The Spell_Permanent (Auras) version of this AF needs to
            // move the card into play before Attaching
            
            sa.getHostCard().setController(ap, 0);
            final Card c = ap.getGame().getAction().moveTo(ap.getZone(ZoneType.Battlefield), sa.getHostCard());
            sa.setHostCard(c);
        }

        Card source = sa.getHostCard();
        Card card = sa.getHostCard();

        final List<GameObject> targets = getTargets(sa);

        final Player p = sa.getActivatingPlayer();
        String message = "Do you want to attach " + card + " to " + targets + "?";
        if ( sa.hasParam("Optional") && !p.getController().confirmAction(sa, null, message) )
            return;

        if (sa.hasParam("Object")) {
            CardCollection lists = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa);
            if (sa.hasParam("ChooseAnObject")) {
                card = p.getController().chooseSingleEntityForEffect(lists, sa, sa.getParam("ChooseAnObject"));
            } else {
                card = Iterables.getFirst(lists, null);
            }
            if (card == null) {
                return;
            }
        }

        // If Cast Targets will be checked on the Stack
        for (final Object o : targets) {
            handleAttachment(card, o, sa);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        sb.append(" Attach to ");

        final List<GameObject> targets = getTargets(sa);
        // Should never allow more than one Attachment per card

        sb.append(Lang.joinHomogenous(targets));
        return sb.toString();
    }

    /**
     * Handle attachment.
     * 
     * @param card
     *            the card
     * @param o
     *            the o
     */
    public static void handleAttachment(final Card card, final Object o, final SpellAbility sa) {

        if (o instanceof Card) {
            final Card c = (Card) o;
            if (card.isAura()) {
                // Most Auras can enchant permanents, a few can Enchant cards in
                // graveyards
                // Spellweaver Volute, Dance of the Dead, Animate Dead
                // Although honestly, I'm not sure if the three of those could
                // handle being scripted
                // 303.4h: If the card can't be enchanted, the aura doesn't move
                if (c.canBeEnchantedBy(card)) {
                    handleAura(card, c);
                }
            } else if (card.isEquipment()) {
                if(c.canBeEquippedBy(card)) {
                    card.equipCard(c);
                }
            } else if (card.isFortification()) {
                card.fortifyCard(c);
            }
        } else if (o instanceof Player) {
            // Currently, a few cards can enchant players
            // Psychic Possession, Paradox Haze, Wheel of Sun and Moon, New
            // Curse cards
            final Player p = (Player) o;
            if (card.isAura()) {
                handleAura(card, p);
            }
        }
    }

    /**
     * Handle aura.
     * 
     * @param card
     *            the card
     * @param tgt
     *            the tgt
     */
    public static void handleAura(final Card card, final GameEntity tgt) {
        if (card.isEnchanting()) {
            // If this Card is already Enchanting something
            // Need to unenchant it, then clear out the commands
            final GameEntity oldEnchanted = card.getEnchanting();
            oldEnchanted.removeEnchantedBy(card);
            card.removeEnchanting(oldEnchanted);
        }

        final GameCommand onLeavesPlay = new GameCommand() {
            private static final long serialVersionUID = -639204333673364477L;

            @Override
            public void run() {
                final GameEntity entity = card.getEnchanting();
                if (entity == null) {
                    return;
                }

                card.unEnchantEntity(entity);
            }
        }; // Command

        card.addLeavesPlayCommand(onLeavesPlay);
        card.enchantEntity(tgt);
    }

    /**
     * Gets the attach spell ability.
     * 
     * @param source
     *            the source
     * @return the attach spell ability
     */
    public static SpellAbility getAttachSpellAbility(final Card source) {
        for (final SpellAbility sa : source.getSpells()) {
            if (sa.getApi() == ApiType.Attach) {
                return sa;
            }
        }
        return null;
    }

    /**
     * Attach aura on indirect enter battlefield.
     * 
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean attachAuraOnIndirectEnterBattlefield(final Card source) {
        // When an Aura ETB without being cast you can choose a valid card to
        // attach it to
        final SpellAbility aura = getAttachSpellAbility(source);

        if (aura == null) {
            return false;
        }
        aura.setActivatingPlayer(source.getController());
        final Game game = source.getGame();
        final TargetRestrictions tgt = aura.getTargetRestrictions();

        Player p = source.getController();
        if (tgt.canTgtPlayer()) {
            final FCollection<Player> players = new FCollection<Player>();

            for (Player player : game.getPlayers()) {
                if (player.isValid(tgt.getValidTgts(), aura.getActivatingPlayer(), source, null)) {
                    players.add(player);
                }
            }
            final Player pa = p.getController().chooseSingleEntityForEffect(players, aura, source + " - Select a player to attach to.");
            if (pa != null) {
                handleAura(source, pa);
                return true;
            }
        }
        else {
            CardCollectionView list = game.getCardsIn(tgt.getZone());
            list = CardLists.getValidCards(list, tgt.getValidTgts(), aura.getActivatingPlayer(), source, aura);
            if (list.isEmpty()) {
                return false;
            }

            final Card o = p.getController().chooseSingleEntityForEffect(list, aura, source + " - Select a card to attach to.");
            if (o != null) {
                handleAura(source, o);
                //source.enchantEntity((Card) o);
                return true;
            }
        }
        return false;
    }
}

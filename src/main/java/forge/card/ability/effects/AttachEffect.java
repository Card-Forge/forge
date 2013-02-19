package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;
import forge.Card;
import forge.CardLists;
import forge.Command;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.SpellAbilityEffect;
import forge.card.ability.ai.AttachAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;

public class AttachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        if (sa.getSourceCard().isAura() && sa.isSpell()) {

            // The Spell_Permanent (Auras) version of this AF needs to
            // move the card into play before Attaching
            sa.getSourceCard().addController(sa.getActivatingPlayer());
            final Card c = Singletons.getModel().getGame().getAction().moveTo(sa.getActivatingPlayer().getZone(ZoneType.Battlefield), sa.getSourceCard());
            sa.setSourceCard(c);
        }

        Card source = sa.getSourceCard();
        Card card = sa.getSourceCard();

        final List<Object> targets = getTargetObjects(sa);

        if (sa.hasParam("Object")) {
            card = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa).get(0);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Do you want to attach " + card + " to " + targets + "?");
        if (sa.getActivatingPlayer().isHuman() && sa.hasParam("Optional")
                && !GuiDialog.confirm(source, sb.toString())) {
            return;
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

        final List<Object> targets = getTargetObjects(sa);
        // Should never allow more than one Attachment per card

        for (final Object o : targets) {
            sb.append(o).append(" ");
        }

        return sb.toString();
    }

    /**
     * Handle attachment.
     * 
     * @param card
     *            the card
     * @param o
     *            the o
     * @param af
     *            the af
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
                final boolean gainControl = "GainControl".equals(sa.getParam("AILogic"));
                handleAura(card, c, gainControl);
            } else if (card.isEquipment()) {
                card.equipCard(c);
                // else if (card.isFortification())
                // card.fortifyCard(c);
            }
        } else if (o instanceof Player) {
            // Currently, a few cards can enchant players
            // Psychic Possession, Paradox Haze, Wheel of Sun and Moon, New
            // Curse cards
            final Player p = (Player) o;
            if (card.isAura()) {
                handleAura(card, p, false);
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
     * @param gainControl
     *            the gain control
     */
    public static void handleAura(final Card card, final GameEntity tgt, final boolean gainControl) {
        if (card.isEnchanting()) {
            // If this Card is already Enchanting something
            // Need to unenchant it, then clear out the commands
            final GameEntity oldEnchanted = card.getEnchanting();
            oldEnchanted.removeEnchantedBy(card);
            card.removeEnchanting(oldEnchanted);
            card.clearEnchantCommand();
            card.clearUnEnchantCommand();
            card.clearTriggers(); // not sure if cleartriggers is needed?
        }

        if (gainControl) {
            // Handle GainControl Auras
            final Player[] pl = new Player[1];

            if (tgt instanceof Card) {
                pl[0] = ((Card) tgt).getController();
            } else {
                pl[0] = (Player) tgt;
            }

            final Command onEnchant = new Command() {
                private static final long serialVersionUID = -2519887209491512000L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }

                    pl[0] = crd.getController();

                    crd.addController(card);

                } // execute()
            }; // Command

            final Command onUnEnchant = new Command() {
                private static final long serialVersionUID = 3426441132121179288L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }

                    if (crd.isInPlay()) {
                        crd.removeController(card);
                    }

                } // execute()
            }; // Command

            final Command onChangesControl = new Command() {
                /** automatically generated serialVersionUID. */
                private static final long serialVersionUID = -65903786170234039L;

                @Override
                public void execute() {
                    final Card crd = card.getEnchantingCard();
                    if (crd == null) {
                        return;
                    }
                    crd.removeController(card); // This looks odd, but will
                                                // simply refresh controller
                    crd.addController(card);
                } // execute()
            }; // Command

            // Add Enchant Commands for Control changers
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addChangeControllerCommand(onChangesControl);
        }

        final Command onLeavesPlay = new Command() {
            private static final long serialVersionUID = -639204333673364477L;

            @Override
            public void execute() {
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
        SpellAbility aura = null;
        for (final SpellAbility sa : source.getSpells()) {
            if (sa.getApi() == ApiType.Attach) {
                aura = sa;
                break;
            }
        }
        return aura;
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
        final Target tgt = aura.getTarget();
        final boolean gainControl = "GainControl".equals(aura.getParam("AILogic"));

        if (source.getController().isHuman()) {
            if (tgt.canTgtPlayer()) {
                final ArrayList<Player> players = new ArrayList<Player>();

                for (Player player : Singletons.getModel().getGame().getPlayers()) {
                    if (player.isValid(tgt.getValidTgts(), aura.getActivatingPlayer(), source)) {
                        players.add(player);
                    }
                }

                final Player p = GuiChoose.one(source + " - Select a player to attach to.", players);
                if (p != null) {
                    handleAura(source, p, false);
                    //source.enchantEntity((Player) o);
                    return true;
                }
            } else {
                List<Card> list = Singletons.getModel().getGame().getCardsIn(tgt.getZone());
                list = CardLists.getValidCards(list, tgt.getValidTgts(), aura.getActivatingPlayer(), source);
                if (list.isEmpty()) {
                    return false;
                }

                final Object o = GuiChoose.one(source + " - Select a card to attach to.", list);
                if (o instanceof Card) {
                    handleAura(source, (Card) o, gainControl);
                    //source.enchantEntity((Card) o);
                    return true;
                }
            }
        }

        else if (AttachAi.attachPreference(aura, tgt, true)) {
            final Object o = aura.getTarget().getTargets().get(0);
            if (o instanceof Card) {
                //source.enchantEntity((Card) o);
                handleAura(source, (Card) o, gainControl);
                return true;
            } else if (o instanceof Player) {
                //source.enchantEntity((Player) o);
                handleAura(source, (Player) o, false);
                return true;
            }
        }

        return false;
    }
}

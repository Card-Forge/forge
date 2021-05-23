package forge.game.ability.effects;

import java.util.List;

import forge.GameCommand;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardZoneTable;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.collect.FCollection;

public class AttachEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        if (host.isAura() && sa.isSpell()) {
            CardZoneTable table = new CardZoneTable();
            host.setController(sa.getActivatingPlayer(), 0);

            ZoneType previousZone = host.getZone().getZoneType();

            // The Spell_Permanent (Auras) version of this AF needs to
            // move the card into play before Attaching
            final Card c = game.getAction().moveToPlay(host, sa);
            sa.setHostCard(c);

            ZoneType newZone = c.getZone().getZoneType();
            if (newZone != previousZone) {
                table.put(previousZone, newZone, c);
            }
            table.triggerChangesZoneAll(game, sa);
        }

        final Card source = sa.getHostCard();

        CardCollection attachments;
        final List<GameObject> targets = getDefinedOrTargeted(sa, "Defined");
        GameObject attachTo;

        if (targets.isEmpty()) {
            return;
        }

        attachTo = targets.get(0);
        String attachToName = null;
        if (attachTo instanceof Card) {
            attachToName = CardTranslation.getTranslatedName(((Card)attachTo).getName());
        }
        else {
            attachToName = attachTo.toString();
        }

        final Player p = sa.getActivatingPlayer();

        if (sa.hasParam("Choices")) {
            ZoneType choiceZone = ZoneType.Battlefield;
            if (sa.hasParam("ChoiceZone")) {
                choiceZone = ZoneType.smartValueOf(sa.getParam("ChoiceZone"));
            }
            String title = sa.hasParam("ChoiceTitle") ? sa.getParam("ChoiceTitle") : Localizer.getInstance().getMessage("lblChoose") + " ";

            CardCollection choices = CardLists.getValidCards(game.getCardsIn(choiceZone), sa.getParam("Choices"), p, source, sa);

            Card c = p.getController().chooseSingleEntityForEffect(choices, sa, title, null);
            if (c == null) {
                return;
            }
            attachments = new CardCollection(c);
        } else  if (sa.hasParam("Object")) {
            attachments = AbilityUtils.getDefinedCards(source, sa.getParam("Object"), sa);
        } else {
            attachments = new CardCollection(source);
        }

        // If Cast Targets will be checked on the Stack
        for (final Card attachment : attachments) {
            String message = Localizer.getInstance().getMessage("lblDoYouWantAttachSourceToTarget", CardTranslation.getTranslatedName(attachment.getName()), attachToName);
            if ( sa.hasParam("Optional") && !p.getController().confirmAction(sa, null, message) )
                continue;
            handleAttachment(attachment, attachTo, sa);
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

        if (card == null) { return; }

        if (o instanceof Card) {
            final Card c = (Card) o;
            if (card.isAura()) {
                // Most Auras can enchant permanents, a few can Enchant cards in
                // graveyards
                // Spellweaver Volute, Dance of the Dead, Animate Dead
                // Although honestly, I'm not sure if the three of those could
                // handle being scripted
                // 303.4h: If the card can't be enchanted, the aura doesn't move
                if (c.canBeAttached(card)) {
                    handleAura(card, c);
                }
            } else  {
                card.attachToEntity(c);
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
        final GameCommand onLeavesPlay = new GameCommand() {
            private static final long serialVersionUID = -639204333673364477L;

            @Override
            public void run() {
                final GameEntity entity = card.getEntityAttachedTo();
                if (entity == null) {
                    return;
                }

                card.unattachFromEntity(entity);
            }
        }; // Command

        card.addLeavesPlayCommand(onLeavesPlay);
        card.attachToEntity(tgt);
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
        final SpellAbility aura = source.getFirstAttachSpell();

        if (aura == null) {
            return false;
        }
        aura.setActivatingPlayer(source.getController());
        final Game game = source.getGame();
        final TargetRestrictions tgt = aura.getTargetRestrictions();

        Player p = source.getController();
        if (tgt.canTgtPlayer()) {
            final FCollection<Player> players = new FCollection<>();

            for (Player player : game.getPlayers()) {
                if (player.isValid(tgt.getValidTgts(), aura.getActivatingPlayer(), source, aura)) {
                    players.add(player);
                }
            }
            final Player pa = p.getController().chooseSingleEntityForEffect(players, aura,
                    Localizer.getInstance().getMessage("lblSelectAPlayerAttachSourceTo", CardTranslation.getTranslatedName(source.getName())), null);
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

            final Card o = p.getController().chooseSingleEntityForEffect(list, aura,
                    Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(source.getName())), null);
            if (o != null) {
                handleAura(source, o);
                //source.enchantEntity((Card) o);
                return true;
            }
        }
        return false;
    }
}

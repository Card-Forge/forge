package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class SacrificeEffect extends SpellEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        // Expand Sacrifice keyword here depending on what we need out of it.
        final String num = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
        final int amount = AbilityFactory.calculateAmount(card, num, sa);
        final List<Player> tgts = getTargetPlayers(sa);

        String valid = sa.getParam("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String msg = sa.getParam("SacMessage");
        if (msg == null) {
            msg = valid;
        }

        msg = "Sacrifice a " + msg;

        final boolean destroy = sa.hasParam("Destroy");
        final boolean remSacrificed = sa.hasParam("RememberSacrificed");

        if (valid.equals("Self")) {
            if (Singletons.getModel().getGame().getZoneOf(card).is(ZoneType.Battlefield)) {
                if (Singletons.getModel().getGame().getAction().sacrifice(card, sa) && remSacrificed) {
                    card.addRemembered(card);
                }
            }
        }
        else {
            List<Card> sacList = null;
            for (final Player p : tgts) {
                if (sa.hasParam("Random")) {
                    sacList = sacrificeRandom(p, amount, valid, sa, destroy);
                } else if (p.isComputer()) {
                    if (sa.hasParam("Optional") && sa.getActivatingPlayer().isHuman()) {
                        continue;
                    }
                    sacList = sacrificeAI(p, amount, valid, sa, destroy);
                } else {
                    sacList = sacrificeHuman(p, amount, valid, sa, destroy,
                            sa.hasParam("Optional"));
                }
                if (remSacrificed) {
                    for (int i = 0; i < sacList.size(); i++) {
                        card.addRemembered(sacList.get(i));
                    }
                }
            }

        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgts = getTargetPlayers(sa);

        String valid = sa.getParam("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String num = sa.getParam("Amount");
        num = (num == null) ? "1" : num;
        final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

        if (valid.equals("Self")) {
            sb.append("Sacrifice ").append(sa.getSourceCard().toString());
        } else if (valid.equals("Card.AttachedBy")) {
            final Card toSac = sa.getSourceCard().getEnchantingCard();
            sb.append(toSac.getController()).append(" sacrifices ").append(toSac).append(".");
        } else {
            for (final Player p : tgts) {
                sb.append(p.getName()).append(" ");
            }

            String msg = sa.getParam("SacMessage");
            if (msg == null) {
                msg = valid;
            }

            if (sa.hasParam("Destroy")) {
                sb.append("Destroys ");
            } else {
                sb.append("Sacrifices ");
            }
            sb.append(amount).append(" ").append(msg).append(".");
        }

        return sb.toString();
    }

    /**
     * <p>
     * sacrificeAI.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private List<Card> sacrificeAI(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy) {
        List<Card> battlefield = p.getCardsIn(ZoneType.Battlefield);
        List<Card> sacList = AbilityFactory.filterListByType(battlefield, valid, sa);
        sacList = ComputerUtil.sacrificePermanents(p, amount, sacList, destroy, sa);

        return sacList;
    }

    /**
     * <p>
     * sacrificeHuman.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static List<Card> sacrificeHuman(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy, final boolean optional) {
        List<Card> list = AbilityFactory.filterListByType(p.getCardsIn(ZoneType.Battlefield), valid, sa);
        List<Card> sacList = new ArrayList<Card>();

        for (int i = 0; i < amount; i++) {
            if (list.isEmpty()) {
                break;
            }
            Card c;
            if (optional) {
                c = GuiChoose.oneOrNone("Select a card to sacrifice", list);
            } else {
                c = GuiChoose.one("Select a card to sacrifice", list);
            }
            if (c != null) {
                if (destroy) {
                    if (Singletons.getModel().getGame().getAction().destroy(c)) {
                        sacList.add(c);
                    }
                } else {
                    if (Singletons.getModel().getGame().getAction().sacrifice(c, sa)) {
                        sacList.add(c);
                    }
                }

                list.remove(c);

            } else {
                return sacList;
            }
        }
        return sacList;
    }

    /**
     * <p>
     * sacrificeRandom.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @param amount
     *            a int.
     * @param valid
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private List<Card> sacrificeRandom(final Player p, final int amount, final String valid, final SpellAbility sa,
            final boolean destroy) {
        List<Card> sacList = new ArrayList<Card>();
        for (int i = 0; i < amount; i++) {
            List<Card> battlefield = p.getCardsIn(ZoneType.Battlefield);
            List<Card> list = AbilityFactory.filterListByType(battlefield, valid, sa);
            if (list.size() != 0) {
                final Card sac = CardUtil.getRandom(list);
                if (destroy) {
                    if (Singletons.getModel().getGame().getAction().destroy(sac)) {
                        sacList.add(sac);
                    }
                } else {
                    if (Singletons.getModel().getGame().getAction().sacrifice(sac, sa)) {
                        sacList.add(sac);
                    }
                }
            }
        }
        return sacList;
    }

}

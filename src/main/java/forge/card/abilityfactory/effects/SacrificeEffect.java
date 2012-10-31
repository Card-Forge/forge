package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class SacrificeEffect extends SpellEffect {
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();

        // Expand Sacrifice keyword here depending on what we need out of it.
        final String num = params.containsKey("Amount") ? params.get("Amount") : "1";
        final int amount = AbilityFactory.calculateAmount(card, num, sa);

        final Target tgt = sa.getTarget();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(card, params.get("Defined"), sa);
        }

        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }

        String msg = params.get("SacMessage");
        if (msg == null) {
            msg = valid;
        }

        msg = "Sacrifice a " + msg;

        final boolean destroy = params.containsKey("Destroy");
        final boolean remSacrificed = params.containsKey("RememberSacrificed");

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
                if (params.containsKey("Random")) {
                    sacList = sacrificeRandom(p, amount, valid, sa, destroy);
                } else if (p.isComputer()) {
                    if (params.containsKey("Optional") && sa.getActivatingPlayer().isHuman()) {
                        continue;
                    }
                    sacList = sacrificeAI(p, amount, valid, sa, destroy);
                } else {
                    sacList = sacrificeHuman(p, amount, valid, sa, destroy,
                            params.containsKey("Optional"));
                }
                if (remSacrificed) {
                    for (int i = 0; i < sacList.size(); i++) {
                        card.addRemembered(sacList.get(i));
                    }
                }
            }

        }
    }

    /**
     * <p>
     * sacrificeDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        }
    
        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }
    
        final Target tgt = sa.getTarget();
        ArrayList<Player> tgts;
        if (tgt != null) {
            tgts = tgt.getTargetPlayers();
        } else {
            tgts = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        String valid = params.get("SacValid");
        if (valid == null) {
            valid = "Self";
        }
    
        String num = params.get("Amount");
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
    
            String msg = params.get("SacMessage");
            if (msg == null) {
                msg = valid;
            }
    
            if (params.containsKey("Destroy")) {
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
            Object o;
            if (optional) {
                o = GuiChoose.oneOrNone("Select a card to sacrifice", list);
            } else {
                o = GuiChoose.one("Select a card to sacrifice", list);
            }
            if (o != null) {
                final Card c = (Card) o;

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

    // **************************************************************
    // *********************** SacrificeAll *************************
    // **************************************************************


    /**
     * <p>
     * sacrificeAllCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     * @since 1.0.15
     */
    
}
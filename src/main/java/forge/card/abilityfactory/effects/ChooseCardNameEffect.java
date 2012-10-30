package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicates;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.CardPredicates.Presets;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.ListChooser;
import forge.item.CardDb;
import forge.item.CardPrinted;

public class ChooseCardNameEffect extends SpellEffect {

    /**
     * <p>
     * nameCardStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    public String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("names a card.");
    
        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }
    
        return sb.toString();
    }

    /**
     * <p>
     * nameCardResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        String valid = "Card";
        String validDesc = "card";
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
            validDesc = params.get("ValidDesc");
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                boolean ok = false;
                String name = null;
                while (!ok) {
                    if (p.isHuman()) {
                        final String message = validDesc.equals("card") ? "Name a card" : "Name a " + validDesc
                                + " card. (Case sensitive)";
                        /*
                         * name = JOptionPane.showInputDialog(null, message,
                         * host.getName(), JOptionPane.QUESTION_MESSAGE); if
                         * (!valid.equals("Card") && !(null == name)) { try {
                         * final Card temp =
                         * AllZone.getCardFactory().getCard(name, p); ok =
                         * temp.isValid(valid, host.getController(), host); }
                         * catch (final Exception ignored) { ok = false; } }
                         * else { ok = true; } if (ok) { host.setNamedCard(null
                         * == name ? "" : name); }
                         */
                        final List<String> cards = new ArrayList<String>();
                        for (final CardPrinted c : CardDb.instance().getAllUniqueCards()) {
                            cards.add(c.getName());
                        }
                        Collections.sort(cards);

                        // use standard forge's list selection dialog
                        final ListChooser<String> choice = new ListChooser<String>(message, 1, 1, cards);
                        choice.show();
                        // still missing a listener to display the card preview
                        // in the right
                        name = choice.getSelectedValue();
                        if (Singletons.getModel().getCardFactory().getCard(CardDb.instance().getCard(name), p).isValid(valid, host.getController(), host)) {
                            host.setNamedCard(choice.getSelectedValue());
                            ok = true;
                        }
                    } else {
                        String chosen = "";
                        if (params.containsKey("AILogic")) {
                            final String logic = params.get("AILogic");
                            if (logic.equals("MostProminentInComputerDeck")) {
                                chosen = CardFactoryUtil.getMostProminentCardName(p.getCardsIn(ZoneType.Library));
                            } else if (logic.equals("MostProminentInHumanDeck")) {
                                chosen = CardFactoryUtil.getMostProminentCardName(p.getOpponent().getCardsIn(ZoneType.Library));
                            }
                        } else {
                            List<Card> list = CardLists.filterControlledBy(Singletons.getModel().getGame().getCardsInGame(), p.getOpponent());
                            list = CardLists.filter(list, Predicates.not(Presets.LANDS));
                            if (!list.isEmpty()) {
                                chosen = list.get(0).getName();
                            }
                        }
                        if (chosen.equals("")) {
                            chosen = "Morphling";
                        }
                        GuiChoose.one("Computer picked: ", new String[]{chosen});
                        host.setNamedCard(chosen);
                        ok = true;
                    }
                }
            }
        }
    }

    // *************************************************************************
    // *************************** ChooseCard **********************************
    // *************************************************************************


    /**
     * <p>
     * chooseCardCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    
}
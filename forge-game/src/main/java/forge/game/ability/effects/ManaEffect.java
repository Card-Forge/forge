package forge.game.ability.effects;

import static forge.util.TextUtil.toManaString;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import io.sentry.Sentry;
import io.sentry.event.BreadcrumbBuilder;

public class ManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        AbilityManaPart abMana = sa.getManaPart();

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable());

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final boolean optional = sa.hasParam("Optional");
        final Game game = sa.getActivatingPlayer().getGame();

        if (optional && !sa.getActivatingPlayer().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantAddMana"))) {
            return;
        }

        for (Player p : tgtPlayers) {
            if (sa.usesTargeting() && !p.canBeTargetedBy(sa)) {
                // Illegal target. Skip.
                continue;
            }

            if (abMana.isComboMana()) {
                int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa) : 1;

                String express = abMana.getExpressChoice();
                String[] colorsProduced = abMana.getComboColors().split(" ");

                final StringBuilder choiceString = new StringBuilder();
                ColorSet colorOptions = ColorSet.fromNames(colorsProduced);
                String[] colorsNeeded = express.isEmpty() ? null : express.split(" ");
                boolean differentChoice = abMana.getOrigProduced().contains("Different");
                ColorSet fullOptions = colorOptions;
                for (int nMana = 0; nMana < amount; nMana++) {
                    String choice = "";
                    if (colorsNeeded != null && colorsNeeded.length > nMana) { // select from express choices if possible
                        colorOptions = ColorSet
                                .fromMask(fullOptions.getColor() & ManaAtom.fromName(colorsNeeded[nMana]));
                    }
                    if (colorOptions.isColorless() && colorsProduced.length > 0) {
                        // If we just need generic mana, no reason to ask the controller for a choice,
                        // just use the first possible color.
                        choice = colorsProduced[differentChoice ? nMana : 0];
                    } else {
                        byte chosenColor = p.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa,
                                differentChoice && (colorsNeeded == null || colorsNeeded.length <= nMana) ? fullOptions : colorOptions);
                        if (chosenColor == 0)
                            throw new RuntimeException("ManaEffect::resolve() /*combo mana*/ - " + p + " color mana choice is empty for " + card.getName());

                        if (differentChoice) {
                            fullOptions = ColorSet.fromMask(fullOptions.getColor() - chosenColor);
                        }
                        choice = MagicColor.toShortString(chosenColor);
                    }

                    if (nMana > 0) {
                        choiceString.append(" ");
                    }
                    choiceString.append(choice);
                    if (sa.hasParam("TwoEach")) {
                        choiceString.append(" ").append(choice);
                    }
                }

                if (choiceString.toString().isEmpty() && "Combo ColorIdentity".equals(abMana.getOrigProduced())) {
                    // No mana could be produced here (non-EDH match?), so cut short
                    return;
                }

                game.getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), choiceString), p);
                abMana.setExpressChoice(choiceString.toString());
            }
            else if (abMana.isAnyMana()) {
                // AI color choice is set in ComputerUtils so only human players need to make a choice

                String colorsNeeded = abMana.getExpressChoice();
                String choice = "";

                ColorSet colorMenu = null;
                byte mask = 0;
                //loop through colors to make menu
                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                    mask |= MagicColor.fromName(colorsNeeded.charAt(nChar));
                }
                colorMenu = mask == 0 ? ColorSet.ALL_COLORS : ColorSet.fromMask(mask);
                byte val = p.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                if (0 == val) {
                    throw new RuntimeException("ManaEffect::resolve() /*any mana*/ - " + p + " color mana choice is empty for " + card.getName());
                }
                choice = MagicColor.toShortString(val);

                game.getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), choice), p);
                abMana.setExpressChoice(choice);
            }
            else if (abMana.isSpecialMana()) {

                String type = abMana.getOrigProduced().split("Special ")[1];

                if (type.equals("EnchantedManaCost")) {
                    Card enchanted = card.getEnchantingCard();
                    if (enchanted == null ) 
                        continue;

                    StringBuilder sb = new StringBuilder();
                    int generic = enchanted.getManaCost().getGenericCost();
                    if( generic > 0 )
                        sb.append(generic);

                    for (ManaCostShard s : enchanted.getManaCost()) {
                        ColorSet cs = ColorSet.fromMask(s.getColorMask());
                        if(cs.isColorless())
                            continue;
                        sb.append(' ');
                        if (cs.isMonoColor())
                            sb.append(MagicColor.toShortString(s.getColorMask()));
                        else /* (cs.isMulticolor()) */ {
                            byte chosenColor = p.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                            sb.append(MagicColor.toShortString(chosenColor));
                        }
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                } else if (type.equals("LastNotedType")) {
                    final StringBuilder sb = new StringBuilder();
                    int nMana = 0;
                    for (Object o : card.getRemembered()) {
                        if (o instanceof Mana) {
                            if (nMana > 0) {
                                sb.append(" ");
                            }
                            sb.append(o.toString());
                            nMana++;
                        }
                    }
                    if (nMana == 0) {
                        return;
                    }
                    abMana.setExpressChoice(sb.toString());
                } else if (type.startsWith("EachColorAmong")) {
                    final String res = type.split("_")[1];
                    final CardCollection list = CardLists.getValidCards(card.getGame().getCardsIn(ZoneType.Battlefield),
                            res, sa.getActivatingPlayer(), card, sa);
                    byte colors = 0;
                    for (Card c : list) {
                        colors |= c.determineColor().getColor();
                    }
                    if (colors == 0) return;
                    abMana.setExpressChoice(ColorSet.fromMask(colors));
                } else if (type.startsWith("EachColoredManaSymbol")) {
                    final String res = type.split("_")[1];
                    StringBuilder sb = new StringBuilder();
                    for (Card c : AbilityUtils.getDefinedCards(card, res, sa)) {
                        for (ManaCostShard s : c.getManaCost()) {
                            ColorSet cs = ColorSet.fromMask(s.getColorMask());
                            if(cs.isColorless())
                                continue;
                            sb.append(' ');
                            if (cs.isMonoColor())
                                sb.append(MagicColor.toShortString(s.getColorMask()));
                            else /* (cs.isMulticolor()) */ {
                                byte chosenColor = p.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                                sb.append(MagicColor.toShortString(chosenColor));
                            }
                        }
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                } else if (type.startsWith("DoubleManaInPool")) {
                    StringBuilder sb = new StringBuilder();
                    for (byte color : ManaAtom.MANATYPES) {
                        sb.append(StringUtils.repeat(MagicColor.toShortString(color), " ", p.getManaPool().getAmountOfColor(color))).append(" ");
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                }
            }

            String mana = GameActionUtil.generatedMana(sa);

            // this can happen when mana is based on criteria that didn't match
            if (mana.isEmpty()) {
                String msg = "AbilityFactoryMana::manaResolve() - special mana effect is empty for";
                Sentry.getContext().recordBreadcrumb(
                        new BreadcrumbBuilder().setMessage(msg)
                        .withData("Card", card.getName()).withData("SA", sa.toString()).build()
                );
                continue;
            }

            abMana.produceMana(mana, p, sa);
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();

        //resolveDrawback(sa);
    }

    /**
     * <p>
     * manaStackDescription.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     * 
     * @return a {@link java.lang.String} object.
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String mana = !sa.hasParam("Amount") || StringUtils.isNumeric(sa.getParam("Amount"))
                ? GameActionUtil.generatedMana(sa) : "mana";
        sb.append("Add ").append(toManaString(mana)).append(".");
        return sb.toString();
    }
}

package forge.card.abilityFactory;

import forge.*;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.*;
import forge.card.trigger.Trigger;
import forge.gui.GuiUtils;

import java.util.*;

/**
 * <p>AbilityFactory_Copy class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityFactory_Copy {

    // *************************************************************************
    // ************************* CopyPermanent *********************************
    // *************************************************************************

    /**
     * <p>createAbilityCopyPermanent.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityCopyPermanent(final AbilityFactory af) {

        final SpellAbility abCopyPermanent = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 4557071554433108024L;

            @Override
            public String getStackDescription() {
                return copyPermanentStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return copyPermanentCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                copyPermanentResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return copyPermanentTriggerAI(af, this, mandatory);
            }

        };
        return abCopyPermanent;
    }

    /**
     * <p>createSpellCopyPermanent.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellCopyPermanent(final AbilityFactory af) {
        final SpellAbility spCopyPermanent = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 3313370358993251728L;

            @Override
            public String getStackDescription() {
                return copyPermanentStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return copyPermanentCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                copyPermanentResolve(af, this);
            }

        };
        return spCopyPermanent;
    }

    /**
     * <p>createDrawbackCopyPermanent.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackCopyPermanent(final AbilityFactory af) {
        final SpellAbility dbCopyPermanent = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = -7725564505830285184L;

            @Override
            public String getStackDescription() {
                return copyPermanentStackDescription(af, this);
            }

            @Override
            public void resolve() {
                copyPermanentResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return copyPermanentTriggerAI(af, this, mandatory);
            }

        };
        return dbCopyPermanent;
    }

    /**
     * <p>copyPermanentStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String copyPermanentStackDescription(AbilityFactory af, SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        HashMap<String, String> params = af.getMapParams();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard()).append(" - ");
        else
            sb.append(" ");

        ArrayList<Card> tgtCards;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtCards = tgt.getTargetCards();
        else
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        sb.append("Copy ");
        Iterator<Card> it = tgtCards.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) sb.append(", ");
        }
        sb.append(".");

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>copyPermanentCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean copyPermanentCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
    	//Card source = sa.getSourceCard();
        //TODO - I'm sure someone can do this AI better

        HashMap<String, String> params = af.getMapParams();
        if (params.containsKey("AtEOT") && !AllZone.getPhase().is(Constant.Phase.Main1)) {
            return false;
        } else {
            double chance = .4;    // 40 percent chance with instant speed stuff
            if (AbilityFactory.isSorcerySpeed(sa))
                chance = .667;    // 66.7% chance for sorcery speed (since it will never activate EOT)
            Random r = MyRandom.random;
            if (r.nextFloat() <= Math.pow(chance, sa.getActivationsThisTurn() + 1))
            	return copyPermanentTriggerAI(af, sa, false);
            else return false;
        }
    }

    /**
     * <p>copyPermanentTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean copyPermanentTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
        //HashMap<String,String> params = af.getMapParams();
        Card source = sa.getSourceCard();

        if (!ComputerUtil.canPayCost(sa) && !mandatory)
            return false;

        //////
        // Targeting

        Target abTgt = sa.getTarget();

        if (abTgt != null) {
            CardList list = AllZoneUtil.getCardsInPlay();
            list = list.getValidCards(abTgt.getValidTgts(), source.getController(), source);
            abTgt.resetTargets();
            // target loop
            while (abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)) {
                if (list.size() == 0) {
                    if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }

                Card choice;
                if (list.filter(AllZoneUtil.creatures).size() > 0) {
                    choice = CardFactoryUtil.AI_getBestCreature(list);
                } else {
                    choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, source, true);
                }

                if (choice == null) {    // can't find anything left
                    if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0) {
                        abTgt.resetTargets();
                        return false;
                    } else {
                        // TODO is this good enough? for up to amounts?
                        break;
                    }
                }
                list.remove(choice);
                abTgt.addTarget(choice);
            }
        } else {
            //if no targeting, it should always be ok
        }

        //end Targeting

        if (af.hasSubAbility()) {
            Ability_Sub abSub = sa.getSubAbility();
            if (abSub != null) {
                return abSub.chkAI_Drawback();
            }
        }
        return true;
    }

    /**
     * <p>copyPermanentResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void copyPermanentResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card hostCard = af.getHostCard();
        ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }
        int numCopies = params.containsKey("NumCopies") ? AbilityFactory.calculateAmount(hostCard, params.get("NumCopies"), sa) : 1;

        ArrayList<Card> tgtCards;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtCards = tgt.getTargetCards();
        else
            tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        hostCard.clearClones();

        for (Card c : tgtCards) {
            if (tgt == null || CardFactoryUtil.canTarget(hostCard, c)) {

                //start copied Kiki code
                int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(hostCard.getController());
                multiplier *= numCopies;
                Card[] crds = new Card[multiplier];

                for (int i = 0; i < multiplier; i++) {
                    //TODO: Use central copy methods
                    Card copy;
                    if (!c.isToken()) {
                        //copy creature and put it onto the battlefield
                        copy = AllZone.getCardFactory().getCard(c.getName(), sa.getActivatingPlayer());

                        //when copying something stolen:
                        copy.addController(sa.getActivatingPlayer());

                        copy.setToken(true);
                        copy.setCopiedToken(true);
                    } else { //isToken()
                        copy = CardFactoryUtil.copyStats(c);

                        copy.setName(c.getName());
                        copy.setImageName(c.getImageName());

                        copy.setOwner(sa.getActivatingPlayer());
                        copy.addController(sa.getActivatingPlayer());

                        copy.setManaCost(c.getManaCost());
                        copy.setColor(c.getColor());
                        copy.setToken(true);

                        copy.setType(c.getType());

                        copy.setBaseAttack(c.getBaseAttack());
                        copy.setBaseDefense(c.getBaseDefense());
                    }

                    //add keywords from params
                    for (String kw : keywords) {
                        copy.addIntrinsicKeyword(kw);
                    }

                    //Slight hack in case we copy a creature with triggers.
                    for (Trigger t : copy.getTriggers()) {
                        AllZone.getTriggerHandler().registerTrigger(t);
                    }

                    copy.setCurSetCode(c.getCurSetCode());
                    copy.setImageFilename(c.getImageFilename());

                    if (c.isFaceDown()) {
                        copy.setIsFaceDown(true);
                        copy.setManaCost("");
                        copy.setBaseAttack(2);
                        copy.setBaseDefense(2);
                        copy.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
                        copy.setType(new ArrayList<String>()); //remove all types
                        copy.addType("Creature");
                        copy.clearSpellAbility(); //disallow "morph_up"
                        copy.setCurSetCode("");
                        copy.setImageFilename("morph.jpg");
                    }
                    copy = AllZone.getGameAction().moveToPlay(copy);

                    copy.setCloneOrigin(hostCard);
                    sa.getSourceCard().addClone(copy);
                    crds[i] = copy;
                }

                //have to do this since getTargetCard() might change
                //if Kiki-Jiki somehow gets untapped again
                final Card[] target = new Card[multiplier];
                for (int i = 0; i < multiplier; i++) {
                    final int index = i;
                    target[index] = crds[index];

                    final SpellAbility sac = new Ability(target[index], "0") {
                        @Override
                        public void resolve() {
                            //technically your opponent could steal the token
                            //and the token shouldn't be sacrificed
                            if (AllZoneUtil.isCardInPlay(target[index])) {
                                if (params.get("AtEOT").equals("Sacrifice")) {
                                    AllZone.getGameAction().sacrifice(target[index]); //maybe do a setSacrificeAtEOT, but probably not.
                                } else if (params.get("AtEOT").equals("Exile")) {
                                    AllZone.getGameAction().exile(target[index]);
                                }

                                //Slight hack in case we copy a creature with triggers
                                AllZone.getTriggerHandler().removeAllFromCard(target[index]);
                            }
                        }
                    };

                    Command atEOT = new Command() {
                        private static final long serialVersionUID = -4184510100801568140L;

                        public void execute() {
                            sac.setStackDescription(params.get("AtEOT") + " " + target[index] + ".");
                            AllZone.getStack().addSimultaneousStackEntry(sac);
                        }
                    };//Command
                    if (params.containsKey("AtEOT")) {
                        AllZone.getEndOfTurn().addAt(atEOT);
                    }
                    //end copied Kiki code

                }
            }//end canTarget
        }//end foreach Card
    }//end resolve

    // *************************************************************************
    // ************************* CopySpell *************************************
    // *************************************************************************

    /**
     * <p>createAbilityCopySpell.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createAbilityCopySpell(final AbilityFactory af) {

        final SpellAbility abCopySpell = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 5232548517225345052L;

            @Override
            public String getStackDescription() {
                return copySpellStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return copySpellCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                copySpellResolve(af, this);
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return copySpellTriggerAI(af, this, mandatory);
            }

        };
        return abCopySpell;
    }

    /**
     * <p>createSpellCopySpell.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createSpellCopySpell(final AbilityFactory af) {
        final SpellAbility spCopySpell = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
            private static final long serialVersionUID = 1878946074608916745L;

            @Override
            public String getStackDescription() {
                return copySpellStackDescription(af, this);
            }

            @Override
            public boolean canPlayAI() {
                return copySpellCanPlayAI(af, this);
            }

            @Override
            public void resolve() {
                copySpellResolve(af, this);
            }

        };
        return spCopySpell;
    }

    /**
     * <p>createDrawbackCopySpell.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public static SpellAbility createDrawbackCopySpell(final AbilityFactory af) {
        final SpellAbility dbCopySpell = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
            private static final long serialVersionUID = 1927508119173644632L;

            @Override
            public String getStackDescription() {
                return copySpellStackDescription(af, this);
            }

            @Override
            public void resolve() {
                copySpellResolve(af, this);
            }

            @Override
            public boolean chkAI_Drawback() {
                return true;
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return copySpellTriggerAI(af, this, mandatory);
            }

        };
        return dbCopySpell;
    }

    /**
     * <p>copySpellStackDescription.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String copySpellStackDescription(AbilityFactory af, SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        HashMap<String, String> params = af.getMapParams();

        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard().getName()).append(" - ");
        else
            sb.append(" ");

        ArrayList<SpellAbility> tgtSpells;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtSpells = tgt.getTargetSAs();
        else
            tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);

        sb.append("Copy ");
        // TODO Someone fix this Description when Copying Charms
        Iterator<SpellAbility> it = tgtSpells.iterator();
        while (it.hasNext()) {
            sb.append(it.next().getSourceCard());
            if (it.hasNext()) sb.append(", ");
        }
        int amount = 1;
        if(params.containsKey("Amount"))
        {
            amount = AbilityFactory.calculateAmount(af.getHostCard(),params.get("Amount"),sa);
        }
        if(amount > 1)
        {
            sb.append(amount).append(" times");
        }
        sb.append(".");
        //TODO probably add an optional "You may choose new targets..."

        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null) {
            sb.append(abSub.getStackDescription());
        }

        return sb.toString();
    }

    /**
     * <p>copySpellCanPlayAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean copySpellCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
        return false;
    }

    /**
     * <p>copySpellTriggerAI.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory a boolean.
     * @return a boolean.
     */
    private static boolean copySpellTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
        boolean randomReturn = false;

        if (af.hasSubAbility()) {
            Ability_Sub abSub = sa.getSubAbility();
            if (abSub != null) {
                return randomReturn && abSub.chkAI_Drawback();
            }
        }
        return randomReturn;
    }

    /**
     * <p>copySpellResolve.</p>
     *
     * @param af a {@link forge.card.abilityFactory.AbilityFactory} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    private static void copySpellResolve(final AbilityFactory af, final SpellAbility sa) {
        final HashMap<String, String> params = af.getMapParams();
        Card card = af.getHostCard();

        int amount = 1;
        if(params.containsKey("Amount"))
        {
            amount = AbilityFactory.calculateAmount(card,params.get("Amount"),sa);
        }

        ArrayList<SpellAbility> tgtSpells;

        Target tgt = af.getAbTgt();
        if (tgt != null)
            tgtSpells = tgt.getTargetSAs();
        else
            tgtSpells = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);

        if (tgtSpells.size() == 0)
            return;

        SpellAbility chosenSA = null;
        if (tgtSpells.size() == 1)
            chosenSA = tgtSpells.get(0);
        else if (sa.getActivatingPlayer().isHuman())
            chosenSA = (SpellAbility) GuiUtils.getChoice("Select a spell to copy", tgtSpells.toArray());
        else
            chosenSA = tgtSpells.get(0);
        
        chosenSA.setActivatingPlayer(sa.getActivatingPlayer());
        if (tgt == null || CardFactoryUtil.canTarget(card, chosenSA.getSourceCard()))
        {
            for(int i=0;i<amount;i++)
            {
                AllZone.getCardFactory().copySpellontoStack(card, chosenSA.getSourceCard(), chosenSA, true);
            }
        }
    }//end resolve

}//end class AbilityFactory_Copy


package forge;


public class Spell_Evoke extends Spell_Permanent {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    // evoke
    private boolean           evoke;
    
    public Spell_Evoke(Card sourceCard, String manaCost) {
        super(sourceCard);
        this.setManaCost(manaCost);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Evoke ").append(manaCost);
        sb.append(" - Sacrifice this creature when it comes into play.");
        this.setDescription(sb.toString());
    }
    
    @Override
    public void resolve() {
        super.resolve();
        
        final Card card = getSourceCard();
        
        final SpellAbility ability = new Ability(card, "0") {
            @Override
            public void resolve() {
                AllZone.GameAction.sacrifice(card);
            }
        };
        
        StringBuilder sb = new StringBuilder();
        sb.append("Evoke - sacrifice ").append(card);
        ability.setStackDescription(sb.toString());
        
        AllZone.Stack.add(ability);
    }//resolve()
}

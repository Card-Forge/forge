
package forge.card.spellability;


import java.util.ArrayList;

import forge.ComputerUtil;


public class SpellAbilityList {
    private ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();
    
    public SpellAbilityList() {}
    
    public SpellAbilityList(SpellAbility s) {
        add(s);
    }
    
    public SpellAbilityList(SpellAbility[] s) {
        for(int i = 0; i < s.length; i++)
            add(s[i]);
    }
    
    public void remove(int n) {
        list.remove(n);
    }
    
    public void add(SpellAbility s) {
        list.add(s);
    }
    
    public int size() {
        return list.size();
    }
    
    public SpellAbility get(int n) {
        return list.get(n);
    }
    
    public void addAll(SpellAbilityList s) {
        for(int i = 0; i < s.size(); i++)
            add(s.get(i));
    }
    
    //Move1.getMax() uses this
    public void execute() {
        for(int i = 0; i < size(); i++) {
            if(!ComputerUtil.canPlay(get(i))) throw new RuntimeException(
                    "SpellAbilityList : execute() error, cannot pay for the spell " + get(i).getSourceCard()
                            + " - " + get(i).getStackDescription());
            
            ComputerUtil.playNoStack(get(i));
        }
    }//execute()
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < size(); i++) {
            sb.append(get(i).getSourceCard().toString());
            sb.append(" - ");
            sb.append(get(i).getStackDescription());
            sb.append("\r\n");
        }
        return sb.toString();
    }//toString()
    
    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        return toString().equals(o.toString());
    }
}

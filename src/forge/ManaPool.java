
package forge;


import java.util.ArrayList;
import java.util.Iterator;


public class ManaPool extends Card {
    private ArrayList<Ability_Mana> used = new ArrayList<Ability_Mana>();
    
    //boolean[] spendAsCless ={true,true,true,true,true,true};
    //boolean spendAll = true;
    /*	private int cIndex(String s)
    	{
    		//String c =(s.length()==1 ? s : Input_PayManaCostUtil.getColor2(s)); 
    		if(s.length()!=1) throw new IllegalArgumentException(s + "isn't an indexable single character.");
    		if(!colors.contains(s)) return 0;
    		return colors.indexOf(s) + 1;
    	}
    	private String indexC(int index)
    	{
    		if (index == 0) return "1";
    		return colors.charAt(index - 1) + "";
    	}*/
    private void updateKeywords() {
        extrinsicKeyword.clear();
        for(char c:has.toCharArray())
            /*for(int val=0;val<6;val++)
            	for(int i=0; i < has[val]; i++)*/
            extrinsicKeyword.add("ManaPool:" + c);//indexC(val));*/
    }
    
    private ArrayList<String> extrinsicKeyword = new ArrayList<String>();
    
    @Override
    public ArrayList<String> getExtrinsicKeyword() {
        return new ArrayList<String>(extrinsicKeyword);
    }
    
    @Override
    public void setExtrinsicKeyword(ArrayList<String> a) {
        extrinsicKeyword = new ArrayList<String>(a);
        //Arrays.fill(has, 0);
        has = "";
        for(String Manaword:extrinsicKeyword)
            if(Manaword.startsWith("ManaPool:")) {
                String[] cost = Manaword.split(":");
                if(cost[1].length() == 1) has += cost[1];//[cIndex(cost[1])]++;
            }
        this.updateObservers();
    }
    
    @Override
    public void addExtrinsicKeyword(String s) {
        if(s.startsWith("ManaPool:")) {
            extrinsicKeyword.add(s);
            addMana(s.split(":")[1]);
        }
    }
    
    @Override
    public void removeExtrinsicKeyword(String s) {
        if(s.startsWith("ManaPool:")) {
            updateKeywords();
            extrinsicKeyword.remove(s);
            subtractOne(s.split(":")[1]);
            this.updateObservers();
        }
    }
    
    @Override
    public int getExtrinsicKeywordSize() {
        updateKeywords();
        return extrinsicKeyword.size();
    }
    
    public ManaPool smp;
    
    public ManaPool(String contents) {
        this();
        this.addMana(contents);
    }
    
    public ManaPool() {
        this(false);
    }
    
    public ManaPool(boolean snow) {
        super();
        if(!snow) smp = new ManaPool(true);
        else {
            smp = this;
            addType("Snow");
        }
        updateObservers();
        
        setName("Mana Pool");
        addIntrinsicKeyword("Shroud");
        addIntrinsicKeyword("Indestructible");
        clear();
    }
    
    @Override
    public String getText() {
        //empty = true;
        String res = (isSnow()? "Snow ":"") + "Mana available:\r\n";
        StringBuilder sb = new StringBuilder();
        sb.append(res);
        if(isEmpty()) sb.append("None\r\n");
        //if(has[0]>0) {res+=Integer.toString(has[0]); empty = false;}
        else for(char c:mcolors.toCharArray())//int j=0; j<colors.length();j++){char c=colors.charAt(j);
        {
            int n = containsColor(c);//has[cIndex(c+"")];
            if(n == 0) continue;
            if(c == '1') sb.append(n);
            else {
                for(int i = 0; i < n; i++)
                    sb.append(c);//(c+"");
                //if (n > 0) {
                //res+="("+n/*(Integer.toString(n)*/+")";// empty = false;}
                sb.append("(");
                sb.append(n);
                sb.append(")");
            }
            sb.append("\r\n");
        }
        if(!isSnow()) sb.append(smp.getText());
        return sb.toString();
    }
    
    public final static String colors  = "WUBRG";
    public final static String mcolors = "1WUBRG";
    
    public boolean isEmpty() {
        return has.equals("");
    }
    
    /*	private boolean empty = false;
    	private int[] paid= new int[6];
    	private int[] has = new int[6];*/
    String paid = "";
    String has  = "";
    
    void sortContents() {
        has = sortContents(has);
        paid = sortContents(paid);
    }
    
    String sortContents(String mana) {
        StringBuilder sb = new StringBuilder();
        for(char color:mcolors.toCharArray())
            for(char c:mana.toCharArray())
                if(c == color) sb.append(c);
        return sb.toString();
    }
    
    int containsColor(String color) {
        sortContents();
        if(color.length() > 1) throw new IllegalArgumentException(color + " is not a color");
        if(color.equals("")) return Integer.MAX_VALUE;
        return containsColor(color.charAt(0));
    }
    
    int containsColor(char color) {
        sortContents();
        if(!has.contains(color + "")) return 0;
        int res = has.lastIndexOf(color) - has.indexOf(color) + 1;
        if(!isSnow()) res += smp.containsColor(color);
        return res;
    }
    
    public static String oraclize(String manaCost) {
        //if(!manaCost.contains(" ")) return manaCost;
        String[] parts = manaCost.split(" ");
        StringBuilder res = new StringBuilder();
        for(String s:parts) {
            if(s.length() == 2 && colors.contains(s.charAt(1) + "")) s = s.charAt(0) + "/" + s.charAt(1);
            if(s.length() == 3) s = "(" + s + ")";
            if(s.equals("S")) s = "(S)";//for if/when we implement snow mana
            if(s.equals("X")) s = "(X)";//X costs?
            res.append(s);
        }
        return res.toString();
    }
    
    public ArrayList<String> getColors() {
        ArrayList<String> mana = new ArrayList<String>();
        for(char c:mcolors.toCharArray())//int i = 1; i <= 5; i++)
        {
            if(containsColor(c)/*has[i]*/> 0) mana.add(getColor("" + c));//olors.charAt(i-1)+""));
        }
        //if(has[0]>0) mana.add(Constant.Color.Colorless);
        return mana;
    }
    
    String getColor(String s) {
        return Input_PayManaCostUtil.getLongColorString(s);
    }
    
    public void addMana(Ability_Mana am) {
        (!isSnow() && am.isSnow()? smp:this).addMana(!am.mana().contains("X")? am.mana():am.mana().replaceAll("X",
                am.getX() + ""));
    }
    
    public void addMana(String mana) {
        if(mana.length() <= 1) {
            addOne(mana);
            return;
        }
        String[] cost = mana.split("");
        String Colorless = "";
        int cless = 0;
        for(String s:cost) {
            if(s.trim().equals("")) continue;//mana.split gave me a "" for some reason
            if(colors.contains(s)) {
                addOne(s);//has[colors.indexOf(s) + 1]++;
                if(!Colorless.trim().equals("")) {
                    try {
                        cless += Integer.parseInt(Colorless);
                        Colorless = "";
                    } catch(NumberFormatException ex) {
                        throw new RuntimeException("Mana_Pool : Error, noncolor mana cost is not a number - "
                                + Colorless);
                    }
                }
            } else Colorless += s;
        }
        addOne(cless + "");
        //has[0]+=cless;
    }
    
    public void addOne(String Mana) {
        if(Mana.trim().equals("")) return;
//		int cInt = cIndex(Mana);
        if(Mana.length() == 1 && colors.contains(Mana)) //cInt > 0)
        has += Mana;//[cInt]++;
        else try {
            for(int i = Integer.parseInt(Mana); i > 0; i--)
                //	has[cInt]+= Integer.parseInt(Mana);
                has = "1" + has;
        } catch(NumberFormatException ex) {
            throw new RuntimeException("Mana_Pool.AddOne : Error, noncolor mana cost is not a number - " + Mana);
        }
    }
    
    public static String[] getManaParts(Ability_Mana manaAbility) {
        return getManaParts(manaAbility.mana(), true);
    }//wrapper
    
    public static String[] getManaParts(String Mana_2)//turns "G G" -> {"G","G"}, "2 UG"->"{"2","U/G"}, "B W U R G" -> {"B","W","U","R","G"}, etc.
    {
        return getManaParts(Mana_2, false);
    }
    
    public static String[] getManaParts(String Mana_2, boolean parsed) {
        String Mana = Mana_2;
        //if (Mana.isEmpty()) return null;
        if(Mana.trim().equals("")) return null;
        if(!parsed) Mana = oraclize(Mana);
        try {
            String[] Colorless = {Integer.parseInt(Mana) + ""};
            return Colorless;
        } catch(NumberFormatException ex) {}
        
        ArrayList<String> res = new ArrayList<String>();
        int Colorless = 0;
        String clessString = "";
        boolean parentheses = false;
        String current = "";
        
        for(int i = 0; i < Mana.length(); i++) {
            char c = Mana.charAt(i);
            if(c == '(') {
                parentheses = true;
                continue;
            }//Split cost handling ("(" +<W/U/B/R/G/2> + "/" + <W/U/B/R/G> + ")")
            else if(parentheses) {
                if(c != ')') {
                    current += c;
                    continue;
                } else {
                    parentheses = false;
                    res.add(current);
                    current = "";
                    continue;
                }
            }
            String s = c + "";
            if(colors.contains(s)) {
                res.add(s);
                if(clessString.trim().equals("")) continue;
                try {
                    Colorless += Integer.parseInt(clessString);
                } catch(NumberFormatException ex) {
                    throw new RuntimeException(
                            "Mana_Pool.getManaParts : Error, sum of noncolor mana parts is not a number - "
                                    + clessString);
                }
                clessString = "";
            } else clessString += s;
        }
        if(Colorless > 0) res.add(0, Colorless + "");
        return res.toArray(new String[0]);
    }
    
    public ManaCost subtractMana(ManaCost m, Ability_Mana... mabilities) {
        if(mabilities.length == 0) {
            //spendAll = true;//TODO:check something? GUI?
            if(m.isPaid() || (isEmpty() && (isSnow() || smp.isEmpty()))) return m;
            else if(isEmpty()) return smp.subtractMana(m, mabilities);
            String mana = oraclize(m.toString());
            if(mana.length() == 1) {
                m = subtractOne(m, mana);
                return m;
            }
            String[] cost = getManaParts(m.toString());
            for(String s:cost)
                m = subtractOne(m, s);
            return m;
        } else //redundant
        for(Ability_Mana mability:mabilities) {
            if(mability.isSnow() && !isSnow()) {
                m = smp.subtractMana(m, mability);
                continue;
            }
            used.add(mability);
            if(null != getManaParts(mability)) {
            	for(String c:getManaParts(mability)) {
            		if(c.equals("")) continue; // some sort of glitch
            		m = subtractOne(m, c);
            	}
            }
        }
        return m;
    }
    
    public void subtractOne(String Mana) {
        subtractOne(new ManaCost(Mana), Mana);
    }
    
    public ManaCost subtractOne(ManaCost manaCost, String Mana) {
        if(Mana.trim().equals("") || manaCost.isPaid()) return manaCost;
        if(colors.contains(Mana))//Index(Mana) > 0 )
        {
            if(containsColor(Mana) == 0) return manaCost;
            if(isSnow() && manaCost.isNeeded("S")) manaCost.subtractMana("S");
            else {
                if(!manaCost.isNeeded(Mana)) return manaCost;
                manaCost.subtractMana(getColor(Mana));
            }
            has = has.replaceFirst(Mana, "");
            paid += Mana;
        } else {
            
            if(Mana.equals("S")) manaCost = smp.subtractOne(manaCost, "1");
            else if(Mana.equals("1") || !Character.isDigit(Mana.charAt(0))) {
                if(containsColor('1') > 0 && manaCost.isNeeded(Constant.Color.Colorless)) {
                    has = has.replaceFirst("1", "");
                    paid += Mana;//[0]++;
                    manaCost.subtractMana(Constant.Color.Colorless);
                    return manaCost;
                } else {
                    //if (has[0]>0){manaCost=subtractOne(manaCost,"1"); cless--; continue;}
                    String chosen;
                    ArrayList<String> choices = getColors();
                    if(!Mana.equals("1")) {
                        choices.clear();
                        if(containsColor(Mana.charAt(2)) > 0) choices.add(getColor(Mana.charAt(2) + ""));
                        if(Mana.charAt(1) == '2'? choices.isEmpty():containsColor(Mana.charAt(0)) > 0) choices.add(getColor(Mana.charAt(0)
                                + ""));
                    }
                    if(isSnow() && manaCost.isNeeded("S")) choices.add(0, Constant.Color.Snow);
                    Iterator<String> it = choices.iterator();
                    while(it.hasNext())
                        if(!manaCost.isNeeded(getColor2(it.next()))) it.remove();
                    if(choices.size() == 0) return manaCost;
                    chosen = choices.get(0);
                    if(choices.size() > 1) chosen = (String) AllZone.Display.getChoiceOptional("Choose "
                            + (isSnow()? "snow ":"") + "mana to pay " + Mana, choices.toArray());
                    if(chosen == null) {
                        //spendAll = false;
                        return manaCost;
                    }
                    if(chosen.equals(Constant.Color.Snow)) manaCost.subtractMana(chosen);
                    else manaCost = subtractOne(manaCost, getColor2(chosen));
                }
            } else {
                int cless;
                try {
                    cless = Integer.parseInt(Mana);
                } catch(NumberFormatException ex) {
                    throw new RuntimeException(
                            "Mana_Pool.SubtractOne : Error, noncolor mana cost is not a number - " + Mana);
                }
                //if (cless == 0) return manaCost;
                if(cless > totalMana()) {
                    manaCost = subtractOne(manaCost, totalMana() + "");
                    return manaCost;
                } else while(totalMana() > 0 && cless > 0) {
                    cless--;
                    manaCost = subtractOne(manaCost, "1");
                }
            }
        }
        return manaCost;
    }
    
    public String getColor2(String s) {
        return Input_PayManaCostUtil.getShortColorString(s);
    }//wrapper
    
    public int hasMana(String color) {
        String s = (color.length() == 1? color:Input_PayManaCostUtil.getShortColorString(color));
        Mana_Part.checkSingleMana(s);
        return (containsColor(color));
    }
    
    public int totalMana() {
        /*int res = 0;
        for (int n : has)
        	res += n;*/
        sortContents();
        int res = has.length();
        if(!isSnow()) res += smp.totalMana();
        return res;
    }
    
    public void clear() {
        if(!isSnow()) smp.clear();
        used.clear();
        paid = "";//Arrays.fill(paid, 0);
        has = "";//Arrays.fill(has, 0);
    }
    
    public void paid() {
        if(!isSnow()) smp.paid();
        used.clear();
        paid = "";//Arrays.fill(paid, 0);
        sortContents();
    }
    
    public void unpaid() {
        if(!isSnow()) smp.unpaid();
        String hasbak = has;
        has = paid;
        if(!used.isEmpty()) {
            for(Ability_Mana am:used) {
                if(am.undoable()) {
                    for(String c:getManaParts(am))
                        //paid[cIndex(am.Mana())]--;
                        subtractOne(c);
                    am.undo();
                }
            }
            
        }
        //for(int i = 0; i < 6; i++)
        //has[i]+=paid[i];
        has += hasbak;
        paid();
    }
    
}

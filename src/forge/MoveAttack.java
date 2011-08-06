package forge;
import java.util.*;

public class MoveAttack
{
    private MoveAttack bestMove = null;
    private int bestScore = Integer.MIN_VALUE;    
    
    private static Random random = new Random();
    
    private static Card[] aCreature;
    private static Card[] bCreature;
    
    @SuppressWarnings("unused") // aLife
	private static int aLife;
    private static int bLife;

    private SimpleCombat combat;
    
    private static ArrayList<MoveAttack> generate1List = new ArrayList<MoveAttack>();
    
    //trying to find the move for A player
    //Card[] a and Card[] b, are all untapped creatures that can attack or block
    public MoveAttack(Card[] a, Card[] b, int alife, int blife)
    {
	aCreature = a;
	bCreature = b;
	aLife = alife;
	bLife = blife;
    }    
    private MoveAttack(SimpleCombat c) {setCombat(c);}

    //for internal use and testing purposes
    public void setCombat(SimpleCombat c) {combat = c;}

    public SimpleCombat getCombat() {return combat;}
    public CardList getAttackers() {return combat.getAttackers();}
    public MoveAttack getBestMove() {return bestMove;}
    //should be called with a depth of 3, ShouldAttack.findMove(3)
    //depth 3 is randomly choosing which creatures to attack
    //depth 2 is randomly choosing which creature will block
    //depth 1 is the outcome after combat damage, to see which creatures are left    
    public MoveAttack[] generateMoves(int depth)
    {
	if(depth == 1-1)
	    return generate1();
	else if(depth == 2-1)
	    return generate2();
	else if(depth == 3-1)
	    return generate3();
	else
	    throw new RuntimeException("MoveAttack : generateMoves() invalid depth: " +depth);
    }
    //calls Combat.combatDamage() on all moves generates my generate2()
    public MoveAttack[] generate1()
    {	
	MoveAttack[] m = new MoveAttack[generate1List.size()];
	
	for(int i = 0; i < generate1List.size(); i++)
	    m[i] = (MoveAttack) generate1List.get(i);
	
	for(int i = 0; i < m.length; i++)
	    m[i].combat.combatDamage();
	
	return m;
    }
    //generate blockers
    public MoveAttack[] generate2()
    {
	if(combat.getAttackers().isEmpty())
	{
	    MoveAttack[] a = {new MoveAttack(combat)};
	   generate1List.add(a[0]);
	   return a;
	}
	
	MoveAttack move[] = new MoveAttack[200];
	Card attacker;
	Card blocker;	
	
	for(int outer = 0; outer < move.length; outer++)
	{
	    SimpleCombat battle = new SimpleCombat(combat.getAttackers());
	    //not sure what "stop" should be, might need to change this
	    int stop = random.nextInt(combat.getAttackers().size() + 1);
	    CardList pool = new CardList(bCreature);

	    for(int i = 0; i < stop && !pool.isEmpty(); i++)
	    {
		attacker = CardUtil.getRandom(combat.getAttackers().toArray());
		blocker = CardUtil.getRandom(pool.toArray());
		
		if(CombatUtil.canBlock(attacker, blocker, AllZone.Combat))
		{
		    pool.remove(blocker);
		    battle.addBlocker(attacker, blocker);
		}
	    }
	    move[outer] = new MoveAttack(battle);

	    generate1List.add(move[outer]);
	}
	return move;
    }
    //generate attackers
    public MoveAttack[] generate3()
    {
	MoveAttack move[] = new MoveAttack[200];
	
	for(int outer = 0; outer < move.length; outer++)
	{
	    SimpleCombat battle = new SimpleCombat();
	    CardList pool = new CardList(aCreature);
	    int stop = random.nextInt(aCreature.length + 1);
	    
	    for(int i = 0; i < stop; i++)
	    {
		battle.addAttacker(randomRemove(pool));
	    }
	    move[outer] = new MoveAttack(battle);
	}
	return move;
    }
    public Card randomRemove(CardList c)
    {
	if(c.isEmpty())
	    throw new RuntimeException("MoveAttack : randomRemove() argument is size 0");
	int n = random.nextInt(c.size());
	return c.remove(n);
    }
    public int getScore()
    {	
	if(bLife <= totalAttack(combat.getUnblockedAttackers()))
	    return 10000;

	CardList[] destroy = combat.combatDamage();
	CardList aDestroy = destroy[0];
	CardList bDestroy = destroy[1];
//System.out.println("destroyed " +aDestroy);
	int score = 0;
	score += totalAttack(combat.getUnblockedAttackers());
	score -= aDestroy.size() * 10;//do not trade creatures, ie. both creatures die
	
	score += bDestroy.size();
//if(score == 2)
//    System.out.println(combat.toString());
	return score;
    }
    private int totalAttack(CardList c)
    {
	int total = 0;

	for(int i = 0; i < c.size(); i++)
	    total += c.get(i).getNetAttack();
	
	return total;
    }
    
    
    private int min(MoveAttack move, int depth)
    {
        if(depth == 0)
            return move.getScore();
        
        MoveAttack v[] = move.generateMoves(depth);
        int score = Integer.MAX_VALUE;
        for(int i  = 0; i < v.length; i++)
            score = Math.min(score, max(v[i], depth-1, false));
        return score;
    }
    public int max(MoveAttack move, int depth, boolean first)
    {
        if(depth == 0)
            return move.getScore();
        
        MoveAttack v[] = move.generateMoves(depth);
        int score = Integer.MIN_VALUE;
        for(int i  = 0; i < v.length; i++)
        {
            score = Math.max(score, min(v[i], depth-1));

            if(first && bestScore < score)
            {
                bestScore = score;
                bestMove = v[i];
             }
        }//for
        return score;
    }//max()        
    //does not implement multiple blockers
}
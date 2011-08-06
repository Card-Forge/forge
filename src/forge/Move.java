package forge;
public abstract class Move
{        
    abstract public Move[] generateMoves();
    abstract public int getScore();
  
    public Move bestMove = null;
    public int bestScore = Integer.MIN_VALUE;
                
    public int min(Move move, int depth)
    {
        if(depth == 0)
            return move.getScore();

        Move v[] = move.generateMoves();
        int score = Integer.MAX_VALUE;
        for(int i  = 0; i < v.length; i++)
            score = Math.min(score, max(v[i], depth-1, false));
        return score;
    }
    public int max(Move move, int depth, boolean first)
    {
        if(depth == 0)
            return move.getScore();

        Move v[] = move.generateMoves();
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
}
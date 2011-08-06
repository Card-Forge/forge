    package forge;

    public class HandSizeOp {
       public String Mode;
       public int hsTimeStamp;
       public int Amount;
       
       public HandSizeOp(String M,int A,int TS)
       {
          Mode = M;
          Amount = A;
          hsTimeStamp = TS;
       }
       
       public String toString()
       {
          return "Mode(" + Mode + ") Amount(" + Amount + ") Timestamp(" + hsTimeStamp + ")";
       }
    }

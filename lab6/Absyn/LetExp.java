package Absyn;
import Symbol.Symbol;
@SuppressWarnings("unused")
public class LetExp extends Exp {
   public DecList decs;
   public Exp body;
   public LetExp(int p, DecList d, Exp b) {pos=p; decs=d; body=b;}
}

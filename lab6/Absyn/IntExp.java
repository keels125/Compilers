package Absyn;
import Symbol.Symbol;
@SuppressWarnings("unused")
public class IntExp extends Exp {
   public int value;
   public IntExp(int p, int v) {pos=p; value=v;}
}

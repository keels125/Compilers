package Absyn;
import Symbol.Symbol;
@SuppressWarnings("unused")
public class VarExp extends Exp {
   public Var var;
   public VarExp(int p, Var v) {pos=p; var=v;}
}   

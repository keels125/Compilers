package Tree;
import Temp.Temp;
import Temp.Label;
@SuppressWarnings("unused")
public class CONST extends Exp {
  public int value;
  public CONST(int v) {value=v;}
  public ExpList kids() {return null;}
  public Exp build(ExpList kids) {return this;}
}

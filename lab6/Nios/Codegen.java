package Nios;
import Temp.Temp;
import Temp.TempList;
import Temp.Label;
import Temp.LabelList;
import java.util.Hashtable;

import Absyn.Exp;

public class Codegen {
  NiosFrame frame;
  public Codegen(NiosFrame f) {frame = f;}

  private Assem.InstrList ilist = null, last = null;

  private void emit(Assem.Instr inst) {
    if (last != null)
      last = last.tail = new Assem.InstrList(inst, null);
    else {
      if (ilist != null)
	throw new Error("Codegen.emit");
      last = ilist = new Assem.InstrList(inst, null);
    }
  }

  Assem.InstrList codegen(Tree.Stm s) {
    munchStm(s);
    Assem.InstrList l = ilist;
    ilist = last = null;
    return l;
  }

  static Assem.Instr OPER(String a, TempList d, TempList s, LabelList j) {
    return new Assem.OPER("\t" + a, d, s, j);
  }
  static Assem.Instr OPER(String a, TempList d, TempList s) {
    return new Assem.OPER("\t" + a, d, s);
  }
  static Assem.Instr MOVE(String a, Temp d, Temp s) {
    return new Assem.MOVE("\t" + a, d, s);
  }

  static TempList L(Temp h) {
    return new TempList(h, null);
  }
  static TempList L(Temp h, TempList t) {
    return new TempList(h, t);
  }

  void munchStm(Tree.Stm s) {
    if (s instanceof Tree.MOVE) 
      munchStm((Tree.MOVE)s);
    else if (s instanceof Tree.UEXP)
      munchStm((Tree.UEXP)s);
    else if (s instanceof Tree.JUMP)
      munchStm((Tree.JUMP)s);
    else if (s instanceof Tree.CJUMP)
      munchStm((Tree.CJUMP)s);
    else if (s instanceof Tree.LABEL)
      munchStm((Tree.LABEL)s);
    else
      throw new Error("Codegen.munchStm");
  }
void munchStm(Tree.MOVE s) {
	if (s.dst instanceof Tree.MEM){
		Tree.MEM memExp = (Tree.MEM)s.dst; 
		Temp rightExp = munchExp(s.src); 
		if (memExp.exp instanceof Tree.BINOP){ //covers all 4 cases of BINOP--4 possible emits. 
			Tree.BINOP binExp =(Tree.BINOP)memExp.exp; 
			if (binExp.binop==Tree.BINOP.PLUS){
				if (binExp.right instanceof Tree.CONST){
					Tree.CONST conExp = (Tree.CONST)binExp.right; 
					if (binExp.left instanceof Tree.TEMP){
						Tree.TEMP temExp = (Tree.TEMP)binExp.left; 
						
						if (temExp.temp == frame.FP){
							
							emit (MOVE("stw `d0, "+conExp.value+ "+"+ frame.name+"_framesize(`s0)", rightExp, frame.SP)); //CASE: MOVE(MEM(+(FP, CONST)),exp)
						}
						else{
							emit (MOVE("stw `d0, "+conExp.value+"(`s0)", rightExp, temExp.temp)); //CASE: same as above but Temp instead of FP
						}
					}
					else{
						Temp binLeftExp = munchExp(binExp.left); 
						emit (MOVE("stw `d0, "+conExp.value+"(`s0)", rightExp, binLeftExp)); //CASE: MOVE(MEM(+(exp, CONST)), exp)	
					}
				}
				else if (binExp.left instanceof Tree.CONST){
					Tree.CONST conExp = (Tree.CONST)binExp.left; 
					if (binExp.right instanceof Tree.TEMP){
						Tree.TEMP temExp = (Tree.TEMP)binExp.right; 
						
						if (temExp.temp == frame.FP){
							
							emit (MOVE("stw `d0, "+conExp.value+ "+"+ frame.name+"_framesize(`s0)", rightExp, frame.SP)); //CASE: MOVE(MEM(+(CONST, FP)),exp)
						}
						else{
							emit (MOVE("stw `d0, "+conExp.value+"(`s0)", rightExp, temExp.temp)); //CASE: same as above but Temp instead of FP
						}
					}
					else{
						Temp binRightExp = munchExp(binExp.right); 
						emit (MOVE("stw `d0, "+conExp.value+"(`s0)", rightExp, binRightExp)); //CASE: MOVE(MEM(+(CONST, exp)), exp)	
					}
				}
				else{
					Temp elseExp = munchExp(memExp.exp); 
					emit (MOVE("stw `d0, (`s0)", rightExp, elseExp)); 
					
				}

				
			}
		}
		else if (memExp.exp instanceof Tree.CONST){
			Tree.CONST conExp = (Tree.CONST)memExp.exp; 
			emit (MOVE("stw `d0, "+conExp.value+"(`s0)", rightExp, frame.ZERO)); //CASE: MOVE(MEM(CONST), exp)
		}
		else if (memExp.exp instanceof Tree.TEMP){
			Tree.TEMP tmpExp = (Tree.TEMP)memExp.exp; 
			if (tmpExp.temp==frame.FP){
				emit (MOVE("stw `d0, "+frame.name+"_framesize(`s0)", rightExp, frame.SP)); //CASE: MOVE(MEM(FP), exp)
			}
			else{
				emit (MOVE("stw `d0, (`s0)", rightExp, tmpExp.temp)); //CASE: same as above but with TEMP instead
			}
		}
		else{
			Temp memExpTmp = munchExp(memExp.exp); 
			emit (MOVE("stw `d0, (`s0)", rightExp, memExpTmp)); //CASE: MOVE(MEM(exp), exp)
		}
	}
	else if (s.dst instanceof Tree.TEMP){
		Tree.TEMP tmpExp = (Tree.TEMP)s.dst; 
		if (s.src instanceof Tree.MEM){
			Tree.MEM memExp = (Tree.MEM)s.src; 
			if (memExp.exp instanceof Tree.BINOP){ //covers all 4 cases of BINOP--4 possible emits. 
				Tree.BINOP binExp =(Tree.BINOP)memExp.exp; 
				if (binExp.binop==Tree.BINOP.PLUS){
					if (binExp.right instanceof Tree.CONST){
						Tree.CONST conExp = (Tree.CONST)binExp.right; 
						if (binExp.left instanceof Tree.TEMP){
							Tree.TEMP temExp = (Tree.TEMP)binExp.left; 
							
							if (temExp.temp == frame.FP){
								
								emit (MOVE("ldw `s0, "+conExp.value+ "+"+ frame.name+"_framesize(`d0)", frame.SP, tmpExp.temp)); //MOVE(TEMP, MEM(+(FP, CONST)))
								
							}
							else{
								emit (MOVE("ldw `s0, "+conExp.value+"(`d0)", temExp.temp, tmpExp.temp )); //CASE: same as above but Temp instead of FP
								
							}
						}
						else{
							Temp binLeftExp = munchExp(binExp.left); 
							emit (MOVE("ldw `s0, "+conExp.value+"(`d0)",  binLeftExp, tmpExp.temp)); //CASE: MOVE(TEMP, MEM(+(exp, CONST)))	
						}
					}
					else if (binExp.left instanceof Tree.CONST){
						Tree.CONST conExp = (Tree.CONST)binExp.left; 
						if (binExp.right instanceof Tree.TEMP){
							Tree.TEMP temExp = (Tree.TEMP)binExp.right; 
							
							if (temExp.temp == frame.FP){
								
								emit (MOVE("ldw `s0, "+conExp.value+ "+"+ frame.name+"_framesize(`d0)", frame.SP,tmpExp.temp)); //CASE: MOVE(MEM(+(CONST, FP)),exp)
							}
							else{
								emit (MOVE("ldw `s0, "+conExp.value+"(`d0)", temExp.temp, tmpExp.temp)); //CASE: same as above but Temp instead of FP
							}
						}
						else{
							Temp binRightExp = munchExp(binExp.right); 
							emit (MOVE("ldw `s0, "+conExp.value+"(`d0)", binRightExp, tmpExp.temp )); //CASE: MOVE(TEMP, MEM(+(CONST, exp)))	
						}
					}
					else{
						Temp elseExp = munchExp(memExp.exp); 
						emit (MOVE("ldw `s0, (`d0)", elseExp, tmpExp.temp)); //`s0 and `d0 are listed backwards but they match the order of the templists. 
					}
				}
			}
			else if (memExp.exp instanceof Tree.CONST){
				Tree.CONST conExp = (Tree.CONST)memExp.exp; 
				emit (MOVE("ldw `s0, "+conExp.value+"(`d0)", frame.ZERO, tmpExp.temp)); //CASE: MOVE(TEMP, MEM(CONST))
			}
			else if (memExp.exp instanceof Tree.TEMP){
				Tree.TEMP tmpExp2 = (Tree.TEMP)memExp.exp; 
				if (tmpExp2.temp==frame.FP){
					emit (MOVE("ldw `s0, "+frame.name+"_framesize(`d0)",  frame.SP, tmpExp.temp )); //CASE: MOVE(TEMP,MEM(FP))
				}
				else{
					emit (MOVE("ldw  `s0, (`d0)", tmpExp2.temp,tmpExp.temp )); //CASE: same as above but with TEMP instead
				}
			}
			else{
				Temp memExpTmp = munchExp(memExp.exp); 
				emit (MOVE("ldw `s0, (`d0)", memExpTmp, tmpExp.temp )); //CASE: MOVE(TEMP, MEM(exp))
			}
		}

		else{
			Temp srcExp = munchExp(s.src); 
			emit(MOVE("mov `s0, `d0", srcExp, tmpExp.temp )); //CASE MOVE(TEMP, exp)
			
			
		}
	}
	else{
		Temp srcExp = munchExp(s.src); 
		Temp dstExp = munchExp(s.dst); 
		emit(MOVE("mov `d0, `s0", dstExp, srcExp)); //CASE MOVE(e1,e2)
		
	}

}


  void munchStm(Tree.UEXP s) {
    munchExp(s.exp);
  }

  void munchStm(Tree.JUMP s) {
	  
	  if (s.exp instanceof Tree.NAME){
		  Tree.NAME jmpStm = (Tree.NAME)s.exp; 
		  emit(OPER("br "+jmpStm.label.toString(), null, null)); 
	  }
	  else{
		  Temp reg = munchExp(s.exp); 
		  emit(OPER("jmp `d0", L(reg), null)); 
	  }
	  
  }

  private static String[] CJUMP = new String[10];
  static {
    CJUMP[Tree.CJUMP.EQ ] = "beq";
    CJUMP[Tree.CJUMP.NE ] = "bne";
    CJUMP[Tree.CJUMP.LT ] = "blt";
    CJUMP[Tree.CJUMP.GT ] = "bgt";
    CJUMP[Tree.CJUMP.LE ] = "ble";
    CJUMP[Tree.CJUMP.GE ] = "bge";
    CJUMP[Tree.CJUMP.ULT] = "bltu";
    CJUMP[Tree.CJUMP.ULE] = "bleu";
    CJUMP[Tree.CJUMP.UGT] = "bgtu";
    CJUMP[Tree.CJUMP.UGE] = "bgeu";
  }

  void munchStm(Tree.CJUMP s) {
	  LabelList falseLabel = new LabelList(s.iffalse, null); 
	  LabelList trueLabel = new LabelList(s.iftrue, falseLabel);
	  
	
	  Temp dest = munchExp(s.left); 
	  Temp source = munchExp(s.right); 
	  emit(OPER(CJUMP[s.relop]+" `d0, `s0, "+s.iftrue.toString(), L(dest), L(source), trueLabel));   
	
  }

  void munchStm(Tree.LABEL l) {
	  
	  emit(new Assem.LABEL(l.label.toString() + ":", l.label));
  }

  Temp munchExp(Tree.Exp s) {
    if (s instanceof Tree.CONST)
      return munchExp((Tree.CONST)s);
    else if (s instanceof Tree.NAME)
      return munchExp((Tree.NAME)s);
    else if (s instanceof Tree.TEMP)
      return munchExp((Tree.TEMP)s);
    else if (s instanceof Tree.BINOP)
      return munchExp((Tree.BINOP)s);
    else if (s instanceof Tree.MEM)
      return munchExp((Tree.MEM)s);
    else if (s instanceof Tree.CALL)
      return munchExp((Tree.CALL)s);
    else
      throw new Error("Codegen.munchExp");
  }

  Temp munchExp(Tree.CONST e) {
	  
	  if (e.value==0)
		  return frame.ZERO;
	  else {
		  Temp t= new Temp();
		  emit(OPER("movi `d0, "+e.value, L(t), null));
		  return t; 
	  }
	  
  }

  Temp munchExp(Tree.NAME e) {
		Temp dest  = new Temp(); 
		
		emit (OPER("movia `d0, "+e.label.toString(), L(dest), null)); 
		return dest; 
	    
	  }

  Temp munchExp(Tree.TEMP e) {
    if (e.temp == frame.FP) {
      Temp t = new Temp();
      emit(OPER("addi `d0, `s0, " + frame.name + "_framesize",
		L(t), L(frame.SP)));
      return t;
    }
    return e.temp;
  }

  private static String[] BINOP = new String[10];
  static {
    BINOP[Tree.BINOP.PLUS   ] = "add";
    BINOP[Tree.BINOP.MINUS  ] = "sub";
    BINOP[Tree.BINOP.MUL    ] = "mul";
    BINOP[Tree.BINOP.DIV    ] = "div";
    BINOP[Tree.BINOP.AND    ] = "and";
    BINOP[Tree.BINOP.OR     ] = "or";
    BINOP[Tree.BINOP.LSHIFT ] = "sll";
    BINOP[Tree.BINOP.RSHIFT ] = "srl";
    BINOP[Tree.BINOP.ARSHIFT] = "sra";
    BINOP[Tree.BINOP.XOR    ] = "xor";
  }


  private static String[] IBINOP = new String[10];
  static {
    IBINOP[Tree.BINOP.PLUS   ] = "addi";
    IBINOP[Tree.BINOP.MINUS  ] = "subi";
    IBINOP[Tree.BINOP.MUL    ] = "muli";
    IBINOP[Tree.BINOP.DIV    ] = "divi";
    IBINOP[Tree.BINOP.AND    ] = "andi";
    IBINOP[Tree.BINOP.OR     ] = "ori";
    IBINOP[Tree.BINOP.LSHIFT ] = "slli";
    IBINOP[Tree.BINOP.RSHIFT ] = "srli";
    IBINOP[Tree.BINOP.ARSHIFT] = "srai";
    IBINOP[Tree.BINOP.XOR    ] = "xori";
  }

  private static int shift(int i) {
    int shift = 0;
    if ((i >= 2) && ((i & (i - 1)) == 0)) {
      while (i > 1) {
	shift += 1;
	i >>= 1;
      }
    }
    return shift;
  }


  Temp munchExp(Tree.BINOP e){
		Temp r = new Temp();
		/*PLUS*/
		if (fix(e)!=null && e.binop == Tree.BINOP.PLUS){     
			Tree.BINOP m =fix(e);
			if ((m.left instanceof Tree.TEMP) && ((Tree.TEMP)m.left).temp == frame.FP){
				emit(OPER("addi"+" `d0, `s0, " +((Tree.CONST)m.right).value+ "+"+ frame.name + "_framesize" ,L(r), L(frame.SP)));
				return r;
			}
			else if (m.left instanceof Tree.TEMP){
				
				Tree.TEMP temp = (Tree.TEMP)m.left;
				emit(OPER("addi"+" `d0, `s0, "+((Tree.CONST)m.right).value, L(r), L(temp.temp)));
				return r;
			}
			else {
				Temp t = munchExp(m.left);
				emit(OPER("addi"+" `d0, `s0, "+((Tree.CONST)m.right).value, L(r), L(t)));
				return r;
			}
		}
		
		/*MULTIPLY*/
		else if (fix(e)!=null && e.binop == Tree.BINOP.MUL){ 
			Tree.BINOP m=fix(e);
			Tree.CONST temp = (Tree.CONST)m.right;
			if (shift(temp.value)!=0){
				Temp t1 = munchExp(e.left);
				
				emit(OPER("slli "+"`d0, `s0, " + shift(temp.value), L(r), L(t1)));
				return r;
			}
		}
		
		/*DIVIDE*/
		else if (e.right instanceof Tree.CONST && e.binop == Tree.BINOP.DIV){ // //////////////////////////////////
			Tree.CONST temp = (Tree.CONST)e.right;
			if (shift(temp.value)!=0){
				Temp t1 = munchExp(e.left);
				
				emit(OPER("srai"+" `d0, `s0, " + shift(temp.value), L(r), L(t1)));
				return r;
			}
		}
		
		/*BASE CASES*/
		if (e.right instanceof Tree.CONST){
			Tree.CONST temp = (Tree.CONST) e.right;
			Temp t1 = munchExp(e.left);
			emit(OPER(IBINOP[e.binop]+" `d0, `s0, " + temp.value, L(r), L(t1)));
			return r;
		}
		else { //most generic case
			Temp t1 = munchExp(e.left);
			Temp t2 = munchExp(e.right);
			emit(OPER(BINOP[e.binop]+" `d0, `s0, `s1", L(r), L(t1, L(t2))));
			return r;
		}
		
	}

	Tree.BINOP fix(Tree.BINOP e){ //helper function to always put const on the right
		if (e.left instanceof Tree.CONST){
			Tree.CONST t = (Tree.CONST)e.left;
			e.left=e.right;
			e.right=t;
			return e;
		}
		else if (e.right instanceof Tree.CONST){
			return e;
		}
		else
			return null;
	}
  Temp munchExp(Tree.MEM e) {
	  
	  Temp retValue = new Temp(); 
	  if (e.exp instanceof Tree.BINOP){
		  Tree.BINOP binExp = (Tree.BINOP)e.exp; 
		  if (binExp.binop==Tree.BINOP.PLUS){
			  if (binExp.right instanceof Tree.TEMP && binExp.left instanceof Tree.CONST){
				  Tree.TEMP tempExp = (Tree.TEMP)binExp.right; 
				  Tree.CONST conExp  =(Tree.CONST)binExp.left; 
				  if (tempExp.temp == frame.FP){
					  emit(MOVE("ldw `d0, "+ conExp.value + "+"+frame.name+ "_framesize" + "(`s0)",retValue,frame.SP)); 
				  }
				  else{
					  emit(OPER("ldw `d0, "+conExp.value+"(`s0)", L(retValue), L(tempExp.temp))); 
				  }
			  }
			  else if (binExp.right instanceof Tree.CONST  && binExp.left instanceof Tree.TEMP){
				  Tree.TEMP tempExp = (Tree.TEMP)binExp.left; 
				  Tree.CONST conExp  =(Tree.CONST)binExp.right; 
				  if (tempExp.temp == frame.FP){
					  emit(MOVE("ldw `d0, "+ conExp.value + "+"+frame.name+ "_framesize" + "(`s0)", retValue, frame.SP)); 
					   
				  }
				  else{
					  emit(MOVE("ldw `d0, "+conExp.value+"(`s0)", retValue, tempExp.temp)); 
				  }
			  }
			  else if (binExp.right instanceof Tree.CONST){
				  Tree.CONST offset = (Tree.CONST)binExp.right; 
				  Temp reg = munchExp(binExp.left); 
				 
				  emit(MOVE("ldw `d0, "+ offset.value + "(`s0)", retValue, reg)); 
			  }
			  else if (binExp.left instanceof Tree.CONST){
				  Temp reg  =munchExp(binExp.right); 
				 
				  
				  Tree.CONST offset = (Tree.CONST)binExp.left; 
				  emit(MOVE("ldw `d0, "+ offset.value + "(`s0)", retValue, reg)); 
			  }
			  else {
				  Temp elseExp = munchExp(e.exp); 
				  emit(MOVE("ldw `d0, (`s0)", retValue, elseExp)); 
			  }
		  }
	  }
	  else if (e.exp instanceof Tree.CONST){
		  Tree.CONST conExp = (Tree.CONST)e.exp;  
		  emit(MOVE("ldw `d0, "+conExp.value+"(`s0)", retValue,frame.ZERO)); 
	  }
	  else if (e.exp instanceof Tree.TEMP){
		  Tree.TEMP temExp = (Tree.TEMP)e.exp; 
		  if (temExp.temp == frame.FP){
			  emit(MOVE("ldw `d0, "+ frame.name + "_framesize" + "(`s0)", retValue, frame.SP)); 
		  }
		  else{
			  emit(MOVE("ldw `d0, 0(`s0)", retValue, temExp.temp)); 
		  }
	  }
	  else{
		  Temp reg = munchExp(e.exp); 
		   
		  emit(MOVE("ldw `d0, (`s0)", retValue, reg));  
	  }
    return retValue; 
  }

  Temp munchExp(Tree.CALL s) {
	
	if (s.func instanceof Tree.NAME){
		TempList args = munchArgs(0,s.args); 
		Tree.NAME fncName = (Tree.NAME)s.func; 
		emit(OPER("call "+ fncName.label.toString(), frame.calldefs,args)); 
	    return frame.V0;
	}
	return frame.ZERO; 
  }

  private TempList munchArgs(int i, Tree.ExpList args) {
    if (args == null)
      return null;
    Temp src = munchExp(args.head);
    if (i > frame.maxArgs)
      frame.maxArgs = i;
    switch (i) {
    case 0:
      emit(MOVE("mov `d0, `s0", frame.A0, src));
      break;
    case 1:
      emit(MOVE("mov `d0, `s0", frame.A1, src));
      break;
    case 2:
      emit(MOVE("mov `d0, `s0", frame.A2, src));
      break;
    case 3:
      emit(MOVE("mov `d0, `s0", frame.A3, src));
      break;
    default:
      emit(OPER("sdw `s0, " + (i-1)*frame.wordSize() + "(`s1)",
		null, L(src, L(frame.SP))));
      break;
    }
    return L(src, munchArgs(i+1, args.tail));
  }
}

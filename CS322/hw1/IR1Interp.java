// 
// A starting version of IR1 interpreter. (For CS322 W15 Assignment 1)
//
//
import java.util.*;
import java.io.*;

import ir1.*;
import ir1.IR1.AOP;
import ir1.IR1.BoolLit;
import ir1.IR1.Dest;
import ir1.IR1.Inst;
import ir1.IR1.IntLit;
import ir1.IR1.Jump;
import ir1.IR1.Label;
import ir1.IR1.LabelDec;
import ir1.IR1.ROP;
import ir1.IR1.Return;
import ir1.IR1.Src;
import ir1.IR1.StrLit;
import ir1.IR1.Temp;
import ir1.IR1.UOP;

public class IR1Interp
{
	
	static class IntException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public IntException(String msg)
		{
			super(msg);
		}
	}
	
	// -----------------------------------------------------------------
	// Value representation
	// -----------------------------------------------------------------
	//
	abstract static class Val
	{
	}
	
	// Integer values
	//
	static class IntVal extends Val
	{
		int i;
		
		IntVal(int i)
		{
			this.i = i;
		}
		
		public String toString()
		{
			return "" + i;
		}
	}
	
	// Boolean values
	//
	static class BoolVal extends Val
	{
		boolean b;
		
		BoolVal(boolean b)
		{
			this.b = b;
		}
		
		public String toString()
		{
			return "" + b;
		}
	}
	
	// String values
	//
	static class StrVal extends Val
	{
		String s;
		
		StrVal(String s)
		{
			this.s = s;
		}
		
		public String toString()
		{
			return s;
		}
	}
	
	// A special "undefined" value
	//
	static class UndVal extends Val
	{
		public String toString()
		{
			return "UndVal";
		}
	}
	
	// -----------------------------------------------------------------
	// Environment representation
	// -----------------------------------------------------------------
	//
	// Think of how to organize environments.
	//
	// The following environments are shown in the lecture for use in
	// an IR0 interpreter:
	//
	// HashMap<String,Integer> labelMap; // label table
	// HashMap<Integer,Val> tempMap; // temp table
	// HashMap<String,Val> varMap; // var table
	//
	// For IR1, they need to be managed at per function level.
	//
	
	/**
	 * Represents an Environment.
	 *
	 * @author Ian
	 */
	static class Environment
	{
		private Map<String, Integer> labelMap;
		private Map<Integer, Val> tempMap;
		private Map<String, Val> varMap;
		
		public Environment()
		{
			labelMap = new HashMap<String, Integer>();
			tempMap = new HashMap<Integer, IR1Interp.Val>();
			varMap = new HashMap<String, IR1Interp.Val>();
		}
		
		public Integer getLabelLocation(String label)
		{
				return labelMap.get(label);
		}
		
		public void setLabelLocation(String label, Integer location)
		{
			labelMap.put(label, location);
		}
		
		public Val getTempVal(Integer identifier)
		{
				return tempMap.get(identifier);
		}
		
		public void setTempVal(Integer identifier, Val value)
		{
			tempMap.put(identifier, value);
		}
		
		public Val getVarVal(String variable)
		{
				return varMap.get(variable);
		}
		
		public void setVarVal(String variable, Val value)
		{
			varMap.put(variable, value);
		}
	}
	
	// The current environment.
	static Environment env;

	
	// -----------------------------------------------------------------
	// Global variables and constants
	// -----------------------------------------------------------------
	//
	// These variables and constants are for your reference only.
	// You may decide to use all of them, some of these, or not at all.
	//
	
	// Function lookup table
	// - maps function names to their AST nodes
	//
	static HashMap<String, IR1.Func> funcMap;
	
	// Heap memory
	// - for handling 'malloc'ed data
	// - you need to define allocation and access methods for it
	//
	static ArrayList<Val> heap;
	
	// Return value
	// - for passing return value from callee to caller
	//
	static Val returnVal;
	
	static Stack<Environment> callStack = new Stack<Environment>();
	
	// Execution status
	// - tells whether to continue with the nest inst, to jump to
	// a new target inst, or to return to the caller
	//
	static final int CONTINUE = 0;
	static final int RETURN = -1;
	
	// -----------------------------------------------------------------
	// The main method
	// -----------------------------------------------------------------
	//
	// 1. Open an IR1 program file.
	// 2. Call the IR1 AST parser to read in the program and
	// convert it to an AST (rooted at an IR1.Program node).
	// 3. Invoke the interpretation process on the root node.
	//
	@SuppressWarnings ("static-access")
	public static void main(String[] args) throws Exception
	{
		if (args.length == 1)
		{
			FileInputStream stream = new FileInputStream(args[0]);
			IR1.Program p = new ir1Parser(stream).Program();
			stream.close();
			IR1Interp.execute(p);
		}
		else
		{
			System.out.println("You must provide an input file name.");
		}
	}
	
	// -----------------------------------------------------------------
	// Top-level IR nodes
	// -----------------------------------------------------------------
	//
	
	// Program ---
	// Func[] funcs;
	//
	// 1. Establish the function lookup map
	// 2. Lookup 'main' in funcMap, and
	// 3. start interpreting from main's AST node
	//
	public static void execute(IR1.Program n) throws Exception
	{
		funcMap = new HashMap<String, IR1.Func>();
		heap = new ArrayList<Val>();
		returnVal = new UndVal();
		
		for (IR1.Func f : n.funcs)
		{
			funcMap.put(f.name, f);
		}
		
		execute(funcMap.get("main"));
	}
	
	// Func ---
	// String name;
	// Var[] params;
	// Var[] locals;
	// Inst[] code;
	//
	// 1. Collect label decls information and store them in
	// a label-lookup table for later use.
	// 2. Execute the fetch-and-execute loop.
	//
	static void execute(IR1.Func n) throws Exception
	{
		// Push the current environment onto the stack, if any.
		if (env != null)
		{
			callStack.push(env);
		}
		
		// Regardless, make a new environment.
		env = new Environment();
		
		for (int offset = 0; offset < n.code.length; offset++)
		{
			Inst instr = n.code[offset];
			
			if (!(instr instanceof LabelDec))
			{
				continue;
			}
			
			LabelDec labelDecl = (LabelDec) instr;
			env.setLabelLocation(labelDecl.name, offset);
		}
		
		// The fetch-and-execute loop
		int idx = 0;
		while (idx < n.code.length)
		{
			int next = execute(n.code[idx]);
			
			if (next == CONTINUE)
			{
				idx++;
			}
			else if (next == RETURN)
			{
				break;
			}
			else
			{
				idx = next;
			}
		}
		
		if (callStack.isEmpty())
		{
			env = null;
		}
		else
		{
			// Restore the environment.
			env = callStack.pop();
		}
	}
	
	// Dispatch execution to an individual Inst node.
	//
	static int execute(IR1.Inst n) throws Exception
	{
		if (n instanceof IR1.Binop)
			return execute((IR1.Binop) n);
		if (n instanceof IR1.Unop)
			return execute((IR1.Unop) n);
		if (n instanceof IR1.Move)
			return execute((IR1.Move) n);
		if (n instanceof IR1.Load)
			return execute((IR1.Load) n);
		if (n instanceof IR1.Store)
			return execute((IR1.Store) n);
		if (n instanceof IR1.Jump)
			return execute((IR1.Jump) n);
		if (n instanceof IR1.CJump)
			return execute((IR1.CJump) n);
		if (n instanceof IR1.Call)
			return execute((IR1.Call) n);
		if (n instanceof IR1.Return)
			return execute((IR1.Return) n);
		if (n instanceof IR1.LabelDec)
			return CONTINUE;
		throw new IntException("Unknown Inst: " + n);
	}
	
	// -----------------------------------------------------------------
	// Execution routines for individual Inst nodes
	// -----------------------------------------------------------------
	//
	// - Each execute() routine returns CONTINUE, RETURN, or a new idx
	// (target of jump).
	//
	
	// Binop ---
	// BOP op;
	// Dest dst;
	// Src src1, src2;
	//
	static int execute(IR1.Binop n) throws Exception
	{
		Val res;
		
		if (n.op instanceof AOP)
		{
			res = evaluate((AOP) n.op, n.src1, n.src2);
		}
		else if (n.op instanceof ROP)
		{
			boolean result = evaluate((ROP) n.op, n.src1, n.src2);
			res = new BoolVal(result);
		}
		else
		{
			throw new IntException("Unhandled operator: " + n.op.getClass().getName() + ".");
		}
		
		assign(n.dst, res);
		
		return CONTINUE;
	}
	
	// Unop ---
	// UOP op;
	// Dest dst;
	// Src src;
	//
	static int execute(IR1.Unop n) throws Exception
	{
		Val val = evaluate(n.src);
		Val res;
		
		if (n.op == IR1.UOP.NEG)
		{
			res = new IntVal(-((IntVal) val).i);
		}
		else if (n.op == UOP.NOT)
		{
			res = new BoolVal(!((BoolVal) val).b);
		}
		else
		{
			throw new IntException("Wrong op in Unop inst: " + n.op);
		}
		
		assign(n.dst, res);
		
		return CONTINUE;
	}
	
	// Move ---
	// Dest dst;
	// Src src;
	//
	static int execute(IR1.Move n) throws Exception
	{
		Val val = evaluate(n.src);
		
		assign(n.dst, val);
		
		return CONTINUE;
	}
	
	// Load ---
	// Dest dst;
	// Addr addr;
	//
	static int execute(IR1.Load n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Store ---
	// Addr addr;
	// Src src;
	//
	static int execute(IR1.Store n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// CJump ---
	// ROP op;
	// Src src1, src2;
	// Label lab;
	//
	static int execute(IR1.CJump n) throws Exception
	{
		boolean result = evaluate(n.op, n.src1, n.src2);
		
		if (!result)
		{
			return CONTINUE;
		}
		
		if (env != null && env.getLabelLocation(n.lab.name) != null)
		{
			return env.getLabelLocation(n.lab.name);
		}
		
		throw new IntException("The label is not defined: " + n.lab.name + ".");
	}
	
	// Jump ---
	// Label lab;
	//
	static int execute(IR1.Jump n) throws Exception
	{
		if (env != null && env.getLabelLocation(n.lab.name) != null)
		{
			return env.getLabelLocation(n.lab.name);
		}
		
		throw new IntException("The label is not defined: " + n.lab.name + ".");
	}
	
	// Call ---
	// String name;
	// Src[] args;
	// Dest rdst;
	//
	static int execute(IR1.Call n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// Return ---
	// Src val;
	//
	static int execute(IR1.Return n) throws Exception
	{
		returnVal = evaluate(n.val);
		
		return RETURN;
	}
	
	// -----------------------------------------------------------------
	// Evaluation routines for address
	// -----------------------------------------------------------------
	//
	// - Returns an integer (representing index to the heap memory).
	//
	// Address ---
	// Src base;
	// int offset;
	//
	static int evalute(IR1.Addr n) throws Exception
	{
		
		// ... code needed ...
		
	}
	
	// -----------------------------------------------------------------
	// Evaluation routines for operands
	// -----------------------------------------------------------------
	//
	// - Each evaluate() routine returns a Val object.
	//
	static Val evaluate(IR1.Src n) throws Exception
	{
		Val val = null;
		
		if (n instanceof IntLit)
		{
			return new IntVal(((IntLit) n).i);
		}
		else if (n instanceof BoolLit)
		{
			return new BoolVal(((BoolVal) n).b);
		}
		else if (n instanceof StrLit)
		{
			return new StrVal(((StrLit) n).s);
		}
		
		// TODO: Will need to look over this when answered about globals.
		if (n instanceof IR1.Temp)
		{
			IR1.Temp temp = (IR1.Temp) n;
			val = env != null && env.getTempVal(temp.num) != null ? env
					.getTempVal(temp.num) : null;
			
			if (val == null)
			{
				throw new IntException(
						"The temporary variable is not defined: " + temp.num
								+ ".");
			}
		}
		else if (n instanceof IR1.Id)
		{
			IR1.Id id = (IR1.Id) n;
			val = env != null && env.getVarVal(id.name) != null ? env
					.getVarVal(id.name) : null;
			
			if (val == null)
			{
				throw new IntException("The variable is not defined: "
						+ id.name + ".");
			}
		}
		
		return val;
	}
	
	static Val evaluate(IR1.Dest n) throws Exception
	{
		Val val = null;
		
		// TODO: Will need to look over this when answered about globals.
		if (n instanceof IR1.Temp)
		{
			IR1.Temp temp = (IR1.Temp) n;
			val = env != null && env.getTempVal(temp.num) != null ? env
					.getTempVal(temp.num) : null;
			
			if (val == null)
			{
				throw new IntException(
						"The temporary variable is not defined: " + temp.num
								+ ".");
			}
		}
		else if (n instanceof IR1.Id)
		{
			IR1.Id id = (IR1.Id) n;
			val = env != null && env.getVarVal(id.name) != null ? env
					.getVarVal(id.name) : null;
			
			if (val == null)
			{
				throw new IntException("The variable is not defined: "
						+ id.name + ".");
			}
		}
		
		return val;
	}
	
	static boolean evaluate(ROP op, Src src1, Src src2) throws Exception
	{
		Val lhs = evaluate(src1);
		Val rhs = evaluate(src2);
		
		if (!lhs.getClass().getName().equals(rhs.getClass().getName()))
		{
			throw new IntException(
					"The left and right hand operands do not match types.");
		}
		
		if (lhs instanceof BoolVal)
		{
			if (ROP.EQ == op)
			{
				return ((BoolVal) lhs).b == ((BoolVal) rhs).b;
			}
			else if (ROP.NE == op)
			{
				return ((BoolVal) lhs).b == ((BoolVal) rhs).b;
			}
			
			throw new IntException(
					"The following operator cannot be performed on booleans: "
							+ op + ".");
		}
		
		int result = 0;
		if (lhs instanceof StrVal)
		{
			result = ((StrVal) lhs).s.compareTo(((StrVal) rhs).s);
		}
		else if (lhs instanceof IntVal)
		{
			result = Integer.compare(((IntVal) lhs).i, ((IntVal) rhs).i);
		}
		else
		{
			throw new IntException("The following value type was not handled: "
					+ lhs.getClass().getName() + ".");
		}
		
		if (op == ROP.EQ)
		{
			return result == 0;
		}
		else if (op == ROP.NE)
		{
			return result != 0;
		}
		else if (op == ROP.LT)
		{
			return result < 0;
		}
		else if (op == ROP.GT)
		{
			return result > 0;
		}
		else if (op == ROP.LE)
		{
			return result <= 0;
		}
		else if (op == ROP.GE)
		{
			return result >= 0;
		}
		
		throw new IntException("Unhandled ROP: " + op + ".");
	}
	
	static Val evaluate(AOP op, Src src1, Src src2) throws Exception
	{
		// TODO, there is ADD("+"), SUB("-"), MUL("*"), DIV("/"), AND("&&"), OR("||"),
		//       this can return an integer or bool.
		return null;
	}
	
	static void assign(Dest dest, Val value)
	{
		// TODO eval dest, update the value.
	}
	
}

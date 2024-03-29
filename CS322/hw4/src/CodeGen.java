// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// X86-64 code generator for IR1 (A starter version, For CS322 HW4)
//
// Homework 4 completed by Ian Aldrighetti <aldrig@pdx.edu>
//

import java.io.*;
import java.util.*;

import com.sun.nio.sctp.PeerAddressChangeNotification.AddressChangeEvent;

import ir1.*;
import ir1.IR1.ROP;

class CodeGen
{
	static class GenException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public GenException(String msg)
		{
			super(msg);
		}
	}
	
	@SuppressWarnings ("static-access")
	public static void main(String[] args) throws Exception
	{
		if (args.length == 1)
		{
			FileInputStream stream = new FileInputStream(args[0]);
			IR1.Program p = new ir1Parser(stream).Program();
			stream.close();
			// IR1.indexed = true;
			gen(p);
		}
		else
		{
			System.out.println("You must provide an input file name.");
		}
	}
	
	// ----------------------------------------------------------------------------------
	// Global Variables
	// ------------------
	
	// Per-program globals
	//
	static List<String> stringLiterals; // accumulated string literals,
										// indexed by position
	static final X86.Reg tempReg1 = X86.R10; // scratch registers - need to
	static final X86.Reg tempReg2 = X86.R11; // in sync with RegAlloc
	
	// Per-function globals
	//
	static Map<IR1.Dest, X86.Reg> regMap; // register mapping
	static int frameSize; // in bytes
	static String fnName; // function's name
	
	// ----------------------------------------------------------------------------------
	// Gen Routines
	// --------------
	
	// Program ---
	// Func[] funcs;
	//
	// Guideline:
	// - generate code for each function
	// - emit any accumulated string literals
	//
	public static void gen(IR1.Program n) throws Exception
	{
		stringLiterals = new ArrayList<String>();
		X86.emit0(".text");
		for (IR1.Func f : n.funcs)
			gen(f);
		int i = 0;
		for (String s : stringLiterals)
		{
			X86.GLabel lab = new X86.GLabel("_S" + i);
			X86.emitGLabel(lab);
			X86.emitString(s);
			i++;
		}
	}
	
	// Func ---
	// String name;
	// Var[] params;
	// Var[] locals;
	// Inst[] code;
	//
	// Guideline:
	// - call reg-alloc routine to assign registers to all Ids and Temps
	// - emit the function header
	// - save any callee-save registers on the stack
	// - make space for the local frame --- use the following calculation:
	// "if ((calleeSaveSize % 16) == 0)
	// frameSize += 8;"
	// where 'calleeSaveSize' represents the total size (in bytes) of
	// all saved callee-registers
	// - move the incoming actual arguments to their assigned locations
	// . simply fail if function has more than 6 args
	// . call X86's parallelMove routine to emit code
	// - emit code for the body
	//
	// Note: The restoring of the saved registers is carried out in the
	// code for Return instruction.
	//
	static void gen(IR1.Func n) throws Exception
	{
		fnName = n.name;
		System.out.print("\t\t\t  # " + n.header());
		
		// call reg-alloc routine to assign registers to all Ids and Temps
		regMap = RegAlloc.linearScan(n);
		List<X86.Reg> calleeSaveAllocated = new ArrayList<X86.Reg>();
		for (Map.Entry<IR1.Dest, X86.Reg> me : regMap.entrySet())
		{
			System.out.print("\t\t\t  # " + me.getKey() + "\t" + me.getValue() + "\n");
			
			if (Arrays.asList(X86.calleeSaveRegs).contains(me.getValue()))
			{
				calleeSaveAllocated.add(me.getValue());
			}
		}
		
		// TODO things
		
		System.out.println("\t.p2align 4,0x90");
		System.out.println("\t.globl _" + fnName);
		
		X86.emitGLabel(new X86.GLabel("_" + fnName));
		
		// Now we need to push all those that are callee save with pushq.
		int calleeSaveSize = 0;
		for (X86.Reg reg : X86.calleeSaveRegs)
		{
			if (!calleeSaveAllocated.contains(reg))
			{
				continue;
			}
			
			X86.emit1("pushq", reg);
			calleeSaveSize += 8;
		}
		
		if ((calleeSaveSize % 16) == 0)
		{
			frameSize += 8;
		}
		
		if (frameSize > 0)
		{
			X86.emit2(getX86Op(IR1.AOP.SUB), new X86.Imm(frameSize), X86.RSP);
		}
		
		// Now we need to move the parameters into here...
		int index = 0;
		for (String param : n.params)
		{
			if (index >= X86.argRegs.length)
			{
				throw new GenException("gen(IR1.Func): Too many arguments (>6).");
			}
			
			IR1.Dest p = new IR1.Id(param);
			X86.Reg destReg = regMap.get(p);
			X86.Reg srcReg = X86.argRegs[index++];
			
			if (destReg == null)
			{
				continue;
			}
			
			X86.emitMov(srcReg.s, srcReg, destReg);
		}
		
		for (IR1.Inst inst : n.code)
		{
			gen(inst);
		}
		
		frameSize -= frameSize;
	}
	
	// INSTRUCTIONS
	
	static void gen(IR1.Inst n) throws Exception
	{
		System.out.print("\t\t\t  # " + n);
		if (n instanceof IR1.Binop)
			gen((IR1.Binop) n);
		else if (n instanceof IR1.Unop)
			gen((IR1.Unop) n);
		else if (n instanceof IR1.Move)
			gen((IR1.Move) n);
		else if (n instanceof IR1.Load)
			gen((IR1.Load) n);
		else if (n instanceof IR1.Store)
			gen((IR1.Store) n);
		else if (n instanceof IR1.LabelDec)
			gen((IR1.LabelDec) n);
		else if (n instanceof IR1.CJump)
			gen((IR1.CJump) n);
		else if (n instanceof IR1.Jump)
			gen((IR1.Jump) n);
		else if (n instanceof IR1.Call)
			gen((IR1.Call) n);
		else if (n instanceof IR1.Return)
			gen((IR1.Return) n);
		else
			throw new GenException("Illegal IR1 instruction: " + n);
	}
	
	// For Binop, Unop, Move, and Load nodes:
	// - If dst is not assigned a register, it means that the
	// instruction is dead; just return
	//
	
	// Binop ---
	// BOP op;
	// Dest dst;
	// Src src1, src2;
	//
	// Guideline:
	// - call gen_source() to generate code for both left and right
	// and right operands
	//
	// * Regular cases (ADD, SUB, MUL, AND, OR):
	// - make sure right operand is not occupying the dst reg (if so,
	// generate a "mov" to move it to a tempReg)
	// - generate a "mov" to move left operand to the dst reg
	// - generate code for the Binop
	//
	// * For DIV:
	// The RegAlloc module guaranteeds that no caller-save register
	// (including RAX, RDX) is allocated across a DIV. (It also
	// preferenced the left operand and result to RAX.) But it is
	// still possible that the right operand is in RAX or RDX.
	// - if so, generate a "mov" to move it to a tempReg
	// - generate "cqto" (sign-extend into RDX) and "idivq"
	// - generate a "mov" to move the result to the dst reg
	//
	// * For relational ops:
	// - generate "cmp" and "set"
	// . note that set takes a byte-sized register
	// - generate "movzbq" to size--extend the result register
	//
	static void gen(IR1.Binop n) throws Exception
	{
		// It's dead!
		if (regMap.get(n.dst) == null)
		{
			return;
		}
		
		X86.Reg rhs = null;
		X86.Reg lhs = null;
		
		if (!(n.op instanceof IR1.ROP))
		{
			X86.Reg rhsTempReg = tempReg1;
			X86.Reg lhsTempReg = tempReg2;
			
			if (isAssignedAReg(n.src1) || isA(n.op, IR1.AOP.DIV))
			{
				//rhsTempReg = tempReg2;
				//lhsTempReg = tempReg1;
			}
			
			rhs = gen_source(n.src2, rhsTempReg);
			lhs = gen_source(n.src1, isA(n.op, IR1.AOP.DIV) ? X86.RAX : lhsTempReg);
		}

		if (isA(n.op, IR1.AOP.ADD, IR1.AOP.SUB, IR1.AOP.MUL, IR1.AOP.ADD, IR1.AOP.OR))
		{
			X86.Reg dest = regMap.get(n.dst);
			
			// If the rhs is within the destination, move it out.
			if (rhs.equals(dest))
			{
				X86.emitMov(rhs.s, rhs, tempReg1);
				
				rhs = tempReg1;
			}
			
			X86.emitMov(lhs.s, lhs, dest);
			
			String op = getX86Op(n.op);
			X86.emit2(op, rhs, dest);
			
			return;
		}
		else if (isA(n.op, IR1.AOP.DIV))
		{
			if (rhs.equals(X86.RAX) || rhs.equals(X86.RDX))
			{
				X86.emitMov(rhs.s, rhs, tempReg2);
				
				rhs = tempReg2;
			}
			
			X86.emit0("cqto");
			X86.emit1("idivq", rhs);
			
			X86.Reg dest = regMap.get(n.dst);
			X86.emitMov(dest.s, X86.RAX, dest);
			
			return;
		}
		else if (isA(n.op, IR1.ROP.EQ, IR1.ROP.GE, IR1.ROP.GT, IR1.ROP.LE, IR1.ROP.LT, IR1.ROP.LT, IR1.ROP.NE))
		{
			X86.Reg dest = regMap.get(n.dst);
			
			rhs = gen_source(n.src1, tempReg1);
			lhs = gen_source(n.src2, tempReg2);
			
			X86.emit2("cmpq", lhs, rhs);
			X86.emit1("set" + getRelationalIndicator((IR1.ROP) n.op), X86.resize_reg(X86.Size.B, dest));
			X86.emit2("movzbq", X86.resize_reg(X86.Size.B, dest), dest);
			
			return;
		}
		
		throw new GenException("gen(IR1.Binop): Unhandled case of binary operator: " + n.op.getClass().getCanonicalName() + " (" + n.op + ")");
	}
	
	static String getRelationalIndicator(IR1.ROP op) throws GenException
	{
		switch (op)
		{
			case EQ:
				return "e";
				
			case GE:
				return "ge";
				
			case GT:
				return "g";
				
			case LE:
				return "le";
				
			case LT:
				return "l";
				
			case NE:
				return "ne";
				
			default:
				throw new GenException("getRelationalIndicator(IR1.ROP): Unknown operator.");
		}
	}
	
	static String getX86Op(IR1.BOP op) throws Exception
	{
		if (isA(op, IR1.AOP.ADD))
		{
			return "addq";
		}
		else if (isA(op, IR1.AOP.SUB))
		{
			return "subq";
		}
		else if (isA(op, IR1.AOP.MUL))
		{
			return "imulq";
		}
		
		throw new GenException("getX86Op(IR1.BO): Unknown binary operator.");
	}
	
	static boolean isA(IR1.BOP actualOp, IR1.BOP... ops)
	{
		for (IR1.BOP expectedOp : ops)
		{
			if (actualOp == expectedOp)
			{
				return true;
			}
		}
		
		return false;
	}
	
	// Unop ---
	// UOP op;
	// Dest dst;
	// Src src;
	//
	// Guideline:
	// - call gen_source() to generate code for the operand
	// - generate a "mov" to move the operand to the dst reg
	// - generate code for the op
	//
	static void gen(IR1.Unop n) throws Exception
	{
		if (regMap.get(n.dst) == null)
		{
			return;
		}
		
		X86.Reg src = gen_source(n.src, tempReg1);
		X86.Reg dest = regMap.get(n.dst);
		
		X86.emitMov(dest.s, src, dest);
		
		X86.emit1((n.op == IR1.UOP.NOT ? "not" : "neg") + dest.s, dest);
	}
	
	// Move ---
	// Dest dst;
	// Src src;
	//
	// Guideline:
	// - call gen_source() to generate code for the src
	// - generate a "mov"
	//
	static void gen(IR1.Move n) throws Exception
	{
		// It's dead!
		if (regMap.get(n.dst) == null)
		{
			return;
		}
		
		X86.Reg dest = regMap.get(n.dst) != null ? regMap.get(n.dst) : tempReg1;
		
		X86.Reg src = gen_source(n.src, dest);
		
		// If source and destination are the same, no need to make a move.
		if (src.equals(dest))
		{
			return;
		}
		
		X86.emitMov(dest.s, src, dest);
	}
	
	// Load ---
	// Dest dst;
	// Addr addr;
	//
	// Guideline:
	// - call gen_addr() to generate code for addr
	// - generate a "mov"
	// . pay attention to size info (all IR1's stored values
	// are integers)
	//
	static void gen(IR1.Load n) throws Exception
	{
		// It's dead!
		if (regMap.get(n.dst) == null)
		{
			return;
		}
		
		X86.Operand addr = gen_addr(n.addr, tempReg1);
		X86.Reg dest = regMap.get(n.dst);

		// TODO THis may not work...
		X86.emit2("movslq", addr, dest);
		//X86.emitMov(dest.s, addr, dest);	
	}
	
	// Store ---
	// Addr addr;
	// Src src;
	//
	// Guideline:
	// - call gen_source() to generate code for src
	// - call gen_addr() to generate code for addr
	// - generate a "mov"
	// . pay attention to size info (IR1's stored values
	// are all integers)
	//
	static void gen(IR1.Store n) throws Exception
	{
		X86.Reg src = gen_source(n.src, tempReg1);
		X86.Operand addr = gen_addr(n.addr, tempReg2);
		
		X86.emitMov(X86.Size.L, X86.resize_reg(X86.Size.L, src), addr);
		
		// TODO need code
		//System.out.println("<--- TODO: Store");
		
	}
	
	// LabelDec ---
	// Label lab;
	//
	// Guideline:
	// - emit an unique local label by adding func's name in
	// front of IR1's label name
	//
	static void gen(IR1.LabelDec n)
	{
		X86.emitLabel(new X86.Label(fnName + "_" + n.lab.name));
	}
	
	// CJump ---
	// ROP op;
	// Src src1, src2;
	// Label lab;
	//
	// Guideline:
	// - recursively generate code for the two operands
	// - generate a "cmp" and a jump instruction
	// . remember: left and right are switched under gnu assembler
	// . conveniently, IR1 and X86 names for the condition
	// suffixes are the same
	//
	static void gen(IR1.CJump n) throws Exception
	{
		X86.Reg lhs = gen_source(n.src1, tempReg1);
		X86.Reg rhs = gen_source(n.src2, tempReg2);
		
		X86.emit2("cmpq", rhs, lhs);
		X86.emit1("je", new X86.Label(fnName + "_" + n.lab.name));
	}
	
	// Jump ---
	// Label lab;
	//
	// Guideline:
	// - generate a "jmp" to a local label
	// . again, add func's name in front of IR1's label name
	//
	static void gen(IR1.Jump n) throws Exception
	{
		
		X86.emit1("jmp", new X86.Label(fnName + "_" + n.lab.name));
		
	}
	
	// Call ---
	// String name;
	// Src[] args;
	// Dest rdst;
	//
	// Guideline:
	// - count args; if there are more than 6 args, just fail
	// - move arguments into the argument regs
	// . first call X86's parallelMove() to move registered args
	// . then generate "mov" to move immediate args
	// - emit a "call" with a global label (i.e. "_" preceding func's name)
	// - if return value is expected, emit a "mov" to move result from
	// rax to target reg
	//
	static void gen(IR1.Call n) throws Exception
	{
		if (n.args.length > 6)
		{
			throw new GenException("gen(IR1.Call): Too many arguments (>6).");
		}
		
		//%rdi, %rsi, %rdx, %rcx, %r8, %r9.
		X86.Reg[] params = { X86.RDI, X86.RSI, X86.RDX, X86.RCX, X86.R8, X86.R9 };
		
		int i = 0;
		for (IR1.Src arg : n.args)
		{
			X86.Reg reg = gen_source(arg, params[i]);
			
			// If they're equal, no need to move them again.
			if (reg.equals(params[i]))
			{
				i++;
				continue;
			}
			
			X86.emitMov(reg.s, reg, params[i++]);
		}
		
		X86.emit1("call", new X86.GLabel("_" + n.name));
		
		X86.Reg dest = regMap.get(n.rdst);
		
		if (dest == null)
		{
			return;
		}
		
		X86.emitMov(dest.s, X86.RAX, dest);
		
		//X86.Reg src = gen_source(n.src, dest);
		
		//X86.emitMov(X86.Size.Q, src, dest);
	}
	
	// Return ---
	// Src val;
	//
	// Guideline:
	// - if there is a value, emit a "mov" to move it to rax
	// - pop the frame (add framesize back to stack pointer)
	// - restore any saved callee-save registers
	// - emit a "ret"
	//
	static void gen(IR1.Return n) throws Exception
	{
		// TODO The rest.
		
		if (n.val != null)
		{
			X86.Reg reg = gen_source(n.val,  X86.RAX);
			
			if (!reg.equals(X86.RAX))
			{
				X86.emitMov(reg.s, reg, X86.RAX);
			}
		}
		
		if (frameSize > 0)
		{
			X86.emit2("addq", new X86.Imm(frameSize), X86.RSP);
		}
		
		List<X86.Reg> calleeSaveAllocated = new ArrayList<X86.Reg>();
		for (Map.Entry<IR1.Dest, X86.Reg> me : regMap.entrySet())
		{
			if (!Arrays.asList(X86.calleeSaveRegs).contains(me.getValue()))
			{
				continue;
			}
			
			calleeSaveAllocated.add(me.getValue());
		}
		
		for (int index = X86.calleeSaveRegs.length - 1; index >= 0; index--)
		{
			X86.Reg reg = X86.calleeSaveRegs[index];
			
			if (!calleeSaveAllocated.contains(reg))
			{
				continue;
			}
			
			X86.emit1("popq", reg);
		}
		
		X86.emit0("ret");
	}
	
	static List<X86.Reg> getCalleeSaveRegisters()
	{
		X86.Reg[] calleeSave = { X86.RBX, X86.RBP, X86.R12, X86.R13, X86.R14, X86.R15 };

		return Arrays.asList(calleeSave);
	}
	
	static boolean isAssignedAReg(IR1.Src n) throws GenException
	{
		return isA(n, Type.Id, Type.Temp);
	}
	
	// OPERANDS
	
	// Src -> Id | Temp | IntLit | BoolLit | StrLit
	//
	// Return the Src's value in a register. Use the temp register
	// for the literal nodes.
	//
	// Guideline:
	// * Id and Temp:
	// - get their assigned reg from regMap and return it
	// * IntLit:
	// - emit code to move the value to the temp reg and return the reg
	// * BoolLit:
	// - same as IntLit, except that use 1 for "true" and 0 for "false"
	// * StrLit:
	// - add the string to 'stringLiterals' collection to be emitted late
	// - construct a globel label "_Sn" where n is the index of the string
	// in the 'stringLiterals' collection
	// - emit a "lea" to move the label to the temp reg and return the reg
	//
	static X86.Reg gen_source(IR1.Src n, final X86.Reg temp) throws Exception
	{
		if (isA(n, Type.Id, Type.Temp))
		{
			IR1.Dest dest = (IR1.Dest) n;
			
			return regMap.get(dest);
		}
		else if (isA(n, Type.Int))
		{
			IR1.IntLit lit = (IR1.IntLit) n;
			
			X86.emitMov(X86.Size.Q, new X86.Imm(lit.i), temp);
			
			return temp;
		}
		else if (isA(n, Type.Bool))
		{
			IR1.BoolLit lit = (IR1.BoolLit) n;
			
			X86.emitMov(X86.Size.Q, new X86.Imm(lit.b ? 1 : 0), temp);
			
			return temp;
		}
		else if (isA(n, Type.Str))
		{
			IR1.StrLit lit = (IR1.StrLit) n;
			
			// Add the string literal, but save it's offset.
			int offset = stringLiterals.size();
			stringLiterals.add(lit.s);
			
			X86.GLabel strLabel = new X86.GLabel("_S" + offset);
		
			X86.emit2("leaq", new X86.AddrName(strLabel.s), temp);
			
			return temp;
		}
		
		throw new GenException("gen_source(IR1.Src, X86.Reg): Unknown src type: " + n.getClass().getCanonicalName());
	}
	
	static boolean isA(IR1.Src src, Type... types) throws GenException
	{
		Type actualType = getType(src);
		
		for (Type type : types)
		{
			if (actualType == type)
			{
				return true;
			}
		}
		
		return false;
	}
	
	static enum Type {
		Id, Temp, Int, Bool, Str
	};
	
	static Type getType(IR1.Src src) throws GenException
	{
		if (src instanceof IR1.Id)
		{
			return Type.Id;
		}
		else if (src instanceof IR1.Temp)
		{
			return Type.Temp;
		}
		else if (src instanceof IR1.IntLit)
		{
			return Type.Int;
		}
		else if (src instanceof IR1.BoolLit)
		{
			return Type.Bool;
		}
		else if (src instanceof IR1.StrLit)
		{
			return Type.Str;
		}
		
		throw new GenException("Unable to determine the type of: " + src.getClass().getCanonicalName());
	}
	
	// Addr ---
	// Src base;
	// int offset;
	//
	// Guideline:
	// - call gen_source() on base to place it in a reg
	// - generate a memory operand (i.e. X86.Mem)
	//
	static X86.Operand gen_addr(IR1.Addr addr, X86.Reg temp) throws Exception
	{
		X86.Reg base = gen_source(addr.base, temp);
		return new X86.Mem(base, addr.offset);
	}
	
	// ----------------------------------------------------------------------------------
	// Ultilities
	// ------------
	
	static String opname(IR1.AOP op)
	{
		switch (op)
		{
			case ADD:
				return "add";
			case SUB:
				return "sub";
			case MUL:
				return "imul";
			case DIV:
				return "idiv"; // not used
			case AND:
				return "and";
			case OR:
				return "or";
		}
		return null; // impossible
	}
	
	static String opname(IR1.UOP op)
	{
		switch (op)
		{
			case NEG:
				return "neg";
			case NOT:
				return "not";
		}
		return null; // impossible
	}
	
	static String opname(IR1.ROP op)
	{
		switch (op)
		{
			case EQ:
				return "e";
			case NE:
				return "ne";
			case LT:
				return "l";
			case LE:
				return "le";
			case GT:
				return "g";
			case GE:
				return "ge";
		}
		return null; // impossible
	}
	
}

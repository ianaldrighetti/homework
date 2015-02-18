// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
// 
// IR code generator for miniJava's AST.
//
// (Starter version.)
//

import ir.IR;
import ir.IR.Addr;
import ir.IR.BoolLit;
import ir.IR.Call;
import ir.IR.Dest;
import ir.IR.Global;
import ir.IR.Id;
import ir.IR.Inst;
import ir.IR.LabelDec;
import ir.IR.StrLit;
import ir.IR.Temp;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import ast.Ast;
import ast.Ast.MethodDecl;
import ast.Ast.Param;
import ast.Ast.Stmt;
import ast.Ast.VarDecl;
import ast.astParser;

public class IRGen
{
	
	static class GenException extends Exception
	{
		public GenException(String msg)
		{
			super(msg);
		}
	}
	
	// ------------------------------------------------------------------------------
	// ClassInfo
	// ----------
	// For keeping all useful information about a class declaration for use
	// in the codegen.
	//
	static class ClassInfo
	{
		Ast.ClassDecl cdecl; // classDecl AST
		ClassInfo parent; // pointer to parent
		List<String> vtable; // method-label table
		List<Ast.VarDecl> fdecls; // field decls (incl. inherited ones)
		List<Integer> offsets; // field offsets
		int objSize; // object size
		
		// Constructor -- clone a parent's record
		//
		ClassInfo(Ast.ClassDecl cdecl, ClassInfo parent)
		{
			this.cdecl = cdecl;
			this.parent = parent;
			this.vtable = new ArrayList<String>(parent.vtable);
			this.fdecls = new ArrayList<Ast.VarDecl>(parent.fdecls);
			this.offsets = new ArrayList<Integer>(parent.offsets);
			this.objSize = parent.objSize;
		}
		
		// Constructor -- create a new record
		//
		ClassInfo(Ast.ClassDecl cdecl)
		{
			this.cdecl = cdecl;
			this.parent = null;
			this.vtable = new ArrayList<String>();
			this.fdecls = new ArrayList<Ast.VarDecl>();
			this.offsets = new ArrayList<Integer>();
			this.objSize = IR.Type.PTR.size; // reserve space for ptr to class
		}
		
		// Utility Routines
		// ----------------
		// For accessing information stored in class information record
		//
		
		// Return the name of this class
		//
		String className()
		{
			return cdecl.nm;
		}
		
		// Find method's base class record
		//
		ClassInfo methodBaseClass(String mname) throws Exception
		{
			for (Ast.MethodDecl mdecl : cdecl.mthds)
				if (mdecl.nm.equals(mname))
					return this;
			if (parent != null)
				return parent.methodBaseClass(mname);
			throw new GenException("Can't find base class for method " + mname);
		}
		
		// Find method's return type
		//
		Ast.Type methodType(String mname) throws Exception
		{
			for (Ast.MethodDecl mdecl : cdecl.mthds)
				if (mdecl.nm.equals(mname))
					return mdecl.t;
			if (parent != null)
				return parent.methodType(mname);
			throw new GenException("Can't find MethodDecl for method " + mname);
		}
		
		// Return method's vtable offset
		//
		int methodOffset(String mname)
		{
			return vtable.indexOf(mname) * IR.Type.PTR.size;
		}
		
		// Find field variable's type
		//
		Ast.Type fieldType(String fname) throws Exception
		{
			for (Ast.VarDecl fdecl : cdecl.flds)
			{
				if (fdecl.nm.equals(fname))
					return fdecl.t;
			}
			if (parent != null)
				return parent.fieldType(fname);
			throw new GenException("Can't find VarDecl for field " + fname);
		}
		
		// Return field variable's offset
		//
		int fieldOffset(String fname) throws Exception
		{
			for (int i = fdecls.size() - 1; i >= 0; i--)
			{
				if (fdecls.get(i).nm.equals(fname))
					return offsets.get(i);
			}
			throw new GenException("Can't find offset for field " + fname);
		}
		
		public String toString()
		{
			return "ClassInfo: " + " " + cdecl + " " + parent + " " + " "
					+ vtable + " " + offsets + " " + objSize;
		}
	}
	
	// ------------------------------------------------------------------------------
	// Other Supporting Data Structures
	// ---------------------------------
	
	// CodePack
	// --------
	// For returning <type,src,code> tuple from gen() routines
	//
	static class CodePack
	{
		IR.Type type;
		IR.Src src;
		List<IR.Inst> code;
		
		CodePack(IR.Type type, IR.Src src, List<IR.Inst> code)
		{
			this.type = type;
			this.src = src;
			this.code = code;
		}
		
		CodePack(IR.Type type, IR.Src src)
		{
			this.type = type;
			this.src = src;
			code = new ArrayList<IR.Inst>();
		}
	}
	
	// AddrPack
	// --------
	// For returning <type,addr,code> tuple from genAddr routines
	//
	static class AddrPack
	{
		IR.Type type;
		IR.Addr addr;
		List<IR.Inst> code;
		
		AddrPack(IR.Type type, IR.Addr addr, List<IR.Inst> code)
		{
			this.type = type;
			this.addr = addr;
			this.code = code;
		}
	}
	
	// Env
	// ---
	// For keeping track of local variables and parameters and for finding
	// their types.
	//
	private static class Env extends HashMap<String, Ast.Type>
	{
	}
	
	// ------------------------------------------------------------------------------
	// Global Variables
	// ----------------
	//
	
	// Env for ClassInfo records
	private static HashMap<String, ClassInfo> classEnv = new HashMap<String, ClassInfo>();
	
	// IR code representation of the current object
	private static IR.Src thisObj = new IR.Id("obj");
	
	// ------------------------------------------------------------------------------
	// Utility routines
	// ----------------
	//
	
	// Sort ClassDecls based on parent-children relationship.
	//
	private static Ast.ClassDecl[] topoSort(Ast.ClassDecl[] classes)
	{
		List<Ast.ClassDecl> cl = new ArrayList<Ast.ClassDecl>();
		Vector<String> done = new Vector<String>();
		int cnt = classes.length;
		while (cnt > 0)
		{
			for (Ast.ClassDecl cd : classes)
				if (!done.contains(cd.nm)
						&& ((cd.pnm == null) || done.contains(cd.pnm)))
				{
					cl.add(cd);
					done.add(cd.nm);
					cnt--;
				}
		}
		return cl.toArray(new Ast.ClassDecl[0]);
	}
	
	// Return an object's base classInfo.
	// (The parameter n is known to represent an object when call
	// is made.)
	//
	private static ClassInfo getClassInfo(Ast.Exp n, ClassInfo cinfo, Env env)
			throws Exception
	{
		Ast.Type typ = null;
		if (n instanceof Ast.This)
			return cinfo;
		if (n instanceof Ast.Id)
		{
			typ = env.get(((Ast.Id) n).nm);
			if (typ == null) // id is a field with a missing "this" pointer
				typ = cinfo.fieldType(((Ast.Id) n).nm);
		}
		else if (n instanceof Ast.Field)
		{
			ClassInfo base = getClassInfo(((Ast.Field) n).obj, cinfo, env);
			typ = base.fieldType(((Ast.Field) n).nm);
		}
		else
		{
			throw new GenException("Unexpected obj epxression " + n);
		}
		if (!(typ instanceof Ast.ObjType))
			throw new GenException("Expects an ObjType, got " + typ);
		return classEnv.get(((Ast.ObjType) typ).nm);
	}
	
	// Create ClassInfo record
	//
	// Codegen Guideline:
	// 1. If parent exists, clone parent's record; otherwise create a new one
	// 2. Walk the MethodDecl list. If a method is not in the v-table, add it
	// in;
	// 3. Compute offset values for field variables
	// 4. Decide object's size
	//
	private static ClassInfo createClassInfo(Ast.ClassDecl n) throws Exception
	{
		ClassInfo cinfo = (n.pnm != null) ? new ClassInfo(n,
				classEnv.get(n.pnm)) : new ClassInfo(n);
		
		for (MethodDecl methodDecl : n.mthds)
		{
			if (cinfo.vtable.contains(methodDecl.nm))
			{
				continue;
			}
			
			cinfo.vtable.add(methodDecl.nm);
		}
		
		int offset = cinfo.objSize;
		for (VarDecl varDecl : n.flds)
		{
			cinfo.fdecls.add(varDecl);
			cinfo.offsets.add(offset);
			
			offset += (varDecl.t instanceof Ast.BoolType ? 1 : (varDecl.t instanceof Ast.IntType ? 4 : 8));
		}
		
		// TODO this?
		cinfo.objSize = offset;
		
		return cinfo;
	}
	
	// ------------------------------------------------------------------------------
	// The Main Routine
	// -----------------
	//
	public static void main(String[] args) throws Exception
	{
		if (args.length == 1)
		{
			FileInputStream stream = new FileInputStream(args[0]);
			Ast.Program p = new astParser(stream).Program();
			stream.close();
			IR.Program ir = gen(p);
			System.out.print(ir.toString());
		}
		else
		{
			System.out.println("You must provide an input file name.");
		}
	}
	
	// ------------------------------------------------------------------------------
	// Codegen Routines for Individual AST Nodes
	// ------------------------------------------
	
	// Program ---
	// ClassDecl[] classes;
	//
	// Three passes over a program:
	// 0. topo-sort class decls
	// 1. create ClassInfo records
	// 2. generate IR code
	// 2.1 generate list of static data (i.e. class descriptors)
	// 2.2 generate list of functions
	//
	public static IR.Program gen(Ast.Program n) throws Exception
	{
		Ast.ClassDecl[] classes = topoSort(n.classes);
		ClassInfo cinfo;
		for (Ast.ClassDecl c : classes)
		{
			cinfo = createClassInfo(c);
			classEnv.put(c.nm, cinfo);
		}
		List<IR.Data> allData = new ArrayList<IR.Data>();
		List<IR.Func> allFuncs = new ArrayList<IR.Func>();
		boolean isFirst = true;
		for (Ast.ClassDecl c : classes)
		{
			cinfo = classEnv.get(c.nm);
			IR.Data data = genData(c, cinfo, isFirst);
			List<IR.Func> funcs = gen(c, cinfo, isFirst);
			if (data != null)
				allData.add(data);
			allFuncs.addAll(funcs);
			isFirst = false;
		}
		return new IR.Program(allData, allFuncs);
	}
	
	// ClassDecl ---
	// String nm, pnm;
	// VarDecl[] flds;
	// MethodDecl[] mthds;
	//
	
	// 1. Generate static data
	//
	// Codegen Guideline:
	// 1.1 For each method in class's vtable, construct a global label of form
	// "<base class name>_<method name>" and save it in an IR.Global node
	// 1.2 Assemble the list of IR.Global nodes into an IR.Data node with a
	// global label "class_<class name>"
	//
	static IR.Data genData(Ast.ClassDecl n, ClassInfo cinfo, boolean isFirst) throws Exception
	{
		if (n.mthds.length == 0)
		{
			return null;
		}
		
		IR.Global classGlobal = new IR.Global("class_" + n.nm);
		
		ClassInfo classInfo = classEnv.get(n.nm);
		
		IR.Global[] methodGlobals = new IR.Global[n.mthds.length];
		for (int i = 0; i < n.mthds.length; i++)
		{
			Ast.MethodDecl methodDecl = n.mthds[i];
			
			// TODO unsure about the isFirst thing.
			methodGlobals[i] = new IR.Global((!isFirst ? classInfo.className() + "_" : "") + methodDecl.nm); 
		}
		
		return new IR.Data(classGlobal, 8 * methodGlobals.length, methodGlobals);
	}
	
	// 2. Generate code
	//
	// Codegen Guideline:
	// Straightforward -- generate a IR.Func for each mthdDecl.
	//
	static List<IR.Func> gen(Ast.ClassDecl n, ClassInfo cinfo, boolean isFirst) throws Exception
	{
		List<IR.Func> funcs = new ArrayList<IR.Func>();
	
		for (Ast.MethodDecl methodDecl : n.mthds)
		{
			funcs.add(gen(methodDecl, cinfo, isFirst));
		}
		
		return funcs;
	}
	
	// MethodDecl ---
	// Type t;
	// String nm;
	// Param[] params;
	// VarDecl[] vars;
	// Stmt[] stmts;
	//
	// Codegen Guideline:
	// 1. Construct a global label of form "<base class name>_<method name>"
	// 2. Add "obj" into the params list as the 0th item
	// (Skip these two steps if method is "main".)
	// 3. Create an Env() containing all params and all local vars
	// 4. Generate IR code for all statements
	// 5. Return an IR.Func with the above
	//
	static IR.Func gen(Ast.MethodDecl n, ClassInfo cinfo, boolean isFirst) throws Exception
	{
		IR.Temp.reset();
		
		// Create global label.
		// TODO something with this
		String globalLabel = cinfo.className() + "_" + n.nm;
		
		// Add obj to params list.
		String[] params = new String[n.params.length + 1];
		params[0] = "obj";
		for (int i = 0; i < n.params.length; i++)
		{
			params[i + 1] = n.params[i].nm;
		}
		
		// Nothing for main!
		if (n.nm.equals("main"))
		{
			params = new String[0];
		}
		
		String[] locals = new String[n.vars.length];
		for (int i = 0; i < n.vars.length; i++)
		{
			locals[i] = n.vars[i].nm; 
		}
		
		Env environment = new Env();
		environment.put("obj", new Ast.ObjType(cinfo.className()));
		
		// Add everything to the environment.
		for (Param param : n.params)
		{
			environment.put(param.nm, param.t);
		}
		
		// Start gathering the method body.
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		
		// Variable declarations.
		for (VarDecl varDecl : n.vars)
		{
			environment.put(varDecl.nm, varDecl.t);
			
			// Might have initialization.
			code.addAll(gen(varDecl, cinfo, environment));
		}
		
		// Then all the statements.
		for (Stmt stmt : n.stmts)
		{
			code.addAll(gen(stmt, cinfo, environment));
		}
		
		if (n.t == null)
		{
			code.add(new IR.Return());
		}
		
		IR.Inst[] codeArray = (IR.Inst[]) code.toArray(new IR.Inst[code.size()]);
		
		IR.Func func = new IR.Func((!isFirst ? cinfo.className() + "_" : "") + n.nm, params, locals, codeArray);
		
		return func;
	}
	
	// VarDecl ---
	// Type t;
	// String nm;
	// Exp init;
	//
	// Codegen Guideline:
	// 1. If init exp exists, generate IR code for it and assign result to var
	// 2. Return generated code (or null if none)
	//
	private static List<IR.Inst> gen(Ast.VarDecl n, ClassInfo cinfo, Env env) throws Exception
	{
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		
		if (n.init != null)
		{
			//CodePack val = gen(n.init, cinfo, env);
			
			Ast.Id dest = new Ast.Id(n.nm);
			
			Ast.Assign assign = new Ast.Assign(dest, n.init);
			
			code.addAll(gen(assign, cinfo, env));
		}
		
		return code;
	}
	
	// STATEMENTS
	
	// Dispatch a generic call to a specific Stmt routine
	//
	static List<IR.Inst> gen(Ast.Stmt n, ClassInfo cinfo, Env env) throws Exception
	{
		if (n instanceof Ast.Block)
			return gen((Ast.Block) n, cinfo, env);
		if (n instanceof Ast.Assign)
			return gen((Ast.Assign) n, cinfo, env);
		if (n instanceof Ast.CallStmt)
			return gen((Ast.CallStmt) n, cinfo, env);
		if (n instanceof Ast.If)
			return gen((Ast.If) n, cinfo, env);
		if (n instanceof Ast.While)
			return gen((Ast.While) n, cinfo, env);
		if (n instanceof Ast.Print)
			return gen((Ast.Print) n, cinfo, env);
		if (n instanceof Ast.Return)
			return gen((Ast.Return) n, cinfo, env);
		throw new GenException("Illegal Ast Stmt: " + n);
	}
	
	// Block ---
	// Stmt[] stmts;
	//
	static List<IR.Inst> gen(Ast.Block n, ClassInfo cinfo, Env env)
			throws Exception
	{
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		
		for (Stmt stmt : n.stmts)
		{
			code.addAll(gen(stmt, cinfo, env));
		}
		
		return code;
	}
	
	// Assign ---
	// Exp lhs, rhs;
	//
	// Codegen Guideline:
	// 1. call gen() on rhs
	// 2. if lhs is ID, check against Env to see if it's a local var or a param;
	// if yes, generate an IR.Move instruction
	// 3. otherwise, call genAddr() on lhs, and generate an IR.Store instruction
	//
	static List<IR.Inst> gen(Ast.Assign n, ClassInfo cinfo, Env env)
			throws Exception
	{
		
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		
		CodePack rhs = gen(n.rhs, cinfo, env);
		
		if (n.lhs instanceof Ast.Id)
		{
			Ast.Id id = (Ast.Id) n.lhs;
			
			//System.out.println(cinfo.fieldOffset(id.nm));
			
			if (!env.containsKey(id.nm))
			{
				// It's a field, then.
				int fieldOffset = cinfo.fieldOffset(id.nm);
				
				code.add(new IR.Store(gen(cinfo.fieldType(id.nm)), new IR.Addr(new IR.Id("obj"), fieldOffset), rhs.src));
				
				return code;
			}
		
			code.addAll(rhs.code);
			
			code.add(new IR.Move(new IR.Id(id.nm), rhs.src));
		}
		else if (n.lhs instanceof Ast.Field)
		{
			AddrPack addrPack = genAddr(n.lhs, cinfo, env);
			
			code.addAll(addrPack.code);
			
			code.add(new IR.Store(addrPack.type, addrPack.addr, rhs.src));
		}
		else
		{
			throw new GenException("Ast.Assign: Unhandled Exp type: " + n.lhs.getClass().getCanonicalName() + ".");
		}
		
		return code;
	}
	
	// CallStmt ---
	// Exp obj;
	// String nm;
	// Exp[] args;
	//
	//
	static List<IR.Inst> gen(Ast.CallStmt n, ClassInfo cinfo, Env env)
			throws Exception
	{
		if (n.obj != null)
		{
			CodePack p = handleCall(n.obj, n.nm, n.args, cinfo, env, false);
			return p.code;
		}
		throw new GenException("In CallStmt, obj is null " + n);
	}
	
	// handleCall
	// ----------
	// Common routine for Call and CallStmt nodes
	//
	// Codegen Guideline:
	// 1. Invoke gen() on obj, which returns obj's storage address (and type and
	// code)
	// 2. Call getClassInfo() on obj to get base ClassInfo
	// 3. Access the base class's ClassInfo rec to get the method's offset in
	// vtable
	// 4. Add obj's as the 0th argument to the args list
	// 5. Generate an IR.Load to get the class descriptor from obj's storage
	// 6. Generate another IR.Load to get the method's global label
	// 7. If retFlag is set, prepare a temp for receiving return value; also
	// figure
	// out return value's type (through method's decl in ClassInfo rec)
	// 8. Generate an indirect call with the global label
	//
	static CodePack handleCall(Ast.Exp obj, String name, Ast.Exp[] args,
			ClassInfo cinfo, Env env, boolean retFlag) throws Exception
	{
		
		CodePack objPack = gen(obj, cinfo, env);
		
		/*ClassInfo objInfo = getClassInfo(obj, cinfo, env);
		int methodOffset = objInfo.methodOffset(name);
		
		String globalLabel = objInfo.className() + "_" + name;
		
		Dest dest = (objPack.src instanceof IR.Id) ? (IR.Id) objPack.src : (IR.Temp) objPack.src;  
		
		// TODO FIX THIS
		objPack.code.add(new IR.Load(IR.Type.PTR, dest, new IR.Addr(thisObj, methodOffset)));
		
		Ast.Exp[] argsWithObj = new Ast.Exp[args.length + 1];
		argsWithObj[0] = new Ast.Id(((IR.Id) objPack.src).name);
		for (int i = 0; i < args.length; i++)
		{
			argsWithObj[i + 1] = args[i];
		}*/
		
		// Make a temp get the object's base location.
		IR.Temp objTemp = new IR.Temp();
		objPack.code.add(new IR.Load(IR.Type.PTR, objTemp, new IR.Addr(objPack.src, 0)));
		
		ClassInfo objInfo = getClassInfo(obj, cinfo, env);
		int methodOffset = objInfo.methodOffset(name);
		
		// Now the method location.
		IR.Temp methodTemp = new IR.Temp();
		objPack.code.add(new IR.Load(IR.Type.PTR, methodTemp, new IR.Addr(objTemp, methodOffset)));
		
		// Get the arguments.
		IR.Src[] methodArgs = getMethodCallArgs(args, objPack.src);
		
		// TODO set dest, if necessary.
		IR.Dest dest = retFlag ? new IR.Temp() : null;
		
		// Now generate the call.
		objPack.code.add(new IR.Call(methodTemp, true, methodArgs, dest));
		
		objPack.src = retFlag ? (IR.Temp) dest : null;
		
		return objPack;
	}
	
	private static IR.Src[] getMethodCallArgs(Ast.Exp[] args, IR.Src self) throws Exception
	{
		List<IR.Src> methodArgs = new ArrayList<IR.Src>();
		
		// Always add a argument for the object itself.
		methodArgs.add(self);
		
		for (int i = 0; i < args.length; i++)
		{
			methodArgs.add(expToSrc(args[i]));
		}
		
		return (IR.Src[]) methodArgs.toArray(new IR.Src[0]);
	}
	
	// If ---
	// Exp cond;
	// Stmt s1, s2;
	//
	// (See class notes.)
	//
	static List<IR.Inst> gen(Ast.If n, ClassInfo cinfo, Env env)
			throws Exception
	{
		List<IR.Inst> code = new ArrayList<IR.Inst>();
		IR.Label L1 = new IR.Label();
		
		CodePack p = gen(n.cond, cinfo, env);
		code.addAll(p.code);
		code.add(new IR.CJump(IR.ROP.EQ, p.src, new IR.BoolLit(false), L1));
		code.addAll(gen(n.s1, cinfo, env));
		
		if (n.s2 == null)
		{
			code.add(new IR.LabelDec(L1));
		}
		else
		{
			IR.Label L2 = new IR.Label();
			code.add(new IR.Jump(L2));
			code.add(new IR.LabelDec(L1));
			code.addAll(gen(n.s2, cinfo, env));
			code.add(new IR.LabelDec(L2));
		}
		
		return code;
		
	}
	
	// While ---
	// Exp cond;
	// Stmt s;
	//
	// (See class notes.)
	//
	static List<IR.Inst> gen(Ast.While n, ClassInfo cinfo, Env env)
			throws Exception
	{
		CodePack condPack = gen(n.cond, cinfo, env);
		
		// Define a loop label (to get back to the start).
		IR.Label loopLabel = new IR.Label();
		
		// An exit loop label.
		IR.Label exitLabel = new IR.Label();
		
		// Then the condition check.
		condPack.code.add(new IR.LabelDec(loopLabel));
		condPack.code.add(new IR.CJump(IR.ROP.EQ, condPack.src, new IR.BoolLit(false), exitLabel));
		
		// The code within the while.
		condPack.code.addAll(gen(n.s, cinfo, env));
		
		// Go back to the start.
		condPack.code.add(new IR.Jump(loopLabel));
		
		// Then the exit label.
		condPack.code.add(new IR.LabelDec(exitLabel));
		
		return condPack.code;
	}
	
	// Print ---
	// PrArg arg;
	//
	// Codegen Guideline:
	// 1. If arg is null, generate an IR.Call to "printStr" with an empty string
	// arg
	// 2. If arg is StrLit, generate an IR.Call to "printStr"
	// 3. Otherwise, generate IR code for arg, and use its type info
	// to decide which of the two functions, "printInt" and "printBool",
	// to call
	//
	static List<IR.Inst> gen(Ast.Print n, ClassInfo cinfo, Env env) throws Exception
	{
		List<IR.Inst> code = new ArrayList<IR.Inst>();

		if (n.arg == null || n.arg instanceof Ast.StrLit)
		{
			IR.CallTgt printStr = new IR.Global("printStr");
			IR.Src[] strArgs = new IR.Src[1];
			
			if (n.arg != null)
			{
				strArgs[0] = new IR.StrLit(((Ast.StrLit) n.arg).s);
			}
			else
			{
				strArgs[0] = new IR.StrLit("");
			}
			
			code.add(new IR.Call(printStr, false, strArgs, null));
			
			return code;
		}
		else if (n.arg instanceof Ast.BoolLit)
		{
			IR.CallTgt printBool = new IR.Global("printBool");
			IR.Src[] boolArgs = new IR.Src[1];
			
			boolArgs[0] = new IR.BoolLit(((Ast.BoolLit) n.arg).b);
			
			code.add(new IR.Call(printBool, false, boolArgs, null));
			
			return code;
		}
		else if (n.arg instanceof Ast.IntLit)
		{
			IR.CallTgt printInt = new IR.Global("printInt");
			IR.Src[] intArgs = new IR.Src[1];
			
			intArgs[0] = new IR.IntLit(((Ast.IntLit) n.arg).i);
			
			code.add(new IR.Call(printInt, false, intArgs, null));
			
			return code;
		}
		else if (n.arg instanceof Ast.Id)
		{
			IR.Id id = new IR.Id(((Ast.Id) n.arg).nm);
			
			if (!env.containsKey(id.name))
			{
				return gen(new Ast.Print(new Ast.Field(Ast.This, id.name)), cinfo, env);
			}
			
			IR.CallTgt print = new IR.Global(getPrintFunction(env, cinfo, id.name));
			IR.Src[] printArgs = new IR.Src[1];
			
			printArgs[0] = id;
			
			code.add(new IR.Call(print, false, printArgs, null));
			
			return code;
		}
		else if (n.arg instanceof Ast.Field)
		{
			Ast.Field field = (Ast.Field) n.arg;
			ClassInfo objInfo = getClassInfo(field.obj, cinfo, env);
			
			IR.CallTgt print = new IR.Global(getPrintFunction(env, objInfo, field.nm));
			IR.Src[] printArgs = new IR.Src[1];
			
			// TODO Make this not such a damn mess.
			IR.Temp temp = new IR.Temp();
			
			code.add(new IR.Load(
					gen(objInfo.fieldType(field.nm)),
					temp,
					new IR.Addr(expToSrc(field.obj),
					objInfo.fieldOffset(field.nm))));
			
			printArgs[0] = temp;
			
			code.add(new IR.Call(print, false, printArgs, null));
			
			return code;
		}
		else if (n.arg instanceof Ast.Call)
		{
			CodePack call = gen((Ast.Call) n.arg, cinfo, env);
			
			code.addAll(call.code);
			
			// TODO print the temporary
			IR.Src[] printArgs = { call.src };
			
			IR.CallTgt print = new IR.Global(getPrintFunction(call.type));
			
			code.add(new IR.Call(print, false, printArgs, null));
			
			return code;
		}
		
		throw new GenException("Ast.Print: Unknown type: " + n.arg.getClass().getCanonicalName() + ".");
	}
	
	private static String getPrintFunction(IR.Type type) throws Exception
	{
		if (type == IR.Type.BOOL)
		{
			return "printBool";
		}
		else if (type == IR.Type.INT || type == IR.Type.PTR)
		{
			return "printInt";
		}
		else
		{
			throw new GenException("GetPrintFunction(IR.Type): Unknown type: " + type.getClass().getCanonicalName() + ".");
		}
	}
	
	private static String getPrintFunction(Env env, ClassInfo cinfo, String name) throws Exception
	{
		Ast.Type type = env.get(name) != null ? env.get(name) : cinfo.fieldType(name);
		if (type instanceof Ast.BoolType)
		{
			return "printBool";
		}
		else if (type instanceof Ast.IntType || type instanceof Ast.ArrayType || type instanceof Ast.ObjType)
		{
			return "printInt";
		}
		else if (type == null)
		{
			throw new GenException("GetPrintFunction: Type is null.");
		}
		
		
		throw new GenException("GetPrintFunction: Unknown type: " + type.getClass().getCanonicalName() + ".");
	}
	
	// Return ---
	// Exp val;
	//
	// Codegen Guideline:
	// 1. If val is non-null, generate IR code for it, and generate an IR.Return
	// with its value
	// 2. Otherwise, generate an IR.Return with no value
	//
	static List<IR.Inst> gen(Ast.Return n, ClassInfo cinfo, Env env) throws Exception
	{
		if (n.val == null)
		{
			List<IR.Inst> code = new ArrayList<IR.Inst>();
			
			code.add(new IR.Return());
			
			return code;
		}
		
		CodePack pack = gen(n.val, cinfo, env);
		
		pack.code.add(new IR.Return(pack.src));
		
		return pack.code;
	}
	
	// EXPRESSIONS
	
	// 1. Dispatch a generic gen() call to a specific gen() routine
	//
	static CodePack gen(Ast.Exp n, ClassInfo cinfo, Env env) throws Exception
	{
		if (n instanceof Ast.Call)
			return gen((Ast.Call) n, cinfo, env);
		if (n instanceof Ast.NewObj)
			return gen((Ast.NewObj) n, cinfo, env);
		if (n instanceof Ast.Field)
			return gen((Ast.Field) n, cinfo, env);
		if (n instanceof Ast.Id)
			return gen((Ast.Id) n, cinfo, env);
		if (n instanceof Ast.This)
			return gen((Ast.This) n, cinfo);
		if (n instanceof Ast.IntLit)
			return gen((Ast.IntLit) n);
		if (n instanceof Ast.BoolLit)
			return gen((Ast.BoolLit) n);
		throw new GenException("Exp node not supported in this codegen: " + n);
	}
	
	// 2. Dispatch a generic genAddr call to a specific genAddr routine
	// (Only one LHS Exp needs to be implemented for this assignment)
	//
	static AddrPack genAddr(Ast.Exp n, ClassInfo cinfo, Env env)
			throws Exception
	{
		if (n instanceof Ast.Field)
			return genAddr((Ast.Field) n, cinfo, env);
		throw new GenException(" LHS Exp node not supported in this codegen: "
				+ n);
	}
	
	// Call ---
	// Exp obj;
	// String nm;
	// Exp[] args;
	//
	static CodePack gen(Ast.Call n, ClassInfo cinfo, Env env) throws Exception
	{
		if (n.obj != null)
			return handleCall(n.obj, n.nm, n.args, cinfo, env, true);
		throw new GenException("In Call, obj is null: " + n);
	}
	
	// NewObj ---
	// String nm;
	//
	// Codegen Guideline:
	// 1. Use class name to find the corresponding ClassInfo record from
	// classEnv
	// 2. Find the class's type and object size from the ClassInfo record
	// 3. Cosntruct a malloc call to allocate space for the object
	// 4. Store a pointer to the class's descriptor into the first slot of
	// the allocated space
	//
	static CodePack gen(Ast.NewObj n, ClassInfo cinfo, Env env) throws Exception
	{
		List<IR.Inst> code = new ArrayList<>();
		
		ClassInfo newClassInfo = classEnv.get(n.nm);
		
		Ast.Type newClassType = new Ast.ObjType(n.nm);
		int newClassSize = newClassInfo.objSize;
		
		// Invoke malloc.
		IR.Global mallocGlobal = new IR.Global("malloc");
		IR.Temp temp = new IR.Temp();
		IR.Call malloc = new IR.Call(mallocGlobal, false, new IR.Src[] { new IR.IntLit(newClassSize) }, temp);
		code.add(malloc);
		
		// TODO I know temp should not be the last thing... it should store the class's descriptor.
		IR.Global classGlobal = new IR.Global("class_" + n.nm);
		IR.Store store = new IR.Store(gen(newClassType), new IR.Addr(temp, 0), classGlobal);
		code.add(store);
		
		return new CodePack(gen(newClassType), temp, code);
		
	}
	
	// Field ---
	// Exp obj;
	// String nm;
	//
	
	// 1. gen()
	//
	// Codegen Guideline:
	// 1.1 Call genAddr() to generate field variable's address
	// 1.2 Add an IR.Load to get its value
	//
	static CodePack gen(Ast.Field n, ClassInfo cinfo, Env env) throws Exception
	{
		AddrPack addrPack = genAddr(n, cinfo, env);
		
		IR.Temp temp = new IR.Temp();
		IR.Load load = new IR.Load(addrPack.type, temp, addrPack.addr);
		addrPack.code.add(load);
		
		return new CodePack(addrPack.type, temp, addrPack.code);
	}
	
	// 2. genAddr()
	//
	// Codegen Guideline:
	// 2.1 Call gen() on the obj component
	// 2.2 Call getClassInfo() on the obj component to get base ClassInfo
	// 2.3 Access base ClassInfo rec to get field variable's offset
	// 2.4 Generate an IR.Addr based on the offset
	//
	static AddrPack genAddr(Ast.Field n, ClassInfo cinfo, Env env) throws Exception
	{
		CodePack objPack = gen(n.obj, cinfo, env);
		ClassInfo objInfo = getClassInfo(n.obj, cinfo, env);
		
		Ast.Type fieldType = objInfo.fieldType(n.nm);
		int fieldOffset = objInfo.fieldOffset(n.nm);
	
		// TODO Unsure about this...
		IR.Src src = expToSrc(n.obj);
		IR.Addr addr = new Addr(src, fieldOffset);
		
		return new AddrPack(gen(fieldType), addr, objPack.code);
	}
	
	// Id ---
	// String nm;
	//
	// Codegen Guideline:
	// 1. Check to see if the Id is in the env.
	// 2. If so, it means it is a local variable or a parameter. Just return
	// a CodePack containing the Id.
	// 3. Otherwise, the Id is an instance variable. Convert it into an
	// Ast.Field node with Ast.This() as its obj, and invoke the gen() routine
	// on this new node
	//
	static CodePack gen(Ast.Id n, ClassInfo cinfo, Env env) throws Exception
	{
		// If the variable name exists in the environment, it's local/parameter.
		if (env.containsKey(n.nm))
		{
			IR.Src id = new IR.Id(n.nm);
			
			return new CodePack(gen(env.get(n.nm)), id, new ArrayList<>());
		}
		
		// Otherwise repackage this as a field, and call gen on it's Field representation.
		Ast.Field field = new Ast.Field(Ast.This, n.nm);
		
		return gen(field, cinfo, env);
	}
	
	// This ---
	//
	static CodePack gen(Ast.This n, ClassInfo cinfo) throws Exception
	{
		return new CodePack(IR.Type.PTR, thisObj);
	}
	
	// IntLit ---
	// int i;
	//
	static CodePack gen(Ast.IntLit n) throws Exception
	{
		return new CodePack(IR.Type.INT, new IR.IntLit(n.i));
	}
	
	// BoolLit ---
	// boolean b;
	//
	static CodePack gen(Ast.BoolLit n) throws Exception
	{
		return new CodePack(IR.Type.BOOL, n.b ? IR.TRUE : IR.FALSE);
	}
	
	// StrLit ---
	// String s;
	//
	static CodePack gen(Ast.StrLit n) throws Exception
	{
		return new CodePack(null, new IR.StrLit(n.s));
	}
	
	// Type mapping (AST -> IR)
	//
	static IR.Type gen(Ast.Type n) throws Exception
	{
		if (n == null)
			return null;
		if (n instanceof Ast.IntType)
			return IR.Type.INT;
		if (n instanceof Ast.BoolType)
			return IR.Type.BOOL;
		if (n instanceof Ast.ArrayType)
			return IR.Type.PTR;
		if (n instanceof Ast.ObjType)
			return IR.Type.PTR;
		throw new GenException("Invalid Ast type: " + n);
	}
	
	static IR.Src expToSrc(Ast.Exp e) throws Exception
	{
		if (e instanceof Ast.Id)
		{
			return new IR.Id(((Ast.Id) e).nm);
		}
		else if (e instanceof Ast.IntLit)
		{
			return new IR.IntLit(((Ast.IntLit) e).i);
		}
		else if (e instanceof Ast.This)
		{
			return new IR.Id("obj");
		}
		
		throw new GenException("expToSrc: Unhandled expression type: "+ e.getClass().getName());
	}
}

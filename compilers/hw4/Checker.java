// This is supporting software for CS322 Compilers and Language Design II
// Copyright (c) Portland State University
//
// Static analysis for miniJava (F14) ((A starting version.)
//  1. Type-checking
//  2. Detecting missing return statement
//  3. (Optional) Detecting uninitialized variables
//
// (For CS321 Fall 2014 - Jingke Li)
//

import java.util.*;
import java.io.*;

import ast.*;

public class Checker
{

    static class TypeException extends Exception
    {
        public TypeException(String msg)
        {
            super(msg);
        }
    }

    //------------------------------------------------------------------------------
    // ClassInfo
    //----------
    // For easy access to class hierarchies (i.e. finding parent's info).
    //
    static class ClassInfo
    {
        Ast.ClassDecl cdecl;    // classDecl AST
        ClassInfo parent;        // pointer to parent

        ClassInfo(Ast.ClassDecl cdecl, ClassInfo parent)
        {
            this.cdecl = cdecl;
            this.parent = parent;
        }

        // Return the name of this class
        //
        String className()
        {
            return cdecl.nm;
        }

        // Given a method name, return the method's declaration
        // - if the method is not found in the current class, recursively
        //   search ancestor classes; return null if all fail
        //
        Ast.MethodDecl findMethodDecl(String mname)
        {
            for (Ast.MethodDecl mdecl : cdecl.mthds)
            {
                if (mdecl.nm.equals(mname))
                {
                    return mdecl;
                }
            }
            if (parent != null)
            {
                return parent.findMethodDecl(mname);
            }
            return null;
        }

        // Given a field name, return the field's declaration
        // - if the field is not found in the current class, recursively
        //   search ancestor classes; return null if all fail
        //
        Ast.VarDecl findFieldDecl(String fname)
        {
            for (Ast.VarDecl fdecl : cdecl.flds)
            {
                if (fdecl.nm.equals(fname))
                {
                    return fdecl;
                }
            }
            if (parent != null)
            {
                return parent.findFieldDecl(fname);
            }
            return null;
        }
    }

    //------------------------------------------------------------------------------
    // Global Variables
    // ----------------
    // For type-checking:
    // classEnv - an environment (a className-classInfo mapping) for class declarations
    // typeEnv - an environment (a var-type mapping) for a method's params and local vars
    // thisCInfo - points to the current class's ClassInfo
    // thisMDecl - points to the current method's MethodDecl
    //
    // For other analyses:
    // (Define as you need.)
    //
    private static HashMap<String, ClassInfo> classEnv = new HashMap<String, ClassInfo>();
    private static HashMap<String, Ast.Type> typeEnv = new HashMap<String, Ast.Type>();
    private static ClassInfo thisCInfo = null;
    private static Ast.MethodDecl thisMDecl = null;

    //------------------------------------------------------------------------------
    // Type Compatibility Routines
    // ---------------------------

    // Returns true if tsrc is assignable to tdst.
    //
    // Pseudo code:
    //   if tdst==tsrc or both are the same basic type
    //     return true
    //   else if both are ArrayType // structure equivalence
    //     return assignable result on their element types
    //   else if both are ObjType   // name equivalence
    //     if (their class names match, or
    //         tdst's class name matches an tsrc ancestor's class name)
    //       return true
    //   else
    //     return false
    //
    private static boolean assignable(Ast.Type tdst, Ast.Type tsrc) throws Exception
    {
        if (tdst == tsrc || (tdst instanceof Ast.IntType) && (tsrc instanceof Ast.IntType) || (tdst instanceof Ast
                .BoolType) && (tsrc instanceof Ast.BoolType))
        {
            return true;
        }

        // ... need code ...

    }

    // Returns true if t1 and t2 can be compared with "==" or "!=".
    //
    private static boolean comparable(Ast.Type t1, Ast.Type t2) throws Exception
    {
        return assignable(t1, t2) || assignable(t2, t1);
    }

    //------------------------------------------------------------------------------
    // The Main Routine
    //-----------------
    //
    public static void main(String[] args) throws Exception
    {
        try
        {
            if (args.length == 1)
            {
                FileInputStream stream = new FileInputStream(args[0]);
                Ast.Program p = new astParser(stream).Program();
                stream.close();
                check(p);
            } else
            {
                System.out.println("Need a file name as command-line argument.");
            }
        }
        catch (TypeException e)
        {
            System.err.print(e + "\n");
        }
        catch (Exception e)
        {
            System.err.print(e + "\n");
        }
    }

    //------------------------------------------------------------------------------
    // Checker Routines for Individual AST Nodes
    //------------------------------------------

    // Program ---
    //  ClassDecl[] classes;
    //
    // 1. Sort ClassDecls, so parent will be visited before children.
    // 2. For each ClassDecl, create an ClassInfo (with link to parent if exists),
    //    and add to classEnv.
    // 3. Actual type-checking traversal over ClassDecls.
    //
    static void check(Ast.Program n) throws Exception
    {
        Ast.ClassDecl[] classes = topoSort(n.classes);
        for (Ast.ClassDecl c : classes)
        {
            ClassInfo pcinfo = (c.pnm == null) ? null : classEnv.get(c.pnm);
            classEnv.put(c.nm, new ClassInfo(c, pcinfo));
        }
        for (Ast.ClassDecl c : classes)
        {
            check(c);
        }
    }

    // Utility routine
    // - Sort ClassDecls based on parent-chidren relationship.
    //
    private static Ast.ClassDecl[] topoSort(Ast.ClassDecl[] classes)
    {
        List<Ast.ClassDecl> cl = new ArrayList<Ast.ClassDecl>();
        Vector<String> done = new Vector<String>();
        int cnt = classes.length;
        while (cnt > 0)
        {
            for (Ast.ClassDecl cd : classes)
            {
                if (!done.contains(cd.nm) && ((cd.pnm == null) || done.contains(cd.pnm)))
                {
                    cl.add(cd);
                    done.add(cd.nm);
                    cnt--;
                }
            }
        }
        return cl.toArray(new Ast.ClassDecl[0]);
    }

    // ClassDecl ---
    //  String nm, pnm;
    //  VarDecl[] flds;
    //  MethodDecl[] mthds;
    //
    //  1. Set thisCInfo pointer to this class's ClassInfo, and reset
    //     typeEnv to empty.
    //  2. Recursively check n.flds and n.mthds.
    //
    static void check(Ast.ClassDecl n) throws Exception
    {

        // Construct the ClassInfo instance.
        ClassInfo classInfo = new ClassInfo(n, classEnv.get(n.pnm));
        thisCInfo = classInfo;

        // Reset the type environment.
        typeEnv = new HashMap<String, Ast.Type>();

        // TODO probably need to invoke check on each property, in both, down the chain.
        for (VarDecl varDecl : n.flds)
        {
            check(varDecl);
        }

        for (MethodDecl methodDecl : n.mthds)
        {
            check(methodDecl);
        }

    }

    // MethodDecl ---
    //  Type t;
    //  String nm;
    //  Param[] params;
    //  VarDecl[] vars;
    //  Stmt[] stmts;
    //
    //  1. Set thisMDecl pointer and reset typeEnv to empty.
    //  2. Recursively check n.params, n.vars, and n.stmts.
    //  3. For each VarDecl, add a new name-type binding to typeEnv.
    //
    static void check(Ast.MethodDecl n) throws Exception
    {

        thisMDecl = n;
        typeEnv = new HashMap<String, Ast.Type>();

        for (Param p : n.params)
        {
            check(p);
        }

        for (VarDecl v : n.vars)
        {
            check(v);
            typeEnv.put(v.nm, v.t);
        }

        for (Stmt s : n.stmts)
        {
            check(s);
        }
    }

    // Param ---
    //  Type t;
    //  String nm;
    //
    //  If n.t is ObjType, make sure its corresponding class exists.
    //
    static void check(Ast.Param n) throws Exception
    {

        if (!(n.t instanceof Ast.ObjType))
        {
            return;
        }

        if (!classEnv.containsKey(n.t.nm))
        {
            throw new TypeException(n.nm + " attempted to use " + n.t.nm + " class which does not exist.");
        }
    }

    // VarDecl ---
    //  Type t;
    //  String nm;
    //  Exp init;
    //
    //  1. If n.t is ObjType, make sure its corresponding class exists.
    //  2. If n.init exists, make sure it is assignable to the var.
    //
    static void check(Ast.VarDecl n) throws Exception
    {

        if (n.t instanceof Ast.ObjType && !classEnv.containsKey(((Ast.ObjType) n.t).nm))
        {
            throw new TypeException(n.nm + " is defined as a class " + ((Ast.ObjType) n.t).nm + " which is not " +
                    "defined.");
        }

        if (n.init == null)
        {
            return;
        }

        if (!assignable(n.t, (Ast.Type) n.init))
        {
            throw new TypeException("Not assignable");
        }
    }

    // STATEMENTS

    // Dispatch a generic check call to a specific check routine
    //
    static void check(Ast.Stmt n) throws Exception
    {
        if (n instanceof Ast.Block)
        {
            check((Ast.Block) n);
        } else if (n instanceof Ast.Assign)
        {
            check((Ast.Assign) n);
        } else if (n instanceof Ast.CallStmt)
        {
            check((Ast.CallStmt) n);
        } else if (n instanceof Ast.If)
        {
            check((Ast.If) n);
        } else if (n instanceof Ast.While)
        {
            check((Ast.While) n);
        } else if (n instanceof Ast.Print)
        {
            check((Ast.Print) n);
        } else if (n instanceof Ast.Return)
        {
            check((Ast.Return) n);
        } else
        {
            throw new TypeException("Illegal Ast Stmt: " + n);
        }
    }

    // Block ---
    //  Stmt[] stmts;
    //
    static void check(Ast.Block n) throws Exception
    {

        if (n.stmts.length == 0)
        {
            return;
        }

        for (Stmt s : n.stmts)
        {
            check(s);
        }
    }

    // Assign ---
    //  Exp lhs;
    //  Exp rhs;
    //
    //  Make sure n.rhs is assignable to n.lhs.
    //
    static void check(Ast.Assign n) throws Exception
    {

        // ... need code ...

    }

    // CallStmt ---
    //  Exp obj;
    //  String nm;
    //  Exp[] args;
    //
    //  1. Check that n.obj is ObjType and the corresponding class exists.
    //  2. Check that the method n.nm exists.
    //  3. Check that the count and types of the actual arguments match those of
    //     the formal parameters.
    //
    static void check(Ast.CallStmt n) throws Exception
    {

        // ... need code ...

    }

    // If ---
    //  Exp cond;
    //  Stmt s1, s2;
    //
    //  Make sure n.cond is boolean.
    //
    static void check(Ast.If n) throws Exception
    {

        // ... need code ...

    }

    // While ---
    //  Exp cond;
    //  Stmt s;
    //
    //  Make sure n.cond is boolean.
    //
    static void check(Ast.While n) throws Exception
    {

        // ... need code ...

    }

    // Print ---
    //  PrArg arg;
    //
    //  Make sure n.arg is integer, boolean, or string.
    //
    static void check(Ast.Print n) throws Exception
    {

        // ... need code ...

    }

    // Return ---
    //  Exp val;
    //
    //  If n.val exists, make sure it matches the expected return type.
    //
    static void check(Ast.Return n) throws Exception
    {

        // ... need code ...

    }

    // EXPRESSIONS

    // Dispatch a generic check call to a specific check routine
    //
    static Ast.Type check(Ast.Exp n) throws Exception
    {
        if (n instanceof Ast.Binop)
        {
            return check((Ast.Binop) n);
        }
        if (n instanceof Ast.Unop)
        {
            return check((Ast.Unop) n);
        }
        if (n instanceof Ast.Call)
        {
            return check((Ast.Call) n);
        }
        if (n instanceof Ast.NewArray)
        {
            return check((Ast.NewArray) n);
        }
        if (n instanceof Ast.ArrayElm)
        {
            return check((Ast.ArrayElm) n);
        }
        if (n instanceof Ast.NewObj)
        {
            return check((Ast.NewObj) n);
        }
        if (n instanceof Ast.Field)
        {
            return check((Ast.Field) n);
        }
        if (n instanceof Ast.Id)
        {
            return check((Ast.Id) n);
        }
        if (n instanceof Ast.This)
        {
            return check((Ast.This) n);
        }
        if (n instanceof Ast.IntLit)
        {
            return check((Ast.IntLit) n);
        }
        if (n instanceof Ast.BoolLit)
        {
            return check((Ast.BoolLit) n);
        }
        throw new TypeException("Exp node not recognized: " + n);
    }

    // Binop ---
    //  BOP op;
    //  Exp e1,e2;
    //
    //  Make sure n.e1's and n.e2's types are legal with respect to n.op.
    //
    static Ast.Type check(Ast.Binop n) throws Exception
    {

        // ... need code ...

    }

    // Unop ---
    //  UOP op;
    //  Exp e;
    //
    //  Make sure n.e's type is legal with respect to n.op.
    //
    static Ast.Type check(Ast.Unop n) throws Exception
    {

        // ... need code ...

    }

    // Call ---
    //  Exp obj;
    //  String nm;
    //  Exp[] args;
    //
    //  (See the hints in CallStmt.)
    //  In addition, this routine needs to return the method's return type.
    //
    static Ast.Type check(Ast.Call n) throws Exception
    {

        // ... need code ...

    }

    // NewArray ---
    //  Type et;
    //  int len;
    //
    //  1. Verify that n.et is either integer or boolean.
    //  2. Verify that n.len is non-negative.
    //  (Note: While the AST representation allows these cases to happen, our
    //  miniJava parser does not, so these checks are not very meaningful.)
    //
    static Ast.Type check(Ast.NewArray n) throws Exception
    {

        if (!(n.et instanceof Ast.IntLit) && !(n.et instanceof Ast.BoolLit))
        {
            throw new TypeException("Must be integer or bool.");
        }

        if (n.len < 0)
        {
            throw new TypeException("Length must be non-negative.");
        }

        return n.et;
    }

    // ArrayElm ---
    //  Exp ar, idx;
    //
    //  Varify that n.ar is array and n.idx is integer.
    //
    static Ast.Type check(Ast.ArrayElm n) throws Exception
    {

        // TODO verify this...
        if (!(n.ar instanceof Ast.ArrayType))
        {
            throw new TypeException("not an array");
        }

        if (!(n.idx instanceof Ast.IntLit))
        {
            throw new TypeException("expecting an integer");
        }

        return n.ar;
    }

    // NewObj ---
    //  String nm;
    //
    //  Verify that the corresponding class exists.
    //
    static Ast.Type check(Ast.NewObj n) throws Exception
    {

        if (!classEnv.containsKey(n.nm))
        {
            throw new TypeException(n.nm + " is not a defined class.");
        }

        Ast.ObjType objType = classEnv.get(n.nm);

        return objType;
    }

    // Field ---
    //  Exp obj;
    //  String nm;
    //
    //  1. Verify that n.obj is ObjType, and its corresponding class exists.
    //  2. Verify that n.nm is a valid field in the object.
    //
    static Ast.Type check(Ast.Field n) throws Exception
    {

        // TODO: n.nm is the name of the variable... not the type.
        if (!(n.obj instanceof Ast.ObjType))
        {
            throw new TypeException(n.nm + " is not an ObjType");
        }

        Ast.ObjType objType = (Ast.ObjType) n.obj;
        if (!classEnv.containsKey(objType.nm))
        {
            throw new TypeException(objType.nm + " is not a defined class, for variable " + n.nm);
        }

        Ast.VarDecl fieldDecl = classEnv.get(n.nm);

        if (fieldDecl == null)
        {
            throw new TypeException(n.nm + " is not defined on the class.");
        }

        return objType;
    }

    // Id ---
    //  String nm;
    //
    //  1. Check if n.nm is in typeEnv. If so, the Id is a param or a local var.
    //     Obtain and return its type.
    //  2. Otherwise, the Id is a field variable. Find and return its type (through
    //     the current ClassInfo).
    //
    static Ast.Type check(Ast.Id n) throws Exception
    {

        // First check if it exists within typeEnv.
        if (typeEnv.containsKey(n.nm))
        {
            return typeEnv.get(n.nm);
        }

        // It might be an attribute (field) on the class.
        Ast.VarDecl fieldDecl = thisCInfo.findFieldDecl(n.nm);

        if (fieldDecl == null)
        {
            throw new TypeException(n.nm + " variable not found.");
        }

        return fieldDecl.t;
    }

    // This ---
    //
    //  Find and return an ObjType that corresponds to the current class
    //  (through the current ClassInfo).
    //
    static Ast.Type check(Ast.This n)
    {

        // This shouldn't be null.
        if (thisCInfo == null)
        {
            throw new TypeException("Referencing this when none exists.");
        }

        // Otherwise we're going to return an ObjType instance with the name of the class.
        Ast.ObjType objType = new Ast.ObjType(thisCInfo.className());

        return objType;
    }

    // Literals
    //
    public static Ast.Type check(Ast.IntLit n)
    {
        return Ast.IntType;
    }

    public static Ast.Type check(Ast.BoolLit n)
    {
        return Ast.BoolType;
    }

    public static void check(Ast.StrLit n)
    {
        // nothing to check or return
    }

}

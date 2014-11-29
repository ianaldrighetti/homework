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

import java.lang.IllegalArgumentException;
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
    private static int returnStmtCount = 0;

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
        else if (tdst instanceof Ast.ArrayType && tsrc instanceof Ast.ArrayType)
        {
            return assignable(((Ast.ArrayType)tdst).et, ((Ast.ArrayType)tsrc).et);
        }
        else if (tdst instanceof Ast.ObjType && tsrc instanceof Ast.ObjType)
        {
            ClassInfo pClassInfo = classEnv.get(((Ast.ObjType)tsrc).nm);
            return hasAncestor(pClassInfo, ((Ast.ObjType)tdst).nm);
        }

        return false;
    }

    // Returns true if t1 and t2 can be compared with "==" or "!=".
    //
    private static boolean comparable(Ast.Type t1, Ast.Type t2) throws Exception
    {
        return assignable(t1, t2) || assignable(t2, t1);
    }

    /**
     * Determines whether the parent is the same class as childName, or if any parent of the parent matches.
     *
     * @param parent The parent ClassInfo.
     * @param childName Name of the child class.
     * @return Whether childName is a direct match or an ancestor of parent.
     */
    private static boolean hasAncestor(ClassInfo parent, String childName)
    {
        if (parent.className().equals(childName))
        {
            return true;
        }

        return parent.parent != null && hasAncestor(parent.parent, childName);

    }

    /**
     * Returns the name of the type. Example, if the type is Ast.IntType, IntType is returned. However, ArrayType and
     * ObjType will return: (ArrayType {Element Type}) for arrays and (ObjType {Class Name}) for objects.
     *
     * @param type The type instance.
     * @return The type name.
     */
    private static String getTypeName(Ast.Type type)
    {
        if (type instanceof Ast.ObjType)
        {
            return "(ObjType " + ((Ast.ObjType)type).nm + ")";
        }
        else if (type instanceof Ast.ArrayType)
        {
            return "(ArrayType " + getTypeName(((Ast.ArrayType)type).et) + ")";
        }

        String actualType = type.getClass().getCanonicalName();

        if (actualType.contains("."))
        {
            actualType = actualType.substring(actualType.lastIndexOf('.') + 1);
        }

        return actualType;
    }

    /**
     * Finds a variable declaration.
     *
     * @param varName
     * @return The type.
     * @throws Exception
     */
    private static Ast.Type getVarDef(String varName) throws Exception
    {
        // First check local variable definitions.
        if (typeEnv.containsKey(varName))
        {
            return typeEnv.get(varName);
        }

        // Now check for parameters.
        for (Ast.Param param : thisMDecl.params)
        {
            if (param.nm.equals(varName))
            {
                return param.t;
            }
        }

        // Finally, fields on the class itself.
        Ast.VarDecl field = thisCInfo.findFieldDecl(varName);

        if (field != null)
        {
            return field.t;
        }

        throw new TypeException("(In Id) Can't find variable " + varName);
    }

    /**
     * Returns the type of the expression. This resolves an Identifier to it's defined type.
     *
     * @param exp The expression.
     * @return The Expression type.
     */
    private static Ast.Type getExprType(Ast.Exp exp) throws Exception
    {
        // Exp: Binop, Unop, Call, NewArray, ArrayElm, NewObj, Field, Id, This, IntLit, BoolLit
        // Types: IntType, BoolType, ArrayType, ObjType.

        if (exp instanceof Ast.Binop)
        {
            Ast.Binop binop = (Ast.Binop) exp;
            Ast.Type e1Type = getExprType(binop.e1);
            Ast.Type e2Type = getExprType(binop.e2);

            if (e1Type instanceof Ast.IntType && e2Type instanceof Ast.IntType && (binop.op == Ast.BOP.ADD
                || binop.op == Ast.BOP.DIV || binop.op == Ast.BOP.MUL || binop.op == Ast.BOP.SUB))
            {
                return Ast.IntType;
            }

            return Ast.BoolType;
        }
        else if (exp instanceof Ast.Unop)
        {
            Ast.Unop unOp = (Ast.Unop)exp;
            return unOp.op == Ast.UOP.NOT ? Ast.BoolType : Ast.IntType;
        }
        else if (exp instanceof Ast.Call)
        {
            Ast.Call call = (Ast.Call)exp;

            ClassInfo callObj = classEnv.get(getClassNameFromExp(call.obj));

            if (callObj == null) {
                throw new TypeException("Class " + getClassNameFromExp(call.obj) + " does not exist.");
            }

            Ast.MethodDecl method = callObj.findMethodDecl(call.nm);

            if (method == null)
            {
                throw new TypeException("(In CallStmt) Can't find method " + call.nm);
            }

            return method.t;
        }
        else if (exp instanceof Ast.NewArray)
        {
            Ast.NewArray arr = (Ast.NewArray) exp;
            return new Ast.ArrayType(arr.et);
        }
        else if (exp instanceof Ast.ArrayElm)
        {
            Ast.ArrayElm arrElm = (Ast.ArrayElm)exp;
            String arrName = ((Ast.Id)arrElm.ar).nm;

            Ast.Type arrType = getVarDef(arrName);

            if (arrType == null)
            {
                throw new TypeException("Array " + arrName + " not defined.");
            }
            else if (!(arrType instanceof Ast.ArrayType))
            {
                throw new TypeException("(In ArrayElm) Object is not array: " + getTypeName(arrType));
            }

            return ((Ast.ArrayType)arrType).et;
        }
        else if (exp instanceof Ast.NewObj)
        {
            Ast.NewObj newObj = (Ast.NewObj)exp;

            return new Ast.ObjType(newObj.nm);
        }
        else if (exp instanceof Ast.Field)
        {
            Ast.Field field = (Ast.Field)exp;
            String className = getClassNameFromExp(field.obj);

            Ast.Exp objExp = field.obj;
            while(objExp instanceof Ast.Field)
            {
                Ast.Type nestedType = getExprType(objExp);

                if (nestedType instanceof Ast.ObjType)
                {
                    className = ((Ast.ObjType)nestedType).nm;
                }

                objExp = ((Ast.Field)objExp).obj;
            }

            ClassInfo classInfo = classEnv.get(className);

            if (classInfo == null) {
                throw new TypeException(className + " not defined class.");
            }

            Ast.VarDecl varDecl = classInfo.findFieldDecl(field.nm);

            if (varDecl == null)
            {
                throw new TypeException("(In Field) Can't find field " + field.nm);
            }

            return varDecl.t;
        }
        else if (exp instanceof Ast.Id)
        {
            Ast.Id id = (Ast.Id)exp;

            Ast.Type type = getVarDef(id.nm);

            if (type == null) {
                throw new TypeException("(In Id) Can't find variable " + id.nm);
            }

            return type;
        }
        else if (exp instanceof Ast.This)
        {
            return new Ast.ObjType(thisCInfo.className());
        }
        else if (exp instanceof Ast.IntLit)
        {
            return Ast.IntType;
        }
        else if (exp instanceof Ast.BoolLit)
        {
            return Ast.BoolType;
        }
        else
        {
            throw new IllegalArgumentException("Unknown expression: " + exp.getClass().getName());
        }
    }

    /**
     * Returns the name of the class from an expression.
     *
     * @param callObj The expression.
     * @return Class name.
     * @throws Exception
     */
    private static String getClassNameFromExp(Ast.Exp callObj) throws Exception
    {
        if (callObj instanceof Ast.NewObj)
        {
            return ((Ast.NewObj)callObj).nm;
        }
        else if (callObj instanceof Ast.Id)
        {
            Ast.Id var = (Ast.Id) callObj;
            Ast.Type varDef = typeEnv.get(var.nm);

            if (varDef == null)
            {
                throw new TypeException("Variable " + var.nm + " not defined.");
            }
            else if (!(varDef instanceof Ast.ObjType))
            {
                throw new TypeException("(In Field) Object is not of ObjType: " + getTypeName(varDef));
            }

            return ((Ast.ObjType) varDef).nm;
        }
        else if (callObj instanceof Ast.This)
        {
            return thisCInfo.className();
        }
        else if (callObj instanceof Ast.Field)
        {
            Ast.Field field = (Ast.Field) callObj;
            String className = getClassNameFromExp(field.obj);

            ClassInfo obj = classEnv.get(className);

            if (obj == null)
            {
                throw new TypeException("The class " + className + " is not defined.");
            }

            return className;
        }

        throw new IllegalArgumentException("Cannot handle getClassNameFromExp for type: " + callObj.getClass().getName());
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
        return cl.toArray(new Ast.ClassDecl[cl.size()]);
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
        thisCInfo = new ClassInfo(n, classEnv.get(n.pnm));

        // Reset the type environment.
        typeEnv = new HashMap<String, Ast.Type>();

        // TODO probably need to invoke check on each property, in both, down the chain.
        for (Ast.VarDecl varDecl : n.flds)
        {
            check(varDecl);
        }

        for (Ast.MethodDecl methodDecl : n.mthds)
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
        returnStmtCount = 0;

        for (Ast.Param p : n.params)
        {
            check(p);
        }

        for (Ast.VarDecl v : n.vars)
        {
            check(v);
            typeEnv.put(v.nm, v.t);
        }

        for (Ast.Stmt s : n.stmts)
        {
            check(s);
        }

        // If this method requires a return (has a return type) and none of the statements were return, then
        // there is definitely a missing return statement.
        if (n.t != null && returnStmtCount == 0)
        {
            throw new TypeException("(In MethodDecl) Missing return statement");
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

        Ast.ObjType objType = (Ast.ObjType)n.t;
        if (!classEnv.containsKey(objType.nm))
        {
            throw new TypeException("(In Param) Can't find class " + objType.nm);
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
            throw new TypeException("(In VarDecl) Can't find class " + ((Ast.ObjType) n.t).nm);
        }

        if (n.init == null)
        {
            return;
        }

        // Run the check on the initialization... just in case.
        check(n.init);

        Ast.Type expType = getExprType(n.init);
        if (!assignable(n.t, expType))
        {
            throw new TypeException("(In VarDecl) Cannot assign variable " + getTypeName(n.t) +
                    " <- " + getTypeName(expType));
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

        for (Ast.Stmt s : n.stmts)
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
        Ast.Type lhsType = getExprType(n.lhs);
        Ast.Type rhsType = getExprType(n.rhs);

        // Run a check on the right-hand side.
        check(n.rhs);

        if (!assignable(lhsType, rhsType))
        {
            throw new TypeException("(In Assign) lhs and rhs types don't match: " + getTypeName(lhsType) +
                    " <- " + getTypeName(rhsType));
        }
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
        Ast.Type astType = getExprType(n.obj);

        if (!(astType instanceof Ast.ObjType))
        {
            throw new TypeException("CallStmt requires obj be an Object.");
        }

        Ast.ObjType objType = (Ast.ObjType)astType;

        ClassInfo classInfo = classEnv.get(objType.nm);

        if (classInfo == null)
        {
            throw new TypeException("The class " + objType.nm + " does not exist.");
        }

        Ast.MethodDecl method = classInfo.findMethodDecl(n.nm);

        if (method == null)
        {
            throw new TypeException("(In CallStmt) Can't find method " + n.nm);
        }

        if (n.args.length != method.params.length)
        {
            throw new TypeException("Not enough parameters passed.");
        }

        for (int index = 0; index < n.args.length; index++)
        {
            Ast.Param pDef = method.params[index];
            Ast.Type argType = getExprType(n.args[index]);

            if (!getTypeName(pDef.t).equals(getTypeName(argType)))
            {
                throw new TypeException("(In CallStmt) Param and arg types don't match: " + getTypeName(argType) +
                        " vs. " + getTypeName(pDef.t));
            }
        }
    }

    // If ---
    //  Exp cond;
    //  Stmt s1, s2;
    //
    //  Make sure n.cond is boolean.
    //
    static void check(Ast.If n) throws Exception
    {
        Ast.Type condType = getExprType(n.cond);

        if (!(condType instanceof Ast.BoolType))
        {
            throw new TypeException("(In If) Cond exp type is not boolean: " + getTypeName(condType));
        }

        if (n.s1 != null)
        {
            check(n.s1);
        }

        if (n.s2 != null)
        {
            check(n.s2);
        }
    }

    // While ---
    //  Exp cond;
    //  Stmt s;
    //
    //  Make sure n.cond is boolean.
    //
    static void check(Ast.While n) throws Exception
    {
        Ast.Type condType = getExprType(n.cond);

        if (!(condType instanceof Ast.BoolType))
        {
            throw new TypeException("condition must be boolean.");
        }

        if (n.s != null)
        {
            check(n.s);
        }
    }

    // Print ---
    //  PrArg arg;
    //
    //  Make sure n.arg is integer, boolean, or string.
    //
    static void check(Ast.Print n) throws Exception
    {
        if (n.arg == null)
        {
            // This is okay too (test01).
            return;
        }

        if (n.arg instanceof Ast.StrLit)
        {
            return;
        }

        Ast.Type prArgType = getExprType((Ast.Exp) n.arg);

        if (prArgType instanceof Ast.IntType || prArgType instanceof Ast.BoolType)
        {
            return;
        }

        throw new TypeException("(In Print) Arg type is not int, boolean, or string: " + getTypeName(prArgType));
    }

    // Return ---
    //  Exp val;
    //
    //  If n.val exists, make sure it matches the expected return type.
    //
    static void check(Ast.Return n) throws Exception
    {
        returnStmtCount++;

        if (thisMDecl.t == null)
        {
            if (n.val == null)
            {
                return;
            }

            throw new TypeException("(In Return) Unexpected return value");
        }

        // Now the return type is not null, but nothing was returned.
        if (n.val == null)
        {
            throw new TypeException("(In Return) Missing return value of type " + getTypeName(thisMDecl.t));
        }

        Ast.Type returnType = getExprType(n.val);

        if (thisMDecl.t.getClass().getName().equals(returnType.getClass().getName()))
        {
            return;
        }

        throw new TypeException("(In Return) Return type mismatch: " + getTypeName(thisMDecl.t) +
                " <- " + getTypeName(returnType));
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
        Ast.Type e1Type = getExprType(n.e1);
        Ast.Type e2Type = getExprType(n.e2);

        //ADD("+"), SUB("-"), MUL("*"), DIV("/"), AND("&&"), OR("||"),
        //EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">=");

        // Arithmetic.
        if (n.op == Ast.BOP.ADD || n.op == Ast.BOP.SUB || n.op == Ast.BOP.MUL || n.op == Ast.BOP.DIV)
        {
            // Both must be integer.
            if (e1Type instanceof Ast.IntType && e2Type instanceof Ast.IntType)
            {
                return Ast.IntType;
            }

            throw new TypeException("Must be integers to use arithmetic.");
        }

        // Logical.
        if (n.op == Ast.BOP.AND || n.op == Ast.BOP.OR)
        {
            if (e1Type instanceof Ast.BoolType && e2Type instanceof Ast.BoolType)
            {
                return Ast.BoolType;
            }

            throw new TypeException("(In Binop) Operand types don't match: " + getTypeName(e1Type) +
                    " " + n.op + " " + getTypeName(e2Type));
        }

        // Equals and not equals can work on integers, bools, arrays or objects of same type.
        if (n.op == Ast.BOP.EQ || n.op == Ast.BOP.NE)
        {
            if (e1Type instanceof Ast.IntType && e2Type instanceof Ast.IntType)
            {
                return Ast.IntType;
            }
            else if (e1Type instanceof Ast.BoolType && e2Type instanceof Ast.BoolType)
            {
                return Ast.BoolType;
            }
            else if (e1Type instanceof Ast.ArrayType && e2Type instanceof Ast.ArrayType)
            {
                Ast.ArrayType a1 = (Ast.ArrayType)e1Type;
                Ast.ArrayType a2 = (Ast.ArrayType)e2Type;

                if (a1.et.getClass().getName().equals(a2.getClass().getName()))
                {
                    return a1;
                }

                throw new TypeException("Array element type must match.");
            }
            else if (e1Type instanceof Ast.ObjType && e2Type instanceof Ast.ObjType)
            {
                Ast.ObjType a1 = (Ast.ObjType)e1Type;
                Ast.ObjType a2 = (Ast.ObjType)e2Type;

                if (a1.nm.equals(a2.nm))
                {
                    return a1;
                }

                throw new TypeException("Object name must match.");
            }

            throw new TypeException("(In Binop) Operand types don't match: " + getTypeName(e1Type) +
                    " " + n.op + " " + getTypeName(e2Type));
        }

        //LT("<"), LE("<="), GT(">"), GE(">=")
        if (n.op == Ast.BOP.LT || n.op == Ast.BOP.LE || n.op == Ast.BOP.GT || n.op == Ast.BOP.GE)
        {
            if (e1Type instanceof Ast.IntType && e2Type instanceof Ast.IntType)
            {
                return Ast.IntType;
            }

            throw new TypeException("Must use integers with LT, LE, GT and GE.");
        }

        throw new TypeException("Unhandled case of operator.");
    }

    // Unop ---
    //  UOP op;
    //  Exp e;
    //
    //  Make sure n.e's type is legal with respect to n.op.
    //
    static Ast.Type check(Ast.Unop n) throws Exception
    {
        Ast.Type eType = getExprType(n.e);

        if (n.op == Ast.UOP.NEG)
        {
            if (eType instanceof Ast.IntType)
            {
                return eType;
            }

            throw new TypeException("(In Unop) Bad operand type: - " + getTypeName(eType));
        }

        if (eType instanceof Ast.BoolType)
        {
            return eType;
        }

        throw new TypeException("(In Unop) Bad operand type: ! " + getTypeName(eType));
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

        Ast.Type astType = getExprType(n.obj);

        if (!(astType instanceof Ast.ObjType))
        {
            throw new TypeException("Call requires obj be an Object.");
        }

        Ast.ObjType objType = (Ast.ObjType)astType;

        ClassInfo classInfo = classEnv.get(objType.nm);

        if (classInfo == null)
        {
            throw new TypeException("The class " + objType.nm + " does not exist.");
        }

        Ast.MethodDecl method = classInfo.findMethodDecl(n.nm);

        if (method == null)
        {
            throw new TypeException("Method " + n.nm + " does not exist on " + objType.nm);
        }

        if (n.args.length != method.params.length)
        {
            throw new TypeException("(In Call) Param and arg counts don't match: " + method.params.length +
                    " vs. " + n.args.length);
        }

        for (int index = 0; index < n.args.length; index++)
        {
            Ast.Param pDef = method.params[index];
            Ast.Type argType = getExprType(n.args[index]);

            if (!pDef.t.getClass().getName().equals(argType.getClass().getName()))
            {
                throw new TypeException("(In Call) Param and arg types don't match: " + getTypeName(pDef.t) +
                        " vs. " + getTypeName(argType));
            }
        }

        return method.t;
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

        if (!(n.et instanceof Ast.IntType) && !(n.et instanceof Ast.BoolType))
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
    //  Verify that n.ar is array and n.idx is integer.
    //
    static Ast.Type check(Ast.ArrayElm n) throws Exception
    {
        if (!(getExprType(n.ar) instanceof Ast.ArrayType))
        {
            throw new TypeException("not an array");
        }

        if (!(n.idx instanceof Ast.IntLit) && !(getExprType(n.idx) instanceof Ast.IntType))
        {
            throw new TypeException("(In ArrayElm) Index is not integer: " + getTypeName(getExprType(n.idx)));
        }

        return getExprType(n.ar);
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
            throw new TypeException("(In NewObj) Can't find class " + n.nm);
        }

        return new Ast.ObjType(n.nm);
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
        if (!(getExprType(n.obj) instanceof Ast.ObjType))
        {
            throw new TypeException("(In Field) Object is not of ObjType: " + getTypeName(getExprType(n.obj)));
        }

        Ast.ObjType objType = (Ast.ObjType)getExprType(n.obj);
        if (!classEnv.containsKey(objType.nm))
        {
            throw new TypeException(objType.nm + " is not a defined class, for variable " + n.nm);
        }

        Ast.VarDecl fieldDecl = classEnv.get(objType.nm).findFieldDecl(n.nm);

        if (fieldDecl == null)
        {
            throw new TypeException("(In Field) Can't find field " + n.nm);
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
    static Ast.Type check(Ast.This n) throws Exception
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

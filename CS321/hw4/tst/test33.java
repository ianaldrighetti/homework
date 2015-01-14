// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Mutually dependent class decls
// 
class Test {
  public static void main(String[] ignore) {
    A x = new A();
    B y = new B();
    System.out.println(x.i);
    System.out.println(y.a.i);
  }
}

class A {
  int i=1;
  B b;
}  

class B {
  A a = new A();
}  

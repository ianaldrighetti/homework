// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Class fields
// 
class Test {
  public static void main(String[] ignore) {
    A x = new A();
    B y = new B();
    System.out.println(x.i);
    System.out.println(y.i);
    System.out.println(y.j);
  }
}

class A {
  int i = 1;
}  

class B extends A {
  int j = 2;
}  

// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Class fields overriding
// 
class Test {
  public static void main(String[] ignore){
    B b = new B();
    A a = b;
    System.out.println(b.x);
    System.out.println(a.x);
  }
}

class A {
  boolean x = true;
}

class B extends A {
  int x = 6;
}

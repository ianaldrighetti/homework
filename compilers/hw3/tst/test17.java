// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Class declarations
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    B b = new B();
    int i = a.foo(1);
    int j = b.foo(1);
    System.out.println(i);
    System.out.println(j);
  }
}

class B extends A {
  public int foo(int i) {
    return i;
  }
}  

class A {
  int i;

  public int foo(int i) {
    int y;
    return i + 1;
  }
}  

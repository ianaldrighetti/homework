// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Method bindings
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    B b = new B();
    A c = new B();
    a.foo();
    b.foo();
    c.foo();
  }
}

class A {
  public void foo() {
    System.out.println("A");
  }
}  

class B extends A {
  public void foo() {
    System.out.println("B");
  }
}  

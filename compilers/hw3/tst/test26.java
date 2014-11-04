// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Variable access
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    B b = new B();
    a.foo();
    b.i = 10;
    b.j = 11;
    b.foo();
  }
}

class A {
  int i = 1;
  int j = i + 1;
  public void foo() {
    bar();
  }
  public void bar() {
    System.out.println(i);
  }
}  

class B extends A {
  int k = 10;
}  

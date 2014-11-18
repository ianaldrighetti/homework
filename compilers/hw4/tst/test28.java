// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Variable and method shared the same name
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    a.foo = a.foo(1);
    System.out.println(a.foo);
  }
}

class A {
  int foo;
  public int foo(int i) {
    return i;
  }
}

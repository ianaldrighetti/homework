class A { }
class B extends A { }
class C extends B { }

class Test {
  public void foo(A x) { }
  public void bar(B x) { }
  public void test() {
    A a = new A();
    B b = new B();
    C c = new C();
    foo(b);  // OK
    foo(c);  // OK
    bar(c);  // OK
    bar(a);  // error
  }
  public static void main(String[] ignore) {
  }
}

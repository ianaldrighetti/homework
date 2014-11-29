class A { }
class B extends A { }
class C extends B { }
class D extends A { }

class Test {
  public void test() {
    A a = new A();
    B b = new B();
    C c = new C();
    D d = new D();
    a = b;  // OK
    a = c;  // OK
    b = c;  // OK
    b = a;  // error
    b = d;  // error
  }
  public static void main(String[] ignore) {
  }
}

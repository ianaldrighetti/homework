// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Class field initialization.
//
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    System.out.println(b.i + b.j);
  }
}

class B {
  int i = 5;
  int j = i + 2;
}

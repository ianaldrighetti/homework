// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Param and local var declarations
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    boolean b;
    int i;
    int j;
    b = true;
    i = a.foo(1, 2);
    j = 2 * 3;
    System.out.println(b);
    System.out.println(i);
    System.out.println(j);
  }
}

class A {
  public int foo(int i, int j) {
    return i + j;
  }
}

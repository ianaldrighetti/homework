// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Method declarations
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    int i;
    int j;
    i = a.foo(1);
    j = a.bar(1);
    System.out.println(i);
    System.out.println(j);
  }
}

class A {
  public int foo(int i) {
    int x;
    return i;
  }

  public int bar(int i) {
    int x=2;
    return x;
  }
}

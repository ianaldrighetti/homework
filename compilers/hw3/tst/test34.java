// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Nested calls
// 
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    System.out.println(a.foo(2));
  }
}

class A {
  int k = 10;
  public int foo(int i) {
    if (i>0)
      k = k + foo(bar(i));
    return k;
  }

  public int bar(int i) {
    return i - 1;
  }
}  



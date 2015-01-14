// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Class and object
//
class Test {
  public static void main(String[] ignore) {
    B b = new B();
    int i = 2;    
    b.i = 3;
    System.out.println(i + b.i);
  }
}

class B {
  int i;
}

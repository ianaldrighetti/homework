// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Operator precedence
// 
class Test {
  public static void main(String[] ignore){
    A a = new A();
    System.out.println(a.go());
  }
}

class A {
  boolean b;
  int i = 0;
  public int go() {
    int[] a = new int [4];
    b = 1 < 2 || 3 > 4 && 5 == 6 + 7 * 8 || ! true;
    i =  - - 3 + 5 * 4 / 2 * a[1] + this . i * 2;
    System.out.println(b);
    return i;
  } 
}

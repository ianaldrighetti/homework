// miniJava test program (For CS321 Language and Compiler Design, PSU)
//
// Sorting
//
class Test {
  public static void main(String[] ignore) {
    A a = new A();
    int[] numbers = new int[10];
    int cnt = 0;
    
    while (cnt < 10) {
      numbers[cnt] = 10 - cnt;
      cnt = cnt + 1;
    }

    a.selectionSort(numbers, cnt);
    
    System.out.println("Your numbers in sorted order are:");
    cnt = 0;
    while (cnt < 10) {
      System.out.println(numbers[cnt]);
      cnt = cnt + 1;
    }
  }
}   

class A {
  public void selectionSort(int[] A, int count) {
    int temp;
    int i = count - 1;
    int j;
    int k;

    while (i >= 0) {
      j = 0;
      k = 0;
      while (j <= i) {
	if (A[j] > A[k]) k = j;
	j = j + 1;
      }
      if (k != i) {
	temp = A[k];
	A[k] = A[i];
	A[i] = temp;
      }
      i = i - 1;
    }
  } 
}

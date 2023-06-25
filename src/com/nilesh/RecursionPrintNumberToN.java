package com.nilesh;

public class RecursionPrintNumberToN {
    public static void main(String[] args) {
       printNumbers(5);
    }
    static void printNumbers(int n) {
        if(n==1) {
            System.out.println(n);
            return;
        }
        //System.out.println(n);//if 5->4->3->2->1
        printNumbers(n-1);
        System.out.println(n);//if 1->2->3->4->5
    }
}

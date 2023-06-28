package com.nilesh;

public class RecurssionRevertNum {
    public static void main(String[] args) {
        System.out.println(reverse(1020));
        System.out.println(reverseUsingRec(123));
    }

    static int reverse(int n) {
        int rev = 0;
        while(n>0){
            rev = rev*10+ n%10 ;
            n= n/10;
        }
        return rev;
    }
    static int reverseUsingRec(int n) {
        int rev = 0;
        while(n>0){
            rev = rev*10+ n%10 ;
            n = n/10;
            reverseUsingRec(n);
        }
        return rev;
    }
}

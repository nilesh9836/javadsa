package com.nilesh;

public class RecurssionRevertNum {
    public static void main(String[] args) {
        System.out.println(reverse(1020));
        System.out.println(reverseUsingRec(123));
        System.out.println(rev2(123));
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
        return rev;//thisjdsfkjsd
    }

    static int rev2(int n) {
        int digit = (int)Math.log10(n) +1;
        return helper(n,digit);
    }

    static int helper(int n, int digit) {
        if(n%10 == n) return n;
        int rem  = n%10;
        return rem*(int)Math.pow(10,digit-1) + rev2(n/10);
    }
}

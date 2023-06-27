package com.nilesh;

public class RecursionSumOfDigits {
    public static void main(String[] args) {
        System.out.println(sumOfDigits(17823));
    }
    static int sumOfDigits(int n) {
        if(n<10) return n;
        return n%10 + sumOfDigits(n/10);
    }
}

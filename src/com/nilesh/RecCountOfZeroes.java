package com.nilesh;

public class RecCountOfZeroes {
    public static void main(String[] args) {
        System.out.println(countNoOfZero(2010302250));
        System.out.println(recCountOfZeros(2010302250,0));
    }

    static int countNoOfZero(int n) {
        int count = 0;
        while(n>0) {
            if (n % 10 == 0) count++;
            n = n / 10;
        }
        return count;
    }

    static int recCountOfZeros(int n,int c) {
        if(n%10 == n ) {
            if(n == 0) return ++c;
            return c;
        }
        if(n%10 == 0) return recCountOfZeros(n/10,++c) ;
        else return recCountOfZeros(n/10, c)  ;
    }
}

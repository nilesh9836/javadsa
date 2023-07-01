package com.nilesh;

public class LeetCode1342 {
    public static void main(String[] args) {
        System.out.println(numOfStepsToReduceNumAsZero(14));
        System.out.println(numberOfSteps(14));
    }
    static int numOfStepsToReduceNumAsZero(int num) {
        int c = 0;
        while(num > 0) {
            if(num % 2 == 0) {
                num = num/2;
                c++;
            } else {
                num = num-1;
                c++;
            }

        }
        return c;
    }
    static int numberOfSteps(int num) {
        return helper(num,0);
    }

    static int helper(int num,int c) {
        if(num == 0) return c;
        if(num % 2 == 0) return helper(num/2,++c);
        return helper(num-1,++c);
    }
}

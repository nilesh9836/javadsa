package com.nilesh;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        int[] arr = {9};
        int[] ans = plusOne(arr);
        for (int i =0 ; i< ans.length;i++){
            System.out.println(ans[i]);
        }
    }
        public static int[] plusOne(int[] digits) {
            int number =0;
            int[] result = new int[digits.length];
            for(int i =0;i< digits.length ;i++) {
                number = number + digits[i]*(int)Math.pow(10,digits.length-1-i);
            }
            int j = 0;
            number = number+1;
            while(number > 0) {
                result[digits.length-1 -j++] = number%10;
                number = number/10;
            }
            return result;
        }

}
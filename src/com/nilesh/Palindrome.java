package com.nilesh;


public class Palindrome {
    public static boolean isPalindrome(String s) {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i<s.length();++i) {
            if(s.toLowerCase().charAt(i)>=97 && s.toLowerCase().charAt(i)<=122) builder.append(s.toLowerCase().charAt(i));
        }
        System.out.println(builder.toString().toCharArray());
        char[] arr = builder.toString().toCharArray();
        int st = 0,e=arr.length-1;
        while(st <= e) {
            if(arr[st] != arr[e]) {
                return false;
            } else {
                st++;
                e--;
            }
        }
        return true;
    }

    public static void main(String[] args) {
       String s = "0P";
        System.out.println(isPalindrome(s));
    }
}
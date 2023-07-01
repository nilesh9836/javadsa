package com.nilesh;

public class RecLinearSearch {
    public static void main(String[] args) {
        int[] arr = new int[]{3,2,1,8,9};
        System.out.println(linearSearch(arr,10));
    }
    static int linearSearch(int[] arr, int t) {
        return helper(arr,t,0);
    }
    static int helper(int[] arr,int t ,int c)  {
        if(c == arr.length-1) return -1;
        if(arr[c] == t) return c;
        return helper(arr,t,++c);
    }
}

package com.nilesh;

public class IsArrSorted {
    public static void main(String[] args) {
        int arr[] = new int[5];
        arr = new int[]{1, 4, 8, 9, 12};
        System.out.println(isSorted(arr));
    }
    static boolean isSorted(int[] arr) {
        return helper(arr,0);
    }

    static boolean helper(int[] arr,int c) {
        if(c == arr.length - 1 ) return true;
        if(arr[c] > arr[c+1]) return false;
        return helper(arr,++c);
    }
}

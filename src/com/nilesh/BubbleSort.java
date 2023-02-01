package com.nilesh;

public class BubbleSort {
    public static void main(String[] args) {
        int[] arr = {12,89,45,6,878,-4};
        int[] ans = bubbleSort(arr);
        for (int i = 0; i < ans.length; i++) {
            System.out.println(ans[i]);
        }
    }
    public static int[] bubbleSort(int[] arr) {
        boolean isSwap = false;
        for (int i = 0; i < arr.length; i++) {
            isSwap = false;
            for (int j = 1; j < arr.length-i; j++) {
                if(arr[j]<arr[j-1]){
                    swap(arr,j-1,j);
                    isSwap = true;
                }
            }
            if(!isSwap) return arr;
        }
        return arr;
    }
    public static void swap(int[] arr,int s,int e) {
        int temp = arr[s];
        arr[s] = arr[e];
        arr[e]=temp;
    }
}

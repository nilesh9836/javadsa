package com.nilesh;

public class Ceiling {
    public static void main(String[] args) {
      int[] arr ={2,3,5,9,14,16,18};
        System.out.println(ceiling(arr,15));
    }

    public static  int ceiling(int[] arr, int target) {
        int s= 0,e = arr.length-1;
        int mid = 0,ceil = 0;
        mid = s+(e-s)/2;
        while(s<=e) {
            if(target >  arr[mid]) {
                s = mid+1;
            } else if(target< arr[mid]) {
                e = mid-1;
            }
            else {
                return arr[mid];
            }
            mid = s+(e-s)/2;
        }
        return arr[s];
    }
}

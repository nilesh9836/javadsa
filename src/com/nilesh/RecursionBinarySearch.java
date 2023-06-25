package com.nilesh;

public class RecursionBinarySearch {
    public static void main(String[] args) {
        int nums[] ={3,6,9,12,14};
        int target = 4;
        System.out.println(binarySearch(nums,0, nums.length, target));
    }
    static int binarySearch(int[] arr,int s,int e,int target) {

        while( s<=e) {
            int m = s + (e -s)/2;
            if (arr[m] == target) return m;
            if (arr[m] < target) {
                return binarySearch(arr, m + 1, e, target);
            }
            if (arr[m] > target) {
                return binarySearch(arr, s, m - 1, target);
            }
        }
        return -1;
    }
}

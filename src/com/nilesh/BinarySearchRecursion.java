package com.nilesh;

public class BinarySearchRecursion {
    public static int search(int[] nums, int target) {
        return binSearch(nums,0,nums.length-1,target);
    }

    public static int binSearch(int[] arr,int s,int e, int target) {
        if(s>e) return -1;
        int mid = s+(e-s)/2;
        if(arr[mid] == target) {
            return mid;
        }
        else if(arr[mid] < target) return binSearch(arr,mid+1,e,target);
        else return binSearch(arr,s,mid-1,target);
    }

    public static void main(String[] args) {
        int[] arr = {-1,0,3,5,9,12};
        System.out.println(search(arr,9));
    }
}
package com.nilesh;

class FindPeakElement {
    public static void main(String[] args) {
        int[] arr = {1,3,2,1};
        System.out.println(findPeakElement(arr));
    }
    public static int findPeakElement(int[] nums) {
        int s =0, e= nums.length -1;
        int mid = s+(e-s)/2;
        while(s <= e) {
            if(s==e) return mid;
            if(nums[mid] < nums[mid+1]){
                s = mid +1;
            } else if( nums[mid] > nums[mid+1]){
                e = mid;
            } else {
                return mid;
            }
            mid = s+(e-s)/2;
        }
        return -1;
    }
}
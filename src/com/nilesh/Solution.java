package com.nilesh;
class Solution {
    public static void main(String[] args) {
        int[] arr= {2,3,4,7,11};
        int k = 5;
        findKthPositive(arr,k);
    }
    public static int findKthPositive(int[] arr, int k) {
        int i=0,j=0;
        int[] result = new int[k];
        while(i<k){
            if(!binarysearch(arr,++j)){
                result[i++] = j;
            }
        }
        System.out.println(result[k-1]);
        return result[k-1];
    }
    public static boolean binarysearch(int[] arr,int target) {
        int s=0,e=arr.length-1;

        while(s<=e){
            int mid = s+ (e-s)/2;
            if(target < arr[mid]){
                e = mid -1;
            } else if(target > arr[mid]){
                s=mid+1;
            }else {
                return true;
            }

        }
        return false;
    }
}

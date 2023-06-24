package com.nilesh;

import java.lang.reflect.Array;

public class FindDuplicate {
    public static boolean containsDuplicate(int[] nums) {
        int i=0,j =nums.length;
        while(i<j) {
            int correct = nums[i] - 1;
            if (nums[correct] != nums[i]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        for(int k =0; k< nums.length;k++) {
            System.out.println(nums[k]);
            if(nums[k] != k+1) return true;
            else return false;
        }
        return false;
    }
    public static void swap(int[] nums,int a, int b) {
        int temp = nums[a];
        nums[a] = nums[b];
        nums[b] = temp;
    }

    public static void main(String[] args) {
        int[] nums = {1,2,3,1};
        System.out.println(containsDuplicate(nums));
    }
}

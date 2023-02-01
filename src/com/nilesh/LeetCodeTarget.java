package com.nilesh;

class LeetCodeTarget {
    public static void main(String[] args) {
        char[] arr = {'c','f','j'};
        char target = 'c';
        System.out.println(nextGreatestLetter(arr,target));
    }
    public static  char nextGreatestLetter(char[] letters, char target) {
        int s=0,e=letters.length -1;
        int mid = s + (e-s)/2;

        while(s <= e) {
            if(letters[mid]  > target) {
                e = mid - 1;
            } else {
                s = mid  + 1;
            }
            mid = s + (e-s)/2;
        }
        return letters[s];
    }
}
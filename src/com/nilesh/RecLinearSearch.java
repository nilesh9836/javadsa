package com.nilesh;

import java.util.ArrayList;

public class RecLinearSearch {
    public static void main(String[] args) {
        int[] arr = new int[]{3,2,9,1,8,9,9,9,9};
        System.out.println(linearSearch(arr,9));
    }
    static ArrayList linearSearch(int[] arr, int t) {
        ArrayList<Integer> list = new ArrayList<>();
        //return helper2(arr,t,0, list);
        return findAllIndex(arr,t,0);
    }
    static int helper(int[] arr,int t ,int c)  {
        if(c == arr.length-1) return -1;
        if(arr[c] == t) return c;
        return helper(arr,t,++c);
    }
    static ArrayList helper2(int[] arr, int t ,int c,ArrayList<Integer> list)  {

        if(arr[c] == t) {
            list.add(c);
        }
        if(c == arr.length-1) {
            return list;
        }
        return helper2(arr,t,++c,list);
    }

    //Without taking ArrayList as argument

    static ArrayList findAllIndex(int[] arr,int t,int c) {
        ArrayList<Integer> list = new ArrayList<>();

        if(arr.length == c) {
            return list;
        }
        //Every function call it will contain answer of that function only
        if(arr[c] == t) {
            list.add(c);
        }
        ArrayList<Integer> ans = findAllIndex(arr,t,++c);

        list.addAll(ans);
        return list;
    }
}

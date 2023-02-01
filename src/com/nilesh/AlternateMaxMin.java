package com.nilesh;

public class AlternateMaxMin {
    /******************************************************************************

     Welcome to GDB Online.
     GDB online is an online compiler and debugger tool for C, C++, Python, Java, PHP, Ruby, Perl,
     C#, OCaml, VB, Swift, Pascal, Fortran, Haskell, Objective-C, Assembly, HTML, CSS, JS, SQLite, Prolog.
     Code, Compile, Run and Debug online from anywhere in world.
     1) [ 6, 4, 3, 1, 5, 2]   ==> [ 6, 1, 5, 2, 4, 3 ]
     *******************************************************************************/

        public static void main(String[] args) {
            int[] arr = {6,4,3,1,5,2};
            int i =0 ,j = arr.length-1,temp =0;
            while(i<=j){
                temp =0;
                if(i%2 == 0){

                    temp = findMax(arr,i);
                    swap(arr,temp,i);
                    //System.out.println("e"+temp);

                } else {
                    temp = findMin(arr,i);
                    swap(arr,temp,i);
                    //System.out.println("o"+temp);

                }
                i++;
            }
            for(int k = 0;k< arr.length; ++k)
              System.out.println(arr[k]);
        }

        public static void swap(int[] arr,int a,int b) {
            int temp = arr[a];
            arr[a] = arr[b];
            arr[b] = temp ;
        }

        public static int findMax(int[] arr,int s) {
            int max = arr[s],index =s;
            for(int i=s; i< arr.length;i++) {
                if(max < arr[i]) {
                    max = arr[i];
                    index = i;
                }
            }

            return index;
        }
        public static int findMin(int[] arr,int s) {
            int min = arr[s],index = s;
            for(int i=s; i< arr.length;i++) {
                if(min  > arr[i]){
                    min = arr[i];
                    index = i;
                }
            }
            return index;
        }
    }

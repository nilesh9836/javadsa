package recursion;

public class BinarySearch {
    public static void main(String[] args) {
        //Binary search for rotated array using recursion
        int[] arr = new int[]{5,6,1,2,3,4};
        System.out.println(binarySearch(arr,6,0, arr.length-1));
    }

    static int binarySearch(int[] arr, int target,int s,int e) {
         //s=0,e = arr.length-1;
        while(s<=e) {
            int m = s+ (e-s)/2;
            if(arr[s]<=arr[m]) {
                if(target < arr[m]) {
                    e = m -1;
                }
                else if(target > arr[m]) {
                    s = m + 1;
                } else {
                    return m;
                }
               return binarySearch(arr,s,e,target);
            }
            if(arr[s]>arr[m]) {
                if(target < arr[m]) {
                    e = m -1;
                }
                else if(target > arr[m]) {
                    s = m + 1;
                } else {
                    return m;
                }
                return binarySearch(arr,s,e,target);
            }
        }
        return -1;
    }

}

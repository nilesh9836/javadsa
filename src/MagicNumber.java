public class MagicNumber {
    public static void main(String[] args) {
        System.out.println(nthMagicNumber(6));
    }
    public static int nthMagicNumber(int n) {
        int base = 5;
        int ans = 0;
        while(n>0) {
            int lastDigit = n & 1;
            n= n >> 1;
            ans += base*lastDigit;
            base = base * 5;
        }
        return ans;
    }
}

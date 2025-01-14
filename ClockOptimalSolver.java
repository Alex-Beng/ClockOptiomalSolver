public class ClockOptimalSolver {
    
    /**
     *   0 1 2    -2  9 -0
     *   3 4 5    10 11 12
     *   6 7 8    -8 13 -6
     *  (front)    (back)
     */
    public static class ClockSolver {
        private static final int N_MOVES = 18;
        private static final int N_HANDS = 14;

        static int[][] moveArr = {
            { 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},    //UR
            { 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0},    //DR
            { 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0},    //DL
            { 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0},    //UL
            { 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0},    //U
            { 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0},    //R
            { 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},    //D
            { 1, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0},    //L
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},    //ALL
            {11, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0},    //UR
            { 0, 0, 0, 0, 0, 0,11, 0, 0, 0, 0, 1, 1, 1},    //DR
            { 0, 0, 0, 0, 0, 0, 0, 0,11, 0, 1, 1, 0, 1},    //DL
            { 0, 0,11, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0},    //UL
            {11, 0,11, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0},    //U
            {11, 0, 0, 0, 0, 0,11, 0, 0, 1, 0, 1, 1, 1},    //R
            { 0, 0, 0, 0, 0, 0,11, 0,11, 0, 1, 1, 1, 1},    //D
            { 0, 0,11, 0, 0, 0, 0, 0,11, 1, 1, 1, 0, 1},    //L
            {11, 0,11, 0, 0, 0,11, 0,11, 1, 1, 1, 1, 1}     //ALL
        };

        /*
         *  Combination number. Cnk[n][k] = n!/k!/(n-k)!.
         */
        static int[][] Cnk = new int[32][32];

        static {
            for (int i=0; i<32; i++) {
                Cnk[i][i] = 1;
                Cnk[i][0] = 1;
            }
            for (int i=1; i<32; i++) {
                for (int j=1; j<=i; j++) {
                    Cnk[i][j] = Cnk[i-1][j] + Cnk[i-1][j-1];
                }
            }
        }

        /*
         *  The bit map to filter linearly dependent combinations of moves.
         *  The i-th bit denotes whether the i-th move is in the combinations.
         */
        static int[] ld_list = {
            // Combinations of 8 moves
            7695,   //000001111000001111
            42588,  //001010011001011100
            47187,  //001011100001010011
            85158,  //010100110010100110
            86697,  //010101001010101001
            156568, //100110001110011000
            181700, //101100010111000100
            209201, //110011000100110001
            231778, //111000100101100010

            // Combinations of 12 moves
            125690, //011110101011111010
            128245, //011111010011110101
            163223, //100111110110010111
            187339, //101101101111001011
            208702, //110010111100111110
            235373  //111001011101101101
        };

        /*
         *  The inverse table of the ring Z/Z12. If the value is -1, the element is not inversable.
         */
        static int[] inv = {-1, 1,-1,-1,-1, 5,-1, 7,-1,-1,-1,11};

        /**
         *  Index [0, C(n,k)) to all C(n,k) combinations
         *  @return the idx_th combination, represented in bitmap
         */
        static int select(int n, int k, int idx) {
            int r = k;
            int val = 0;
            for (int i=n-1; i>=0; i--) {
                if (idx >= Cnk[i][r]) {
                    idx -= Cnk[i][r--];
                    val |= 1 << i;
                }
            }
            return val;
        }

        public static int[] randomState(java.util.Random r) {
            int[] ret = new int[N_HANDS];
            for (int i=0; i<N_HANDS; i++) {
                ret[i] = r.nextInt(12);
                if (false){
                    ret[i] = 0;
                    if (i==1) {
                        ret[i] = 1;
                    }
                }
            }
            return ret;
        }

        /**
         *  @param hands
         *      The 14 hands of the clock. See the comment of the class.
         *  @param solution
         *      The solution of the clock is written in the array. The value is NOT the moves which solves the state, but the moves which generates the state.
         *  @return the length of the solution (the number of non-zero elements in the solution array)
         *      -1: invalid input
         */
        public static int solution(int[] hands, int[] solution) {
            if (hands.length != N_HANDS || solution.length != N_MOVES) {
                return -1;
            }
            int ret = enumAllComb(N_HANDS, hands, solution);
            if (!checkSolution(hands, solution)) {
                assert(false);
            }
            return ret;
        }

        /**
         *  Check whether the solution is valid.
         */
        public static boolean checkSolution(int[] hands, int[] solution) {
            int[] clk = new int[N_HANDS];
            for (int i=0; i<N_MOVES; i++) {
                if (solution[i] == 0) {
                    continue;
                }
                for (int j=0; j<N_HANDS; j++) {
                    clk[j] += solution[i] * moveArr[i][j];
                }
            }
            for (int i=0; i<N_HANDS; i++) {
                if (clk[i] % 12 != hands[i]) {
                    return false;
                }
            }
            return true;
        }

        /**
         *  Enumerate all k combinations from all 18 possible moves.
         *  For each linearly independent combination, use Gaussian Elimination to solve the clock
         *  @param k
         *      The number of moves, if k > 14, the selected k moves are always linearly dependent.
         *  @param hands
         *      The 14 hands of the clock. See the comment of the class.
         *  @param solution
         *      The shortest solution is stored in this parameter.
         *  @return the length of the shortest solution, which is equal to the number of non-zero element in the solution array.
         *      -1: the clock cannot be solved in k moves.
         */
        static int enumAllComb(int k, int[] hands, int[] solution) {
            int n = N_MOVES;
            int min_nz = k+1;

            for (int idx=0; idx<Cnk[n][k]; idx++) {
                int val = select(n, k, idx);
                //All of linearly dependent combinations are filtered if k <= 14. Otherwise, k moves are always linearly dependent.
                boolean isLD = false;
                for (int r: ld_list) {
                    if ((val & r) == r) {
                        isLD = true;
                        break;
                    }
                }
                if (isLD) {
                    continue;
                }
                int[] map = new int[k];
                int cnt = 0;
                for (int j=0; j<n; j++) {
                    if (((val >> j) & 1) == 1) {
                        map[cnt++] = j;
                    }
                }
                int[][] arr = new int[N_HANDS][k+1];
                for (int i=0; i<N_HANDS; i++) {
                    for (int j=0; j<k; j++) {
                        arr[i][j] = moveArr[map[j]][i];
                    }
                    arr[i][k] = hands[i];
                }
                int ret = gaussianElimination(arr);

                //We have filtered all linearly dependent combinations. However, if more moves are added into the move set, the ld_list should be re-generated.
                if (ret != 0) {
                    assert(false);
                    continue;
                }

                //Check the rank of the coefficient matrix equal to that of the augmented matrix.
                //If not, the clock cannot be solved by the selected moves.
                boolean isSolved = true;
                for (int i=k; i<N_HANDS; i++) {
                    if (arr[i][k] != 0) {
                        isSolved = false;
                        break;
                    }
                }
                if (!isSolved) {
                    continue;
                }
                backSubstitution(arr);
                int cnt_nz = 0;
                for (int i=0; i<k; i++) {
                    if (arr[i][k] != 0) {
                        cnt_nz++;
                    }
                }
                if (cnt_nz < min_nz) {
                    for (int i=0; i<N_MOVES; i++) {
                        solution[i] = 0;
                    }
                    for (int i=0; i<k; i++) {
                        solution[map[i]] = arr[i][k];
                    }
                    min_nz = cnt_nz;
                }
            }
            return min_nz == k+1 ? -1 : min_nz;
        }

        /**
         *  Gaussian Elimination over the ring Z/Z12.
         *  @return 0 if success, n if the algorithm exited at n-th step, which means the row vectors are linearly dependent.
         */
        static int gaussianElimination(int[][] arr) {
            int m = N_HANDS;
            int n = arr[0].length;
            for (int i=0; i<n-1; i++) {

                //If arr[i][i] is not inversable, select or generate an inversable element in i-th column, and swap it to the i-th row.
                if (inv[arr[i][i]] == -1) {
                    int ivtidx = -1;

                    for (int j=i+1; j<m; j++) {
                        if (inv[arr[j][i]] != -1) {
                            ivtidx = j;
                            break;
                        }
                    }
                    if (ivtidx == -1) {
                        //If all elements in i-th column are uninversable, we will try to find two elements x, y such that the ideal generated by {x, y} == Z/Z12, then we can generate an inversable element in i-th column.
                        //Luckly, in Z/Z12, the ideal generated by two uninversable element {x, y} == Z/Z12 is equivalent to the inversablility of x+y.
                        OUT:
                        for (int j1=i; j1<m-1; j1++) {
                            for (int j2=j1+1; j2<m; j2++) {
                                if (inv[(arr[j1][i] + arr[j2][i]) % 12] != -1) {
                                    addTo(arr, j2, j1, i, 1);
                                    ivtidx = j1;
                                    break OUT;
                                }
                            }
                        }
                    }
                    if (ivtidx == -1) { //k vectors are linearly dependent
                        for (int j=i+1; j<m; j++) {
                            assert(arr[j][i] == 0);
                        }
                        return i + 1;
                    }
                    swap(arr, i, ivtidx);
                }
                int invVal = inv[arr[i][i]];
                for (int j=i; j<n; j++) {
                    arr[i][j] = arr[i][j] * invVal % 12;
                }
                for (int j=i+1; j<m; j++) {
                    addTo(arr, i, j, i, 12 - arr[j][i]);
                }
            }
            return 0;
        }

        static void backSubstitution(int[][] arr) {
            int n = arr[0].length;
            for (int i=n-2; i>0; i--) {
                for (int j=i-1; j>=0; j--) {
                    if (arr[j][i] != 0) {
                        addTo(arr, i, j, i, 12 - arr[j][i]);
                    }
                }
            }
        }

        static void swap(int[][] arr, int row1, int row2) {
            int[] tmp = arr[row1];
            arr[row1] = arr[row2];
            arr[row2] = tmp;
        }

        /**
         *  arr[row2][startidx:end] += arr[row1][startidx:end] * mul
         */
        static void addTo(int[][] arr, int row1, int row2, int startidx, int mul) {
            int length = arr[0].length;
            for (int i=startidx; i<length; i++) {
                arr[row2][i] = (arr[row2][i] + arr[row1][i] * mul) % 12;
            }
        }
    }

    private static final String[] turns={"UR","DR","DL","UL","U","R","D","L","ALL"};

    public static void help() {
        System.out.println("Usage: java ClockOptimalSolver [clock status]");
        System.out.println("    clock status: 14 numbers in [0,12), which represent the clock's status");
        System.out.println("    e.g. java ClockOptimalSolver 0 1 0  0 0 0  0 0 0  0 0 0 0 ");
        System.out.println("    the order, x mean not-needed: ");
        System.out.println("      1 2 3      x  10 x");
        System.out.println("      4 5 6      11 12 13");
        System.out.println("      7 8 9      x  14 x");
        System.out.println("      (front) y2 (back)");
    }

    public static void main(String[] args) {
        StringBuilder scramble = new StringBuilder();

        java.util.Random r = new java.util.Random();
        if (args.length == 0 || args.length != 14) {
            System.out.println("    args.length = " + args.length);
            help();
            return;
        }

        // int[] clk_state = ClockSolver.randomState(r);
        int[] clk_state = new int[14];
        for (int i=0; i<14; i++) {
            clk_state[i] = Integer.parseInt(args[i]);
        }

        int[] solution = new int[18];
        ClockSolver.solution(clk_state, solution);
        
        for (int x=0; x<9; x++) {
            int turn = solution[x];
            if (turn == 0) {
                continue;
            }
            boolean clockwise = turn <= 6;
            if (turn > 6) {
                turn = 12 - turn;
            }
            scramble.append( turns[x] + turn + (clockwise?"+":"-") + " ");
        }
        scramble.append( "y2 ");
        for (int x=9; x<18; x++) {
            int turn = solution[x];
            if (turn == 0) {
                continue;
            }
            boolean clockwise = turn <= 6;
            if (turn > 6) {
                turn = 12 - turn;
            }
            scramble.append( turns[x - 9] + turn + (clockwise?"+":"-") + " ");
        }

        boolean isFirst = true;
        for(int x=0;x<4;x++) {
            if (r.nextInt(2) == 1) {
                scramble.append((isFirst?"":" ")+turns[x]);
                isFirst = false;
            }
        }
        String scrambleStr = scramble.toString().trim();
        
        System.err.println(scrambleStr);
    }
}
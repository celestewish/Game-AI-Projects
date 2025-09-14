import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Scanner;

public class Assignment1 {
    static final int N = 9;

    static final int[][] DEFAULT_BOARD = {
            {3, 0, 6, 5, 0, 8, 4, 0, 0},
            {5, 2, 0, 0, 0, 0, 0, 0, 0},
            {0, 8, 7, 0, 0, 0, 0, 3, 1},
            {0, 0, 3, 0, 1, 0, 0, 8, 0},
            {9, 0, 0, 8, 6, 3, 0, 0, 5},
            {0, 5, 0, 0, 9, 0, 6, 0, 0},
            {1, 3, 0, 0, 0, 0, 2, 5, 0},
            {0, 0, 0, 0, 0, 0, 0, 7, 4},
            {0, 0, 5, 2, 0, 6, 3, 0, 0}
    };


    public static void main (String[] args){
        System.out.println("Hello! Welcome to sudoku! Enter puzzle of choice.");

        int[][] board = DEFAULT_BOARD;
        Scanner scanner = new Scanner(System.in);
        String puzzle = scanner.nextLine();
        if (!puzzle.isEmpty()) {
            try {
                board = loadBoardFromFile(puzzle);
                System.out.println("Loaded board from: " + puzzle);
            } catch (Exception e) {
                System.out.println("Failed to load '" + puzzle + "': " + e.getMessage());
                System.out.println("Falling back to built-in DEFAULT_BOARD.");
            }
        } else {
            System.out.println("No file provided; using built-in DEFAULT_BOARD.");
        }

        boolean result = sudoku(board);
        if (result){
            System.out.println("AI Won.");
        } else {
            System.out.println("AI lost.");
        }
    }


    // ===================== SUDOKU DRIVER =====================

    public static boolean sudoku(int[][] board) {

        printBoard(board);

        boolean changedOverall;
        do {
            if (isFull(board)) break;

            java.util.List<UnitCount> ranked = unitsByDensity(board);
            if (ranked.isEmpty()) break;

            changedOverall = false;

            int K = Math.min(3, ranked.size());
            for (int i = 0; i < K; i++) {
                Unit u = ranked.get(i).unit;
                if (workOneUnit(board, u)) {
                    changedOverall = true;
                    printBoard(board);
                    break;
                }
            }

            if (!changedOverall) {
                for (int i = K; i < ranked.size(); i++) {
                    Unit u = ranked.get(i).unit;
                    if (workOneUnit(board, u)) {
                        changedOverall = true;
                        printBoard(board);
                        break;
                    }
                }
            }

        } while (changedOverall && !isFull(board));

        return isFull(board);
    }


    // ===================== UNIT TYPES & HELPERS =====================

    enum UnitType { ROW, COL, BOX }

    static class Unit {
        final UnitType type;
        final int index;
        Unit(UnitType t, int i) { this.type = t; this.index = i; }
    }

    static class UnitCount {
        final Unit unit;
        final int filled;
        UnitCount(Unit u, int f){ unit = u; filled = f; }
    }

    static java.util.List<UnitCount> unitsByDensity(int[][] b) {
        java.util.List<UnitCount> list = new java.util.ArrayList<>();
        // rows
        for (int r = 0; r < N; r++) {
            int filled = countFilledRow(b, r);
            if (filled < N) list.add(new UnitCount(new Unit(UnitType.ROW, r), filled));
        }
        // cols
        for (int c = 0; c < N; c++) {
            int filled = countFilledCol(b, c);
            if (filled < N) list.add(new UnitCount(new Unit(UnitType.COL, c), filled));
        }

        for (int box = 0; box < 9; box++) {
            int filled = countFilledBox(b, box);
            if (filled < N) list.add(new UnitCount(new Unit(UnitType.BOX, box), filled));
        }

        list.sort((a, b2) -> Integer.compare(b2.filled, a.filled));
        return list;
    }

    static int countFilledRow(int[][] b, int r){
        int cnt = 0;
        for (int c = 0; c < N; c++){
            if (b[r][c] != 0){
                cnt++;
            }
        }
        return cnt;
    }

    static int countFilledCol(int[][] b, int c){
        int cnt = 0;
        for (int r = 0; r < N; r++){
            if (b[r][c] != 0){
                cnt++;
            }
        }
        return cnt;
    }

    static int boxRowStart(int box) { return (box / 3) * 3; }
    static int boxColStart(int box) { return (box % 3) * 3; }

    static int countFilledBox(int[][] b, int box) {
        int br = boxRowStart(box), bc = boxColStart(box);
        int cnt = 0;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if (b[r][c] != 0) cnt++;
        return cnt;
    }


    static boolean workOneUnit(int[][] board, Unit u) {
        boolean changed = false;
        switch (u.type) {
            case ROW:
                changed = fillHiddenSinglesInRow(board, u.index);
                if (!changed) changed = fillNakedSinglesInRow(board, u.index);
                return changed;
            case COL:
                changed = fillHiddenSinglesInCol(board, u.index);
                if (!changed) changed = fillNakedSinglesInCol(board, u.index);
                return changed;
            case BOX:
                changed = fillHiddenSinglesInBox(board, u.index);
                if (!changed) changed = fillNakedSinglesInBox(board, u.index);
                return changed;
            default:
                return false;
        }
    }

    // ===================== HIDDEN SINGLES =====================

    static boolean fillHiddenSinglesInRow(int[][] b, int r){
        boolean changed = false;
        boolean[] present = new boolean[10];
        int[] empties = new int[N];
        int emptiesCount = 0;

        for (int c = 0; c < N; c++){
            int v = b[r][c];
            if (v == 0){
                empties[emptiesCount++] = c;
            } else {
                present[v] = true;
            }
        }
        if (emptiesCount == 0) return false;

        for (int d = 1; d <= 9; d++){
            if (present[d]) continue;
            int onlyCol = -1, places = 0;
            for (int i = 0; i < emptiesCount; i++){
                int c = empties[i];
                if (isCandidate(b,r,c,d)){
                    places++;
                    onlyCol = c;
                    if (places > 1) break;
                }
            }
            if (places == 1){
                b[r][onlyCol] = d;
                changed = true;
            }
        }
        return changed;
    }

    static boolean fillHiddenSinglesInCol(int[][] b, int c){
        boolean changed = false;
        boolean[] present = new boolean[10];
        int[] empties = new int[N];
        int emptiesCount = 0;

        for (int r = 0; r < N; r++){
            int v = b[r][c];
            if (v == 0){
                empties[emptiesCount++] = r;
            } else {
                present[v] = true;
            }
        }
        if (emptiesCount == 0) return false;

        for (int d = 1; d <= 9; d++){
            if (present[d]) continue;
            int onlyRow = -1, places = 0;
            for (int i = 0; i < emptiesCount; i++){
                int r = empties[i];
                if (isCandidate(b,r,c,d)){
                    places++;
                    onlyRow = r;
                    if (places > 1) break;
                }
            }
            if (places == 1){
                b[onlyRow][c] = d;
                changed = true;
            }
        }
        return changed;
    }

    static boolean fillHiddenSinglesInBox(int[][] b, int box) {
        boolean changed = false;
        boolean[] present = new boolean[10];
        java.util.List<int[]> empties = new java.util.ArrayList<>();

        int br = boxRowStart(box), bc = boxColStart(box);
        for (int r = br; r < br + 3; r++) {
            for (int c = bc; c < bc + 3; c++) {
                int v = b[r][c];
                if (v == 0) empties.add(new int[]{r,c}); else present[v] = true;
            }
        }
        if (empties.isEmpty()) return false;

        for (int d = 1; d <= 9; d++) {
            if (present[d]) continue;
            int[] only = null; int places = 0;
            for (int[] rc : empties) {
                int r = rc[0], c = rc[1];
                if (isCandidate(b, r, c, d)) {
                    places++; only = rc;
                    if (places > 1) break;
                }
            }
            if (places == 1) {
                b[only[0]][only[1]] = d;
                changed = true;
            }
        }
        return changed;
    }

    // ===================== NAKED SINGLES =====================

    static boolean fillNakedSinglesInRow(int[][] b, int r) {
        boolean changed = false, progress;
        do {
            progress = false;
            for (int c = 0; c < N; c++) {
                if (b[r][c] != 0) continue;
                int lastDigit = -1, count = 0;
                for (int d = 1; d <= 9; d++) {
                    if (isCandidate(b, r, c, d)) {
                        count++; lastDigit = d;
                        if (count > 1) break;
                    }
                }
                if (count == 1) {
                    b[r][c] = lastDigit;
                    progress = true; changed = true;
                }
            }
        } while (progress);
        return changed;
    }

    static boolean fillNakedSinglesInCol(int[][] b, int c) {
        boolean changed = false, progress;
        do {
            progress = false;
            for (int r = 0; r < N; r++) {
                if (b[r][c] != 0) continue;
                int lastDigit = -1, count = 0;
                for (int d = 1; d <= 9; d++) {
                    if (isCandidate(b, r, c, d)) {
                        count++; lastDigit = d;
                        if (count > 1) break;
                    }
                }
                if (count == 1) {
                    b[r][c] = lastDigit;
                    progress = true; changed = true;
                }
            }
        } while (progress);
        return changed;
    }

    static boolean fillNakedSinglesInBox(int[][] b, int box) {
        boolean changed = false, progress;
        int br = boxRowStart(box), bc = boxColStart(box);
        do {
            progress = false;
            for (int r = br; r < br + 3; r++) {
                for (int c = bc; c < bc + 3; c++) {
                    if (b[r][c] != 0) continue;
                    int last = -1, count = 0;
                    for (int d = 1; d <= 9; d++) {
                        if (isCandidate(b, r, c, d)) {
                            count++; last = d;
                            if (count > 1) break;
                        }
                    }
                    if (count == 1) {
                        b[r][c] = last;
                        progress = true; changed = true;
                    }
                }
            }
        } while (progress);
        return changed;
    }

    // ===================== CONSTRAINT CHECKS & UTIL =====================

    static boolean isCandidate(int[][] b, int r, int c, int d){
        if (b[r][c] != 0){
            return false;
        }
        // row
        for (int cc = 0; cc < N; cc++){
            if (b[r][cc] == d){
                return false;
            }
        }
        // col
        for (int rr = 0; rr < N; rr++){
            if (b[rr][c] == d){
                return false;
            }
        }
        // box
        int br = (r / 3) * 3;
        int bc = (c / 3) * 3;
        for (int rr = br; rr < br + 3; rr++){
            for (int cc = bc; cc < bc + 3; cc++){
                if (b[rr][cc] == d){
                    return false;
                }
            }
        }
        return true;
    }

    static int[][] loadBoardFromFile(String path) throws IOException {
        String raw = Files.readString(Path.of(path));

        StringBuilder sb = new StringBuilder(81);
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if ((ch >= '0' && ch <= '9') || ch == '.') sb.append(ch);
        }
        if (sb.length() != 81) {
            throw new IllegalArgumentException("File must contain exactly 81 characters of 0-9 or '.', got " + sb.length());
        }
        int[][] board = new int[N][N];
        int k = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                char ch = sb.charAt(k++);
                board[r][c] = (ch == '.' ? 0 : (ch - '0'));
            }
        }
        return board;
    }


    static boolean isFull(int[][] b) {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (b[r][c] == 0) return false;
        return true;
    }

    static void printBoard(int[][] b) {
        System.out.println("+-------+-------+-------+");
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (c % 3 == 0) System.out.print("| ");
                System.out.print((b[r][c] == 0 ? ". " : (b[r][c] + " ")));
            }
            System.out.println("|");
            if (r % 3 == 2) System.out.println("+-------+-------+-------+");
        }
    }
}

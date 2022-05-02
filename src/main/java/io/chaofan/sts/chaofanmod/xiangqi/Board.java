package io.chaofan.sts.chaofanmod.xiangqi;

import io.chaofan.sts.chaofanmod.xiangqi.piece.*;

import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int RIVER_MIN = 4;
    public static final int RIVER_MAX = 5;
    public static final int BOARD_WIDTH = 9;
    public static final int BOARD_HEIGHT = 10;

    private final PieceBase[][] board;

    public Board() {
        this(new PieceBase[BOARD_WIDTH][BOARD_HEIGHT]);
    }

    protected Board(PieceBase[][] board) {
        this.board = board;
    }

    public boolean hasPieceAt(int x, int y) {
        return isInsideBoard(x, y) && board[x][y] != null;
    }

    public boolean isInsideBoard(int x, int y) {
        return x >= 0 && x < Board.BOARD_WIDTH && y >= 0 && y < Board.BOARD_HEIGHT;
    }

    public PieceBase getPieceAt(int x, int y) {
        return isInsideBoard(x, y) ? board[x][y] : null;
    }

    public void setPiece(PieceBase piece) {
        if (!isInsideBoard(piece.x, piece.y)) {
            return;
        }

        board[piece.x][piece.y] = piece;
    }

    public void clearPiece(int x, int y) {
        if (!isInsideBoard(x, y)) {
            return;
        }

        board[x][y] = null;
    }

    public void movePiece(int fromX, int fromY, int toX, int toY) {
        PieceBase piece = getPieceAt(fromX, fromY);
        if (piece != null) {
            setPiece(piece.moveTo(toX, toY));
            clearPiece(fromX, fromY);
        }
    }

    public List<PieceBase> getAllPieces() {
        List<PieceBase> pieces = new ArrayList<>();

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                PieceBase piece = board[x][y];
                if (piece != null) {
                    pieces.add(piece);
                }
            }
        }

        return pieces;
    }

    public List<PieceBase> getAllPieces(boolean isFirstPlayer) {
        List<PieceBase> pieces = new ArrayList<>();

        for (int y = 0; y < BOARD_HEIGHT; y++) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                PieceBase piece = board[x][y];
                if (piece != null) {
                    if (piece.isFirstPlayer == isFirstPlayer) {
                        pieces.add(piece);
                    }
                }
            }
        }

        return pieces;
    }

    public void draw() {
        for (int y = BOARD_HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < BOARD_WIDTH; x++) {
                PieceBase p = board[x][y];
                if (x != 0) {
                    System.out.print("一");
                }
                if (p == null) {
                    System.out.print(x == 0 || x == BOARD_WIDTH - 1 ? "丨" : (y >= RIVER_MIN && y <= RIVER_MAX ? "一" : "十"));
                } else {
                    if (p.isFirstPlayer) {
                        System.out.print("\033[31m");
                    }
                    System.out.print(p.getPieceName());
                    if (p.isFirstPlayer) {
                        System.out.print("\033[0m");
                    }
                }
            }
            System.out.println();
        }
    }

    public void reset() {
        setPiece(new Che(0, 0, true));
        setPiece(new Che(8, 0, true));
        setPiece(new Ma(1, 0, true));
        setPiece(new Ma(7, 0, true));
        setPiece(new Xiang(2, 0, true));
        setPiece(new Xiang(6, 0, true));
        setPiece(new Shi(3, 0, true));
        setPiece(new Shi(5, 0, true));
        setPiece(new Jiang(4, 0, true));
        setPiece(new Pao(1, 2, true));
        setPiece(new Pao(7, 2, true));
        setPiece(new Zu(0, 3, true));
        setPiece(new Zu(2, 3, true));
        setPiece(new Zu(4, 3, true));
        setPiece(new Zu(6, 3, true));
        setPiece(new Zu(8, 3, true));

        setPiece(new Che(0, 9, false));
        setPiece(new Che(8, 9, false));
        setPiece(new Ma(1, 9, false));
        setPiece(new Ma(7, 9, false));
        setPiece(new Xiang(2, 9, false));
        setPiece(new Xiang(6, 9, false));
        setPiece(new Shi(3, 9, false));
        setPiece(new Shi(5, 9, false));
        setPiece(new Jiang(4, 9, false));
        setPiece(new Pao(1, 7, false));
        setPiece(new Pao(7, 7, false));
        setPiece(new Zu(0, 6, false));
        setPiece(new Zu(2, 6, false));
        setPiece(new Zu(4, 6, false));
        setPiece(new Zu(6, 6, false));
        setPiece(new Zu(8, 6, false));
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Board clone() {
        PieceBase[][] cloned = new PieceBase[BOARD_WIDTH][BOARD_HEIGHT];
        for (int x = 0; x < BOARD_WIDTH; x++) {
            cloned[x] = board[x].clone();
        }
        return new Board(cloned);
    }
}

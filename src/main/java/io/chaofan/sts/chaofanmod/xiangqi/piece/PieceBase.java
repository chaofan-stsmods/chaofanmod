package io.chaofan.sts.chaofanmod.xiangqi.piece;

import io.chaofan.sts.chaofanmod.xiangqi.Board;
import io.chaofan.sts.chaofanmod.xiangqi.Position;

import java.util.List;

public abstract class PieceBase {
    public final int x;
    public final int y;
    public final boolean isFirstPlayer;

    protected boolean showMoveLikeMa = false;

    public PieceBase(int x, int y, boolean isFirstPlayer) {
        this.x = x;
        this.y = y;
        this.isFirstPlayer = isFirstPlayer;
    }

    public abstract void getPossibleMoves(Board board, List<Position> result);

    protected boolean addMoveIfValid(Board board, List<Position> result, int x, int y) {
        if (board.isInsideBoard(x, y)) {
            PieceBase existingPiece = board.getPieceAt(x, y);
            if (existingPiece == null || existingPiece.isFirstPlayer != isFirstPlayer) {
                result.add(new Position(x, y));
                return existingPiece == null;
            }
        }

        return false;
    }

    protected boolean addMoveIfEmpty(Board board, List<Position> result, int x, int y) {
        if (board.isInsideBoard(x, y)) {
            PieceBase existingPiece = board.getPieceAt(x, y);
            if (existingPiece == null) {
                result.add(new Position(x, y));
                return true;
            }
        }

        return false;
    }

    public abstract String getPieceName();

    public abstract PieceBase moveTo(int x, int y);

    public abstract float getPieceScore();

    public String getMoveName(int x, int y) {
        if (y == this.y) {
            return getPieceName() + getColumnString(this.x) + "平" + getColumnString(x);
        } else {
            return getPieceName() + getColumnString(this.x) + getRowMovement(x, y);
        }
    }

    private static final String[] NUMBER_TO_STRING = new String[] {
            "", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
    };
    protected String numberToString(int number) {
        if (isFirstPlayer) {
            return NUMBER_TO_STRING[number];
        } else {
            return Integer.toString(number);
        }
    }

    protected String getColumnString(int x) {
        if (isFirstPlayer) {
            return numberToString(9 - x);
        } else {
            return numberToString(x + 1);
        }
    }

    protected String getRowMovement(int x, int y) {
        if (showMoveLikeMa) {
            return getRowMovementLikeMa(x, y);
        }
        if (isFirstPlayer) {
            if (y > this.y) {
                return "进" + numberToString(y - this.y);
            } else {
                return "退" + numberToString(this.y - y);
            }
        } else {
            if (y < this.y) {
                return "进" + numberToString(this.y - y);
            } else {
                return "退" + numberToString(y - this.y);
            }
        }
    }

    private String getRowMovementLikeMa(int x, int y) {
        if (isFirstPlayer) {
            if (y > this.y) {
                return "进" + getColumnString(x);
            } else {
                return "退" + getColumnString(x);
            }
        } else {
            if (y < this.y) {
                return "进" + getColumnString(x);
            } else {
                return "退" + getColumnString(x);
            }
        }
    }
}

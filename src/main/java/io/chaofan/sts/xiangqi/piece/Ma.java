package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Ma extends PieceBase {

    public Ma(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
        showMoveLikeMa = true;
    }

    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        if (!board.hasPieceAt(x, y - 1)) {
            addMoveIfValid(board, result, x - 1, y - 2);
            addMoveIfValid(board, result, x + 1, y - 2);
        }
        if (!board.hasPieceAt(x, y + 1)) {
            addMoveIfValid(board, result, x - 1, y + 2);
            addMoveIfValid(board, result, x + 1, y + 2);
        }
        if (!board.hasPieceAt(x - 1, y)) {
            addMoveIfValid(board, result, x - 2, y - 1);
            addMoveIfValid(board, result, x - 2, y + 1);
        }
        if (!board.hasPieceAt(x + 1, y)) {
            addMoveIfValid(board, result, x + 2, y - 1);
            addMoveIfValid(board, result, x + 2, y + 1);
        }
    }

    @Override
    public String getPieceName() {
        return "马";
    }

    @Override
    public boolean canAttack(Board board, int x, int y) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        if (Math.abs(xDiff) + Math.abs(yDiff) != 3) {
            return false;
        }

        if (xDiff == 0 || yDiff == 0) {
            return false;
        }

        if (xDiff == 2) {
            return !board.hasPieceAt(this.x + 1, this.y);
        }

        if (xDiff == -2) {
            return !board.hasPieceAt(this.x - 1, this.y);
        }

        if (yDiff == 2) {
            return !board.hasPieceAt(this.x, this.y + 1);
        }

        if (yDiff == -2) {
            return !board.hasPieceAt(this.x, this.y - 1);
        }

        return false;
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Ma(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 400;
    }
}

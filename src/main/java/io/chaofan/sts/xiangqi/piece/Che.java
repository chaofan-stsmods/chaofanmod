package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Che extends PieceBase {
    public Che(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        int v = x;
        while (addMoveIfValid(board, result, --v, y));
        v = x;
        while (addMoveIfValid(board, result, ++v, y));
        v = y;
        while (addMoveIfValid(board, result, x, --v));
        v = y;
        while (addMoveIfValid(board, result, x, ++v));
    }

    @Override
    public String getPieceName() {
        return "车";
    }

    @Override
    public boolean canAttack(Board board, int x, int y) {
        if (x != this.x && y != this.y) {
            return false;
        }

        if (x == this.x) {
            int yEnd = Math.max(y, this.y);
            for (int i = Math.min(y, this.y) + 1; i < yEnd; i++) {
                if (board.hasPieceAt(x, i)) {
                    return false;
                }
            }
            return true;
        } else {
            int xEnd = Math.max(x, this.x);
            for (int i = Math.min(x, this.x) + 1; i < xEnd; i++) {
                if (board.hasPieceAt(i, y)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Che(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 1000;
    }
}

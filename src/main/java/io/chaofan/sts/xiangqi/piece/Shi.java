package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Shi extends PieceBase {

    public Shi(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
        showMoveLikeMa = true;
    }

    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        boolean canMoveDown = isFirstPlayer ? y > 0 : y > 7;
        boolean canMoveUp = isFirstPlayer ? y < 2 : y < 9;
        if (x > 3) {
            if (canMoveDown) {
                addMoveIfValid(board, result, x - 1, y - 1);
            }
            if (canMoveUp) {
                addMoveIfValid(board, result, x - 1, y + 1);
            }
        }
        if (x < 5) {
            if (canMoveDown) {
                addMoveIfValid(board, result, x + 1, y - 1);
            }
            if (canMoveUp) {
                addMoveIfValid(board, result, x + 1, y + 1);
            }
        }
    }

    @Override
    public String getPieceName() {
        return "仕";
    }

    @Override
    public boolean canAttack(Board board, int x, int y) {
        if (x < 3 || x > 5) {
            return false;
        }

        int xDiff = x - this.x;
        int yDiff = y - this.y;
        if (Math.abs(xDiff) != 1 || Math.abs(yDiff) != 1) {
            return false;
        }

        if (isFirstPlayer) {
            return y <= 2;
        } else {
            return y >= 7;
        }
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Shi(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 200;
    }
}

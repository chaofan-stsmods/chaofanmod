package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Xiang extends PieceBase {

    public Xiang(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
        showMoveLikeMa = true;
    }

    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        boolean canMoveDown = isFirstPlayer ? y > 1 : y > 6;
        boolean canMoveUp = isFirstPlayer ? y < 2 : y < 9;
        if (canMoveDown) {
            if (!board.hasPieceAt(x - 1, y - 1)) {
                addMoveIfValid(board, result, x - 2, y - 2);
            }
            if (!board.hasPieceAt(x + 1, y - 1)) {
                addMoveIfValid(board, result, x + 2, y - 2);
            }
        }
        if (canMoveUp) {
            if (!board.hasPieceAt(x - 1, y + 1)) {
                addMoveIfValid(board, result, x - 2, y + 2);
            }
            if (!board.hasPieceAt(x + 1, y + 1)) {
                addMoveIfValid(board, result, x + 2, y + 2);
            }
        }
    }

    @Override
    public String getPieceName() {
        return isFirstPlayer ? "相" : "象";
    }

    @Override
    public boolean canAttack(Board board, int x, int y) {
        if ((isFirstPlayer && y > Board.RIVER_MIN) || (!isFirstPlayer && y < Board.RIVER_MAX)) {
            return false;
        }

        int xDiff = x - this.x;
        int yDiff = y - this.y;
        if (Math.abs(xDiff) != 2 || Math.abs(yDiff) != 2) {
            return false;
        }

        return !board.hasPieceAt(this.x + xDiff / 2, this.y + yDiff / 2);
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Xiang(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 200;
    }
}

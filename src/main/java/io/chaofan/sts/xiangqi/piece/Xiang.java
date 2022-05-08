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
    public PieceBase moveTo(int x, int y) {
        return new Xiang(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 200;
    }
}

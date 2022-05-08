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
    public PieceBase moveTo(int x, int y) {
        return new Ma(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 400;
    }
}

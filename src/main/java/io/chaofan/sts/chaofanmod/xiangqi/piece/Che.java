package io.chaofan.sts.chaofanmod.xiangqi.piece;

import io.chaofan.sts.chaofanmod.xiangqi.Board;
import io.chaofan.sts.chaofanmod.xiangqi.Position;

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
    public PieceBase moveTo(int x, int y) {
        return new Che(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 1000;
    }
}

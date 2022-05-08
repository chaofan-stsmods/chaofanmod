package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Pao extends PieceBase {

    public Pao(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        int v = x;
        while (addMoveIfEmpty(board, result, --v, y));
        while (board.isInsideBoard(--v, y) && !board.hasPieceAt(v, y));
        addMoveIfValid(board, result, v, y);

        v = x;
        while (addMoveIfEmpty(board, result, ++v, y));
        while (board.isInsideBoard(++v, y) && !board.hasPieceAt(v, y));
        addMoveIfValid(board, result, v, y);

        v = y;
        while (addMoveIfEmpty(board, result, x, --v));
        while (board.isInsideBoard(x, --v) && !board.hasPieceAt(x, v));
        addMoveIfValid(board, result, x, v);

        v = y;
        while (addMoveIfEmpty(board, result, x, ++v));
        while (board.isInsideBoard(x, ++v) && !board.hasPieceAt(x, v));
        addMoveIfValid(board, result, x, v);
    }

    @Override
    public String getPieceName() {
        return "炮";
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Pao(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 450;
    }
}

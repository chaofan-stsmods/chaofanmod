package io.chaofan.sts.chaofanmod.xiangqi.piece;

import io.chaofan.sts.chaofanmod.xiangqi.Board;
import io.chaofan.sts.chaofanmod.xiangqi.Position;

import java.util.List;

public class Jiang extends PieceBase {

    public Jiang(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
    }

    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        if (x > 3) {
            addMoveIfValid(board, result, x - 1, y);
        }
        if (x < 5) {
            addMoveIfValid(board, result, x + 1, y);
        }
        if (isFirstPlayer) {
            addMoveIfValid(board, result, x, y - 1);
            if (y < 2) {
                addMoveIfValid(board, result, x, y + 1);
            }
            for (int v = y + 1; v < Board.BOARD_HEIGHT; v++) {
                PieceBase p = board.getPieceAt(x, v);
                if (p instanceof Jiang) {
                    addMoveIfValid(board, result, x, v);
                    break;
                } else if (p != null) {
                    break;
                }
            }
        } else {
            addMoveIfValid(board, result, x, y + 1);
            if (y > 7) {
                addMoveIfValid(board, result, x, y - 1);
            }
            for (int v = y - 1; v >= 0; v--) {
                PieceBase p = board.getPieceAt(x, v);
                if (p instanceof Jiang) {
                    addMoveIfValid(board, result, x, v);
                    break;
                } else if (p != null) {
                    break;
                }
            }
        }
    }

    @Override
    public String getPieceName() {
        return isFirstPlayer ? "帅" : "将";
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Jiang(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        return 999999;
    }
}

package io.chaofan.sts.xiangqi.piece;

import io.chaofan.sts.xiangqi.Board;
import io.chaofan.sts.xiangqi.Position;

import java.util.List;

public class Zu extends PieceBase {

    public Zu(int x, int y, boolean isFirstPlayer) {
        super(x, y, isFirstPlayer);
    }

    @Override
    public void getPossibleMoves(Board board, List<Position> result) {
        addMoveIfValid(board, result, x, y + (isFirstPlayer ? 1 : -1));
        if ((isFirstPlayer && y > Board.RIVER_MIN) || (!isFirstPlayer && y < Board.RIVER_MAX)) {
            addMoveIfValid(board, result, x - 1, y);
            addMoveIfValid(board, result, x + 1, y);
        }
    }

    @Override
    public String getPieceName() {
        return isFirstPlayer ? "兵" : "卒";
    }

    @Override
    public PieceBase moveTo(int x, int y) {
        return new Zu(x, y, isFirstPlayer);
    }

    @Override
    public float getPieceScore() {
        if (isFirstPlayer) {
            if (y <= Board.RIVER_MIN || y == Board.BOARD_HEIGHT - 1) {
                return 100;
            } else {
                return 200;
            }
        } else {
            if (y >= Board.RIVER_MAX || y == 0) {
                return 100;
            } else {
                return 200;
            }
        }
    }
}

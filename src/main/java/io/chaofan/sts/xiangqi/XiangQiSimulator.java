package io.chaofan.sts.xiangqi;

import io.chaofan.sts.xiangqi.piece.Jiang;
import io.chaofan.sts.xiangqi.piece.Ma;
import io.chaofan.sts.xiangqi.piece.PieceBase;
import io.chaofan.sts.xiangqi.piece.Shi;

import java.util.ArrayList;
import java.util.List;

public class XiangQiSimulator extends GeneralSimulator<XiangQiSimulator.State> {
    public static void main(String[] args) {
        Board board = new Board();

        board.setPiece(new Jiang(3, 0, true));
        board.setPiece(new Ma(7, 7, true));
        board.setPiece(new Jiang(4, 9, false));
        board.setPiece(new Shi(5, 9, false));

        State root = new State();
        root.board = board;
        root.isNextMoveForFirstPlayer = true;
        root.score = 0;

        XiangQiSimulator simulator = new XiangQiSimulator();

        for (int i = 0; i < 20; i++) {
            root.board.draw();
            simulator.simulate(root, 100000, 10);

            System.out.println(root.score);

            State nextState = root.chosenState;
            while (nextState != null) {
                System.out.println(nextState);
                nextState = nextState.chosenState;
            }

            root = root.chosenState;
            if (root == null) {
                break;
            }
            root.children = null;
            root.depth = 0;
            root.parent = null;
        }
    }

    @Override
    protected List<State> expandState(State state) {
        List<State> children = new ArrayList<>();

        List<Position> positions = new ArrayList<>();
        for (PieceBase piece : state.board.getAllPieces(state.isNextMoveForFirstPlayer)) {
            positions.clear();
            piece.getPossibleMoves(state.board, positions);
            for (Position move : positions) {
                State child = new State();
                child.moveFrom = new Position(piece.x, piece.y);
                child.moveTo = move;
                Board childBoard = state.board.clone();
                childBoard.movePiece(piece.x, piece.y, move.x, move.y);
                child.board = childBoard;
                children.add(child);
            }
        }

        return children;
    }

    @Override
    protected void estimateScore(State state) {
        Board board = state.board;
        float pieceScore = board.getAllPieces()
                .stream()
                .reduce(0f, (v, p) -> v + p.getPieceScore() * (p.isFirstPlayer ? 1 : -1), Float::sum);
        state.estimatedScore = state.score = pieceScore;
    }

    public static class State extends GeneralSimulator.GeneralSimulatorState<State> {
        Board board;
        Position moveFrom;
        Position moveTo;
        float estimatedScore;

        @Override
        public String toString() {
            return String.format("%s (%f <- %f)", getMoveName(), score, estimatedScore);
        }

        private String getMoveName() {
            if (moveFrom == null || parent == null) {
                return "开局";
            }

            PieceBase p = parent.board.getPieceAt(moveFrom.x, moveFrom.y);
            if (p == null) {
                return "未知";
            }

            return p.getMoveName(moveTo.x, moveTo.y);
        }
    }
}

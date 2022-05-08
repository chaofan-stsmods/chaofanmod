package io.chaofan.sts.xiangqi;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class GeneralSimulator<T extends GeneralSimulator.GeneralSimulatorState<T>> {

    public void simulate(T root, int count, int maxDepth) {
        SortedSet<T> firstPlayerStates = new TreeSet<>();
        SortedSet<T> secondPlayerStates = new TreeSet<>();

        estimateScore(root);
        if (root.isNextMoveForFirstPlayer) {
            firstPlayerStates.add(root);
        } else {
            secondPlayerStates.add(root);
        }

        while ((!firstPlayerStates.isEmpty() || !secondPlayerStates.isEmpty()) && --count > 0) {
            while (!firstPlayerStates.isEmpty()) {
                T state = firstPlayerStates.first();
                firstPlayerStates.remove(state);
                if (Math.abs(state.score) <= 100000 && state.depth < maxDepth) {
                    List<T> children = expandState(state, secondPlayerStates);
                    updateScore(state, children);
                    break;
                }
            }

            while (!secondPlayerStates.isEmpty()) {
                T state = secondPlayerStates.first();
                secondPlayerStates.remove(state);
                if (Math.abs(state.score) <= 100000 && state.depth < maxDepth) {
                    List<T> children = expandState(state, firstPlayerStates);
                    updateScore(state, children);
                    break;
                }
            }
        }
    }

    private List<T> expandState(T state, SortedSet<T> childStates) {
        List<T> children = expandState(state);
        for (T child : children) {
            child.parent = state;
            child.depth = state.depth + 1;
            child.isNextMoveForFirstPlayer = !state.isNextMoveForFirstPlayer;
            estimateScore(child);
            childStates.add(child);
        }

        state.children = children;

        return children;
    }

    private void updateScore(T state, List<T> children) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        T minChildState = null;
        T maxChildState = null;
        for (T childState : children) {
            if (childState.score > max) {
                maxChildState = childState;
                max = childState.score;
            }
            if (childState.score < min) {
                minChildState = childState;
                min = childState.score;
            }
        }

        if (state.isNextMoveForFirstPlayer) {
            state.score = max;
            state.chosenState = maxChildState;
        } else {
            state.score = min;
            state.chosenState = minChildState;
        }

        if (state.parent != null) {
            updateScore(state.parent, state.parent.children);
        }

        /*
        T current = state;
        T parent = state.parent;
        while (parent != null) {
            if (parent.isNextMoveForFirstPlayer) {
                if (current.score > parent.score) {
                    parent.score = current.score;
                    parent.chosenState = current;
                }
            } else {
                if (current.score < parent.score) {
                    parent.score = current.score;
                    parent.chosenState = current;
                }
            }
            current = parent;
            parent = current.parent;
        }*/
    }

    protected abstract List<T> expandState(T state);

    protected abstract void estimateScore(T state);

    public static abstract class GeneralSimulatorState<T extends GeneralSimulatorState<T>> implements Comparable<T> {
        static long nextId;
        final long id = ++nextId;
        T parent;
        List<T> children;
        T chosenState;
        boolean isNextMoveForFirstPlayer;
        float score;
        int depth;

        @Override
        public int compareTo(T o) {
            /*
            float v1 = Math.abs(score) - depth;
            float v2 = Math.abs(o.score) - depth;
            return v1 == v2 ? Long.compare(id, o.id) : Float.compare(v1, v2);
             */
            return depth == o.depth ? Long.compare(id, o.id) : Integer.compare(depth, o.depth);
        }
    }
}

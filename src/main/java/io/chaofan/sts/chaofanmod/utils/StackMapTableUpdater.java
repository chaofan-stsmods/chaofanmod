package io.chaofan.sts.chaofanmod.utils;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.StackMapTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class StackMapTableUpdater extends StackMapTable.Walker {
    static class NewOffset {
        int offset;
        BiConsumer<StackMapTable.Writer, Integer> operation;
        boolean isReplace;
    }

    private final StackMapTable.Writer writer;
    private final List<NewOffset> newOffsets = new ArrayList<>();
    private int newOffsetIndex;
    private int offset;

    public StackMapTableUpdater(byte[] data) {
        super(data);
        this.writer = new StackMapTable.Writer(data.length);
    }

    public void addFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
        NewOffset newOffset = new NewOffset();
        newOffset.offset = offset;
        newOffset.operation = operation;
        newOffset.isReplace = false;
        newOffsets.add(newOffset);
    }

    public void replaceFrame(int offset, BiConsumer<StackMapTable.Writer, Integer> operation) {
        NewOffset newOffset = new NewOffset();
        newOffset.offset = offset;
        newOffset.operation = operation;
        newOffset.isReplace = true;
        newOffsets.add(newOffset);
    }

    public byte[] doIt() throws BadBytecode {
        this.newOffsets.sort(Comparator.comparingInt(a -> a.offset));
        this.newOffsetIndex = 0;
        this.offset = -1;
        this.parse();
        return this.writer.toByteArray();
    }

    @Override
    public void sameFrame(int pos, int offsetDelta) {
        offsetDelta = tryInsert(offsetDelta);
        if (offsetDelta >= 0) {
            this.writer.sameFrame(offsetDelta);
        }
    }

    @Override
    public void sameLocals(int pos, int offsetDelta, int stackTag, int stackData) {
        offsetDelta = tryInsert(offsetDelta);
        if (offsetDelta >= 0) {
            this.writer.sameLocals(offsetDelta, stackTag, stackData);
        }
    }

    @Override
    public void chopFrame(int pos, int offsetDelta, int k) {
        offsetDelta = tryInsert(offsetDelta);
        if (offsetDelta >= 0) {
            this.writer.chopFrame(offsetDelta, k);
        }
    }

    @Override
    public void appendFrame(int pos, int offsetDelta, int[] tags, int[] data) {
        offsetDelta = tryInsert(offsetDelta);
        if (offsetDelta >= 0) {
            this.writer.appendFrame(offsetDelta, tags, data);
        }
    }

    @Override
    public void fullFrame(int pos, int offsetDelta, int[] localTags, int[] localData, int[] stackTags, int[] stackData) {
        offsetDelta = tryInsert(offsetDelta);
        if (offsetDelta >= 0) {
            this.writer.fullFrame(offsetDelta, localTags, localData, stackTags, stackData);
        }
    }

    private int tryInsert(int offsetDelta) {
        boolean skipNextFrame = false;
        while (newOffsetIndex < this.newOffsets.size()) {
            NewOffset newOffset = this.newOffsets.get(newOffsetIndex);
            int nextSameFrameOffset = newOffset.offset;
            if (offset + offsetDelta + 1 > nextSameFrameOffset) {
                int newOffsetDelta = nextSameFrameOffset - offset;

                if (newOffset.isReplace) {
                    skipNextFrame = true;
                    newOffset.operation.accept(this.writer, offsetDelta);
                } else {
                    newOffset.operation.accept(this.writer, newOffsetDelta - 1);
                }

                offset += newOffsetDelta;
                offsetDelta -= newOffsetDelta;
                newOffsetIndex++;
                if (skipNextFrame) {
                    return -1;
                } else {
                    continue;
                }
            }

            break;
        }

        offset += offsetDelta + 1;
        return offsetDelta;
    }
}

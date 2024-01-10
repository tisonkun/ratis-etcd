/*
 * Copyright 2024 tison <wander4096@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tisonkun.ratis.etcd.mvcc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Comparator;
import javax.annotation.Nonnull;

/**
 * A revision indicates modification of the key-value space. The set of changes that
 * share same main revision changes the key-value space atomically.
 *
 * @param main the main revision of a set of changes that happen atomically.
 * @param sub  the sub revision of a change in a set of changes that happen atomically.
 *             Each change has different increasing sub revision in that set.
 */
public record Revision(long main, long sub) implements Comparable<Revision> {
    /**
     * The byte length of a normal revision. First 8 bytes is the {@link #main} in big-endian format.
     * The 9th byte is a '_'. The last 8 bytes is the {@link #sub} in big-endian format.
     */
    private static final int REV_BYTES_LEN = 8 + 1 + 8;

    /**
     * The byte length of marked revision. The first {@link #REV_BYTES_LEN} bytes
     * represents a normal revision. The last one byte is the mark.
     */
    private static final int MARKED_REV_BYTES_LEN = REV_BYTES_LEN + 1;

    private static final byte MARK_TOMBSTONE = 't';

    public static Revision create(long main) {
        return create(main, 0);
    }

    public static Revision create(long main, long sub) {
        return new Revision(main, sub);
    }

    public static Revision fromBytes(ByteBuf bytes) {
        if (bytes.readableBytes() != REV_BYTES_LEN) {
            throw new IllegalArgumentException("revision bytes has wrong length: " + bytes.readableBytes());
        }
        final ByteBuf wrapper = bytes.duplicate();
        final long main = wrapper.readLong();
        wrapper.readByte(); // '_'
        final long sub = wrapper.readLong();
        return create(main, sub);
    }

    public static boolean isTombstone(ByteBuf bytes) {
        if (bytes.readableBytes() != MARKED_REV_BYTES_LEN) {
            return false;
        }
        return bytes.getByte(MARKED_REV_BYTES_LEN - 1) == MARK_TOMBSTONE;
    }

    public ByteBuf toBytes() {
        return Unpooled.buffer(MARKED_REV_BYTES_LEN)
                .writeLong(main)
                .writeByte('_')
                .writeLong(sub);
    }

    public ByteBuf toTombstoneBytes() {
        return Unpooled.buffer(MARKED_REV_BYTES_LEN)
                .writeLong(main)
                .writeByte('_')
                .writeLong(sub)
                .writeByte('t');
    }

    @Override
    public int compareTo(@Nonnull Revision o) {
        return Comparator.comparing(Revision::main).thenComparing(Revision::sub).compare(this, o);
    }
}

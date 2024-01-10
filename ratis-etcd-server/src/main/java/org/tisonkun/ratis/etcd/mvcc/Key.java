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

import com.google.protobuf.ByteString;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import javax.annotation.Nonnull;
import lombok.Data;

@Data
public class Key implements Comparable<Key> {
    private static final byte[] INFINITY = new byte[0];

    public static byte[] infinity() {
        return INFINITY;
    }

    private final byte[] key;

    public Key(String key) {
        this(key.getBytes(StandardCharsets.UTF_8));
    }

    public Key(byte[] key) {
        this.key = key;
    }

    @Override
    public int compareTo(@Nonnull Key o) {
        return Arrays.compare(key, o.key);
    }

    public boolean isInfinite() {
        return Arrays.equals(this.key, INFINITY);
    }

    @Override
    public String toString() {
        return HexFormat.of().formatHex(key);
    }

    public ByteString toByteString() {
        return ByteString.copyFrom(key);
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeIndex {
    private final SortedMap<Key, KeyIndex> tree;

    public static TreeIndex create() {
        return new TreeIndex(new TreeMap<>());
    }

    public void put(Key key, Revision rev) {
        tree.compute(key, (k, v) -> {
            if (v != null) {
                v.put(rev.main(), rev.sub());
                return v;
            } else {
                return KeyIndex.create(k, rev.main(), rev.sub());
            }
        });
    }

    public void tombstone(Key key, Revision rev) throws RevisionNotFoundException {
        final KeyIndex ki = tree.get(key);
        if (ki != null) {
            ki.tombstone(rev.main(), rev.sub());
        } else {
            throw new RevisionNotFoundException();
        }
    }

    public KeyIndex.Get get(Key key, long rev) throws RevisionNotFoundException {
        return unsafeGet(key, rev);
    }

    public record Range(List<Revision> revisions, List<Key> keys, long total) {}

    public Range range(Key key, Key end, long rev) {
        return range(key, end, rev, 0);
    }

    public Range range(Key key, Key end, long rev, long limit) {
        final List<Revision> revisions = new ArrayList<>();
        final List<Key> keys = new ArrayList<>();
        final AtomicInteger count = new AtomicInteger();

        if (end == null) {
            try {
                final KeyIndex.Get result = unsafeGet(key, rev);
                revisions.add(result.modified());
                keys.add(key);
                count.incrementAndGet();
            } catch (RevisionNotFoundException ignore) {
                // not found - return empty result
            }
        } else {
            unsafeVisit(key, end, keyIndex -> {
                try {
                    final KeyIndex.Get result = keyIndex.get(rev);
                    if (limit <= 0 || revisions.size() < limit) {
                        revisions.add(result.modified());
                        keys.add(keyIndex.getKey());
                    }
                    count.incrementAndGet();
                } catch (RevisionNotFoundException ignore) {
                    // not found - skip
                }
                return true;
            });
        }

        return new Range(revisions, keys, count.get());
    }

    private KeyIndex.Get unsafeGet(Key key, long rev) throws RevisionNotFoundException {
        final KeyIndex ki = tree.get(key);
        if (ki != null) {
            return ki.get(rev);
        } else {
            throw new RevisionNotFoundException();
        }
    }

    private void unsafeVisit(Key key, Key end, Function<KeyIndex, Boolean> f) {
        for (Map.Entry<Key, KeyIndex> e : tree.tailMap(key).entrySet()) {
            if (!end.isInfinite() && e.getKey().compareTo(end) >= 0) {
                return;
            }
            if (!f.apply(e.getValue())) {
                return;
            }
        }
    }
}

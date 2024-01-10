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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyIndex {
    private final ByteBuf keys;
    private final List<Generation> generations;

    /**
     * The revision of the last modification.
     */
    private Revision modified;

    public static KeyIndex create(String keys, long main, long sub) {
        final ByteBuf bytes = Unpooled.buffer();
        bytes.writeCharSequence(keys, StandardCharsets.UTF_8);
        return create(bytes, main, sub);
    }

    public static KeyIndex create(ByteBuf keys, long main, long sub) {
        final KeyIndex ki = new KeyIndex(keys, new ArrayList<>());
        ki.modified = Revision.create(0);
        ki.put(main, sub);
        return ki;
    }

    public void put(long main, long sub) {
        final Revision rev = Revision.create(main, sub);
        Preconditions.checkState(
                rev.compareTo(modified) > 0,
                "'put' with an unexpected smaller revision (given: %s, modified: %s, key: %s)",
                rev,
                modified,
                keys);
        if (generations.isEmpty()) {
            generations.add(Generation.create());
        }
        generations.getLast().addRevision(rev);
        modified = rev;
    }

    public void tombstone(long main, long sub) throws RevisionNotFoundException {
        Preconditions.checkState(isNotEmpty(), "'tombstone' got an unexpected empty keyIndex (key: {%s})", keys);
        if (generations.getLast().isEmpty()) {
            throw new RevisionNotFoundException();
        }
        put(main, sub);
        generations.add(Generation.create());
    }

    public record Get(Revision modified, Revision created, long version) {}

    public Get get(long rev) throws RevisionNotFoundException {
        Preconditions.checkState(isNotEmpty(), "'get' got an unexpected empty keyIndex (key: {%s})", keys);
        final Generation g = findGeneration(rev).orElseThrow(RevisionNotFoundException::new);
        final int idx = g.walk(revision -> revision.main() > rev).orElseThrow(RevisionNotFoundException::new);
        return new Get(
                g.getRevisions().get(idx),
                g.getCreated(),
                g.getVersion() - (g.getRevisions().size() - idx - 1));
    }

    public boolean isNotEmpty() {
        if (generations.size() > 1) {
            return true;
        }
        return !generations.getFirst().isEmpty();
    }

    @VisibleForTesting
    Optional<Generation> findGeneration(long rev) {
        final int last = generations.size() - 1;
        int cg = last;
        while (cg >= 0) {
            if (generations.get(cg).getRevisions().isEmpty()) {
                cg -= 1;
                continue;
            }
            final Generation g = generations.get(cg);
            if (cg != last) {
                final long tomb = g.getRevisions().getLast().main();
                if (tomb <= rev) {
                    return Optional.empty();
                }
            }
            if (g.getRevisions().getFirst().main() <= rev) {
                return Optional.of(g);
            }
            cg -= 1;
        }
        return Optional.empty();
    }
}

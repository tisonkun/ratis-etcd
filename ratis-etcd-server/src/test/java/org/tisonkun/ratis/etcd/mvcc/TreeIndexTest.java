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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TreeIndexTest {
    @Test
    void testTreeIndexGet() throws Exception {
        final TreeIndex ti = TreeIndex.create();
        final Key key = new Key("foo");
        final Revision created = Revision.create(2);
        final Revision modified = Revision.create(4);
        final Revision deleted = Revision.create(6);
        ti.put(key, created);
        ti.put(key, modified);
        ti.tombstone(key, deleted);

        assertThatThrownBy(() -> ti.get(key, 0)).isExactlyInstanceOf(RevisionNotFoundException.class);
        assertThatThrownBy(() -> ti.get(key, 1)).isExactlyInstanceOf(RevisionNotFoundException.class);
        assertThat(ti.get(key, 2)).isEqualTo(new KeyIndex.Get(created, created, 1));
        assertThat(ti.get(key, 3)).isEqualTo(new KeyIndex.Get(created, created, 1));
        assertThat(ti.get(key, 4)).isEqualTo(new KeyIndex.Get(modified, created, 2));
        assertThat(ti.get(key, 5)).isEqualTo(new KeyIndex.Get(modified, created, 2));
        assertThatThrownBy(() -> ti.get(key, 6)).isExactlyInstanceOf(RevisionNotFoundException.class);
    }

    @Test
    void testTreeIndexTombstone() throws Exception {
        final TreeIndex ti = TreeIndex.create();
        final Key key = new Key("foo");
        ti.put(key, Revision.create(1));
        ti.tombstone(key, Revision.create(2));
        assertThatThrownBy(() -> ti.get(key, 2)).isExactlyInstanceOf(RevisionNotFoundException.class);
        assertThatThrownBy(() -> ti.tombstone(key, Revision.create(3)))
                .isExactlyInstanceOf(RevisionNotFoundException.class);
    }

    @Test
    void testTreeIndexRevisions() {
        final TreeIndex ti = TreeIndex.create();
        ti.put(new Key("foo"), Revision.create(1));
        ti.put(new Key("foo1"), Revision.create(2));
        ti.put(new Key("foo2"), Revision.create(3));
        ti.put(new Key("foo2"), Revision.create(4));
        ti.put(new Key("foo1"), Revision.create(5));
        ti.put(new Key("foo"), Revision.create(6));

        record TestCase(Key key, Key end, long rev, int limit, long total, List<Revision> revs) {
            public static TestCase create(String key, String end, long rev, int limit, long total, Revision... revs) {
                final Key mappedKey = new Key(key);
                final Key mappedEnd = Optional.ofNullable(end).map(Key::new).orElse(null);
                return new TestCase(mappedKey, mappedEnd, rev, limit, total, Arrays.asList(revs));
            }
        }

        final TestCase[] testCases = {
            // single key that not found
            TestCase.create("bar", null, 6, 0, 0),
            // single key that found
            TestCase.create("foo", null, 6, 0, 1, Revision.create(6)),
            // various range keys, fixed atRev, unlimited
            TestCase.create("foo", "foo1", 6, 0, 1, Revision.create(6)),
            TestCase.create("foo", "foo2", 6, 0, 2, Revision.create(6), Revision.create(5)),
            TestCase.create("foo", "fop", 6, 0, 3, Revision.create(6), Revision.create(5), Revision.create(4)),
            TestCase.create("foo1", "fop", 6, 0, 2, Revision.create(5), Revision.create(4)),
            TestCase.create("foo2", "fop", 6, 0, 1, Revision.create(4)),
            TestCase.create("foo3", "fop", 6, 0, 0),
            // fixed range keys, various atRev, unlimited
            TestCase.create("foo1", "fop", 1, 0, 0),
            TestCase.create("foo1", "fop", 2, 1, 1, Revision.create(2)),
            TestCase.create("foo1", "fop", 3, 2, 2, Revision.create(2), Revision.create(3)),
            TestCase.create("foo1", "fop", 4, 2, 2, Revision.create(2), Revision.create(4)),
            TestCase.create("foo1", "fop", 5, 2, 2, Revision.create(5), Revision.create(4)),
            TestCase.create("foo1", "fop", 6, 2, 2, Revision.create(5), Revision.create(4)),
            // fixed range keys, fixed atRev, various limit
            TestCase.create("foo", "fop", 6, 1, 3, Revision.create(6)),
            TestCase.create("foo", "fop", 6, 2, 3, Revision.create(6), Revision.create(5)),
            TestCase.create("foo", "fop", 6, 3, 3, Revision.create(6), Revision.create(5), Revision.create(4)),
            TestCase.create("foo", "fop", 3, 1, 3, Revision.create(1)),
            TestCase.create("foo", "fop", 3, 2, 3, Revision.create(1), Revision.create(2)),
            TestCase.create("foo", "fop", 3, 3, 3, Revision.create(1), Revision.create(2), Revision.create(3)),
        };

        for (TestCase testCase : testCases) {
            final TreeIndex.Range actual = ti.range(testCase.key, testCase.end, testCase.rev, testCase.limit);
            assertThat(actual.total()).isEqualTo(testCase.total);
            assertThat(actual.revisions()).isEqualTo(testCase.revs);
        }
    }
}

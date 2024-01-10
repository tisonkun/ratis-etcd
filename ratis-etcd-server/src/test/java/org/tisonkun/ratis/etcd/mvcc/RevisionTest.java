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
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

class RevisionTest {
    @Test
    void testRevisions() {
        final Revision[] revisions = {
                Revision.create(0, 0),
                Revision.create(1, 0),
                Revision.create(1, 1),
                Revision.create(2, 0),
                Revision.create(Long.MAX_VALUE, Long.MAX_VALUE),
        };

        for (int i = 0; i < revisions.length - 1; i++) {
            assertThat(revisions[i]).isLessThan(revisions[i + 1]);
        }
    }

    @Test
    void testRevisionCodec() {
        final Revision revision = Revision.create(42, 21);
        assertThat(Revision.isTombstone(revision.toBytes())).isFalse();
        assertThat(Revision.isTombstone(revision.toTombstoneBytes())).isTrue();

        final ByteBuf bytes = revision.toBytes();
        assertThat(Revision.fromBytes(bytes)).isEqualTo(revision);
        assertThat(Revision.fromBytes(bytes)).isEqualTo(revision);
    }
}

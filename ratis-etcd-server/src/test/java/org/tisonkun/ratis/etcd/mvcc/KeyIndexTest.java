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
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class KeyIndexTest {
    @Test
    void testFindGeneration() {
        final KeyIndex ki = createTestKeyIndex();

        final Generation g0 = ki.getGenerations().get(0);
        final Generation g1 = ki.getGenerations().get(1);

        assertThat(ki.findGeneration(0)).isEmpty();
        assertThat(ki.findGeneration(1)).isEmpty();
        assertThat(ki.findGeneration(2)).get().isEqualTo(g0);
        assertThat(ki.findGeneration(3)).get().isEqualTo(g0);
        assertThat(ki.findGeneration(4)).get().isEqualTo(g0);
        assertThat(ki.findGeneration(5)).get().isEqualTo(g0);
        assertThat(ki.findGeneration(6)).isEmpty();
        assertThat(ki.findGeneration(7)).isEmpty();
        assertThat(ki.findGeneration(8)).get().isEqualTo(g1);
        assertThat(ki.findGeneration(9)).get().isEqualTo(g1);
        assertThat(ki.findGeneration(10)).get().isEqualTo(g1);
        assertThat(ki.findGeneration(11)).get().isEqualTo(g1);
        assertThat(ki.findGeneration(12)).isEmpty();
        assertThat(ki.findGeneration(13)).isEmpty();
    }

    @SneakyThrows
    public static KeyIndex createTestKeyIndex() {
        // key: "foo"
        // modified: 16
        // generations:
        //    {empty}
        //    {{14, 0}[1], {14, 1}[2], {16, 0}(t)[3]}
        //    {{8, 0}[1], {10, 0}[2], {12, 0}(t)[3]}
        //    {{2, 0}[1], {4, 0}[2], {6, 0}(t)[3]}

        final KeyIndex ki = KeyIndex.create("foo", 2, 0);
        ki.put(4, 0);
        ki.tombstone(6, 0);
        ki.put(8, 0);
        ki.put(10, 0);
        ki.tombstone(12, 0);
        ki.put(14, 0);
        ki.put(14, 1);
        ki.tombstone(16, 0);
        return ki;
    }
}

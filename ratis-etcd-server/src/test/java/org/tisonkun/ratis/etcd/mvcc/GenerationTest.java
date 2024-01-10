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
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class GenerationTest {
    @Test
    void testGenerationWalk() {
        final Generation g = Generation.create(Revision.create(2), Revision.create(4), Revision.create(6));
        checkGenerationWalkOne(g, rev -> rev.main() >= 7, 2);
        checkGenerationWalkOne(g, rev -> rev.main() >= 6, 1);
        checkGenerationWalkOne(g, rev -> rev.main() >= 5, 1);
        checkGenerationWalkOne(g, rev -> rev.main() >= 4, 0);
        checkGenerationWalkOne(g, rev -> rev.main() >= 3, 0);
        checkGenerationWalkOne(g, rev -> rev.main() >= 2, null);
        checkGenerationWalkOne(g, rev -> rev.main() >= 1, null);
    }

    void checkGenerationWalkOne(Generation g, Predicate<Revision> pred, Integer idx) {
        assertThat(g.walk(pred).orElse(null)).isEqualTo(idx);
    }
}

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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Generation contains multiple revisions of a key.
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Generation {
    @Nullable
    private Revision created;

    @Setter(AccessLevel.PRIVATE)
    private long version;

    private final List<Revision> revisions;

    public static Generation create(Revision... revs) {
        final List<Revision> revisions = new ArrayList<>(Arrays.asList(revs));
        final Generation g = new Generation(revisions);
        if (!revisions.isEmpty()) {
            g.setVersion(revisions.size());
            g.setCreated(revisions.getFirst());
        }
        return g;
    }

    private void setCreated(@Nonnull Revision created) {
        Preconditions.checkState(Objects.isNull(this.created), "created can only be set once");
        this.created = created;
    }

    public boolean isEmpty() {
        return revisions.isEmpty();
    }

    public void addRevision(Revision revision) {
        if (revisions.isEmpty()) {
            setCreated(revision);
        }
        version += 1;
        revisions.add(revision);
    }

    /**
     * This method walks through the revisions in the generation in descending order.
     * It passes the revision to the given predicate.
     *
     * <p>This method returns until: (1) it finishes walking all pairs (2) the predicate returns false.
     *
     * <p>This method returns the position at where it stopped. If it stopped after finishing walking,
     * {@link Optional#empty()} will be returned.
     */
    public Optional<Integer> walk(Predicate<Revision> predicate) {
        final int len = revisions.size();
        for (int i = 0; i < len; i++) {
            final int idx = len - i - 1;
            if (!predicate.test(revisions.get(idx))) {
                return Optional.of(idx);
            }
        }
        return Optional.empty();
    }
}

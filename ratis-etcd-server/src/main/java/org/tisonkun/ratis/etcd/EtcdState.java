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

package org.tisonkun.ratis.etcd;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.SneakyThrows;
import org.tisonkun.ratis.etcd.mvcc.Key;
import org.tisonkun.ratis.etcd.mvcc.KeyIndex;
import org.tisonkun.ratis.etcd.mvcc.Revision;
import org.tisonkun.ratis.etcd.mvcc.RevisionNotFoundException;
import org.tisonkun.ratis.etcd.mvcc.TreeIndex;
import org.tisonkun.ratis.etcd.proto.KeyValue;
import org.tisonkun.ratis.etcd.proto.PutRequest;
import org.tisonkun.ratis.etcd.proto.PutResponse;
import org.tisonkun.ratis.etcd.proto.RangeRequest;
import org.tisonkun.ratis.etcd.proto.RangeResponse;
import org.tisonkun.ratis.etcd.storage.ReadWriteTxn;
import org.tisonkun.ratis.etcd.storage.Storage;

public class EtcdState {
    private final AtomicLong rev = new AtomicLong(0);
    private final TreeIndex ti;
    private final Storage storage;

    public EtcdState(TreeIndex ti, Storage storage) {
        this.ti = ti;
        this.storage = storage;
    }

    // >= is encoded in the range end as '\0' because null and new byte[0] is the same via gRPC.
    // If it is a GTE range, then KeyBytes.infinity() is returned to indicate the empty byte
    // string (vs null being no byte string).
    private static byte[] decodeGteRange(ByteString rangeEnd) {
        if (rangeEnd.isEmpty()) {
            return null;
        }
        if (rangeEnd.size() == 1 && rangeEnd.byteAt(0) == 0) {
            return Key.infinity();
        }
        return rangeEnd.toByteArray();
    }

    @SneakyThrows
    public PutResponse put(PutRequest request) {
        final Revision rev = Revision.create(this.rev.incrementAndGet());
        final Key key = new Key(request.getKey().toByteArray());

        Revision created = rev;
        long version = 1;
        try {
            final KeyIndex.Get index = ti.get(key, rev.main());
            // if the key exists before, use its previous created
            created = index.created();
            version = index.version() + 1;
        } catch (RevisionNotFoundException ignore) {
            // no previous reversions - it is fine
        }

        final KeyValue kv = KeyValue.newBuilder()
                .setKey(request.getKey())
                .setValue(request.getValue())
                .setCreateRevision(created.main())
                .setModRevision(rev.sub())
                .setVersion(version)
                .build();

        try (ReadWriteTxn txn = storage.readWriteTxn()) {
            txn.unsafePut(rev.toBytes().array(), kv.toByteArray());
        }
        ti.put(key, rev);
        return PutResponse.newBuilder().build();
    }

    @SneakyThrows
    public RangeResponse range(RangeRequest request) {
        final Key key = new Key(request.getKey().toByteArray());
        final Key end = Optional.ofNullable(decodeGteRange(request.getRangeEnd()))
                .map(Key::new)
                .orElse(null);
        final long revision = request.getRevision();
        final long limit = request.getLimit();

        final TreeIndex.Range index = ti.range(key, end, revision, limit);
        final long bound;
        if (limit <= 0 || limit > index.revisions().size()) {
            bound = index.revisions().size();
        } else {
            bound = limit;
        }

        final RangeResponse.Builder resp = RangeResponse.newBuilder();
        try (ReadWriteTxn txn = storage.readWriteTxn()) {
            for (int i = 0; i < bound; i++) {
                final ByteBuf k = index.revisions().get(i).toBytes();
                final Storage.Range range = txn.unsafeRange(k.array(), null, 0);
                if (range.values().size() != 1) {
                    throw new IllegalStateException("revision must have exactly one value");
                }
                resp.addKvs(KeyValue.parseFrom(range.values().getFirst()));
            }
        }
        return resp.build();
    }
}

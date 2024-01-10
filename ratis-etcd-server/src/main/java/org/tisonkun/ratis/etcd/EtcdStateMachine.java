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

import com.google.common.io.MoreFiles;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.tisonkun.ratis.etcd.mvcc.TreeIndex;
import org.tisonkun.ratis.etcd.storage.RocksDBStorage;

public class EtcdStateMachine extends BaseStateMachine {
    private File dataDir;
    private EtcdState state;

    @Override
    public void initialize(RaftServer raftServer, RaftGroupId raftGroupId, RaftStorage raftStorage) throws IOException {
        super.initialize(raftServer, raftGroupId, raftStorage);
        dataDir = new File(raftStorage.getStorageDir().getTmpDir(), "backend");
        reinitialize();
    }

    @Override
    public void reinitialize() throws IOException {
        MoreFiles.deleteRecursively(dataDir.toPath());
        MoreFiles.createParentDirectories(dataDir.toPath());
        state = new EtcdState(TreeIndex.create(), new RocksDBStorage(dataDir));
    }

    @Override
    public CompletableFuture<Message> query(Message request) {
        return super.query(request);
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        return super.applyTransaction(trx);
    }
}

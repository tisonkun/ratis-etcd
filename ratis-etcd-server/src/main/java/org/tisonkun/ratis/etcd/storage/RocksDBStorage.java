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

package org.tisonkun.ratis.etcd.storage;

import java.io.File;
import java.io.IOException;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBStorage implements Storage {
    private final RocksDB db;

    public RocksDBStorage(File dataDir) throws IOException {
        try (final Options options = new Options().setCreateIfMissing(true)) {
            this.db = RocksDB.open(options, dataDir.getAbsolutePath());
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public ReadWriteTxn readWriteTxn() {
        return new RocksDBRWBatch(db);
    }

    @Override
    public void close() {
        db.close();
    }
}

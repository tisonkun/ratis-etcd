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

import com.google.common.util.concurrent.AbstractIdleService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.grpc.client.GrpcClientRpc;
import org.apache.ratis.protocol.ClientId;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.util.NetUtils;
import org.tisonkun.etcd.ratis.proto.config.ServerConfig;

@Slf4j
public class EtcdServer extends AbstractIdleService {
    private final RaftServer raftServer;
    private final RaftClient raftClient;
    private final Server grpcServer;

    public EtcdServer(ServerConfig config) throws IOException {
        final RaftPeer peer = RaftPeer.newBuilder()
                .setId(config.getRaftPeerId())
                .setAddress(config.getRaftPeerAddress())
                .build();
        final RaftGroupId raftGroupId = RaftGroupId.valueOf(new UUID(0, 42));
        final RaftGroup group = RaftGroup.valueOf(raftGroupId, peer);

        final RaftProperties properties = new RaftProperties();
        final int raftPeerPort = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(properties, raftPeerPort);
        RaftServerConfigKeys.setStorageDir(properties, config.getRaftStorageDir());

        this.raftServer = RaftServer.newBuilder()
                .setGroup(group)
                .setProperties(properties)
                .setServerId(peer.getId())
                .setStateMachine(new EtcdStateMachine())
                .setOption(RaftStorage.StartupOption.RECOVER)
                .build();

        final Parameters parameters = new Parameters();
        final ClientId clientId = ClientId.randomId();
        final GrpcClientRpc clientRpc = new GrpcFactory(parameters).newRaftClientRpc(clientId, properties);
        this.raftClient = RaftClient.newBuilder()
                .setProperties(properties)
                .setRaftGroup(group)
                .setClientRpc(clientRpc)
                .build();

        this.grpcServer = ServerBuilder.forPort(config.getServerPort()).build();
    }

    @Override
    protected void startUp() throws Exception {
        this.raftServer.start();
        final int port = this.grpcServer.start().getPort();
        log.info("EtcdServer has started with port {}.", port);
    }

    @Override
    protected void shutDown() throws Exception {
        this.grpcServer.shutdown().awaitTermination();
        this.raftServer.close();
        log.info("Controller has been shutdown.");
    }

    public static void main(String[] args) throws Exception {
        final EtcdServer server = new EtcdServer(ServerConfig.builder().build());
        server.startUp();
        server.raftClient.io().send(Message.EMPTY);
        server.shutDown();
    }
}

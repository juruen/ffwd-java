// $LICENSE
/**
 * Copyright 2013-2014 Spotify AB. All rights reserved.
 *
 * The contents of this file are licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 **/
package com.spotify.ffwd.protocol;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import com.spotify.ffwd.output.BatchedPluginSink;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.LazyTransform;

public class ProtocolPluginSink implements BatchedPluginSink {
    @Inject
    private AsyncFramework async;

    @Inject
    private ProtocolClients clients;

    @Inject
    private Protocol protocol;

    @Inject
    private ProtocolClient client;

    @Inject
    private RetryPolicy retry;

    @Inject
    private Logger log;

    private final AtomicReference<ProtocolConnection> connection = new AtomicReference<>();

    @Override
    public void sendEvent(Event event) {
        final ProtocolConnection c = connection.get();

        if (c == null)
            return;

        c.send(event);
    }

    @Override
    public void sendMetric(Metric metric) {
        final ProtocolConnection c = connection.get();

        if (c == null)
            return;

        c.send(metric);
    }

    @Override
    public AsyncFuture<Void> sendEvents(Collection<Event> events) {
        final ProtocolConnection c = connection.get();

        if (c == null)
            return async.failed(new IllegalStateException("not connected to " + protocol));

        return c.sendAll(events);
    }

    @Override
    public AsyncFuture<Void> sendMetrics(Collection<Metric> metrics) {
        final ProtocolConnection c = connection.get();

        if (c == null)
            return async.failed(new IllegalStateException("not connected to " + protocol));

        return c.sendAll(metrics);
    }

    @Override
    public AsyncFuture<Void> start() {
        return clients.connect(log, protocol, client, retry).transform(new LazyTransform<ProtocolConnection, Void>() {
            @Override
            public AsyncFuture<Void> transform(ProtocolConnection result) throws Exception {
                if (!connection.compareAndSet(null, result))
                    return result.stop();

                return async.resolved(null);
            }
        });
    }

    @Override
    public AsyncFuture<Void> stop() {
        final ProtocolConnection c = connection.getAndSet(null);

        if (c == null)
            return async.resolved(null);

        return c.stop();
    }

    @Override
    public boolean isReady() {
        final ProtocolConnection c = connection.get();

        if (c == null)
            return false;

        return c.isConnected();
    }
}

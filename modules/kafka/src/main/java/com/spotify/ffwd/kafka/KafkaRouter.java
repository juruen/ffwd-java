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
package com.spotify.ffwd.kafka;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = KafkaRouter.Attribute.class, name = "attribute"),
        @JsonSubTypes.Type(value = KafkaRouter.Static.class, name = "static") })
public interface KafkaRouter {
    public String route(final Event event);

    public String route(final Metric metric);

    public static class Attribute implements KafkaRouter {
        private static final String DEFAULT = "default";
        private static final String DEFAULT_ATTRIBUTE = "site";
        private static final String DEFAULT_METRICS = "metrics-%s";
        private static final String DEFAULT_EVENTS = "events-%s";

        private final String attribute;
        private final String metrics;
        private final String events;

        @JsonCreator
        public Attribute(@JsonProperty("attribute") final String attribute, @JsonProperty("metrics") String metrics,
                @JsonProperty("events") String events) {
            this.attribute = Optional.fromNullable(attribute).or(DEFAULT_ATTRIBUTE);
            this.metrics = Optional.fromNullable(metrics).or(DEFAULT_METRICS);
            this.events = Optional.fromNullable(events).or(DEFAULT_EVENTS);
        }

        @Override
        public String route(final Event event) {
            final String attr = event.getAttributes().get(attribute);

            if (attr != null)
                return String.format(events, attr);

            return String.format(events, DEFAULT);
        }

        @Override
        public String route(final Metric metric) {
            final String attr = metric.getAttributes().get(attribute);

            if (attr != null)
                return String.format(metrics, attr);

            return String.format(metrics, DEFAULT);
        }

        public static Supplier<KafkaRouter> supplier() {
            return new Supplier<KafkaRouter>() {
                @Override
                public KafkaRouter get() {
                    return new Attribute(null, null, null);
                }
            };
        }
    }

    public static class Static implements KafkaRouter {
        private static final String DEFAULT_METRICS = "metrics";
        private static final String DEFAULT_EVENTS = "events";

        private final String metrics;
        private final String events;

        @JsonCreator
        public Static(@JsonProperty("metrics") String metrics, @JsonProperty("events") String events) {
            this.metrics = Optional.fromNullable(metrics).or(DEFAULT_METRICS);
            this.events = Optional.fromNullable(events).or(DEFAULT_EVENTS);
        }

        @Override
        public String route(final Event event) {
            return events;
        }

        @Override
        public String route(final Metric metric) {
            return metrics;
        }

        public static Supplier<KafkaRouter> supplier() {
            return new Supplier<KafkaRouter>() {
                @Override
                public KafkaRouter get() {
                    return new Static(null, null);
                }
            };
        }
    }
}

package org.hisp.dhis.kafka;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Configuration
public class KafkaConfig
{
    @Bean
    public KafkaAdmin kafkaAdmin()
    {
        Map<String, Object> props = new HashMap<>();
        props.put( AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092" );

        return new KafkaAdmin( props );
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate()
    {
        return new KafkaTemplate<>( kafkaProducerFactory() );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory()
    {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory( kafkaConsumerFactory() );
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> kafkaConsumerFactory()
    {
        Map<String, Object> props = new HashMap<>();
        props.put( ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092" );
        props.put( ConsumerConfig.GROUP_ID_CONFIG, "group1" );
        props.put( ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true );
        props.put( ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100" );
        props.put( ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000" );

        return new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), new StringDeserializer()
        );
    }

    @Bean
    public ProducerFactory<String, String> kafkaProducerFactory()
    {
        Map<String, Object> props = new HashMap<>();
        props.put( ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092" );
        props.put( ProducerConfig.RETRIES_CONFIG, 0 );
        props.put( ProducerConfig.BATCH_SIZE_CONFIG, 16384 );
        props.put( ProducerConfig.LINGER_MS_CONFIG, 1 );
        props.put( ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432 );

        return new DefaultKafkaProducerFactory<>(
            props, new StringSerializer(), new StringSerializer()
        );
    }
}

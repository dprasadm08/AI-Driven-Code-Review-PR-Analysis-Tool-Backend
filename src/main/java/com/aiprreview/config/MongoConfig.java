package com.aiprreview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConfig {

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory,
                                       MongoMappingContext context) {
        MappingMongoConverter converter = new MappingMongoConverter(
            new DefaultDbRefResolver(mongoDatabaseFactory), context);
        
        // Remove _class field from documents
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        
        return new MongoTemplate(mongoDatabaseFactory, converter);
    }
}

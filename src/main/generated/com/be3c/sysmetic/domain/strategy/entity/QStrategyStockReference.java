package com.be3c.sysmetic.domain.strategy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStrategyStockReference is a Querydsl query type for StrategyStockReference
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStrategyStockReference extends EntityPathBase<StrategyStockReference> {

    private static final long serialVersionUID = -1026418613L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStrategyStockReference strategyStockReference = new QStrategyStockReference("strategyStockReference");

    public final NumberPath<Long> createdBy = createNumber("createdBy", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdDate = createDateTime("createdDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> modifiedBy = createNumber("modifiedBy", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedDate = createDateTime("modifiedDate", java.time.LocalDateTime.class);

    public final QStock stock;

    public final QStrategy strategy;

    public QStrategyStockReference(String variable) {
        this(StrategyStockReference.class, forVariable(variable), INITS);
    }

    public QStrategyStockReference(Path<? extends StrategyStockReference> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStrategyStockReference(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStrategyStockReference(PathMetadata metadata, PathInits inits) {
        this(StrategyStockReference.class, metadata, inits);
    }

    public QStrategyStockReference(Class<? extends StrategyStockReference> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.stock = inits.isInitialized("stock") ? new QStock(forProperty("stock")) : null;
        this.strategy = inits.isInitialized("strategy") ? new QStrategy(forProperty("strategy"), inits.get("strategy")) : null;
    }

}


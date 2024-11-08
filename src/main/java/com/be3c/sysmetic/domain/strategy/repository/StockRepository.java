package com.be3c.sysmetic.domain.strategy.repository;

import com.be3c.sysmetic.domain.strategy.dto.StockGetResponseDto;
import com.be3c.sysmetic.domain.strategy.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findById(Long id);
    Optional<Stock> findByIdAndStatusCode(Long id, String statusCode);
    Optional<Stock> findByName(String name);
    Optional<Stock> findByNameAndStatusCode(String name, String statusCode);

    @Query("""
        SELECT new com.be3c.sysmetic.domain.strategy.dto.StockGetResponseDto(
            s.id, s.name, null
        )
        FROM Stock s
        WHERE s.statusCode = :statusCode
    """)
    Page<StockGetResponseDto> findAllByStatusCode(String statusCode, Pageable pageable);
}
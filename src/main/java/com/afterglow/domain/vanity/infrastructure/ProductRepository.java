package com.afterglow.domain.vanity.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.afterglow.domain.vanity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findAllByAccountId(Long accountId);

	Optional<Product> findByIdAndAccountId(Long id, Long accountId);
}

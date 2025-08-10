package com.shopethethao.modules.products;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shopethethao.dto.ProductDetailDTO;

@Repository
public interface ProductsDAO extends JpaRepository<Product, Integer> {

    boolean existsByCategorieId(Integer categorieId);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.sizes WHERE p.id = :id")
    Optional<Product> findByIdWithSizes(@Param("id") Integer id);

    @Query("SELECT new com.shopethethao.dto.ProductDetailDTO(" +
            "p.id, p.name, a.name, s.name, ps.quantity, ps.price) " +
            "FROM Product p " +
            "JOIN p.attributeMappings pam " +
            "JOIN pam.attribute a " +
            "JOIN p.sizes ps " +
            "JOIN ps.size s " +
            "WHERE p.id = :productId")
    List<ProductDetailDTO> findProductDetailsById(@Param("productId") Integer productId);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(?1)")
    Optional<Product> findByNameIgnoreCase(String name);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategorie_NameContainingIgnoreCase(
            String name, String description, String categoryName, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = true AND p.id = :productId")
    Optional<Product> findActiveProductById(@Param("productId") Integer productId);
}

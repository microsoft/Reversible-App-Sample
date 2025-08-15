package com.revapps.customerapi.repository;

import com.revapps.customerapi.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity with optimized queries
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    /**
     * Find customer by email (case-insensitive)
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    Optional<Customer> findByEmailIgnoreCase(@Param("email") String email);

    /**
     * Search customers by name containing the search term (case-insensitive)
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Check if email exists (excluding specific customer ID for updates)
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE LOWER(c.email) = LOWER(:email) AND c.id != :excludeId")
    boolean existsByEmailIgnoreCaseAndIdNot(@Param("email") String email, @Param("excludeId") Integer excludeId);

    /**
     * Check if email exists
     */
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE LOWER(c.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
}

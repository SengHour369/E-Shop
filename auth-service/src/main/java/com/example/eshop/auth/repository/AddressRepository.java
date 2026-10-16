package com.example.eshop.auth.repository;

import com.example.eshop.auth.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId AND u.enabled = true AND (u.deleted IS NULL OR u.deleted = false) AND (a.deleted IS NULL OR a.deleted = false)")
    org.springframework.data.domain.Page<Address> findOwnedPage(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);


    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId AND (u.deleted IS NULL OR u.deleted = False) AND u.enabled = true AND (a.deleted IS NULL OR a.deleted = false)" )
    List<Address> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId AND a.isDefault = true AND (u.deleted IS NULL OR u.deleted = False) AND u.enabled = true AND (a.deleted IS NULL OR a.deleted = false)")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.id IN (SELECT a2.id FROM Address a2 JOIN a2.users u WHERE u.id = :userId AND (u.deleted IS NULL OR u.deleted = False) AND u.enabled = true AND (a2.deleted IS NULL OR a2.deleted = false))")
    void resetDefaultAddressForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Address a JOIN a.users u WHERE u.id = :userId AND a.id = :addressId AND (a.deleted IS NULL OR a.deleted = False ) AND u.enabled = true AND (u.deleted IS NULL OR u.deleted = false)")
    boolean isUserHasAddress(@Param("userId") Long userId, @Param("addressId") Long addressId);
    @Query("""
            SELECT d FROM Address d WHERE d.id =:id AND (d.deleted IS NULL OR d.deleted = False)
            """)
    Optional<Address> findByAddressId(@Param("id") Long id);
}
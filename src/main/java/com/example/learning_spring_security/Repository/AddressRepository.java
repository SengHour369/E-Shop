package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.Address;
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

    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId")
    List<Address> findByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Address a JOIN a.users u WHERE u.id = :userId AND a.isDefault = true")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.id IN (SELECT a2.id FROM Address a2 JOIN a2.users u WHERE u.id = :userId)")
    void resetDefaultAddressForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Address a JOIN a.users u WHERE u.id = :userId AND a.id = :addressId")
    boolean isUserHasAddress(@Param("userId") Long userId, @Param("addressId") Long addressId);
}
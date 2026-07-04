package com.example.learning_spring_security.Repository;

import com.example.learning_spring_security.Model.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    boolean existsByGroupCode(String groupCode);

    Optional<UserGroup> findByGroupCode(String groupCode);

    // type 0 or null: all active groups
    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false")
    Page<UserGroup> findAllActive(Pageable pageable);

    // type 1: by group code (exact)
    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND g.groupCode = :groupCode")
    Page<UserGroup> findAllByGroupCode(@Param("groupCode") String groupCode, Pageable pageable);

    // type 2: by group name (fuzzy)
    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND LOWER(g.groupName) LIKE LOWER(CONCAT('%', :groupName, '%'))")
    Page<UserGroup> findAllByGroupNameFuzzy(@Param("groupName") String groupName, Pageable pageable);

    // type 3: by isActive
    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND g.isActive = :isActive")
    Page<UserGroup> findAllByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);

    // type 4: by display (fuzzy)
    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND LOWER(g.display) LIKE LOWER(CONCAT('%', :display, '%'))")
    Page<UserGroup> findAllByDisplayFuzzy(@Param("display") String display, Pageable pageable);
}
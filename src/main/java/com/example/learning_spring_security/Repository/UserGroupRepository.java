//package com.example.learning_spring_security.Repository;
//
//import com.example.learning_spring_security.Model.UserGroup;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {
//
//    boolean existsByGroupCodeAndIsDeleteFalse(String groupCode);
//
//    Optional<UserGroup> findByGroupCodeAndIsDeleteFalse(String groupCode);
//
//    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false")
//    Page<UserGroup> findAllActive(Pageable pageable);
//
//    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND g.groupCode = :groupCode")
//    Page<UserGroup> findAllByGroupCode(@Param("groupCode") String groupCode, Pageable pageable);
//
//    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND LOWER(g.groupName) LIKE LOWER(CONCAT('%', :groupName, '%'))")
//    Page<UserGroup> findAllByGroupNameFuzzy(@Param("groupName") String groupName, Pageable pageable);
//
//    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND g.isActive = :isActive")
//    Page<UserGroup> findAllByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);
//
//    @Query("SELECT g FROM UserGroup g WHERE g.isDelete = false AND LOWER(g.display) LIKE LOWER(CONCAT('%', :display, '%'))")
//    Page<UserGroup> findAllByDisplayFuzzy(@Param("display") String display, Pageable pageable);
//
//    // For existence check in create
//    @Query("SELECT COUNT(g) > 0 FROM UserGroup g WHERE g.groupCode = :groupCode AND g.isDelete = false")
//    boolean existsByGroupCode(@Param("groupCode") String groupCode);
//}
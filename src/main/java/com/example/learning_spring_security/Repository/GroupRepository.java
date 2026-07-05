//package com.example.learning_spring_security.Repository;
//
//import com.example.learning_spring_security.Model.Group;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.Optional;
//
//public interface GroupRepository extends JpaRepository<Group, Long> {
//
//    @Query("SELECT g FROM Group g WHERE g.isDelete = false")
//    Page<Group> findAllActive(Pageable pageable);
//
//    Optional<Group> findByGroupCodeAndIsDeleteFalse(String groupCode);
//
//    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE g.groupCode = :groupCode AND g.isDelete = false")
//    boolean existsByGroupCode(@Param("groupCode") String groupCode);
//
//    @Query("SELECT g FROM Group g WHERE g.isDelete = false AND g.groupCode = :groupCode")
//    Page<Group> findAllByGroupCode(@Param("groupCode") String groupCode, Pageable pageable);
//
//    @Query("SELECT g FROM Group g WHERE g.isDelete = false AND LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))")
//    Page<Group> findAllByNameFuzzy(@Param("name") String name, Pageable pageable);
//
//    @Query("SELECT g FROM Group g WHERE g.isDelete = false AND g.isActive = :isActive")
//    Page<Group> findAllByIsActive(@Param("isActive") Boolean isActive, Pageable pageable);
//
//    @Query("SELECT g FROM Group g WHERE g.isDelete = false AND LOWER(g.description) LIKE LOWER(CONCAT('%', :description, '%'))")
//    Page<Group> findAllByDescriptionFuzzy(@Param("description") String description, Pageable pageable);
//}
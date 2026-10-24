package com.ecommerce.userservice.repository;

import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long>, JpaSpecificationExecutor<Users> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Users u where u.email = :email")
    Optional<Users> findByEmailForUpdate(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select u from Users u where u.refreshTokenHash = :hash")
    Optional<Users> findByRefreshTokenHash(String refreshTokenHash);

    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);

    List<Users> findAllByRole(Role role);

    List<Users> findAllByIsActive(Boolean isActive);

    //Existe este email en la BD y le pertenece a otro usuario?
    boolean existsByEmailAndIdNot(String email, Long id);

}

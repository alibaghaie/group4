package com.stackroute.userservice.repository;


import com.stackroute.userservice.domain.UserRolesEnum;
import com.stackroute.userservice.domain.UserRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<UserRoleType, Long> {
	Optional<UserRoleType> findByName(UserRolesEnum userRole);
}

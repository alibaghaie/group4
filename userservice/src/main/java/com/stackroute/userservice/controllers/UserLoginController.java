package com.stackroute.userservice.controllers;


import com.stackroute.userservice.domain.UserModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.stackroute.userservice.payload.response.MessageResponse;
import com.stackroute.userservice.repository.RoleRepository;
import com.stackroute.userservice.repository.UserRepository;
import com.stackroute.userservice.security.jwt.JwtUtils;
import com.stackroute.userservice.security.services.UserDetailsImpl;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stackroute.userservice.domain.UserRolesEnum;
import com.stackroute.userservice.domain.UserRoleType;
import org.springframework.beans.factory.annotation.Autowired;



import com.stackroute.userservice.payload.request.LoginRequest;
import com.stackroute.userservice.payload.request.SignupRequest;
import com.stackroute.userservice.payload.response.JwtResponse;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 4000)
@RestController
@RequestMapping("/api/auth")
public class UserLoginController {

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	JwtUtils jwtUtils;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	AuthenticationManager authenticationManager;

	//Authenticate user
	@PostMapping("/signin")
	public ResponseEntity<?> userAuthentication(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String tokenStringJwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(tokenStringJwt,
				userDetails.getId(),
				userDetails.getUsername(),
				userDetails.getEmail(),
				roles));
	}

	//Register the user if they dont already have an account
	@PostMapping("/register")
	public ResponseEntity<?> userRegistration(@Valid @RequestBody SignupRequest signUpRequest) {
		//prevent users to select a username that already exists
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Already taken. Select another username!"));
		}
		//prevent users to select an email that already exists
		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Email entered already exists, use another email"));
		}

		// Create new user's account
		UserModel user = new UserModel(signUpRequest.getUsername(), signUpRequest.getEmail(), passwordEncoder.encode(signUpRequest.getPassword()));

		Set<String> userRoles = signUpRequest.getRole();
		Set<UserRoleType> roles = new HashSet<>();

		//Check the user roles
		if (userRoles == null || userRoles.isEmpty()) {
			UserRoleType userRole = roleRepository.findByName(UserRolesEnum.ROLE_USER).orElseThrow(() -> new RuntimeException("Role does not exist"));
			roles.add(userRole);
		} else {
			//Throw an exception based on the user role
			userRoles.forEach(role -> {
				switch (role) {
					case "admin":
						UserRoleType adminRole = roleRepository.findByName(UserRolesEnum.ROLE_ADMIN).orElseThrow(() -> new RuntimeException("Role does not exist"));
						roles.add(adminRole);
						break;
					case "mod":
						UserRoleType modRole = roleRepository.findByName(UserRolesEnum.ROLE_MODERATOR).orElseThrow(() -> new RuntimeException("Role does not exist"));
						roles.add(modRole);
						break;
					default:
						UserRoleType userRole = roleRepository.findByName(UserRolesEnum.ROLE_USER).orElseThrow(() -> new RuntimeException("Role does not exist"));
						roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("Registration Successful"));
	}
}

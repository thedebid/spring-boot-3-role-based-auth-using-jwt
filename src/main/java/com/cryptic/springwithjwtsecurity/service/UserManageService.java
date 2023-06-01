package com.cryptic.springwithjwtsecurity.service;

import com.cryptic.springwithjwtsecurity.dto.RegisterRequest;
import com.cryptic.springwithjwtsecurity.dto.ResponseDTO;
import com.cryptic.springwithjwtsecurity.dto.UserResponse;
import com.cryptic.springwithjwtsecurity.exceptions.CustomException;
import com.cryptic.springwithjwtsecurity.models.Role;
import com.cryptic.springwithjwtsecurity.models.User;
import com.cryptic.springwithjwtsecurity.repository.RoleRepository;
import com.cryptic.springwithjwtsecurity.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserManageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserManageService.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    ModelMapper modelMapper;

    /**
     * Method to create new user
     *
     * @param registerRequest user detail from client
     * @return success message or error message
     */
    public ResponseEntity<?> create(RegisterRequest registerRequest) {
        //Check Username and Email is already present or not
        if (userRepository.existsByUsername(registerRequest.getUsername().trim())) {
            if (userRepository.existsByEmail(registerRequest.getEmail().trim())) {
                return ResponseDTO.errorResponse("Error: Email is already taken!");
            }
            return ResponseDTO.errorResponse("Error: Username is already taken!");
        }
        // Create new user's account
        User user = new User(registerRequest.getUsername(), registerRequest.getEmail(), passwordEncoder.encode(registerRequest.getPassword()));
        Set<String> strRoles = registerRequest.getRole();
        Set<Role> roles = new HashSet<>();
        strRoles.forEach(role -> {
            String roleName = role.trim().toUpperCase();
            Role verifiedRole = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new CustomException("Error: " + roleName + " is not found."));
            roles.add(verifiedRole);
        });
        user.setRoles(roles);
        userRepository.save(user);
        LOGGER.info("{} Created new user {}", readAuthenticatedUserName(), user.getUsername());
        return ResponseDTO.successResponse("User created successfully!", null);
    }

    public String readAuthenticatedUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        String user = "";
        if (principal instanceof UserDetails) {
            user = ((UserDetails) principal).getUsername();
        }
        return user;
    }

    public ResponseEntity<?> readAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserResponse.class))
//                .map(user->new UserResponse(user.getId(),user.getUsername(),user.getEmail(),user.getRoles()))
                .collect(Collectors.toList());
        return ResponseDTO.successResponse("Users fetched successfully!", users);
    }

    /**
     * Method to delete existing user
     *
     * @param id from client
     * @return success message or exception
     */
    public ResponseEntity<?> deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException("Error: User not found for this id : " + id));
        userRepository.delete(user);
        LOGGER.info("{} deleted user {}", readAuthenticatedUserName(), user.getUsername());
        return ResponseDTO.successResponse("User deleted successfully!");
    }
}

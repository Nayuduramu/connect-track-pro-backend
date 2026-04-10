// src\main\java\com\connecttrack\pro\security\CustomUserDetailsService.java
package com.connecttrack.pro.security;

import com.connecttrack.pro.entity.Employee;
import com.connecttrack.pro.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // --- THIS IS THE CORRECTED CODE ---
        return new CustomUserDetails(
            employee.getId(),
            employee.getEmail(),
            employee.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(employee.getRole().getName())) // <-- CORRECTED
        );
    }
}
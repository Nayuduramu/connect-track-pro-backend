package com.connecttrack.pro.repository;

import com.connecttrack.pro.entity.WiFiRouter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <-- NEW IMPORT

@Repository
public interface WiFiRouterRepository extends JpaRepository<WiFiRouter, Long> {
    // Spring Data JPA provides standard methods.
    // We can add custom finders here later if needed, e.g., findBySsid(String ssid).
    Optional<WiFiRouter> findByMacAddressIgnoreCase(String macAddress);
}
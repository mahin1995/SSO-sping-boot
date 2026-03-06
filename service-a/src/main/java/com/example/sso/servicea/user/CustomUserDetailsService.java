package com.example.sso.servicea.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.sso.servicea.user.repository.AppUserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final AppUserRepository appUserRepository;

	public CustomUserDetailsService(AppUserRepository appUserRepository) {
		this.appUserRepository = appUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		String normalizedUsername = normalizeRequired(username);
		return appUserRepository.findByUsername(normalizedUsername)
				.map(AuthUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
	}

	private String normalizeRequired(String username) {
		if (username == null || username.isBlank()) {
			throw new UsernameNotFoundException("Username is required");
		}
		return username.trim();
	}
}

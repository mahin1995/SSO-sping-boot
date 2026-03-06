package com.example.sso.servicea.config;

import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.sso.servicea.service.AppUserAccountService;
import com.example.sso.servicea.service.RoleScopeService;

@Component
public class AuthorizationDataInitializer implements ApplicationRunner {

	private final AppUserAccountService appUserAccountService;
	private final RoleScopeService roleScopeService;
	private final PasswordEncoder passwordEncoder;

	private final String demoUser;
	private final String demoPassword;

	public AuthorizationDataInitializer(
			AppUserAccountService appUserAccountService,
			RoleScopeService roleScopeService,
			PasswordEncoder passwordEncoder,
			org.springframework.core.env.Environment environment
	) {
		this.appUserAccountService = appUserAccountService;
		this.roleScopeService = roleScopeService;
		this.passwordEncoder = passwordEncoder;
		this.demoUser = environment.getProperty("service-a.seed.demo-user", "demo");
		this.demoPassword = environment.getProperty("service-a.seed.demo-password", "demo1234");
	}

	@Override
	public void run(ApplicationArguments args) {
		seedRolesAndScopes();
		seedDemoUser();
	}

	private void seedDemoUser() {
		appUserAccountService.createUserIfMissing(
				demoUser,
				passwordEncoder.encode(demoPassword),
				Set.of("APP_USER")
		);
	}

	private void seedRolesAndScopes() {
		roleScopeService.createOrUpdateRole(
				"APP_USER",
				"Default application user role",
				Set.of("service.a.read", "service.b.read")
		);
		roleScopeService.createOrUpdateRole(
				"SERVICE_A_READER",
				"Can read Service A endpoint",
				Set.of("service.a.read")
		);
		roleScopeService.createOrUpdateRole(
				"SERVICE_B_READER",
				"Can read Service B endpoint",
				Set.of("service.b.read")
		);
	}
}

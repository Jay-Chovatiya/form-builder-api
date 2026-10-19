package com.dynamicform.api;

import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.repository.FormRepository;
import com.dynamicform.api.service.FormService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
class FormBuilderApiApplicationTests {

	@Autowired
	private FormRepository formRepository;

	@Autowired
	private FormService formService;

	@Autowired
	private com.dynamicform.api.repository.UserRepository userRepository;

	@Autowired
	private com.dynamicform.api.service.AuthService authService;

	@Test
	void contextLoads() {
		List<com.dynamicform.api.entity.UserEntity> users = userRepository.findAll();
		System.out.println("Existing users in DB: " + users.size());
		for (com.dynamicform.api.entity.UserEntity u : users) {
			System.out.println("User: " + u.getUsername() + ", email: " + u.getEmail() + ", role: " + u.getRole());
		}
	}

	@Test
	void testGetForms() {
		List<FormDto> forms = formService.getForms();
		org.junit.jupiter.api.Assertions.assertNotNull(forms);
		System.out.println("Fetched forms count via formService.getForms(): " + forms.size());
		if (!forms.isEmpty()) {
			FormDto first = forms.get(0);
			org.junit.jupiter.api.Assertions.assertNotNull(first.getId());
			org.junit.jupiter.api.Assertions.assertNotNull(first.getCode());
			Optional<FormDto> details = formService.getFormByCode(first.getCode(), true);
			org.junit.jupiter.api.Assertions.assertTrue(details.isPresent());
		}

		// Non-existent form delete should return false
		boolean deleteNonExistent = formService.deleteForm(999999L);
		org.junit.jupiter.api.Assertions.assertFalse(deleteNonExistent);
	}

	@Test
	void testLoginSuccess() {
		com.dynamicform.api.dto.LoginDto dto = new com.dynamicform.api.dto.LoginDto();
		dto.setUsernameOrEmail("superadmin");
		dto.setPassword("Admin@123");
		dto.setRememberMe(false);

		Optional<com.dynamicform.api.dto.AuthResponseDto> result = authService.login(dto);
		org.junit.jupiter.api.Assertions.assertTrue(result.isPresent());
		com.dynamicform.api.dto.AuthResponseDto response = result.get();
		org.junit.jupiter.api.Assertions.assertNotNull(response.getToken());
		org.junit.jupiter.api.Assertions.assertNotNull(response.getRefreshToken());
		org.junit.jupiter.api.Assertions.assertNotNull(response.getUser());
		org.junit.jupiter.api.Assertions.assertEquals("superadmin", response.getUser().getUsername());
		org.junit.jupiter.api.Assertions.assertEquals("admin@formstudio.com", response.getUser().getEmail());
		System.out.println("Login Success! Generated Token: " + response.getToken());
	}

	@Test
	void testLoginInvalidCredentials() {
		com.dynamicform.api.dto.LoginDto dto = new com.dynamicform.api.dto.LoginDto();
		dto.setUsernameOrEmail("superadmin");
		dto.setPassword("WrongPassword@999");
		dto.setRememberMe(false);

		Optional<com.dynamicform.api.dto.AuthResponseDto> result = authService.login(dto);
		org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
	}

	@Test
	void testLoginNonExistentUser() {
		com.dynamicform.api.dto.LoginDto dto = new com.dynamicform.api.dto.LoginDto();
		dto.setUsernameOrEmail("does_not_exist_user");
		dto.setPassword("AnyPassword");
		dto.setRememberMe(false);

		Optional<com.dynamicform.api.dto.AuthResponseDto> result = authService.login(dto);
		org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty());
	}

	@Test
	void testRefreshTokenSuccessAndRevocation() {
		com.dynamicform.api.dto.LoginDto dto = new com.dynamicform.api.dto.LoginDto();
		dto.setUsernameOrEmail("superadmin");
		dto.setPassword("Admin@123");
		dto.setRememberMe(false);

		Optional<com.dynamicform.api.dto.AuthResponseDto> loginResult = authService.login(dto);
		org.junit.jupiter.api.Assertions.assertTrue(loginResult.isPresent());
		String initialRefreshToken = loginResult.get().getRefreshToken();
		org.junit.jupiter.api.Assertions.assertNotNull(initialRefreshToken);

		// 1. Refresh using initial refresh token -> should succeed
		Optional<com.dynamicform.api.dto.AuthResponseDto> refreshResult = authService.refreshToken(initialRefreshToken);
		org.junit.jupiter.api.Assertions.assertTrue(refreshResult.isPresent());
		com.dynamicform.api.dto.AuthResponseDto refreshed = refreshResult.get();
		org.junit.jupiter.api.Assertions.assertNotNull(refreshed.getToken());
		org.junit.jupiter.api.Assertions.assertNotNull(refreshed.getRefreshToken());
		org.junit.jupiter.api.Assertions.assertNotEquals(initialRefreshToken, refreshed.getRefreshToken());
		org.junit.jupiter.api.Assertions.assertEquals("superadmin", refreshed.getUser().getUsername());

		// 2. Re-using the now-revoked initial refresh token -> must fail (empty)
		Optional<com.dynamicform.api.dto.AuthResponseDto> replayResult = authService.refreshToken(initialRefreshToken);
		org.junit.jupiter.api.Assertions.assertTrue(replayResult.isEmpty());

		// 3. Invalid or blank refresh token -> must fail
		org.junit.jupiter.api.Assertions.assertTrue(authService.refreshToken("").isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(authService.refreshToken(null).isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(authService.refreshToken("completely_invalid_token").isEmpty());
	}

	@Test
	void testRevokeToken() {
		com.dynamicform.api.dto.LoginDto dto = new com.dynamicform.api.dto.LoginDto();
		dto.setUsernameOrEmail("superadmin");
		dto.setPassword("Admin@123");
		dto.setRememberMe(false);

		Optional<com.dynamicform.api.dto.AuthResponseDto> loginResult = authService.login(dto);
		org.junit.jupiter.api.Assertions.assertTrue(loginResult.isPresent());
		String refreshToken = loginResult.get().getRefreshToken();
		org.junit.jupiter.api.Assertions.assertNotNull(refreshToken);

		// Revoke the token
		boolean revoked = authService.revokeToken(refreshToken);
		org.junit.jupiter.api.Assertions.assertTrue(revoked);

		// Trying to refresh with the revoked token must fail
		Optional<com.dynamicform.api.dto.AuthResponseDto> refreshResult = authService.refreshToken(refreshToken);
		org.junit.jupiter.api.Assertions.assertTrue(refreshResult.isEmpty());

		// Revoking an already revoked token should return false
		boolean secondRevoke = authService.revokeToken(refreshToken);
		org.junit.jupiter.api.Assertions.assertFalse(secondRevoke);

		// Revoking null, empty, or non-existent token should return false
		org.junit.jupiter.api.Assertions.assertFalse(authService.revokeToken(null));
		org.junit.jupiter.api.Assertions.assertFalse(authService.revokeToken(""));
		org.junit.jupiter.api.Assertions.assertFalse(authService.revokeToken("non_existent_token"));
	}
}

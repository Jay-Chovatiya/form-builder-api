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

	@Autowired
	private com.dynamicform.api.service.UserService userService;

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

	@Test
	void testUserLifecycle() {
		String testUsername = "test_user_" + System.currentTimeMillis();
		String testEmail = testUsername + "@formstudio.com";

		com.dynamicform.api.dto.UserDto dto = new com.dynamicform.api.dto.UserDto();
		dto.setUsername(testUsername);
		dto.setEmail(testEmail);
		dto.setPassword("Secret@123");
		dto.setFullName("Test User");
		dto.setRole(com.dynamicform.api.entity.Role.Administrator);
		dto.setActive(true);

		// 1. Create
		com.dynamicform.api.dto.UserDto created = userService.createUser(dto);
		org.junit.jupiter.api.Assertions.assertNotNull(created.getId());
		org.junit.jupiter.api.Assertions.assertEquals(testUsername, created.getUsername());
		org.junit.jupiter.api.Assertions.assertEquals(testEmail, created.getEmail());

		// 2. Uniqueness check
		org.junit.jupiter.api.Assertions.assertFalse(userService.isUsernameUnique(testUsername, null));
		org.junit.jupiter.api.Assertions.assertTrue(userService.isUsernameUnique(testUsername, created.getId()));
		org.junit.jupiter.api.Assertions.assertFalse(userService.isEmailUnique(testEmail, null));
		org.junit.jupiter.api.Assertions.assertTrue(userService.isEmailUnique(testEmail, created.getId()));

		// 3. Update
		dto.setFullName("Updated Test User");
		dto.setPassword(""); // keep existing password
		Optional<com.dynamicform.api.dto.UserDto> updatedOpt = userService.updateUser(created.getId(), dto);
		org.junit.jupiter.api.Assertions.assertTrue(updatedOpt.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals("Updated Test User", updatedOpt.get().getFullName());

		// 4. Toggle Status
		boolean toggled = userService.toggleUserStatus(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(toggled);
		Optional<com.dynamicform.api.dto.UserDto> toggledUser = userService.getUserById(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(toggledUser.isPresent());
		org.junit.jupiter.api.Assertions.assertFalse(toggledUser.get().isActive());

		// 5. Delete
		boolean deleted = userService.deleteUser(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(deleted);
		org.junit.jupiter.api.Assertions.assertTrue(userService.getUserById(created.getId()).isEmpty());
	}

	@Test
	void testFormLifecycle() {
		String code = "TEST_CODE_" + System.currentTimeMillis();

		FormDto dto = new FormDto();
		dto.setName("Customer Feedback");
		dto.setCode(code);
		dto.setDescription("Testing form creation and updates");
		dto.setStatus(com.dynamicform.api.entity.FormStatus.Draft);

		// Section
		com.dynamicform.api.dto.FormSectionDto section = new com.dynamicform.api.dto.FormSectionDto();
		section.setTitle("General Info");
		section.setDisplayOrder(0);

		// Field with option
		com.dynamicform.api.dto.FormFieldDto field = new com.dynamicform.api.dto.FormFieldDto();
		field.setName("satisfaction_level");
		field.setType("select");
		field.setLabel("Satisfaction Level");
		field.setDisplayOrder(0);

		com.dynamicform.api.dto.FieldOptionDto opt1 = new com.dynamicform.api.dto.FieldOptionDto();
		opt1.setLabel("High");
		opt1.setValue("high");
		opt1.setDisplayOrder(0);
		field.getOptions().add(opt1);

		section.getFields().add(field);
		dto.getSections().add(section);

		// 1. Create Form
		FormDto created = formService.createForm(dto);
		org.junit.jupiter.api.Assertions.assertNotNull(created.getId());
		org.junit.jupiter.api.Assertions.assertEquals(code, created.getCode());
		org.junit.jupiter.api.Assertions.assertFalse(created.getSections().isEmpty());
		org.junit.jupiter.api.Assertions.assertFalse(created.getSections().get(0).getFields().isEmpty());

		// 2. Code check
		org.junit.jupiter.api.Assertions.assertFalse(formService.isFormCodeUnique(code, null));
		org.junit.jupiter.api.Assertions.assertTrue(formService.isFormCodeUnique(code, created.getId()));

		// 3. Status updates: Publish / Unpublish
		Optional<FormDto> published = formService.publishForm(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(published.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals(com.dynamicform.api.entity.FormStatus.Published, published.get().getStatus());

		Optional<FormDto> publishedByCode = formService.getPublishedFormByCode(code);
		org.junit.jupiter.api.Assertions.assertTrue(publishedByCode.isPresent());

		Optional<FormDto> unpublished = formService.unpublishForm(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(unpublished.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals(com.dynamicform.api.entity.FormStatus.Unpublished, unpublished.get().getStatus());
		org.junit.jupiter.api.Assertions.assertTrue(formService.getPublishedFormByCode(code).isEmpty());

		// 4. Duplicate Form
		Optional<FormDto> duplicated = formService.duplicateForm(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(duplicated.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals(created.getName() + " (Copy)", duplicated.get().getName());
		org.junit.jupiter.api.Assertions.assertEquals(com.dynamicform.api.entity.FormStatus.Draft, duplicated.get().getStatus());

		// 5. Clean up forms
		boolean deletedSource = formService.deleteForm(created.getId());
		org.junit.jupiter.api.Assertions.assertTrue(deletedSource);
		boolean deletedCopy = formService.deleteForm(duplicated.get().getId());
		org.junit.jupiter.api.Assertions.assertTrue(deletedCopy);
	}
}

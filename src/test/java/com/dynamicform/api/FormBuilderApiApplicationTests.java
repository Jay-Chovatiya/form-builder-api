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

	@Autowired
	private com.dynamicform.api.service.SubmissionService submissionService;

	@Autowired
	private com.dynamicform.api.service.FileStorageService fileStorageService;

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

	@Test
	void testFileTypeConfigValidation() {
		jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
		jakarta.validation.Validator validator = factory.getValidator();

		// Invalid: MIME type starting with dot
		com.dynamicform.api.dto.FileTypeConfigDto invalidMime = new com.dynamicform.api.dto.FileTypeConfigDto();
		invalidMime.setExtension(".doc");
		invalidMime.setMimeType(".application/msword");
		var violations = validator.validate(invalidMime);
		org.junit.jupiter.api.Assertions.assertFalse(violations.isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(violations.stream()
				.anyMatch(v -> v.getPropertyPath().toString().equals("mimeType")));

		// Invalid: Extension without leading dot
		com.dynamicform.api.dto.FileTypeConfigDto invalidExt = new com.dynamicform.api.dto.FileTypeConfigDto();
		invalidExt.setExtension("doc");
		invalidExt.setMimeType("application/msword");
		violations = validator.validate(invalidExt);
		org.junit.jupiter.api.Assertions.assertFalse(violations.isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(violations.stream()
				.anyMatch(v -> v.getPropertyPath().toString().equals("extension")));

		// Valid: correct extension and mime type
		com.dynamicform.api.dto.FileTypeConfigDto valid = new com.dynamicform.api.dto.FileTypeConfigDto();
		valid.setExtension(".doc");
		valid.setMimeType("application/msword");
		violations = validator.validate(valid);
		org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty());
	}

	@Test
	void testFormFieldValidationRulesConsistency() {
		jakarta.validation.ValidatorFactory factory = jakarta.validation.Validation.buildDefaultValidatorFactory();
		jakarta.validation.Validator validator = factory.getValidator();

		// Invalid: minLength > maxLength
		com.dynamicform.api.dto.FormFieldDto invalidLength = new com.dynamicform.api.dto.FormFieldDto();
		invalidLength.setName("testField");
		invalidLength.setType("Text");
		invalidLength.setLabel("Test Field");
		invalidLength.setValidations(java.util.List.of(
				new com.dynamicform.api.dto.FieldValidationDto("minLength", "7"),
				new com.dynamicform.api.dto.FieldValidationDto("maxLength", "5")
		));

		var violations = validator.validate(invalidLength);
		org.junit.jupiter.api.Assertions.assertFalse(violations.isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(violations.stream()
				.anyMatch(v -> v.getMessage().contains("Minimum length (7) cannot be greater than maximum length (5)")));

		// Invalid: minValue > maxValue
		com.dynamicform.api.dto.FormFieldDto invalidValue = new com.dynamicform.api.dto.FormFieldDto();
		invalidValue.setName("numberField");
		invalidValue.setType("Number");
		invalidValue.setLabel("Number Field");
		invalidValue.setValidations(java.util.List.of(
				new com.dynamicform.api.dto.FieldValidationDto("minValue", "100"),
				new com.dynamicform.api.dto.FieldValidationDto("maxValue", "50")
		));

		violations = validator.validate(invalidValue);
		org.junit.jupiter.api.Assertions.assertFalse(violations.isEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(violations.stream()
				.anyMatch(v -> v.getMessage().contains("Minimum value (100.0) cannot be greater than maximum value (50.0)")));

		// Valid: minLength <= maxLength
		com.dynamicform.api.dto.FormFieldDto validField = new com.dynamicform.api.dto.FormFieldDto();
		validField.setName("validField");
		validField.setType("Text");
		validField.setLabel("Valid Field");
		validField.setValidations(java.util.List.of(
				new com.dynamicform.api.dto.FieldValidationDto("minLength", "3"),
				new com.dynamicform.api.dto.FieldValidationDto("maxLength", "10")
		));

		violations = validator.validate(validField);
		org.junit.jupiter.api.Assertions.assertTrue(violations.isEmpty());
	}

	@Test
	void testFormSubmissionLifecycleAndValidations() {
		// 1. Create a published form with diverse fields
		String code = "submit_test_" + System.currentTimeMillis();
		com.dynamicform.api.dto.FormDto form = new com.dynamicform.api.dto.FormDto();
		form.setName("Submission Test Form");
		form.setCode(code);
		form.setDescription("Form to test submission handling");
		form.setStatus(com.dynamicform.api.entity.FormStatus.Draft);

		com.dynamicform.api.dto.FormSectionDto sec = new com.dynamicform.api.dto.FormSectionDto();
		sec.setTitle("Personal Info");

		com.dynamicform.api.dto.FormFieldDto nameField = new com.dynamicform.api.dto.FormFieldDto();
		nameField.setName("fullName");
		nameField.setType("Text");
		nameField.setLabel("Full Name");
		nameField.setValidations(java.util.List.of(
				new com.dynamicform.api.dto.FieldValidationDto("required", "true")
		));

		com.dynamicform.api.dto.FormFieldDto ageField = new com.dynamicform.api.dto.FormFieldDto();
		ageField.setName("age");
		ageField.setType("Number");
		ageField.setLabel("Age");
		ageField.setValidations(java.util.List.of(
				new com.dynamicform.api.dto.FieldValidationDto("minValue", "18"),
				new com.dynamicform.api.dto.FieldValidationDto("maxValue", "65")
		));

		com.dynamicform.api.dto.FormFieldDto cityField = new com.dynamicform.api.dto.FormFieldDto();
		cityField.setName("city");
		cityField.setType("Dropdown");
		cityField.setLabel("City");
		cityField.setOptions(java.util.List.of(
				new com.dynamicform.api.dto.FieldOptionDto(null, "New York", "NYC", 1),
				new com.dynamicform.api.dto.FieldOptionDto(null, "Los Angeles", "LA", 2)
		));

		sec.setFields(new java.util.ArrayList<>(java.util.List.of(nameField, ageField, cityField)));
		form.setSections(new java.util.ArrayList<>(java.util.List.of(sec)));

		com.dynamicform.api.dto.FormDto createdForm = formService.createForm(form);
		org.junit.jupiter.api.Assertions.assertNotNull(createdForm.getId());

		// 2. Draft form cannot accept submissions
		com.dynamicform.api.dto.FormSubmissionDto subDto = new com.dynamicform.api.dto.FormSubmissionDto();
		org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.ResourceNotFoundException.class,
				() -> submissionService.saveSubmission(code, subDto)
		);

		// Publish form
		formService.publishForm(createdForm.getId());

		// Fetch field IDs from created form
		com.dynamicform.api.dto.FormDto published = formService.getFormWithDetails(createdForm.getId(), false).orElseThrow();
		var fields = published.getSections().get(0).getFields();
		Long nameId = fields.stream().filter(f -> f.getName().equals("fullName")).findFirst().get().getId();
		Long ageId = fields.stream().filter(f -> f.getName().equals("age")).findFirst().get().getId();
		Long cityId = fields.stream().filter(f -> f.getName().equals("city")).findFirst().get().getId();

		// 3. Required field validation error
		com.dynamicform.api.dto.FormSubmissionDto missingReq = new com.dynamicform.api.dto.FormSubmissionDto();
		missingReq.setResponses(java.util.List.of(
				new com.dynamicform.api.dto.FormResponseDto(ageId, 25),
				new com.dynamicform.api.dto.FormResponseDto(cityId, "NYC")
		));
		var exReq = org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.BadRequestException.class,
				() -> submissionService.saveSubmission(code, missingReq)
		);
		org.junit.jupiter.api.Assertions.assertTrue(exReq.getMessage().contains("Field 'Full Name' is required"));

		// 4. Number bounds validation error (age < 18)
		com.dynamicform.api.dto.FormSubmissionDto underAge = new com.dynamicform.api.dto.FormSubmissionDto();
		underAge.setResponses(java.util.List.of(
				new com.dynamicform.api.dto.FormResponseDto(nameId, "John Doe"),
				new com.dynamicform.api.dto.FormResponseDto(ageId, 16),
				new com.dynamicform.api.dto.FormResponseDto(cityId, "NYC")
		));
		var exAge = org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.BadRequestException.class,
				() -> submissionService.saveSubmission(code, underAge)
		);
		org.junit.jupiter.api.Assertions.assertTrue(exAge.getMessage().contains("Field 'Age' must be at least 18.0"));

		// 5. Option choice validation error (invalid city)
		com.dynamicform.api.dto.FormSubmissionDto badCity = new com.dynamicform.api.dto.FormSubmissionDto();
		badCity.setResponses(java.util.List.of(
				new com.dynamicform.api.dto.FormResponseDto(nameId, "John Doe"),
				new com.dynamicform.api.dto.FormResponseDto(ageId, 25),
				new com.dynamicform.api.dto.FormResponseDto(cityId, "Tokyo")
		));
		var exCity = org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.BadRequestException.class,
				() -> submissionService.saveSubmission(code, badCity)
		);
		org.junit.jupiter.api.Assertions.assertTrue(exCity.getMessage().contains("Selected option 'Tokyo' for 'City' is invalid"));

		// 6. Successful submission
		com.dynamicform.api.dto.FormSubmissionDto validSub = new com.dynamicform.api.dto.FormSubmissionDto();
		validSub.setResponses(java.util.List.of(
				new com.dynamicform.api.dto.FormResponseDto(nameId, "Jane Doe"),
				new com.dynamicform.api.dto.FormResponseDto(ageId, 28),
				new com.dynamicform.api.dto.FormResponseDto(cityId, "NYC")
		));
		com.dynamicform.api.dto.FormSubmissionDto savedSub = submissionService.saveSubmission(code, validSub);
		org.junit.jupiter.api.Assertions.assertNotNull(savedSub.getId());
		org.junit.jupiter.api.Assertions.assertEquals(createdForm.getId(), savedSub.getFormId());
		org.junit.jupiter.api.Assertions.assertEquals(3, savedSub.getResponses().size());

		// 7. Retrieve submissions & count
		List<com.dynamicform.api.dto.FormSubmissionDto> list = submissionService.getSubmissions(createdForm.getId());
		org.junit.jupiter.api.Assertions.assertEquals(1, list.size());
		org.junit.jupiter.api.Assertions.assertEquals(savedSub.getId(), list.get(0).getId());

		var single = submissionService.getSubmissionById(createdForm.getId(), savedSub.getId());
		org.junit.jupiter.api.Assertions.assertTrue(single.isPresent());
		org.junit.jupiter.api.Assertions.assertEquals(savedSub.getId(), single.get().getId());

		// 8. Delete submission
		boolean deleted = submissionService.deleteSubmission(createdForm.getId(), savedSub.getId());
		org.junit.jupiter.api.Assertions.assertTrue(deleted);
		org.junit.jupiter.api.Assertions.assertTrue(submissionService.getSubmissions(createdForm.getId()).isEmpty());

		// 9. Clean up test form
		formService.deleteForm(createdForm.getId());
	}

	@Test
	void testFileUploadLifecycleAndValidation() {
		String code = "file_test_" + System.currentTimeMillis();
		com.dynamicform.api.dto.FormDto form = new com.dynamicform.api.dto.FormDto();
		form.setName("File Upload Test Form");
		form.setCode(code);
		form.setDescription("Testing file uploads");
		form.setStatus(com.dynamicform.api.entity.FormStatus.Draft);

		com.dynamicform.api.dto.FormSectionDto sec = new com.dynamicform.api.dto.FormSectionDto();
		sec.setTitle("Attachments");

		com.dynamicform.api.dto.FormFieldDto fileField = new com.dynamicform.api.dto.FormFieldDto();
		fileField.setName("resume");
		fileField.setType("File");
		fileField.setLabel("Resume Document");
		fileField.setMaxSizeInBytes(500000);
		fileField.setAllowedTypes(java.util.List.of(
				new com.dynamicform.api.dto.FileTypeConfigDto(".pdf", "application/pdf")
		));

		sec.setFields(new java.util.ArrayList<>(java.util.List.of(fileField)));
		form.setSections(new java.util.ArrayList<>(java.util.List.of(sec)));

		com.dynamicform.api.dto.FormDto created = formService.createForm(form);
		formService.publishForm(created.getId());

		// 1. Invalid file extension (.txt instead of .pdf)
		org.springframework.mock.web.MockMultipartFile badExtFile =
				new org.springframework.mock.web.MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());
		org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.BadRequestException.class,
				() -> fileStorageService.uploadFieldFile(code, "resume", badExtFile)
		);

		// 2. Disguised executable (.pdf extension with MZ binary header)
		byte[] fakePdfExe = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x00, 0x00, 0x00};
		org.springframework.mock.web.MockMultipartFile fakeFile =
				new org.springframework.mock.web.MockMultipartFile("file", "fake.pdf", "application/pdf", fakePdfExe);
		org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.BadRequestException.class,
				() -> fileStorageService.uploadFieldFile(code, "resume", fakeFile)
		);

		// 3. Valid PDF with %PDF header
		byte[] validPdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34, 0x0A, 0x25};
		org.springframework.mock.web.MockMultipartFile validPdf =
				new org.springframework.mock.web.MockMultipartFile("file", "my_resume.pdf", "application/pdf", validPdfBytes);

		String fileUrl = fileStorageService.uploadFieldFile(code, "resume", validPdf);
		org.junit.jupiter.api.Assertions.assertNotNull(fileUrl);
		org.junit.jupiter.api.Assertions.assertTrue(fileUrl.startsWith("/api/forms/" + code + "/files/"));

		String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
		org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(code, fileName);
		org.junit.jupiter.api.Assertions.assertTrue(resource.exists());

		// 4. Submit form referencing the uploaded file
		Long fieldId = formService.getFormWithDetails(created.getId(), false).get()
				.getSections().get(0).getFields().get(0).getId();

		com.dynamicform.api.dto.FormSubmissionDto sub = new com.dynamicform.api.dto.FormSubmissionDto();
		sub.setResponses(java.util.List.of(
				new com.dynamicform.api.dto.FormResponseDto(fieldId, fileUrl)
		));
		com.dynamicform.api.dto.FormSubmissionDto savedSub = submissionService.saveSubmission(code, sub);
		org.junit.jupiter.api.Assertions.assertNotNull(savedSub.getId());

		// 5. Delete submission -> should delete the uploaded file on disk
		submissionService.deleteSubmission(created.getId(), savedSub.getId());
		org.junit.jupiter.api.Assertions.assertThrows(
				com.dynamicform.api.common.exception.ResourceNotFoundException.class,
				() -> fileStorageService.loadFileAsResource(code, fileName)
		);

		// Clean up
		formService.deleteForm(created.getId());
	}
}

package com.dynamicform.api;

import com.dynamicform.api.dto.FormDto;
import com.dynamicform.api.entity.FormEntity;
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

	@Test
	void contextLoads() {
	}

	@Test
	void testGetForms() {
		List<FormEntity> forms = formRepository.findAll();
		org.junit.jupiter.api.Assertions.assertNotNull(forms);
		if (!forms.isEmpty()) {
			FormEntity first = forms.get(0);
			org.junit.jupiter.api.Assertions.assertNotNull(first.getId());
			org.junit.jupiter.api.Assertions.assertNotNull(first.getCode());
			Optional<FormDto> details = formService.getFormByCode(first.getCode(), true);
			org.junit.jupiter.api.Assertions.assertTrue(details.isPresent());
		}
	}

}

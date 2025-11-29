package com.vaicomtudo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;

@SpringBootTest
class VaiComTudoApplicationTests {

	@Test
	@Requirement("VCT-48")
	void contextLoads() {
	}

}

package com.pdf.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.pdf.demo.PdfDemo;
import com.pdf.demo.PdfDemoApplication;
import com.pdf.demo.model.UserData;
import com.pdf.demo.repo.UserDataRepo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class PdfDemoControllerTests {

	private static String filePath;
	private static File file;

	@InjectMocks
	PdfDemoController pdfControllerObj;

	@Mock
	PdfDemo pdfDemoObj;

	@Mock
	UserDataRepo userRepo;

	static PDPageContentStream contentStream;
	static PDDocument doc;
	static PDPage page;
	static PDRectangle rect;

	static UserData userData;
	static Map<String, String> allParams;
	static List<File> arrList;

	static Properties prop;

	@BeforeAll
	public static void setup() throws Exception {
		MockitoAnnotations.openMocks(PdfDemoControllerTests.class);

		InputStream input = PdfDemoApplication.class.getClassLoader().getResourceAsStream("configTest.properties");
		prop = new Properties();
		prop.load(input);

		userData = new UserData();
		userData.setFullName("Niyati Patel");
		userData.setCovidPositive(1);
		userData.setUserId(1);

		allParams = new HashMap<String, String>();
		allParams.put("id", "1");
		allParams.put("position", "top");
		allParams.put("align", "left");

		filePath = prop.getProperty("path_test_input") + "inputDataTest.txt";
		file = new File(filePath);

		arrList = new ArrayList<File>();
		arrList.add(file);

		try {
			page = new PDPage();
			doc = new PDDocument();
			doc.addPage(page);
			contentStream = new PDPageContentStream(doc, page,
					PDPageContentStream.AppendMode.APPEND, true, false);
			rect = PDRectangle.A4;
		} catch (IOException ioe) {

		}
	}

	@Test
	@Order(1)
	public void getUserDataTest() throws Exception {
		when(userRepo.findById(anyInt())).thenReturn(Optional.of(userData));
		when(pdfDemoObj.generatePdf(any(UserData.class), anyString(),
				anyString())).thenReturn(true);

		assertThat(pdfControllerObj.getUserData(allParams)).isEqualTo("Report Generated");
		assertThat(pdfControllerObj.getUserData(Collections.emptyMap())).isEqualTo("No User Found");

		when(pdfDemoObj.generatePdf(any(UserData.class), anyString(),
				anyString())).thenReturn(false);
		assertThat(pdfControllerObj.getUserData(allParams)).isEqualTo("Error occured while generating a report");

		allParams.remove("id");
		assertThat(pdfControllerObj.getUserData(allParams)).isEqualTo("No User Found");
		allParams.put("id", "test");
		assertThat(pdfControllerObj.getUserData(allParams)).isEqualTo("Input should be a valid number");
		allParams.put("id", "1");
	}

	@Test
	@Order(2)
	public void generatePdfTest() {
		boolean flag = false;
		try {
			when(pdfDemoObj.generatePdf(any(UserData.class), anyString(),
					anyString())).thenReturn(true);
			flag = pdfControllerObj.generatePdf(userData, allParams);
		} catch (Exception ioe) {
			userData = null;
			flag = false;
		}

		assertNotNull(userData);
		assertTrue(flag);

	}

	@AfterAll
	public static void close() throws IOException {
		contentStream.close();
		doc.close();
	}
}

package com.pdf.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.pdf.demo.controller.PdfDemoController;
import com.pdf.demo.model.UserData;
import com.pdf.demo.repo.UserDataRepo;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class PdfDemoApplicationTests {

	@InjectMocks
	PdfDemoController pdfControllerObj;

	@Mock
	PdfDemo pdfDemoObj;

	@Mock
	UserDataRepo userRepo;

	PDPageContentStream contentStream;
	PDDocument doc;
	PDPage page;

	UserData userData;
	Map<String, String> allParams;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);
		userData = new UserData();
		userData.setFullName("Niyati Patel");
		userData.setCovidPositive(1);
		userData.setUserId(1);

		allParams = new HashMap<String, String>();
		allParams.put("id", "1");
		allParams.put("position", "top");
		allParams.put("align", "left");

		try {
			page = new PDPage();
			doc = new PDDocument();
			contentStream = new PDPageContentStream(doc, page);
		} catch (IOException ioe) {

		}
	}

	@Test
	@Order(1)
	public void getUserDataTest() {
		when(userRepo.findById(anyInt())).thenReturn(Optional.of(userData));

		assertNotNull(allParams);
		assertThat(allParams.containsKey("id")).isTrue();
		assertDoesNotThrow(() -> Integer.valueOf(allParams.get("id").toString()));

		UserData userNew = pdfControllerObj.getUserById(allParams);

		assertNotNull(userNew);
		assertEquals(userNew.getUserId(), userData.getUserId());
	}

	@Test
	@Order(2)
	public void generatePdfTest() {
		boolean flag = false;
		try {
			when(pdfDemoObj.generatePdf(any(UserData.class), anyString(), anyString())).thenReturn(true);
			flag = pdfControllerObj.generatePdf(userData, allParams);
		} catch (Exception ioe) {
			userData = null;
			flag = false;
		}

		assertNotNull(userData);
		assertTrue(flag);

	}

	@Test
	@Order(3)
	public void readFileTest() {
		String strData = null;
		try {
			String strPath = "src/test/resources/inputData/inputDataTest.txt";

			when(pdfDemoObj.readFile(anyString())).thenReturn("sample data");

			strData = pdfDemoObj.readFile(strPath);
			assertThat(strData).isNotEmpty();
			assertDoesNotThrow(() -> Paths.get(strPath));

			Path path = Paths.get(strPath);
			assertDoesNotThrow(() -> new String(Files.readAllBytes(path)));// if path is wrong, will throw exception
			assertThat(Files.readAllBytes(path)).isNotEmpty();

		} catch (Exception fex) {
			strData = null;
		}
		assertNotNull(strData);
	}

	@Test
	@Order(4)
	public void updateYvalueTest() {
		pdfDemoObj.value_y = 5f;

		doAnswer(s -> {
			float value = s.getArgument(0);
			assertEquals(pdfDemoObj.value_y - value, pdfDemoObj.value_y - 1f);
			return null;
		}).when(pdfDemoObj).updateYvalue(anyFloat());

		pdfDemoObj.updateYvalue(1f);
		verify(pdfDemoObj, times(1)).updateYvalue(1f);
	}

	@Test
	@Order(5)
	public void callNewLineTest() throws IOException {
		pdfDemoObj.value_y = 5f;

		doAnswer(s -> {
			int count = s.getArgument(0);
			PDPageContentStream content = s.getArgument(1);

			assertThat(count).isGreaterThan(0);
			assertNotNull(content);

			// how can I verify if one method is called from other
			// doCallRealMethod().when(pdfDemoObj).updateYvalue(anyFloat());
			// verify(pdfDemoObj,times(1)).updateYvalue((float)count);

			return null;
		}).when(pdfDemoObj).callNewLine(anyInt(), any(PDPageContentStream.class));

		pdfDemoObj.callNewLine(1, contentStream);
		verify(pdfDemoObj, times(1)).callNewLine(1, contentStream);
	}

	@Test
	@Order(6)
	public void wrapStringTest() throws IOException {
		doAnswer(s -> {
			String text = s.getArgument(0);
			PDPageContentStream content = s.getArgument(1);

			assertThat(text).isNotEmpty();
			assertNotNull(content);
			assertEquals(text, "Test String");

			// Y should be updated

			return null;
		}).when(pdfDemoObj).wrapString(anyString(), any(PDPageContentStream.class));

		pdfDemoObj.wrapString("Test String", contentStream);
		verify(pdfDemoObj, times(1)).wrapString("Test String", contentStream);
	}

	@Test
	@Order(7)
	public void getTextTest() {
		String str = "some name";
		when(pdfDemoObj.getText(anyInt(), anyMap())).thenReturn(str);

		Map<String, String> map = new HashMap<String, String>();
		map.put("name", "some name");
		String value = pdfDemoObj.getText(0, map);
		assertThat(value).isNotNull();
		assertEquals(value, map.get("name"));
	}

	@Test
	@Order(8)
	public void createWatermarkTest() throws IOException {
		doAnswer(s -> {
			PDDocument doc = s.getArgument(0);

			assertNotNull(doc);
			assertDoesNotThrow(() -> PDDocument.load(new File("src/test/resources/pdf/watermarkTest.pdf")));

			return null;
		}).when(pdfDemoObj).createWaterMark(any(PDDocument.class));

		pdfDemoObj.createWaterMark(doc);
		verify(pdfDemoObj, times(1)).createWaterMark(doc);
	}

	@Test
	@Order(9)
	public void createImageTest() throws IOException {
		doAnswer(s -> {
			String position = s.getArgument(0);
			String align = s.getArgument(1);
			PDPageContentStream content = s.getArgument(2);
			PDDocument doc = s.getArgument(3);
			PDPage page = s.getArgument(4);

			assertNotNull(doc);
			assertNotNull(page);
			assertNotNull(content);
			assertThat(position).isIn(Arrays.asList("top", "bottom"));
			assertThat(align).isIn(Arrays.asList("right", "left", "center"));

			assertDoesNotThrow(
					() -> PDImageXObject.createFromFile("src/test/resources/images/Quest Diagnostics logo.jpeg", doc));

			return null;
		}).when(pdfDemoObj).createImage(anyString(), anyString(), any(PDPageContentStream.class), any(PDDocument.class),
				any(PDPage.class));

		pdfDemoObj.createImage("top", "left", contentStream, doc, page);
		verify(pdfDemoObj, times(1)).createImage("top", "left", contentStream, doc, page);
	}

	@Test
	@Order(10)
	public void testContent() {

	}

}

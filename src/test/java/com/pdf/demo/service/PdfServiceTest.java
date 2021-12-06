package com.pdf.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pdf.demo.PdfDemoApplication;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDPageAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSeedValue;
import org.h2.mvstore.Page;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class PdfServiceTest {

    @InjectMocks
    PdfService pdfService;

    @Mock
    AWSS3Service awsService;

    @Mock
    PDFMergerUtility mergerUtil;

    static Properties prop;
    private static String filePath;
    private static File file;
    private static List<File> arrList;
    static PDRectangle rect;
    static PDPage page;
    static PDDocument doc;
    static PDPageContentStream content;

    @BeforeAll
    public static void setup() throws Exception {
        MockitoAnnotations.openMocks(PdfServiceTest.class);

        InputStream input = PdfDemoApplication.class.getClassLoader().getResourceAsStream("configTest.properties");
        prop = new Properties();
        prop.load(input);

        filePath = prop.getProperty("path_test_input") + "inputDataTest.txt";
        file = new File(filePath);

        arrList = new ArrayList<File>();
        arrList.add(file);

        doc = new PDDocument();
        rect = PDRectangle.A4;
        page = new PDPage(rect);
        content = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, false, true);
    }

    @Test
    @Order(1)
    public void testgetPageSizeNew() {
        PDRectangle rectNew = pdfService.getPageSize();
        assertEquals(rectNew, rect);
    }

    @Test
    @Order(2)
    public void testreadFile() throws Exception {
        String str = pdfService.readFile(filePath);
        assertEquals(str, "Sample Data");
    }

    @Test
    @Order(3)
    public void testgetText() throws Exception {

        Map<String, String> mapObj = new HashMap<String, String>();
        mapObj.put("name", "some value");
        mapObj.put("result", "some value");
        mapObj.put("expected", "some value");
        mapObj.put("test", "some value");
        mapObj.put("resultValue", "some value");
        mapObj.put("expectedValue", "some value");

        assertThat(pdfService.getText(null, 0)).isEmpty();
        for (int i = 0; i <= 6; i++) {
            assertThat(pdfService.getText(mapObj, i)).isInstanceOf(String.class);
        }
    }

    @Test
    @Order(4)
    public void testgetFilesFromLocal() throws Exception {

        int sizeInitial = arrList.size();

        arrList = pdfService.getFilesFromLocal(arrList);
        assertNotEquals(sizeInitial, arrList.size());

    }

    @Test
    @Order(5)
    public void testgetFilesFromS3() throws Exception {

        when(awsService.listObjects(anyList())).thenReturn(arrList);

        List<File> arrNewList = new ArrayList<File>(arrList);

        arrNewList = pdfService.getFilesFromS3(arrNewList);
        assertEquals(arrList.size(), arrNewList.size());

    }

    @Test
    @Order(6)
    public void testdeleteS3Files() throws Exception {

        File file = new File("src/test/resources/S3/testfile.txt");
        List<File> arrList = Stream.of(file).collect(Collectors.toList());

        pdfService.deleteS3Files(arrList);
        assertThat(arrList.get(0).getPath()).contains("/S3/");

        Files.deleteIfExists(file.toPath());

    }

    @Test
    @Order(7)
    public void testgetDocPage() throws Exception {
        PDPage pageNew = pdfService.getDocPage();
        assertEquals(pageNew.getMediaBox().getWidth(), page.getMediaBox().getWidth());
        assertEquals(pageNew.getMediaBox().getHeight(), page.getMediaBox().getHeight());

    }

    @Test
    @Order(8)
    public void testgetPageContentStream() throws Exception {
        PDPageContentStream contentNew = pdfService.getPageContentStream(doc, page);
        assertEquals(contentNew.getClass(), content.getClass());

    }

    @Test
    @Order(9)
    public void testgetFiles() throws Exception {

        when(awsService.listObjects(anyList())).thenReturn(arrList);

        List<File> arrNewList = new ArrayList<File>(arrList);

        arrNewList = pdfService.getFiles();
        assertNotNull(arrNewList);
        assertThat(arrNewList).isNotEmpty();
        assertThat(arrNewList.size()).isGreaterThan(0);
        assertThat(arrNewList.get(0)).isInstanceOf(File.class);

    }

    @AfterAll
    public static void close() throws Exception {
        content.close();
        doc.close();
    }
}

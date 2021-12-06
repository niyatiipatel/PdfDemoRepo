package com.pdf.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.pdf.demo.model.UserData;
import com.pdf.demo.service.AWSS3Service;
import com.pdf.demo.service.PdfService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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
public class PdfDemoTest {

    private static String filePath;
    private static File file;

    @InjectMocks
    PdfDemo pdfDemoObj;// class in test

    @Mock
    PdfService pdfService;

    @Mock
    AWSS3Service awsService;

    static List<File> arrList;
    static PDDocument doc;
    static PDRectangle rect;
    static PDPage page;
    static PDPageContentStream contentStream;
    static Properties prop;
    static UserData userData;

    @BeforeAll
    public static void setup() throws Exception {
        MockitoAnnotations.openMocks(PdfDemoTest.class);

        InputStream input = PdfDemoApplication.class.getClassLoader().getResourceAsStream("configTest.properties");
        prop = new Properties();
        prop.load(input);

        doc = new PDDocument();
        rect = PDRectangle.A4;
        page = new PDPage(rect);

        contentStream = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, false, true);

        filePath = prop.getProperty("path_test_input") + "inputDataTest.txt";
        file = new File(filePath);

        arrList = new ArrayList<File>();
        arrList.add(file);

        userData = new UserData();
        userData.setFullName("Niyati Patel");
        userData.setCovidPositive(1);
        userData.setUserId(1);
        userData.setDtService("01/05/2021");
        userData.setDob("01/05/1994");
        userData.setGender("Female");

    }

    private void initializeValues() {
        pdfDemoObj.document = doc;
        pdfDemoObj.font_size = 12f;
        pdfDemoObj.LEADING = pdfDemoObj.font_size * 1.1f;
        pdfDemoObj.imageSize = 50f;
        pdfDemoObj.TIMES_BOLD = PDType1Font.TIMES_BOLD;
        pdfDemoObj.TIMES_ROMAN = PDType1Font.TIMES_ROMAN;
        pdfDemoObj.initialCordinate = new float[] { 20f, 20f };
        pdfDemoObj.paddingCordinate = new float[] { 100f, 20f };
        pdfDemoObj.headerFooterSizeCordinate = 100f;
    }

    @Test
    @Order(1)
    public void testgetPageSize() throws Exception {

        PDRectangle pdRect = PDRectangle.A4;
        when(pdfService.getPageSize()).thenReturn(pdRect);

        PDRectangle pdRectTest = pdfDemoObj.getPageSize();
        assertEquals(pdRect, pdRectTest);
        assertEquals(pdRectTest.getHeight(), pdRect.getHeight());
        assertEquals(pdRectTest.getWidth(), pdRect.getWidth());

    }

    @Test
    @Order(2)
    public void readFileTest() {
        String strData = null;
        try {
            when(pdfService.readFile(anyString())).thenReturn("Sample Data");

            strData = pdfDemoObj.readFile(filePath);
            assertThat(strData).isNotEmpty();
            assertEquals(strData, "Sample Data");
            assertDoesNotThrow(() -> Paths.get(filePath));

            Path path = Paths.get(filePath);
            assertDoesNotThrow(() -> new String(Files.readAllBytes(path)));// if path is wrong, will throw exception
            assertThat(Files.readAllBytes(path)).isNotEmpty();

        } catch (Exception fex) {
            strData = null;
        }
        assertNotNull(strData);
    }

    @Test
    @Order(3)
    public void getTextTest() {
        String str = "some name";
        when(pdfService.getText(anyMap(), anyInt())).thenReturn(str);

        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "some name");
        String value = pdfDemoObj.getText(map, 0);
        assertThat(value).isNotNull();
        assertEquals(value, map.get("name"));
        assertEquals(value, str);
    }

    @Test
    @Order(4)
    public void testGetFiles() throws Exception {

        when(pdfService.getFiles()).thenReturn(arrList);// local files
        when(awsService.listObjects(anyList())).thenReturn(arrList);// S3 files

        List<File> arrListNew = pdfDemoObj.getFiles();
        assertEquals(arrList, arrListNew);
        assertNotNull(arrListNew);
        assertEquals(arrList.size(), arrListNew.size());
        assertThat(arrListNew.size()).isGreaterThan(0);
        assertDoesNotThrow(() -> arrListNew.add(file));
        assertThat(arrListNew.get(0).getClass()).hasSameClassAs(File.class);
        assertThat(arrListNew.get(0)).isEqualTo(file);

    }

    @Test
    @Order(5)
    public void testdeleteFileFromSys() throws Exception {

        doAnswer(s -> {
            File fileTest = s.getArgument(0);
            assertNotNull(fileTest);
            assertNotNull(fileTest.toPath());
            assertDoesNotThrow(() -> Files.deleteIfExists(Paths.get("any path")));

            return null;
        }).when(pdfService).deleteFileFromSys(any(File.class));

        pdfDemoObj.deleteFileFromSys(new File("any path"));

    }

    @Test
    @Order(6)
    public void testdeleteS3Files() throws Exception {

        doAnswer(s -> {
            List<File> arrList = s.getArgument(0);
            assertNotNull(arrList);
            assertThat(arrList.size()).isGreaterThan(0);
            assertThat(arrList.get(0).getClass()).hasSameClassAs(File.class);
            String pathTemp = arrList.get(0).getPath();
            assertNotNull(pathTemp);
            assertThat(pathTemp.contains("/S3/"));
            // how to check if one method is invoked or not
            // verify(pdfService, times(1)).deleteFileFromSys(file);

            return null;
        }).when(pdfService).deleteS3Files(anyList());

        pdfDemoObj.deleteS3Files(arrList);
        verify(pdfService, times(1)).deleteS3Files(arrList);

    }

    @Test
    @Order(7)
    public void testMergeDocuments() throws Exception {

        when(pdfService.mergeDocuments(any(File.class), any(File.class), anyList())).thenReturn(true);

        boolean flag = pdfDemoObj.mergeDocuments(new File("any path"), new File("any path"), arrList);
        assertTrue(flag);
    }

    @Test
    @Order(8)
    public void testgetDocPage() throws Exception {

        when(pdfService.getDocPage()).thenReturn(page);

        PDPage pageNew = pdfDemoObj.getDocPage();
        assertNotNull(pageNew);
        assertEquals(pageNew, page);
    }

    @Test
    @Order(9)
    public void testgetPageContentStream() throws Exception {

        when(pdfService.getPageContentStream(any(PDDocument.class), any(PDPage.class))).thenReturn(contentStream);

        PDPageContentStream contentNew = pdfDemoObj.getPageContentStream(doc, page);
        assertNotNull(contentNew);
        assertEquals(contentNew, contentStream);
    }

    private void updateYvaluebeforeCall() throws Exception {
        when(pdfService.getDocPage()).thenReturn(page);
        when(pdfService.getPageContentStream(any(PDDocument.class), any(PDPage.class))).thenReturn(contentStream);
        initializeValues();
        contentStream.beginText();
        contentStream.setFont(pdfDemoObj.TIMES_ROMAN, pdfDemoObj.font_size);
    }

    private void updateYvalueAfterCall() throws Exception {
        contentStream.endText();
    }

    @Test
    @Order(10)
    public void updateYvalueTest() throws Exception {
        float value_y = 5f;

        updateYvaluebeforeCall();

        PDPageContentStream content = pdfDemoObj.updateYvalue(1f, contentStream);
        assertNotNull(content);
        assertEquals(content, contentStream);
        assertEquals(4f, value_y - 1f);
        content.close();
        updateYvalueAfterCall();

        updateYvaluebeforeCall();
        pdfDemoObj.newLineCordinate[1] = (pdfDemoObj.headerFooterSizeCordinate * 1.55f) + 1f;
        PDPageContentStream contentN = pdfDemoObj.updateYvalue(1f, contentStream);
        assertNotNull(contentN);
        assertEquals(4f, value_y - 1f);
        contentN.close();
        updateYvalueAfterCall();

    }

    @Test
    @Order(11)
    public void callNewLineTest() throws Exception {

        updateYvaluebeforeCall();
        pdfDemoObj.newLineCordinate[1] = (pdfDemoObj.headerFooterSizeCordinate * 1.55f) + 1f;
        PDPageContentStream content = pdfDemoObj.callNewLine(1, contentStream);
        assertNotNull(content);

        updateYvalueAfterCall();
    }

    @Test
    @Order(12)
    public void wrapStringTest() throws Exception {

        updateYvaluebeforeCall();
        pdfDemoObj.newLineCordinate[1] = (pdfDemoObj.headerFooterSizeCordinate * 1.55f) + 1f;

        PDPageContentStream contentNew = pdfDemoObj.wrapString("Test String", contentStream);
        assertNotNull(contentNew);

        updateYvalueAfterCall();
    }

    // @Test
    // @Order(13)
    // public void createWatermarkTest() throws IOException {

    // doc.addPage(page);
    // pdfDemoObj.path_src_pdf = prop.getProperty("path_test_pdf");
    // assertTrue(pdfDemoObj.createWaterMark(doc));
    // }

    @Test
    @Order(14)
    public void createLogoTest() throws Exception {

        when(pdfService.getPageContentStream(any(PDDocument.class), any(PDPage.class))).thenReturn(contentStream);
        pdfDemoObj.path_src_images = prop.getProperty("path_test_images");
        initializeValues();

        assertTrue(pdfDemoObj.createLogo("bottom", "right", doc));
        assertTrue(pdfDemoObj.createLogo("bottom", "center", doc));

    }

    @Test
    @Order(15)
    public void testCreateFooter() throws Exception {

        when(pdfService.getPageContentStream(any(PDDocument.class), any(PDPage.class))).thenReturn(contentStream);
        pdfDemoObj.paddingCordinate = new float[] { 100f, 20f };
        pdfDemoObj.TIMES_ROMAN = PDType1Font.TIMES_ROMAN;

        doc.addPage(page);
        assertTrue(pdfDemoObj.createFooter(doc));
    }

    @Test
    @Order(16)
    public void testCreateHeader() throws Exception {

        when(pdfService.getPageContentStream(any(PDDocument.class), any(PDPage.class))).thenReturn(contentStream);
        pdfDemoObj.paddingCordinate = new float[] { 100f, 20f };
        pdfDemoObj.TIMES_ROMAN = PDType1Font.TIMES_ROMAN;

        doc.addPage(page);
        assertTrue(pdfDemoObj.createHeader(doc));
    }

    @Test
    @Order(17)
    public void testCreateTable() throws Exception {

        when(pdfService.getText(anyMap(), anyInt())).thenReturn("some value");
        updateYvaluebeforeCall();

        updateYvalueAfterCall();
        pdfDemoObj.newLineCordinate[1] = (pdfDemoObj.headerFooterSizeCordinate * 1.55f) + 1f;

        pdfDemoObj.createTable(userData, contentStream, doc);
        assertTrue(true);

    }

    @Test
    @Order(18)
    public void testSaveDoc() throws Exception {

        PDDocument document = new PDDocument();
        File file = new File(prop.getProperty("path_test_pdf") + "watermark.pdf");
        pdfDemoObj.loadDoc(file);
        pdfDemoObj.docSaveAndClose(file, document);
        assertTrue(true);

    }

    @Test
    @Order(19)
    public void testUpdatePageSizeDoc() throws Exception {

        File file = new File(prop.getProperty("path_test_pdf") + "watermark.pdf");
        doc.addPage(page);
        assertTrue(pdfDemoObj.updateDocPageSize(file, doc));

    }

    @Test
    @Order(20)
    public void testWriteContent() throws Exception {
        pdfDemoObj.path_src_input = prop.getProperty("path_test_input");
        when(pdfDemoObj.readFile(anyString())).thenReturn(
                new String(Files.readAllBytes(Paths.get(prop.getProperty("path_test_input") + "inputData.txt"))));

        when(pdfService.getText(anyMap(), anyInt())).thenReturn("some value");
        updateYvaluebeforeCall();

        updateYvalueAfterCall();
        pdfDemoObj.newLineCordinate[1] = (pdfDemoObj.headerFooterSizeCordinate * 1.55f) + 1f;

        pdfDemoObj.writeContent(doc, userData);

    }

    @AfterAll
    public static void close() throws Exception {
        contentStream.close();
        doc.close();
    }
}

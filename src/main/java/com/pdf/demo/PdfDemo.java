package com.pdf.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pdf.demo.model.UserData;
import com.pdf.demo.service.PdfService;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component

public class PdfDemo {

	public Logger logger = LoggerFactory.getLogger(PdfDemo.class);

	@Autowired
	public PdfService pdfService;

	@Value("${path_pdf}")
	String pathPdf;

	@Value("${path_src_pdf}")
	String path_src_pdf;

	@Value("${path_src_images}")
	String path_src_images;

	@Value("${path_src_input}")
	String path_src_input;

	float font_size;
	float LEADING;// can increment by any number or simply set leading by font size
	float[] newLineCordinate = new float[2];
	float headerFooterSizeCordinate;
	float imageSize;
	PDFont TIMES_BOLD;
	PDFont TIMES_ROMAN;
	float[] paddingCordinate;
	float[] initialCordinate;
	PDDocument document;

	private void initializeValues() throws IOException {

		font_size = 12F;
		LEADING = font_size * 1.1f;// can increment by any number or simply set leading by font size
		imageSize = 100f;
		TIMES_BOLD = PDType1Font.TIMES_BOLD;
		TIMES_ROMAN = PDType1Font.TIMES_ROMAN;
		initialCordinate = new float[] { 20f, 20f };
		paddingCordinate = new float[] { 100f, 20f };
		headerFooterSizeCordinate = 100f;
		document = new PDDocument();// main class to create a pdf

	}

	public boolean generatePdf(UserData userObj, String position, String align) throws Exception {

		initializeValues();

		File destFile = new File(pathPdf + "destFile.pdf");

		File mainFile = new File(pathPdf + "MainFile.pdf");
		writeContent(document, userObj);

		docSaveAndClose(mainFile, document);

		List<File> arrFileList = getFiles();
		if (arrFileList != null && !arrFileList.isEmpty()) {
			mergeDocuments(destFile, mainFile, arrFileList);
			deleteS3Files(arrFileList);
		}

		PDDocument destDocument = loadDoc(destFile);
		updateDocPageSize(destFile, destDocument);

		createLogo(position, align, destDocument);
		createHeader(destDocument);
		createFooter(destDocument);
		createWaterMark(destDocument);

		docSaveAndClose(destFile, destDocument);

		deleteFileFromSys(mainFile);

		System.out.println("PDF created");

		return true;
	}

	public boolean updateDocPageSize(File destFile, PDDocument destDocument) throws IOException {
		PDPageTree pageTree = destDocument.getDocumentCatalog().getPages();
		for (PDPage page : pageTree) {
			page.setMediaBox(getPageSize());
		}
		return true;
	}

	public void docSaveAndClose(File mainFile, PDDocument doc) throws IOException {
		doc.save(mainFile);
		doc.close();
	}

	public PDDocument loadDoc(File file) throws IOException {
		return PDDocument.load(file);
	}

	public boolean mergeDocuments(File destFile, File mainFile, List<File> arrFileList) {
		try {
			pdfService.mergeDocuments(destFile, mainFile, arrFileList);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public PDPageContentStream updateYvalue(float value, PDPageContentStream content) throws Exception {

		if (newLineCordinate[1] <= (headerFooterSizeCordinate * 1.55f)) {
			content.endText();
			content.close();

			content = getNewContentStream();
		} else {
			newLineCordinate[1] = newLineCordinate[1] - value;
		}

		return content;
	}

	public PDPageContentStream callNewLine(int count, PDPageContentStream content) throws Exception {

		for (int i = 0; i < count; i++) {
			content = updateYvalue(LEADING, content);
			content.newLine();
		}

		return content;
	}

	public PDPageContentStream wrapString(String text, PDPageContentStream content) throws Exception {
		int length = text.length();
		int firstIndex = 0;
		int lastIndex = 110;
		while (length >= firstIndex) {
			if (length < lastIndex) {
				lastIndex = length;
			}
			String temp = text.substring(firstIndex, lastIndex).trim();
			content.showText(temp);
			content.newLine();
			firstIndex += 110;
			lastIndex += 110;
			content = updateYvalue(LEADING, content);
		}

		return content;
	}

	public boolean createWaterMark(PDDocument doc) throws IOException {

		HashMap<Integer, String> overlayGuide = new HashMap<Integer, String>();
		for (int i = 1; i <= doc.getNumberOfPages(); i++) {
			overlayGuide.put(i, path_src_pdf + "watermark.pdf");
		}

		PDDocument docWaterMark = loadDoc(new File(path_src_pdf + "watermark.pdf"));
		Overlay overlay = new Overlay();
		overlay.setInputPDF(doc);
		overlay.setAllPagesOverlayPDF(docWaterMark);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		overlay.overlay(overlayGuide);
		return true;
	}

	public boolean createLogo(String position, String align, PDDocument doc)
			throws Exception {

		PDImageXObject image = PDImageXObject
				.createFromFile(path_src_images + "Quest Diagnostics logo.jpeg", doc);

		for (PDPage page : doc.getPages()) {
			PDPageContentStream imageContentStream = getPageContentStream(doc, page);

			float tempX = page.getMediaBox().getLowerLeftX() + initialCordinate[0];
			float tempY = page.getMediaBox().getUpperRightY() + initialCordinate[1] - headerFooterSizeCordinate;

			if (position.equalsIgnoreCase("top")) {
				// updateYvalue(imageSize, null);
				// tempY = newLineCordinate[1];
			} else if (position.equalsIgnoreCase("bottom")) {
				tempY = page.getMediaBox().getLowerLeftY();
			}
			if (align.equalsIgnoreCase("left")) {
				// tempX = newLineCordinate[0];
			} else if (align.equalsIgnoreCase("right")) {
				tempX = page.getMediaBox().getUpperRightX() - imageSize;// allocate some space from right for image size
			} else if (align.equalsIgnoreCase("center")) {
				tempX = (page.getMediaBox().getUpperRightX() - imageSize) / 2;
			}

			imageContentStream.drawImage(image, tempX, tempY, imageSize, imageSize);
			imageContentStream.close();

		}
		return true;
		// updateYvalue(LEADING, null);/

	}

	public boolean createFooter(PDDocument doc) throws Exception {

		for (PDPage page : doc.getPages()) {
			PDPageContentStream footerContentStream = getPageContentStream(doc, page);
			footerContentStream.beginText();
			footerContentStream.setFont(TIMES_ROMAN, 10);
			PDRectangle pageSize = page.getMediaBox();
			footerContentStream.newLineAtOffset(pageSize.getUpperRightX() - paddingCordinate[0],
					pageSize.getLowerLeftY() + paddingCordinate[1]);
			String text = "Sample Footer";
			footerContentStream.showText(text);
			footerContentStream.endText();
			footerContentStream.close();

		}

		return true;

	}

	public boolean createHeader(PDDocument doc) throws Exception {

		for (PDPage page : doc.getPages()) {
			PDPageContentStream headerContentStream = getPageContentStream(doc, page);
			headerContentStream.beginText();
			headerContentStream.setFont(TIMES_ROMAN, 10);
			PDRectangle pageSize = page.getMediaBox();

			headerContentStream.newLineAtOffset(pageSize.getUpperRightX() - paddingCordinate[0],
					pageSize.getUpperRightY() - paddingCordinate[1]);
			String text = "Sample Header";
			headerContentStream.showText(text);
			headerContentStream.endText();
			headerContentStream.close();

		}

		return true;
	}

	public List<File> getFiles() {

		return pdfService.getFiles();
	}

	public void deleteS3Files(List<File> fileList) {
		pdfService.deleteS3Files(fileList);
	}

	public void deleteFileFromSys(File mainFile) throws IOException {
		pdfService.deleteFileFromSys(mainFile);
	}

	public PDRectangle getPageSize() {
		return pdfService.getPageSize();
	}

	// static text in main pdf
	public void writeContent(PDDocument doc, UserData userObj) throws Exception {
		boolean continueFlag = true;

		String data = readFile(path_src_input + "inputData.txt");
		String[] arrData = data.split("BREAK");

		PDPageContentStream content = getNewContentStream();

		while (continueFlag) {

			int dataIndex = 0;// for static text

			content.setFont(PDType1Font.TIMES_BOLD, font_size + 2f);
			content.showText(arrData[dataIndex++]);

			callNewLine(2, content);
			content.showText(arrData[dataIndex++]);

			content = callNewLine(2, content);
			content.setFont(TIMES_ROMAN, font_size);
			content.showText(arrData[dataIndex++] + userObj.getDtService());
			content = callNewLine(1, content);
			content.showText(arrData[dataIndex++] + userObj.getFullName());
			content = callNewLine(1, content);
			content.showText(arrData[dataIndex++] + userObj.getDob());
			content = callNewLine(1, content);
			content.showText(arrData[dataIndex++] + userObj.getGender());
			content = callNewLine(1, content);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(1, content);
			content.newLineAtOffset(50f, 0f);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(1, content);
			content.newLineAtOffset(-50f, 0f);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(2, content);
			content.setFont(TIMES_BOLD, font_size);
			content.showText(arrData[dataIndex++]);

			content.setFont(TIMES_ROMAN, font_size);
			content = callNewLine(3, content);
			content.endText();

			// Ques : better solution for table creation
			createTable(userObj, content, doc);
			content.beginText();
			content = callNewLine(3, content);
			content.newLineAtOffset(newLineCordinate[0], newLineCordinate[1]);
			content.setFont(TIMES_BOLD, font_size);
			content.showText(arrData[dataIndex++]);
			content.setFont(TIMES_ROMAN, font_size);

			content = callNewLine(1, content);

			// Ques - better solution to wrap text
			content = wrapString(arrData[dataIndex++], content);
			content = callNewLine(2, content);
			content = wrapString(arrData[dataIndex++], content);
			content = callNewLine(2, content);

			content.setFont(TIMES_ROMAN, font_size - 5);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(2, content);

			content.setFont(TIMES_ROMAN, font_size - 5);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(2, content);
			content.setFont(TIMES_ROMAN, font_size - 5);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(2, content);
			content.setFont(PDType1Font.HELVETICA, font_size - 5);
			content.showText(arrData[dataIndex++]);
			content = callNewLine(3, content);
			content.endText();
			content.close();
			continueFlag = false;
		}

	}

	public String readFile(String path) throws IOException {
		return pdfService.readFile(path);
	}

	public PDPage getDocPage() {
		return pdfService.getDocPage();
	}

	public PDPageContentStream getPageContentStream(PDDocument doc, PDPage page) throws Exception {
		return pdfService.getPageContentStream(doc, page);
	}

	private PDPageContentStream getNewContentStream() throws Exception {

		PDPage page = getDocPage();
		document.addPage(page);
		newLineCordinate[0] = page.getMediaBox().getLowerLeftX() + initialCordinate[0];
		newLineCordinate[1] = page.getMediaBox().getUpperRightY() - headerFooterSizeCordinate;

		PDPageContentStream content = getPageContentStream(document, page);
		content.setLeading(LEADING);
		content.setFont(TIMES_ROMAN, font_size);
		content.beginText();
		content.newLineAtOffset(newLineCordinate[0], newLineCordinate[1]);
		return content;
	}

	public void createTable(UserData userObj, PDPageContentStream contentStream, PDDocument doc)
			throws Exception {

		float margin = newLineCordinate[0];// need a constant X value for rows and column creation
		float tempValueX = newLineCordinate[0];
		float tempValueY = newLineCordinate[1];
		final int rows = 2;
		final int cols = 3;
		final float rowHeight = 20f;
		final float tableWidth = doc.getPage(0).getMediaBox().getWidth() - 50f;
		final float tableHeight = rowHeight * rows;
		final float colWidth = tableWidth / (float) cols;
		final float cellMargin = 5f;

		// draw the rows

		for (int i = 0; i <= rows; i++) {

			contentStream.moveTo(margin, tempValueY);
			contentStream.lineTo(margin + tableWidth, tempValueY);// this 2 commands draws a line
			contentStream.setStrokingColor(0.55f);
			contentStream.stroke();
			tempValueY -= rowHeight;
		}

		// draw the columns
		for (int i = 0; i <= cols; i++) {
			contentStream.moveTo(tempValueX, newLineCordinate[1]);
			contentStream.lineTo(tempValueX, newLineCordinate[1] - tableHeight);
			contentStream.setStrokingColor(0.55f);
			contentStream.stroke();
			tempValueX += colWidth;
		}

		Map<String, String> mapObj = new HashMap<String, String>();
		mapObj.put("name", "Name");
		mapObj.put("result", "Your Result");
		mapObj.put("expected", "Expected Result");
		mapObj.put("test", "SARS CoV 2 RNA(COVID19),");
		mapObj.put("resultValue", userObj.isCovidPositive() == 1 ? "Detected" : "Not Detected");
		mapObj.put("expectedValue", "Not Detected/Negative");

		// this 2 variable are used to move cursor to first cell of table with indent of
		// cellmargin
		int index = 0;
		float textx = margin + cellMargin;
		float texty = newLineCordinate[1] - (3 * cellMargin);
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				contentStream.beginText();
				contentStream.newLineAtOffset(textx, texty);
				contentStream.showText(getText(mapObj, index));
				index++;
				contentStream.endText();
				textx += colWidth;
			}
			texty -= rowHeight; // decrement Y for next row
			textx = margin + cellMargin;// start X at same position for next row
		}

		contentStream = updateYvalue(tableHeight, contentStream);

	}

	public String getText(Map<String, String> mapObj, int index) {
		return pdfService.getText(mapObj, index);
	}
}

package com.pdf.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.text.AbstractDocument.LeafElement;

import com.pdf.demo.model.UserData;
import com.pdf.demo.service.AWSS3Service;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.tools.OverlayPDF;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PdfDemo {

	Logger logger = LoggerFactory.getLogger(PdfDemo.class);

	@Autowired
	AWSS3Service awsService;

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

	private void initializeValues() {

		font_size = 12F;
		LEADING = font_size * 1.1f;// can increment by any number or simply set leading by font size
		imageSize = 50f;
		TIMES_BOLD = PDType1Font.TIMES_BOLD;
		TIMES_ROMAN = PDType1Font.TIMES_ROMAN;
		initialCordinate = new float[] { 20f, 20f };
		paddingCordinate = new float[] { 100f, 20f };
		headerFooterSizeCordinate = 100f;
		document = new PDDocument();// main class to create a pdf
	}

	public boolean generatePdf(UserData userObj, String position, String align) throws Exception {

		initializeValues();

		File destFile = new File("/Users/ttt/Desktop/Work Personal/PdfDocs/destFile.pdf");

		File mainFile = new File("/Users/ttt/Desktop/Work Personal/PdfDocs/MainFile.pdf");
		writeContent(document, userObj);
		document.save(mainFile);
		document.close();

		File mergeFile = new File("/Users/ttt/Desktop/Work Personal/PdfDocs/mergeFile.pdf");
		List<File> arrFileList = getFiles();
		if (arrFileList != null && !arrFileList.isEmpty()) {
			mergeDocuments(arrFileList, mainFile, mergeFile);
			deleteS3Files(arrFileList);
		}

		PDDocument doc = PDDocument.load(mergeFile);
		File fileWatermark = getWaterMarkFile();

		PDDocument docWaterMark = PDDocument.load(fileWatermark);
		Overlay overlay = new Overlay();
		overlay.setInputPDF(doc);
		overlay.setAllPagesOverlayPDF(docWaterMark);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		overlay.overlay(Collections.emptyMap());
		doc.save(destFile);
		doc.close();

		PDDocument destDocument = PDDocument.load(destFile);
		PDPageTree pageTree = destDocument.getDocumentCatalog().getPages();
		for (PDPage page : pageTree) {
			page.setMediaBox(getPageSize());
		}

		// createLogo(position, align, doc);
		// createHeader(doc);
		// createFooter(doc);
		// createWaterMark(doc);

		destDocument.save(destFile);
		destDocument.close();

		Files.deleteIfExists(mainFile.toPath());
		Files.deleteIfExists(fileWatermark.toPath());
		Files.deleteIfExists(mergeFile.toPath());

		System.out.println("PDF created");

		return true;
	}

	private File getWaterMarkFile() throws Exception {

		PDDocument doc = new PDDocument();
		PDPage page = getDocPage();
		doc.addPage(page);
		PDPageContentStream headerStream = getPageContentStream(doc, page);
		headerStream.setLeading(LEADING);
		headerStream.setFont(TIMES_ROMAN, font_size);
		headerStream.beginText();
		headerStream.newLineAtOffset(page.getMediaBox().getUpperRightX() - 100f,
				page.getMediaBox().getUpperRightY() - 15f);
		headerStream.showText("Sample Header");
		headerStream.endText();
		headerStream.close();

		PDPageContentStream footerStream = getPageContentStream(doc, page);
		footerStream.setLeading(LEADING);
		footerStream.setFont(TIMES_ROMAN, font_size);
		footerStream.beginText();
		footerStream.newLineAtOffset(page.getMediaBox().getUpperRightX() - 100f,
				page.getMediaBox().getLowerLeftY() + 15f);
		footerStream.showText("Sample Footer");
		footerStream.endText();
		footerStream.close();

		PDPageContentStream logoStream = getPageContentStream(doc, page);
		logoStream.setLeading(LEADING);
		float tempX = page.getMediaBox().getLowerLeftX() + 10f;
		float tempY = page.getMediaBox().getUpperRightY() - 50f;

		PDImageXObject image = PDImageXObject
				.createFromFile("src/main/resources/static/images/Quest Diagnostics logo.jpeg", doc);
		logoStream.drawImage(image, tempX, tempY, imageSize, imageSize);
		logoStream.close();

		File file = new File("src/main/resources/static/pdf/watermark.pdf");

		PDDocument docWaterMark = PDDocument.load(file);
		Overlay overlay = new Overlay();
		overlay.setInputPDF(doc);
		overlay.setAllPagesOverlayPDF(docWaterMark);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		overlay.overlay(Collections.emptyMap());

		File fileWatermark = new File("/Users/ttt/Desktop/Work Personal/PdfDocs/Watermark.pdf");
		doc.save(fileWatermark);
		doc.close();

		return fileWatermark;
	}

	private PDRectangle getPageSize() {
		return PDRectangle.A4;
	}

	// merge all source documents to destination
	private void mergeDocuments(List<File> arrFileList, File sourceFile, File destFile) throws Exception {
		PDFMergerUtility mergerUtil = new PDFMergerUtility();
		mergerUtil.setDestinationFileName(destFile.getPath());
		mergerUtil.addSource(sourceFile);

		arrFileList.forEach(file -> {
			try {
				mergerUtil.addSource(file);
			} catch (Exception e) {
				logger.error("File Not Found", e);
			}
		});

		mergerUtil.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

	}

	// add pages in existing document as it is
	private void writeFromFileNew(PDDocument document) throws Exception {
		File file = new File("src/main/resources/static/pdf/TestImage.pdf");

		PDDocument docExtracted = PDDocument.load(file);
		PDPageTree tree = docExtracted.getPages();
		for (PDPage pageNew : tree) {
			document.addPage(pageNew);

		}

	}

	// Extracting image from pdf and merging in main pdf
	private void writeFromImage(PDDocument document) throws Exception {
		File file = new File("src/main/resources/static/pdf/TestImage.pdf");

		PDDocument docExtracted = PDDocument.load(file);

		PDPageTree list = docExtracted.getPages();
		for (PDPage page : list) {
			PDResources pdResources = page.getResources();
			for (COSName c : pdResources.getXObjectNames()) {
				PDXObject o = pdResources.getXObject(c);
				PDImageXObject obj = (PDImageXObject) o;

				PDPage pageNew = getDocPage();
				document.addPage(pageNew);
				PDPageContentStream imageContentStream = getPageContentStream(document, pageNew);

				imageContentStream.drawImage(obj, 50, 50, imageSize, imageSize);
				imageContentStream.close();
			}
		}
	}

	// extract text from pdf and merging in main pdf
	private void writeFromFile(PDDocument document) throws Exception {

		File file = new File("src/main/resources/static/pdf/TestText.pdf");

		PDDocument docExtracted = PDDocument.load(file);

		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setLineSeparator(System.lineSeparator());
		stripper.setStartPage(0);
		stripper.setEndPage(docExtracted.getNumberOfPages());
		String text = stripper.getText(docExtracted);

		String[] textDtls = text.split(System.lineSeparator());

		PDPageContentStream content = getNewContentStream();

		for (String line : textDtls) {
			content = callNewLine(1, content);
			content.showText(line);
		}

		content.endText();
		content.close();
		docExtracted.close();

	}

	// static text in main pdf

	public void writeContent(PDDocument doc, UserData userObj) throws Exception {
		boolean continueFlag = true;

		String data = readFile("src/main/resources/static/inputData/inputData.txt");
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

	private PDPage getDocPage() {
		return new PDPage(getPageSize());
	}

	public void createFooter(PDDocument doc) throws Exception {

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

	}

	private PDPageContentStream getPageContentStream(PDDocument doc, PDPage page) throws IOException {
		return new PDPageContentStream(doc, page,
				PDPageContentStream.AppendMode.APPEND, false, true);
	}

	public void createHeader(PDDocument doc) throws Exception {

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

	}

	public void deleteS3Files(List<File> fileList) {

		List<File> arrS3FileList = fileList.stream().filter(file -> file.getPath().contains("/S3/"))
				.collect(Collectors.toList());
		arrS3FileList.forEach(file -> {
			try {
				Files.deleteIfExists(Paths.get(file.getPath()));
			} catch (IOException e) {

			}
		});
	}

	public List<File> getFiles() {

		List<File> arrFileList = new ArrayList<File>();
		try {

			// 1 - Add from local system
			arrFileList.add(new File("src/main/resources/static/pdf/Test.pdf"));

			// 2 - Add from S3 bucket
			addFileFromS3(arrFileList);

		} catch (Exception e) {
			logger.error("Error in getting files", e);
			return null;
		}
		return arrFileList;
	}

	public void addFileFromS3(List<File> fileList) throws Exception {
		awsService.setPrefix("dummy/");
		awsService.setDelimiter("/");
		awsService.listObjects(fileList);
	}

	// add files in form of annotation in main pdf
	public void addAnnotationAsFile(List<File> fileList, PDDocument doc, PDPage blankPage)
			throws Exception {

		fileList.forEach(file -> {

			try {

				PDComplexFileSpecification fs = new PDComplexFileSpecification();

				FileInputStream fins1 = new FileInputStream(file);
				PDEmbeddedFile ef = new PDEmbeddedFile(doc, fins1);

				fs.setEmbeddedFile(ef);
				fs.setFile(file.getPath());

				PDAnnotationFileAttachment txtLink = new PDAnnotationFileAttachment();

				txtLink.setFile(fs);
				PDColor blue = new PDColor(new float[] { 0, 1, 0 }, PDDeviceRGB.INSTANCE);
				txtLink.setColor(blue);
				txtLink.setAttachementName(PDAnnotationFileAttachment.ATTACHMENT_NAME_PAPERCLIP);

				// Set the rectangle containing the link
				int offsetX = (int) newLineCordinate[0];
				int offsetY = (int) newLineCordinate[1];
				PDRectangle positionn = new PDRectangle();
				positionn.setLowerLeftX(offsetX);
				positionn.setLowerLeftY(offsetY);
				positionn.setUpperRightX(offsetX + 5);
				positionn.setUpperRightY(offsetY + 10);

				// Below code is to add custom image as annotation
				/*
				 * PDAppearanceDictionary appearanceDictionary = new PDAppearanceDictionary();
				 * PDAppearanceStream appearanceStream = new PDAppearanceStream(doc);
				 * appearanceStream.setResources(new PDResources());
				 * appearanceStream.setBBox(positionn); PDImageXObject image =
				 * PDImageXObject.createFromFile(
				 * "src/main/resources/static/images/attachImage.png", doc);
				 * content.drawImage(image, offsetX, offsetY, 15f, 15f);
				 * appearanceDictionary.setNormalAppearance(appearanceStream);
				 * txtLink.setAppearance(appearanceDictionary);
				 */

				txtLink.setRectangle(positionn);
				blankPage.getAnnotations().add(txtLink);

				PDPageContentStream annotationContentStream = getPageContentStream(doc, blankPage);

				String text = "Click here to view " + file.getName();
				annotationContentStream.beginText();

				annotationContentStream.setFont(TIMES_ROMAN, font_size - 1);
				annotationContentStream.newLineAtOffset(newLineCordinate[0] + 10f, newLineCordinate[1]);
				annotationContentStream.showText(text);
				callNewLine(1, annotationContentStream);
				annotationContentStream.endText();
				annotationContentStream.close();

			} catch (Exception e) {
				logger.error("msg", e);
			}
		});

	}

	// add files in form of attachment in main pdf
	public void addEmbeddedFile(List<File> fileList, PDDocument doc, PDPage blankPage)
			throws IOException, FileNotFoundException {

		// embedded files are stored in a named tree. this is main tree node
		PDEmbeddedFilesNameTreeNode mainTreeNode = new PDEmbeddedFilesNameTreeNode();

		PDEmbeddedFilesNameTreeNode treeNode = new PDEmbeddedFilesNameTreeNode();

		Map<String, PDComplexFileSpecification> map = fileList.stream()
				.collect(Collectors.toMap(f -> f.getName(), f -> {
					PDComplexFileSpecification fs = new PDComplexFileSpecification();
					fs.setFile(f.getPath());

					// create stream for the file to be embedded
					InputStream inputStream;
					try {
						inputStream = new FileInputStream(f);
						// represents the embedded file
						PDEmbeddedFile ef = new PDEmbeddedFile(doc, inputStream);

						ef.setCreationDate(Calendar.getInstance());
						fs.setEmbeddedFile(ef);

					} catch (Exception e) {

					}
					return fs;
				}));

		treeNode.setNames(map);// add

		// add the new node as kid to the main tree node
		List<PDEmbeddedFilesNameTreeNode> childTreeNodeList = new ArrayList<PDEmbeddedFilesNameTreeNode>();
		childTreeNodeList.add(treeNode);
		mainTreeNode.setKids(childTreeNodeList);

		// add the tree to the document catalog of current document
		PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
		names.setEmbeddedFiles(mainTreeNode);
		doc.getDocumentCatalog().setNames(names);
	}

	public String readFile(String string) throws IOException, InvalidPathException {
		return new String(Files.readAllBytes(Paths.get("src/main/resources/static/inputData/inputData.txt")));
	}

	public void createWaterMark(PDDocument doc) throws IOException {

		HashMap<Integer, String> overlayGuide = new HashMap<Integer, String>();
		for (int i = 1; i <= doc.getNumberOfPages(); i++) {
			overlayGuide.put(i, "src/main/resources/static/pdf/watermark1.pdf");
		}

		PDDocument docWaterMark = PDDocument.load(new File("src/main/resources/static/pdf/watermark1.pdf"));
		Overlay overlay = new Overlay();
		overlay.setInputPDF(doc);
		overlay.setAllPagesOverlayPDF(docWaterMark);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		overlay.overlay(overlayGuide);

	}

	public void createLogo(String position, String align, PDDocument doc)
			throws Exception {

		PDImageXObject image = PDImageXObject
				.createFromFile("src/main/resources/static/images/Quest Diagnostics logo.jpeg", doc);

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

		// updateYvalue(LEADING, null);/

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

	public PDPageContentStream callNewLine(int count, PDPageContentStream content) throws Exception {

		for (int i = 0; i < count; i++) {
			content = updateYvalue(LEADING, content);
			content.newLine();
		}

		return content;
	}

	private void createTable(UserData userObj, PDPageContentStream contentStream, PDDocument doc)
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
				contentStream.showText(getText(index, mapObj));
				index++;
				contentStream.endText();
				textx += colWidth;
			}
			texty -= rowHeight; // decrement Y for next row
			textx = margin + cellMargin;// start X at same position for next row
		}

		contentStream = updateYvalue(tableHeight, contentStream);

	}

	public String getText(int index, Map<String, String> mapObj) {

		if (index == 0) {
			return mapObj.get("name");
		} else if (index == 1) {
			return mapObj.get("result");
		} else if (index == 2) {
			return mapObj.get("expected");
		} else if (index == 3) {
			return mapObj.get("test");
		} else if (index == 4) {
			return mapObj.get("resultValue");
		} else if (index == 5) {
			return mapObj.get("expectedValue");
		}
		return "";
	}

}

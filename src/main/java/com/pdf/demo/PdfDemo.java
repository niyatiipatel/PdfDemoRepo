package com.pdf.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pdf.demo.model.UserData;
import com.pdf.demo.service.AWSS3Service;

import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PdfDemo {

	@Autowired
	AWSS3Service awsService;

	float font_size;
	float LEADING;// can increment by any number or simply set leading by font size
	float value_x;
	float value_y;
	float IMAGE_SIZE;
	PDFont TIMES_BOLD;
	PDFont TIMES_ROMAN;

	public boolean generatePdf(UserData userObj, String position, String align) throws Exception {

		initializeValues();

		String data = readFile("src/main/resources/static/inputData/inputData.txt");
		String[] arrData = data.split("BREAK");

		PDDocument doc = new PDDocument();// main class to create a pdf
		PDPage blankPage = new PDPage(PDRectangle.LETTER); // class to create pages and add in document

		// make initial position in pdf to start with
		// X - lowerLeft(0+20) and Y - UpperRight(max according to page type -100
		value_x = blankPage.getMediaBox().getLowerLeftX() + 20f;
		value_y = blankPage.getMediaBox().getUpperRightY() - 20f;

		PDPageContentStream content = new PDPageContentStream(doc, blankPage);
		content.setLeading(LEADING);// If you don't use this, content will be overridden at the same place and not
									// lead
		// Ques - How to get the image from resource folder, have tried mvc
		// draw image based on postion and update X and Y values accordingly
		createImage(position, align, content, doc, blankPage);

		createContent(userObj, content, doc, blankPage, arrData);

		createFiles(content, doc, blankPage);

		content.close();

		doc.addPage(blankPage); // add pages in document
		createWaterMark(doc);

		doc.save("/Users/ttt/Desktop/Work Personal/PdfDocs/Blank.pdf");// imp to save doc
		System.out.println("PDF created");
		doc.close();// imp to close doc
		return true;
	}

	private void createFiles(PDPageContentStream content, PDDocument doc, PDPage blankPage) throws Exception {

		List<File> fileList = getFiles();

		if (fileList != null && !fileList.isEmpty()) {
			// If the path is incorrect, it won't be embedded in attachment
			addEmbeddedFile(fileList, doc, blankPage);
			// addAnnotationAsFile(fileList, doc, blankPage, content);

			deleteS3Files(fileList);

		}
	}

	private void deleteS3Files(List<File> fileList) {

		List<File> arrS3FileList = fileList.stream().filter(file -> file.getPath().contains("/S3/"))
				.collect(Collectors.toList());
		arrS3FileList.forEach(file -> {
			try {
				Files.deleteIfExists(Paths.get(file.getPath()));
			} catch (IOException e) {

			}
		});
	}

	private List<File> getFiles() {

		List<File> arrFileList = new ArrayList<File>();
		try {

			// 1 - Add from local system
			arrFileList.add(new File("src/main/resources/static/inputData/inputData.txt"));

			// 2 - Add from S3 bucket
			addFileFromS3(arrFileList);

			// test
			// new test

		} catch (Exception e) {
			System.out.println("Error in getting files");
			return null;
		}
		return arrFileList;
	}

	private void addFileFromS3(List<File> fileList) throws Exception {

		awsService.listObjects(fileList);
	}

	private void addAnnotationAsFile(List<File> fileList, PDDocument doc, PDPage blankPage, PDPageContentStream content)
			throws IOException {

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

				txtLink.setAttachmentName(PDAnnotationFileAttachment.ATTACHMENT_NAME_PAPERCLIP);

				// Set the rectangle containing the link
				int offsetX = (int) value_x;
				int offsetY = (int) value_y;
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

				String text = "Click here to view " + file.getName();
				content.beginText();

				content.setFont(TIMES_ROMAN, font_size - 1);
				content.newLineAtOffset(value_x + 10f, value_y);
				content.showText(text);
				callNewLine(1, content);
				content.endText();

			} catch (IOException e) {

			}
		});

	}

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

	private void initializeValues() {

		font_size = 12F;
		LEADING = font_size * 1.1f;// can increment by any number or simply set leading by font size
		IMAGE_SIZE = 100f;
		TIMES_BOLD = PDType1Font.TIMES_BOLD;
		TIMES_ROMAN = PDType1Font.TIMES_ROMAN;

	}

	public void createWaterMark(PDDocument doc) throws IOException {

		PDDocument docWaterMark = PDDocument.load(new File("src/main/resources/static/pdf/watermark.pdf"));
		Overlay overlay = new Overlay();
		overlay.setInputPDF(doc);
		overlay.setAllPagesOverlayPDF(docWaterMark);
		overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
		overlay.overlay(Collections.emptyMap());

	}

	private void createContent(UserData userObj, PDPageContentStream content, PDDocument doc, PDPage blankPage,
			String[] arrData) throws IOException {

		int dataIndex = 0;// for static text
		content.beginText();
		callNewLine(1, content);
		content.newLineAtOffset(value_x, value_y);
		updateYvalue(LEADING);
		content.setFont(PDType1Font.TIMES_BOLD, font_size + 2f);
		content.showText(arrData[dataIndex++]);

		callNewLine(3, content);
		content.showText(arrData[dataIndex++]);

		content.setFont(TIMES_ROMAN, font_size);
		callNewLine(2, content);
		content.showText(arrData[dataIndex++] + userObj.getDtService());
		callNewLine(1, content);
		content.showText(arrData[dataIndex++] + userObj.getFullName());
		callNewLine(1, content);
		content.showText(arrData[dataIndex++] + userObj.getDob());
		callNewLine(1, content);
		content.showText(arrData[dataIndex++] + userObj.getGender());
		callNewLine(1, content);
		content.showText(arrData[dataIndex++]);
		callNewLine(1, content);
		content.newLineAtOffset(50f, 0f);
		updateYvalue(LEADING);
		content.showText(arrData[dataIndex++]);
		callNewLine(1, content);
		content.newLineAtOffset(-50f, 0f);
		updateYvalue(LEADING);
		content.showText(arrData[dataIndex++]);
		callNewLine(3, content);
		content.setFont(TIMES_BOLD, font_size);
		content.showText(arrData[dataIndex++]);

		content.setFont(TIMES_ROMAN, font_size);
		content.endText();

		// Ques : better solution for table creation
		createTable(userObj, content, blankPage, doc);
		content.beginText();
		callNewLine(2, content);
		content.newLineAtOffset(value_x, value_y);
		updateYvalue(LEADING);
		content.setFont(TIMES_BOLD, font_size);
		content.showText(arrData[dataIndex++]);
		content.setFont(TIMES_ROMAN, font_size);

		callNewLine(1, content);

		// Ques - better solution to wrap text
		wrapString(arrData[dataIndex++], content);
		callNewLine(2, content);
		wrapString(arrData[dataIndex++], content);
		callNewLine(2, content);

		content.setFont(TIMES_ROMAN, font_size - 5);
		content.showText(arrData[dataIndex++]);
		callNewLine(1, content);

		content.showText(arrData[dataIndex++]);
		callNewLine(1, content);
		content.showText(arrData[dataIndex++]);
		callNewLine(1, content);
		content.setFont(PDType1Font.HELVETICA, font_size - 5);
		content.showText(arrData[dataIndex++]);
		callNewLine(3, content);

		// Ques How to add Chinese font in pdfbox
		// content.setFont(PDTrueTypeFont., font_size);
		// content.showText("注意：如果您使用繁體中 (Chinese)，您可以免費獲得語言援助服務. 請致電 1.844.698.1022.");

		content.endText();

	}

	public void createImage(String position, String align, PDPageContentStream content, PDDocument doc, PDPage page)
			throws IOException {

		PDImageXObject image = PDImageXObject
				.createFromFile("src/main/resources/static/images/Quest Diagnostics logo.jpeg", doc);
		float tempX = value_x;
		float tempY = value_y;

		if (position.equalsIgnoreCase("top")) {
			updateYvalue(IMAGE_SIZE);// Need to allocate some space for image size
			tempY = value_y;
		} else if (position.equalsIgnoreCase("bottom")) {
			tempY = page.getMediaBox().getLowerLeftY();

		}
		if (align.equalsIgnoreCase("left")) {
			tempX = value_x;
		} else if (align.equalsIgnoreCase("right")) {
			tempX = page.getMediaBox().getUpperRightX() - IMAGE_SIZE;// allocate some space from right for image size
		} else if (align.equalsIgnoreCase("center")) {
			tempX = (page.getMediaBox().getUpperRightX() - IMAGE_SIZE) / 2;
		}

		content.drawImage(image, tempX, tempY, IMAGE_SIZE, IMAGE_SIZE);
		updateYvalue(LEADING);// this is always going to be decrementing since we move down the line
	}

	public void updateYvalue(float value) {

		value_y = value_y - value;
	}

	public void wrapString(String text, PDPageContentStream content) throws IOException {
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
			updateYvalue(LEADING);
		}
	}

	public void callNewLine(int count, PDPageContentStream content) throws IOException {

		for (int i = 0; i < count; i++) {
			content.newLine();
			updateYvalue(LEADING);
		}

	}

	private void createTable(UserData userObj, PDPageContentStream contentStream, PDPage page, PDDocument doc)
			throws IOException {

		float margin = value_x;// need to constant X value for rows and column creation
		float tempValueX = value_x;
		float tempValueY = value_y;
		final int rows = 2;
		final int cols = 3;
		final float rowHeight = 20f;
		final float tableWidth = page.getMediaBox().getWidth() - 50f;
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
			contentStream.moveTo(tempValueX, value_y);
			contentStream.lineTo(tempValueX, value_y - tableHeight);
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
		float texty = value_y - (3 * cellMargin);
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

		updateYvalue(tableHeight);

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

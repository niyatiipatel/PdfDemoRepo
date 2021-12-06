package com.pdf.demo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    @Autowired
    AWSS3Service awsService;

    @Value("${path_src_pdf}")
    String path_src_pdf;

    Logger logger = LoggerFactory.getLogger(PdfService.class);

    public PDRectangle getPageSize() {
        return PDRectangle.A4;
    }

    public String readFile(String path) throws IOException, InvalidPathException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    public String getText(Map<String, String> mapObj, int index) {

        if (mapObj == null || mapObj.isEmpty()) {
            return "";
        }
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

    public List<File> getFiles() {
        List<File> arrFileList = new ArrayList<File>();
        try {

            // 1 - Add from local system
            // arrFileList = getFilesFromLocal(arrFileList);

            // 2 - Add from S3 bucket
            arrFileList = getFilesFromS3(arrFileList);

        } catch (Exception e) {
            logger.error("Error in getting files", e);
            return null;
        }
        return arrFileList;
    }

    public List<File> getFilesFromLocal(List<File> arrFileList) {

        arrFileList.add(new File(path_src_pdf + "Test.pdf"));
        return arrFileList;
    }

    public List<File> getFilesFromS3(List<File> fileList) throws Exception {
        awsService.setPrefix("dummy/");
        awsService.setDelimiter("/");
        return awsService.listObjects(fileList);
    }

    public void deleteS3Files(List<File> fileList) {
        List<File> arrS3FileList = fileList.stream().filter(file -> file.getPath().contains("/S3/"))
                .collect(Collectors.toList());
        arrS3FileList.forEach(file -> {
            try {
                deleteFileFromSys(file);
            } catch (IOException e) {

            }
        });
    }

    public void deleteFileFromSys(File mainFile) throws IOException {
        Files.deleteIfExists(mainFile.toPath());

    }

    public boolean mergeDocuments(File destFile, File mainFile, List<File> arrFileList) throws Exception {

        PDFMergerUtility mergerUtil = new PDFMergerUtility();
        mergerUtil.setDestinationFileName(destFile.getPath());
        mergerUtil.addSource(mainFile);

        arrFileList.forEach(file -> {
            try {
                mergerUtil.addSource(file);
            } catch (Exception e) {
                logger.error("File Not Found", e);
            }
        });

        mergerUtil.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
        return true;
    }

    public PDPage getDocPage() {
        return new PDPage(getPageSize());
    }

    public PDPageContentStream getPageContentStream(PDDocument doc, PDPage page) throws Exception {
        return new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, false, true);
    }
}

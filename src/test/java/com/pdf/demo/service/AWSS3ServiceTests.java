package com.pdf.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pdf.demo.PdfDemoApplication;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class AWSS3ServiceTests {

    private static S3Client s3client;
    private static Region region;
    private static String bucketName;

    @InjectMocks
    AWSS3Service awsService;
    static String objectName;

    static Properties prop = new Properties();

    private void setS3Client(S3Client s3client) {
        AWSS3ServiceTests.s3client = s3client;
    }

    @BeforeAll
    public static void setup() {

        MockitoAnnotations.openMocks(AWSS3ServiceTests.class);
        try {

            InputStream input = PdfDemoApplication.class.getClassLoader().getResourceAsStream("configTest.properties");

            prop.load(input);

            region = Region.of(prop.getProperty("region"));
            bucketName = prop.getProperty("bucketName");

        } catch (IOException e) {
        }
    }

    @Test
    @Order(1)
    public void testS3Client() {
        awsService.setPrefix("dummy/");
        awsService.setDelimiter("/");
        s3client = awsService.getS3Client(region);
        assertNotNull(s3client);
        setS3Client(s3client);
    }

    @Test
    @Order(2)
    public void testListObjects() {

        awsService.setPrefix("dummy/");
        awsService.setDelimiter("/");
        ListObjectsRequest listObjReq = awsService.getListObjectRequest(bucketName);
        assertNotNull(listObjReq);

        ListObjectsResponse listObjRes = awsService.getObjectResponse(s3client, listObjReq);
        assertNotNull(listObjRes);

        List<S3Object> listObjects = awsService.getObjResponseInList(listObjRes);
        assertNotNull(listObjects);
        assertThat(listObjects.size()).isGreaterThan(0);

        listObjects = awsService.getResponseAfterFilter(listObjects);
        assertNotNull(listObjects);
        assertThat(listObjects.size()).isGreaterThan(0);

        objectName = listObjects.get(0).key();
    }

    @Test
    @Order(3)
    public void testGetObjectContent() {

        GetObjectRequest objectRequest = awsService.getObjectRequest(objectName, bucketName);
        assertNotNull(objectRequest);

        ResponseBytes<GetObjectResponse> objectResponse = awsService.getObjectResponse(s3client, objectRequest);
        assertNotNull(objectResponse);

        byte[] data = awsService.getObjResponseInBytes(objectResponse);
        assertNotNull(data);
        assertThat(data.length).isGreaterThan(0);

    }

    @Test
    @Order(4)
    public void testgetAllObjects() {

        awsService.setPrefix("dummy/");
        awsService.setDelimiter("/");
        List<S3Object> arrList = awsService.getAllObjects(s3client, bucketName);
        assertNotNull(arrList);

    }

    @Test
    @Order(5)
    public void testgetListObjects() throws Exception {

        awsService.setPath(prop.getProperty("config.path"));
        awsService.setBucket(bucketName);
        List<File> arrList = Stream.of(new File("any path")).collect(Collectors.toList());

        assertNull(awsService.listObjects(null));
        awsService.setPrefix(null);
        assertNull(awsService.listObjects(arrList));
        awsService.setDelimiter(null);
        assertNull(awsService.listObjects(arrList));

        awsService.setPrefix("dummy/");
        awsService.setDelimiter("/");
        assertNotNull(awsService.listObjects(arrList));

    }

}

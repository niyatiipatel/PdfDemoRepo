package com.pdf.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
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

    static AWSS3Service awsService;
    static String objectName;

    @BeforeAll
    public static void setup() {

        try {
            awsService = new AWSS3Service();
            awsService.setPrefix("dummy/");
            awsService.setDelimiter("/");

            InputStream input = AWSS3ServiceTests.class.getClassLoader().getResourceAsStream("config.properties");
            Properties prop = new Properties();

            prop.load(input);

            region = Region.of(prop.getProperty("region"));
            bucketName = prop.getProperty("bucketName");

            s3client = awsService.getS3Client(region);
        } catch (IOException e) {
        }
    }

    @Test
    @Order(1)
    public void testS3Client() {
        assertNotNull(s3client);
    }

    @Test
    @Order(2)
    public void testListObjects() {

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

}

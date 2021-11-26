package com.pdf.demo.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class AWSS3Service {

    Logger logger = LoggerFactory.getLogger(AWSS3Service.class);

    @Value("${cloud.s3.bucketName}")
    String bucketName;

    @Value("${cloud.aws.region}")
    Region region;

    @Value("${config.path}")
    String path;

    String prefix;
    String delimiter;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void listObjects(List<File> arrFileList) throws Exception {

        if (arrFileList == null) {
            return;
        }

        if (prefix == null || delimiter == null) {
            return;
        }

        S3Client s3 = getS3Client(region);

        try {
            List<S3Object> objects = getAllObjects(s3, bucketName);

            for (ListIterator<S3Object> iterVals = objects.listIterator(); iterVals.hasNext();) {
                S3Object myValue = (S3Object) iterVals.next();

                String objectName = myValue.key();

                byte[] data = getObjectContent(s3, objectName, bucketName);

                // create a local file and write data to it
                File myFile = new File(path + objectName.replace(prefix, ""));
                OutputStream os = new FileOutputStream(myFile);
                os.write(data);
                os.close();

                arrFileList.add(myFile);

            }

        } catch (Exception e) {
            logger.error("Error", e);
            throw new Exception("Exception in listObjects");
        } finally {
            s3.close();
        }
    }

    public byte[] getObjectContent(S3Client s3, String objectName, String bucketName) {

        GetObjectRequest objectRequest = getObjectRequest(objectName, bucketName);
        ResponseBytes<GetObjectResponse> objectResponse = getObjectResponse(s3, objectRequest);
        byte[] data = getObjResponseInBytes(objectResponse);
        return data;
    }

    public byte[] getObjResponseInBytes(ResponseBytes<GetObjectResponse> objectResponse) {
        return objectResponse.asByteArray();
    }

    public ResponseBytes<GetObjectResponse> getObjectResponse(S3Client s3, GetObjectRequest objectRequest) {
        return s3.getObjectAsBytes(objectRequest);
    }

    public GetObjectRequest getObjectRequest(String objectName, String bucketName) {
        return GetObjectRequest.builder().key(objectName).bucket(bucketName).build();
    }

    public List<S3Object> getAllObjects(S3Client s3, String bucketName) {

        ListObjectsRequest listObjectReq = getListObjectRequest(bucketName);

        ListObjectsResponse listObjRes = getObjectResponse(s3, listObjectReq);

        List<S3Object> listS3Obj = getObjResponseInList(listObjRes);

        // remove main folder in bucket
        listS3Obj = getResponseAfterFilter(listS3Obj);

        return listS3Obj;
    }

    public List<S3Object> getResponseAfterFilter(List<S3Object> listS3Obj) {
        return listS3Obj.stream().filter(s -> !s.key().endsWith(delimiter)).collect(Collectors.toList());
    }

    public List<S3Object> getObjResponseInList(ListObjectsResponse res) {
        return res.contents();
    }

    public ListObjectsResponse getObjectResponse(S3Client s3, ListObjectsRequest listObjects) {
        return s3.listObjects(listObjects);
    }

    public ListObjectsRequest getListObjectRequest(String bucketName) {
        return ListObjectsRequest.builder().bucket(bucketName).prefix(prefix).delimiter(delimiter).build();
        // return ListObjectsRequest.builder().bucket(bucketName).build();
    }

    public S3Client getS3Client(Region region) {
        return S3Client.builder().region(region).build();
    }
}

package com.pdf.demo.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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

    @Value("${cloud.s3.bucketName}")
    String bucketName;

    @Value("${cloud.aws.region}")
    Region region;

    @Value("${config.path}")
    String path;

    public void listObjects(List<File> arrFileList) throws Exception {

        if (arrFileList == null) {
            return;
        }

        // String bucketName = "testbucketniyati";// use proprty file instead
        // Region region = Region.AP_SOUTH_1;
        S3Client s3 = S3Client.builder().region(region).build();

        try {

            ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(bucketName).build();

            ListObjectsResponse res = s3.listObjects(listObjects);// get all objects from bucket
            List<S3Object> objects = res.contents();

            for (ListIterator<S3Object> iterVals = objects.listIterator(); iterVals.hasNext();) {
                S3Object myValue = (S3Object) iterVals.next();
                String objectName = myValue.key();

                GetObjectRequest objectRequest = GetObjectRequest.builder().key(objectName).bucket(bucketName).build();

                ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);// get details of
                                                                                                  // object
                byte[] data = objectBytes.asByteArray();

                // String path = "/Users/ttt/Desktop/Work Personal/PdfDocs/S3/";
                // create a local file and write data to it
                File myFile = new File(path + myValue.key());
                OutputStream os = new FileOutputStream(myFile);
                os.write(data);
                os.close();

                arrFileList.add(myFile);

            }

        } catch (Exception e) {
            throw new Exception("Exception in listObjects");
        } finally {
            s3.close();
        }
    }

}

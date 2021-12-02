package com.pdf.demo.controller;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pdf.demo.PdfDemo;
import com.pdf.demo.model.UserData;
import com.pdf.demo.repo.UserDataRepo;

@RestController
public class PdfDemoController {

	Logger logger = LoggerFactory.getLogger(PdfDemoController.class);

	@Autowired
	UserDataRepo userRepo;

	@Autowired
	PdfDemo pdfdemo;

	@RequestMapping(value = "/getUserData", produces = "application/json")
	@ResponseBody()
	public String getUserData(@RequestParam Map<String, String> allParams) {
		try {
			UserData userObj = getUserById(allParams);

			if (userObj == null) {
				return "No User Found";
			}

			boolean flag = generatePdf(userObj, allParams);
			if (!flag) {
				return "Error occured while generating a report";
			}
		} catch (NumberFormatException ne) {
			logger.error("Error", ne);
			return "Input should be a valid number";
		} catch (IOException ioe) {
			logger.error("Error", ioe);
			return "Error occured while generating a report";
		} catch (Exception e) {
			logger.error("Error", e);
			return "Error occured while generating a report";
		}

		return "Report Generated";
	}

	public boolean generatePdf(UserData userObj, Map<String, String> allParams) throws Exception {

		boolean flag = pdfdemo.generatePdf(userObj,
				allParams.containsKey("position") ? allParams.get("position").toString() : "top",
				allParams.containsKey("align") ? allParams.get("align").toString() : "left");
		return flag;
	}

	public UserData getUserById(Map<String, String> allParams) throws NumberFormatException {

		if (allParams.isEmpty() || !allParams.containsKey("id")) {
			return null;
		}

		return userRepo.findById(Integer.valueOf(allParams.get("id").toString())).orElse(null);
	}

}

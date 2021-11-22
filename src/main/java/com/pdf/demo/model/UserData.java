package com.pdf.demo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_data")
public class UserData {

	@Id
	@Column(name = "user_id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int userId;
	
	@Column(name = "full_name")
	private String fullName;
	
	@Column(name = "dob")
	private String dob;
	
	@Column(name = "gender")
	private String gender;
	
	@Column(name = "is_covid_positive")
	private int isCovidPositive;
	
	@Column(name = "dt_of_service")
	private String dtService;
	
	
	public int getUserId() {
		return userId;
	}
	public void setUserId(int userId) {
		this.userId = userId;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getDob() {
		return dob;
	}
	public void setDob(String dob) {
		this.dob = dob;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public int isCovidPositive() {
		return isCovidPositive;
	}
	public void setCovidPositive(int isCovidPositive) {
		this.isCovidPositive = isCovidPositive;
	}
	public String getDtService() {
		return dtService;
	}
	public void setDtService(String dtService) {
		this.dtService = dtService;
	}
	
	
}

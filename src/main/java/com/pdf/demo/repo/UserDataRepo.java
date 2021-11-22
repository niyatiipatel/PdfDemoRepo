package com.pdf.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pdf.demo.model.UserData;

public interface UserDataRepo  extends JpaRepository<UserData, Integer> {

}

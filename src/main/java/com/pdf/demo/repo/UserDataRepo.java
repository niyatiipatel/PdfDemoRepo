package com.pdf.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pdf.demo.model.UserData;

@Repository
public interface UserDataRepo extends JpaRepository<UserData, Integer> {

}

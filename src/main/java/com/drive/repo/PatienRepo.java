package com.drive.repo;

import com.drive.model.Patient;
import org.springframework.stereotype.Repository;

@Repository
public class PatienRepo implements IPatientRepoImple{


    @Override
    public Patient save(Patient patient){
        System.out.println("Saving ....");
        return patient;
    }
}

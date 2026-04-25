package com.drive.repo;

import com.drive.model.Patient;
import org.springframework.stereotype.Repository;

@Repository
public interface IPatientRepoImple {

    Patient save(Patient patient);
}

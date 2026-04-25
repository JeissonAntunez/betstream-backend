package com.drive.service;

import com.drive.model.Patient;
import com.drive.repo.IPatientRepoImple;
import com.drive.repo.PatienRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// Estereotipos para inyecciones en java
// @Service    -- Logica de negocio operaciones, requerimientos
// @Repository  -- Data
// @Controller  -- Intercepcion conn data y model
// @Component  -- En general
@Service
@RequiredArgsConstructor
public class PatientService implements IPatientInterfaceImple {

    //Logica de Negocio
    // Para no tener que instancear de manera verbosa utilizamos estereotipos
    //private PatienRepo repo = new PatienRepo();
    private final IPatientRepoImple repo;

    @Override
    public Patient validAndSave(Patient patient){
        if(patient.getIdPatient() == 0){
            return repo.save(patient);
        }else{
            return new Patient();
        }
    }
}

package com.drive.controller;

import com.drive.model.Patient;
import com.drive.service.IPatientInterfaceImple;
import com.drive.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final IPatientInterfaceImple service ;

    @GetMapping
    public Patient save(){
        Patient patient = new Patient();
        patient.setIdPatient(0);
        patient.setFirstName("mito");
        return  service.validAndSave(patient);
    }
}

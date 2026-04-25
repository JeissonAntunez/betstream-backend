package com.drive.service;

import com.drive.model.Patient;
import org.springframework.stereotype.Service;

@Service
public interface IPatientInterfaceImple {
      Patient validAndSave(Patient patient);
}

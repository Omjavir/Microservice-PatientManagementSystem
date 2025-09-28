package com.example.patient_service.service;

import com.example.patient_service.dto.PatientRequestDTO;
import com.example.patient_service.exception.EmailAlreadyExistsException;
import com.example.patient_service.exception.PatientNotFoundException;
import com.example.patient_service.mapper.PatientMapper;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.example.patient_service.model.Patient;
import com.example.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();

        return patients.stream().map(PatientMapper::toDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO){
        if(patientRepository.existsByEmail((patientRequestDTO.getEmail()))) {
            throw new EmailAlreadyExistsException("Patient with Email" + patientRequestDTO.getEmail() + " already exists");
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO){
        Patient patient = patientRepository.findById(id).orElseThrow(
                () -> new PatientNotFoundException("Patient with id " + id + " not found")
        );

        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("Patient with Email" + patientRequestDTO.getEmail() + " already exists");
        }

        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient patientUpdated = patientRepository.save(patient);

        return PatientMapper.toDTO(patientUpdated);
    }

    public void deletePatient(UUID id){
        patientRepository.deleteById(id);
    }
}

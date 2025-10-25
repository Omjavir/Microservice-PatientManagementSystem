package com.example.patient_service.service;

import com.example.patient_service.dto.PatientRequestDTO;
import com.example.patient_service.exception.EmailAlreadyExistsException;
import com.example.patient_service.exception.PatientNotFoundException;
import com.example.patient_service.grpc.BillingServiceGrpcClient;
import com.example.patient_service.kafka.KafkaProducer;
import com.example.patient_service.mapper.PatientMapper;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.example.patient_service.model.Patient;
import com.example.patient_service.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final KafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository,  BillingServiceGrpcClient billingServiceGrpcClient, KafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();

        return patients.stream().map(PatientMapper::toDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO){
        if(patientRepository.existsByEmail((patientRequestDTO.getEmail()))) {
            throw new EmailAlreadyExistsException("Patient with Email " + patientRequestDTO.getEmail() + " already exists");
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));
        // Making a grpc call after the patient is saved in DB
        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());

        // Sending the kafka producer event
        log.info("Publishing patient event to Kafka: {}", newPatient);
        kafkaProducer.sendEvent(newPatient);
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

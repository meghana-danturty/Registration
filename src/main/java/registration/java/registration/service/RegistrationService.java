package registration.java.registration.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import registration.java.registration.model.Registration;
import registration.java.registration.repository.RegistrationRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.util.List;

@Service
public class RegistrationService {
    
    private final RegistrationRepository registrationRepository;
    private final Path fileStorageLocation;
    
    public RegistrationService(
            RegistrationRepository registrationRepository,
            @Value("${file.upload-dir:uploads}") String uploadDir) {
        this.registrationRepository = registrationRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    
    public Registration saveRegistration(Registration registration, MultipartFile file) {
        // Validate file
        if (file != null && !file.isEmpty()) {
            validatePdfFile(file);
            String fileName = storeFile(file, registration.getId());
            registration.setFilePath(fileName);
            registration.setOriginalFileName(file.getOriginalFilename());
        }
        
        return registrationRepository.save(registration);
    }
    
    private void validatePdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed. Received: " + contentType);
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("File must have a .pdf extension");
        }
    }
    
    private String storeFile(MultipartFile file, Long userId) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new IllegalArgumentException("File name cannot be null");
            }
            
            // Create filename: originalName_userId.pdf
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String baseName = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            String fileName = baseName + "_" + userId + fileExtension;
            
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }
    
    public Registration createRegistration(Registration registration, MultipartFile file) {
        // Check if email already exists
        if (registrationRepository.existsByEmail(registration.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + registration.getEmail());
        }
        
        // Save registration first to get the ID
        Registration savedRegistration = registrationRepository.save(registration);
        
        // Now save the file with the user ID
        if (file != null && !file.isEmpty()) {
            validatePdfFile(file);
            String fileName = storeFile(file, savedRegistration.getId());
            savedRegistration.setFilePath(fileName);
            savedRegistration.setOriginalFileName(file.getOriginalFilename());
            savedRegistration = registrationRepository.save(savedRegistration);
        }
        
        return savedRegistration;
    }
    
    public Registration getRegistrationById(Long id) {
        return registrationRepository.findById(id).orElse(null);
    }
    
    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }
    
    public Resource loadFileAsResource(Long id) {
        try {
            Registration registration = registrationRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Registration not found with id: " + id));
            
            if (registration.getFilePath() == null || registration.getFilePath().isEmpty()) {
                throw new IllegalArgumentException("No file associated with this registration");
            }
            
            Path filePath = this.fileStorageLocation.resolve(registration.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found: " + registration.getFilePath());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error loading file: " + ex.getMessage(), ex);
        }
    }
    
    public String getOriginalFileName(Long id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found with id: " + id));
        return registration.getOriginalFileName() != null ? registration.getOriginalFileName() : "file.pdf";
    }
}


package registration.java.registration.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import registration.java.registration.model.Registration;
import registration.java.registration.service.RegistrationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registration")
@CrossOrigin(origins = "*")
public class RegistrationController {
    
    private final RegistrationService registrationService;
    
    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }
    
    @GetMapping
    public ResponseEntity<?> getAllRegistrations() {
        try {
            List<Registration> registrations = registrationService.getAllRegistrations();
            return ResponseEntity.ok(registrations);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createRegistration(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phoneNum") String phoneNum,
            @RequestParam("grp") String grp,
            @RequestParam("subGrp") String subGrp,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        try {
            // Create registration object
            Registration registration = new Registration();
            registration.setName(name);
            registration.setEmail(email);
            registration.setPhoneNum(phoneNum);
            registration.setGrp(grp);
            registration.setSubGrp(subGrp);
            
            // Save registration with file
            Registration savedRegistration = registrationService.createRegistration(registration, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration submitted successfully");
            response.put("id", savedRegistration.getId());
            response.put("name", savedRegistration.getName());
            response.put("email", savedRegistration.getEmail());
            response.put("phoneNum", savedRegistration.getPhoneNum());
            response.put("grp", savedRegistration.getGrp());
            response.put("subGrp", savedRegistration.getSubGrp());
            response.put("filePath", savedRegistration.getFilePath());
            response.put("originalFileName", savedRegistration.getOriginalFileName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getRegistration(@PathVariable Long id) {
        try {
            Registration registration = registrationService.getRegistrationById(id);
            if (registration == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Registration not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.ok(registration);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadFile(@PathVariable Long id) {
        try {
            Resource resource = registrationService.loadFileAsResource(id);
            String originalFileName = registrationService.getOriginalFileName(id);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                    .body(resource);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}


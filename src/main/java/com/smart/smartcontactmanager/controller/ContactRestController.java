package com.smart.smartcontactmanager.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.contact;
import com.smart.smartcontactmanager.entities.user;

@RestController
@RequestMapping("/api")
public class ContactRestController {

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private userRepo userRepo;

    // ── Helper: email resolve karo (OAuth2 + normal login dono ke liye) ──
    private String resolveEmail(Principal principal) {
        if (principal instanceof Authentication) {
            Object p = ((Authentication) principal).getPrincipal();
            if (p instanceof DefaultOAuth2User) {
                return ((DefaultOAuth2User) p).getAttribute("email");
            } else if (p instanceof org.springframework.security.core.userdetails.User) {
                return ((org.springframework.security.core.userdetails.User) p).getUsername();
            }
        }
        return principal.getName();
    }

    // ──────────────────────────────────────────────
    // 1. GET ALL CONTACTS — with pagination
    // GET /api/contacts?page=0&size=5
    // ──────────────────────────────────────────────
    @GetMapping("/contacts")
    public ResponseEntity<?> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            Page<contact> contacts = contactRepo.findContactsByUserId(
                    user.getId(), PageRequest.of(page, size));

            // Response map banao — clean JSON
            Map<String, Object> response = new HashMap<>();
            response.put("contacts", contacts.getContent());
            response.put("currentPage", contacts.getNumber());
            response.put("totalPages", contacts.getTotalPages());
            response.put("totalContacts", contacts.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Something went wrong: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 2. GET SINGLE CONTACT by ID
    // GET /api/contacts/{cid}
    // ──────────────────────────────────────────────
    @GetMapping("/contacts/{cid}")
    public ResponseEntity<?> getContact(@PathVariable int cid, Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            contact contact = contactRepo.findById(cid).orElse(null);

            if (contact == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Contact not found"));
            }

            // Check — sirf apna contact dekh sakta hai
            if (contact.getUser().getId() != user.getId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            return ResponseEntity.ok(contact);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 3. CREATE CONTACT
    // POST /api/contacts
    // Body: { "name": "John", "email": "john@gmail.com", "phone": "9876543210" }
    // ──────────────────────────────────────────────
    @PostMapping("/contacts")
    public ResponseEntity<?> createContact(@RequestBody contact contact, Principal principal) {
        try {
            // Validation
            if (contact.getName() == null || contact.getName().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Name is required"));
            }
            if (contact.getEmail() == null || contact.getEmail().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email is required"));
            }
            if (contact.getPhone() == null || contact.getPhone().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Phone is required"));
            }

            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            contact.setUser(user);
            contact savedContact = contactRepo.save(contact);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Contact created successfully",
                            "contact", savedContact
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 4. UPDATE CONTACT
    // PUT /api/contacts/{cid}
    // Body: { "name": "Updated Name", "phone": "1234567890" }
    // ──────────────────────────────────────────────
    @PutMapping("/contacts/{cid}")
    public ResponseEntity<?> updateContact(
            @PathVariable int cid,
            @RequestBody contact updatedContact,
            Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            contact existing = contactRepo.findById(cid).orElse(null);

            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Contact not found"));
            }

            // Sirf apna contact update kar sakta hai
            if (existing.getUser().getId() != user.getId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            // Sirf jo fields bheje hain unhe update karo
            if (updatedContact.getName() != null)        existing.setName(updatedContact.getName());
            if (updatedContact.getSecondName() != null)  existing.setSecondName(updatedContact.getSecondName());
            if (updatedContact.getPhone() != null)       existing.setPhone(updatedContact.getPhone());
            if (updatedContact.getEmail() != null)       existing.setEmail(updatedContact.getEmail());
            if (updatedContact.getWork() != null)        existing.setWork(updatedContact.getWork());
            if (updatedContact.getDescription() != null) existing.setDescription(updatedContact.getDescription());

            contact saved = contactRepo.save(existing);

            return ResponseEntity.ok(Map.of(
                    "message", "Contact updated successfully",
                    "contact", saved
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 5. DELETE CONTACT
    // DELETE /api/contacts/{cid}
    // ──────────────────────────────────────────────
    @DeleteMapping("/contacts/{cid}")
    public ResponseEntity<?> deleteContact(@PathVariable int cid, Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            contact contact = contactRepo.findById(cid).orElse(null);

            if (contact == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Contact not found"));
            }

            // Sirf apna contact delete kar sakta hai
            if (contact.getUser().getId() != user.getId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied"));
            }

            contactRepo.delete(contact);

            return ResponseEntity.ok(Map.of("message", "Contact deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 6. SEARCH CONTACTS by name
    // GET /api/contacts/search?keyword=john
    // ──────────────────────────────────────────────
    @GetMapping("/contacts/search")
    public ResponseEntity<?> searchContacts(
            @RequestParam String keyword,
            Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            List<contact> results = contactRepo
                    .findContactsByNameContainingAndUser(keyword, user);

            return ResponseEntity.ok(Map.of(
                    "keyword", keyword,
                    "results", results,
                    "count", results.size()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 7. DASHBOARD SUMMARY
    // GET /api/dashboard/summary
    // ──────────────────────────────────────────────
    @GetMapping("/dashboard/summary")
    public ResponseEntity<?> dashboardSummary(Principal principal) {
        try {
            String email = resolveEmail(principal);
            user user = userRepo.getUserByEmail(email);

            List<contact> allContacts = contactRepo.findContactsByUserId(user.getId());

            // Category wise count (work field = category)
            Map<String, Long> categoryWise = allContacts.stream()
                    .filter(c -> c.getWork() != null && !c.getWork().isBlank())
                    .collect(Collectors.groupingBy(contact::getWork, Collectors.counting()));

            // Recent 5 contacts
            List<contact> recent = allContacts.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalContacts", allContacts.size());
            summary.put("categoryWise", categoryWise);
            summary.put("recentContacts", recent);
            summary.put("userName", user.getName());
            summary.put("userEmail", user.getEmail());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    // 8. ADMIN ONLY — GET ALL USERS' CONTACTS
    // GET /api/admin/all-contacts
    // Sirf ROLE_ADMIN access kar sakta hai
    // ──────────────────────────────────────────────
    @GetMapping("/admin/all-contacts")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getAllContactsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Admin sabke contacts dekh sakta hai
            Page<contact> contacts = contactRepo.findAll(PageRequest.of(page, size));

            Map<String, Object> response = new HashMap<>();
            response.put("contacts", contacts.getContent());
            response.put("currentPage", contacts.getNumber());
            response.put("totalPages", contacts.getTotalPages());
            response.put("totalContacts", contacts.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

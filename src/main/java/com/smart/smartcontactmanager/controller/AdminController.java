package com.smart.smartcontactmanager.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private userRepo userRepo;

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    @Qualifier("io")
    private ExecutorService ioExecutor;

    // ── Dashboard ────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers    = userRepo.count();
        long totalContacts = contactRepo.count();
        long activeUsers   = userRepo.countByEnabled(true);
        long inactiveUsers = userRepo.countByEnabled(false);

        model.addAttribute("title",        "Admin Dashboard");
        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("totalContacts", totalContacts);
        model.addAttribute("activeUsers",   activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);
        return "admin/dashboard";
    }

    // ── All Users ────────────────────────────────────────────────
    @GetMapping("/users")
    public String allUsers(Model model) {
        List<user> users = userRepo.findAll();
        model.addAttribute("title", "Manage Users");
        model.addAttribute("users", users);
        return "admin/users";
    }

    // ── View Single User ─────────────────────────────────────────
    @GetMapping("/user/{id}")
    public String viewUser(@PathVariable("id") int id, Model model) {
        user u = userRepo.getUserById(id);
        if (u == null) {
            model.addAttribute("message", "User not found");
            return "error";
        }
        int contactCount = contactRepo.findContactsByUserId(u.getId()).size();
        model.addAttribute("title",        "User Detail — " + u.getName());
        model.addAttribute("viewUser",      u);
        model.addAttribute("contactCount", contactCount);
        return "admin/user_detail";
    }

    // ── Enable / Disable User ────────────────────────────────────
    @GetMapping("/user/{id}/toggle-status")
    public String toggleStatus(@PathVariable("id") int id) throws InterruptedException, ExecutionException {
        user u = userRepo.getUserById(id);
        if (u != null) {
            u.setEnabled(!u.isEnabled());
            ioExecutor.submit(() -> userRepo.save(u)).get();
        }
        return "redirect:/admin/users";
    }

    // ── Change Role ──────────────────────────────────────────────
    @PostMapping("/user/{id}/change-role")
    public String changeRole(@PathVariable("id") int id,
                             @RequestParam("role") String role) throws InterruptedException, ExecutionException {
        user u = userRepo.getUserById(id);
        if (u != null && (role.equals("ROLE_USER") || role.equals("ROLE_ADMIN"))) {
            u.setRole(role);
            ioExecutor.submit(() -> userRepo.save(u)).get();
        }
        return "redirect:/admin/user/" + id;
    }

    // ── Delete User ──────────────────────────────────────────────
    @GetMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable("id") int id) throws InterruptedException, ExecutionException {
        user u = userRepo.getUserById(id);
        if (u != null) {
            // Delete all contacts of this user first (cascade should handle it,
            // but explicit clear avoids FK constraint issues if cascade is off)
            u.getContacts().clear();
            ioExecutor.submit(() -> userRepo.delete(u)).get();
        }
        return "redirect:/admin/users";
    }
}

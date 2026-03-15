package com.smart.smartcontactmanager.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="USER")
public class user {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	
	@NotBlank(message = "Email cannot be blank")
	@Size(min = 5, max = 50, message = "Email must be between 5 and 50 characters")
	@Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Invalid email format")
	@Column(unique = true)
	private String email;
	
	@NotBlank(message="Password is required")
	@Size(min=4,max=61,message="min 4 and max 10 letter is required")
	private String password;
	
	private String role;
	private boolean enabled;
	private String imageUrl;
	@Column(length = 500)
	private String about;
	
	@OneToMany(cascade = CascadeType.ALL,fetch = FetchType.LAZY,mappedBy = "user")
	@JsonBackReference
	private List<contact> contacts = new ArrayList<>();
	
	@NotBlank(message="name is required")
	@Size(min=2,max=40,message="min 2 and max 20 letter is required")
	private String name;
	
	//for storing public id of image in cloudinary
	private String publicId;
	
	private String emailToken;
	
	private LocalDateTime emailtokenExpiry;
	
	
	
	public LocalDateTime getEmailtokenExpiry() {
		return emailtokenExpiry;
	}

	public void setEmailtokenExpiry(LocalDateTime emailtokenExpiry) {
		this.emailtokenExpiry = emailtokenExpiry;
	}

	public String getEmailToken() {
		return emailToken;
	}

	public void setEmailToken(String emailToken) {
		this.emailToken = emailToken;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	@Override
	public String toString() {
	    return "User [id=" + id + ", name=" + name + ", email=" + email + "]";
	}
	
	public user(int id, String name, String email, String password, String role, boolean enabled, String imageUrl,
			String about , List<contact> contacts , String publicId , String emailToken , LocalDateTime emailtokenExpiry) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.password = password;
		this.role = role;
		this.enabled = enabled;
		this.imageUrl = imageUrl;
		this.about = about;
		this.contacts = contacts;
		this.publicId = publicId;
		this.emailToken = emailToken;
		this.emailtokenExpiry = emailtokenExpiry;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getAbout() {
		return about;
	}
	public void setAbout(String about) {
		this.about = about;
	}
	
	
	
	public List<contact> getContacts() {
		return contacts;
	}
	public void setContacts(List<contact> contacts) {
		this.contacts = contacts;
	}
	public user() {
		super();
	}

}

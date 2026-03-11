package com.smart.smartcontactmanager.entities;

import javax.persistence.*;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="CONTACT")
public class contact {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int cid;
	private String name;
	private String secondName;
	private String work;
	@Column(unique = true)
	private String email;
	@Column(unique = true)
	private String phone;
	private String image;
	@Column(length = 5000)
	private String description;
	private String publicId;
	
	

	
	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}


	@ManyToOne
	@JsonBackReference

	private user user;

	public int getCid() {
		return cid;
	}

	public void setCid(int cid) {
		this.cid = cid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSecondName() {
		return secondName;
	}

	public void setSecondName(String secondName) {
		this.secondName = secondName;
	}

	public String getWork() {
		return work;
	}

	public void setWork(String work) {
		this.work = work;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public user getUser() {
		return user;
	}

	public void setUser(user user) {
		this.user = user;
	}

	public contact(int cid, String name, String secondName, String work, String email, String phone, String image,
			String description, com.smart.smartcontactmanager.entities.user user , String publicId) {
		super();
		this.cid = cid;
		this.name = name;
		this.secondName = secondName;
		this.work = work;
		this.email = email;
		this.phone = phone;
		this.image = image;
		this.description = description;
		this.user = user;
		this.publicId = publicId;
	}
	
	public contact() {
		super();
		// TODO Auto-generated constructor stub
	}


	@Override
	public String toString() {
	    return "Contact [cid=" + cid + ", name=" + name + ", email=" + email + "]";
	}
	
	
}

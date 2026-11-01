package com.clothingstore.service;

import com.clothingstore.domain.user.Customer;
import com.clothingstore.domain.user.User;
import com.clothingstore.exception.ValidationException;
import com.clothingstore.repository.UserRepository;

/** Đăng ký khách hàng và kiểm tra đăng nhập. Mật khẩu được so sánh dưới dạng hash SHA-256. */
public final class AuthenticationService {
    private final UserRepository users;

    public AuthenticationService(UserRepository users) {
        this.users = users;
    }

    public Customer register(String fullName, String email, String password) {
        if (password == null || password.length() < 6) throw new ValidationException("Mật khẩu cần ít nhất 6 ký tự.", "password");
        Customer customer = Customer.create(fullName, email, Passwords.sha256(password));
        if (users.findByEmail(customer.getEmail()).isPresent()) {
            throw new ValidationException("Email này đã được đăng ký. Vui lòng đăng nhập hoặc dùng email khác.", "email");
        }
        return (Customer) users.insert(customer);
    }

    public User login(String email, String password) {
        User user = users.findByEmail(email == null ? "" : email.trim()).orElse(null);
        if (user == null || password == null || !user.getPasswordHash().equals(Passwords.sha256(password))) {
            throw new ValidationException("Email hoặc mật khẩu không đúng.");
        }
        return user;
    }
}

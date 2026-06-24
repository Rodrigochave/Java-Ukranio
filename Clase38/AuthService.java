package com.escom.app.service;

import com.escom.app.util.JWTUtil;
import com.escom.app.util.PasswordUtil;
import com.escom.app.user.User;
import com.escom.app.user.UserRepository;

public class AuthService {

    private final UserRepository repo = UserRepository.getInstance();

    public boolean register(String curp, String password) {
        if (repo.findByCurp(curp) != null) return false;
        String hash = PasswordUtil.hash(password);
        repo.save(new User(curp, hash));
        return true;
    }

    public String login(String curp, String password) {
        User u = repo.findByCurp(curp);
        if (u == null) return null;

        if (!PasswordUtil.verify(password, u.getPasswordHash())) return null;

        return JWTUtil.generateToken(curp);
    }
}

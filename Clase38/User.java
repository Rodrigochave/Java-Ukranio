package com.escom.app.user;

public class User {
    private final String curp;
    private final String passwordHash;

    public User(String curp, String passwordHash) {
        this.curp = curp;
        this.passwordHash = passwordHash;
    }

    public String getCurp() { return curp; }
    public String getPasswordHash() { return passwordHash; }
}

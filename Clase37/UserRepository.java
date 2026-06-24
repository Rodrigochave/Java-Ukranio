package com.escom.app.user;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private static UserRepository instance = new UserRepository();
    public static UserRepository getInstance() { return instance; }

    private final Map<String, User> db = new HashMap<>();

    public User findByCurp(String curp) {
        return db.get(curp);
    }

    public void save(User user) {
        db.put(user.getCurp(), user);
    }
}

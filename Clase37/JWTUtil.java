package com.escom.app.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;

public class JWTUtil {

    private static final String SECRET = "mi_clave_secreta_super_segura";
    private static final Algorithm ALGO = Algorithm.HMAC256(SECRET);
    private static final String ISSUER = "mi-servidor";

    // Genera un token
    public static String generateToken(String curp) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(curp)
                .sign(ALGO);
    }

    // Verifica un token 
    public static DecodedJWT verify(String token) throws JWTVerificationException {
        return JWT.require(ALGO)
                .withIssuer(ISSUER)
                .build()
                .verify(token);
    }
}

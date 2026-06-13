We will build **JWT Authentication + Role-Based Authorization** in a way that you can:

* explain in interviews
* debug confidently
* extend later (refresh tokens, logout, etc.)

---

# 🧠 BIG PICTURE (Read this first)

## ❓ Why JWT?

Because HTTP is **stateless**.

Instead of:

* server storing session ❌

We do:

* server issues **JWT token**
* client sends token with every request
* server **verifies token**, not session

➡️ Scalable, frontend-friendly, production-ready.

---

# 🧩 WHAT WE ARE ADDING (Overview)

### Authentication (WHO are you?)

* Login → generate JWT
* Token contains user email + role

### Authorization (WHAT can you do?)

* ADMIN → everything
* INSTRUCTOR → course management
* STUDENT → enroll, view

---

# 📁 UPDATED / NEW FOLDER STRUCTURE

```
com.lms.backend
 ├── security
 │    ├── JwtUtil.java
 │    ├── JwtAuthenticationFilter.java
 │    ├── SecurityConfig.java
 │    └── CustomUserDetailsService.java
 ├── dto
 │    └── LoginResponse.java
```

We’ll update **existing files**, not rewrite everything.

---

# 1️⃣ `JwtUtil.java`

📁 `security/JwtUtil.java`

### 👉 WHY this file exists

* Central place for JWT logic
* Creates & validates tokens
* Avoids duplication

```java
package com.lms.backend.security;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

/*
 * JWT UTILITY CLASS
 * -----------------
 * Responsible for generating and validating JWT tokens.
 */

@Component
public class JwtUtil {

    // Secret key (keep long & secure)
    private final String SECRET_KEY =
            "my_super_secret_key_for_lms_project_which_should_be_long";

    // Token validity (10 hours)
    private final long EXPIRATION_TIME = 10 * 60 * 60 * 1000;

    /*
     * Generate JWT token using email & role
     */
    public String generateToken(String email, String role) {

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .compact();
    }

    /*
     * Extract email (subject) from token
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /*
     * Extract role from token
     */
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /*
     * Validate token expiration
     */
    public boolean isTokenValid(String token) {
        return extractClaims(token)
                .getExpiration()
                .after(new Date());
    }

    /*
     * Internal helper method
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

---

# 2️⃣ `LoginResponse.java`

📁 `dto/LoginResponse.java`

### 👉 WHY this file exists

* Never return token as plain string
* Standard API response
* Easy for frontend

```java
package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
 * LOGIN RESPONSE DTO
 * ------------------
 * Sent to client after successful login.
 */

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String role;
}
```

---

# 3️⃣ Update `UserServiceImpl.java`

### 👉 WHY this file is updated

* Login now issues JWT
* Central auth logic lives here

```java
import com.lms.backend.dto.LoginResponse;
import com.lms.backend.security.JwtUtil;
```

```java
private final JwtUtil jwtUtil;
```

### Replace `login()` method with this:

```java
@Override
public LoginResponse login(LoginRequest request) {

    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->
                    new InvalidCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new InvalidCredentialsException("Invalid email or password");
    }

    // Generate JWT token
    String token = jwtUtil.generateToken(
            user.getEmail(),
            user.getRole().name()
    );

    return new LoginResponse(token, user.getRole().name());
}
```

---

# 4️⃣ Update `UserService.java`

```java
import com.lms.backend.dto.LoginResponse;

LoginResponse login(LoginRequest request);
```

---

# 5️⃣ Update `UserController.java`

### 👉 WHY this file is updated

* Login now returns JWT, not User

```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request) {

    return ResponseEntity.ok(userService.login(request));
}
```

---

# 6️⃣ `JwtAuthenticationFilter.java`

📁 `security/JwtAuthenticationFilter.java`

### 👉 WHY this file exists

* Runs **before controller**
* Extracts JWT from header
* Sets authenticated user in context

```java
package com.lms.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/*
 * JWT FILTER
 * ----------
 * Intercepts every request to validate JWT token.
 */

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Token must start with "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {

                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                java.util.List.of(
                                        new org.springframework.security.core.authority
                                                .SimpleGrantedAuthority("ROLE_" + role)
                                )
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

# 7️⃣ Update `SecurityConfig.java`

### 👉 WHY this file is updated

* Adds JWT filter
* Enables role-based access

```java
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
```

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .csrf(csrf -> csrf.disable())
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())

        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                HttpMethod.POST,
                "/api/users/register",
                "/api/users/login"
            ).permitAll()

            // Role-based rules
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
            .requestMatchers("/api/student/**").hasRole("STUDENT")

            .anyRequest().authenticated()
        )

        // Add JWT filter
        .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

Add field:

```java
private final JwtAuthenticationFilter jwtAuthenticationFilter;
```

---

# 🧪 HOW TO TEST (IMPORTANT)

### 1️⃣ Login

```
POST /api/users/login
```

➡️ Copy `token`

---

### 2️⃣ Call protected API

```
GET /api/student/test
```

Headers:

```
Authorization: Bearer <PASTE_TOKEN>
```

✔ Works if role matches
❌ 403 if role mismatch

---

# 🧠 ROLE-BASED AUTH SUMMARY

| Role       | Access      |
| ---------- | ----------- |
| ADMIN      | Full        |
| INSTRUCTOR | Course APIs |
| STUDENT    | Enrollment  |

Spring automatically checks:

```
ROLE_<ROLE_NAME>
```

---

## ✅ WHAT YOU NOW UNDERSTAND

✔ Stateless authentication
✔ JWT internals
✔ Security filter chain
✔ Role-based authorization
✔ How real APIs are secured

This is **mid-senior backend level knowledge**.

---

## 🚀 NEXT (Recommended Path)

1️⃣ Course Module (Entity → CRUD → Role-based access)
2️⃣ Refresh tokens
3️⃣ Logout strategy
4️⃣ React authentication integration

Reply with:

> **NEXT: Course Module**

You are building a **real production backend** now 💪

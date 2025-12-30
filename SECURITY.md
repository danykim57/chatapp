# Security Documentation

## Overview

This document outlines the security measures implemented in the gRPC-Web Chat Application.

## Encryption Methods

### 1. Transport Layer Security (TLS)

**Implementation:**
- TLS 1.3 configured in Envoy proxy
- All client-server communication encrypted
- Prevents man-in-the-middle attacks

**Location:** `envoy/envoy.yaml`

### 2. Password Storage

**Algorithm:** bcrypt with cost factor 12

**Implementation:**
```java
// Password hashing
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hashedPassword = encoder.encode(plainPassword);

// Password verification
boolean matches = encoder.matches(plainPassword, hashedPassword);
```

**Security Features:**
- Salted hashes
- Adaptive hashing (cost factor adjustable)
- No plain text passwords stored
- One-way hashing (irreversible)

**Location:** `backend/src/main/java/com/chatapp/config/SecurityConfig.java`

### 3. JWT Token Management

**Algorithm:** HS256 (HMAC with SHA-256)

**Token Structure:**
```json
{
  "sub": "userId",
  "userType": "REGISTERED|GUEST|ADMIN",
  "username": "username",
  "iat": 1234567890,
  "exp": 1234567890
}
```

**Expiration:**
- Registered users: 24 hours (86400000 ms)
- Guest users: 1 hour (3600000 ms)

**Location:** `backend/src/main/java/com/chatapp/security/JwtUtil.java`

## Security Vulnerabilities Addressed

### 1. Authentication & Authorization

**Measures:**
- ✅ JWT token validation on every gRPC request
- ✅ User type-based permissions
- ✅ Token expiration enforcement
- ✅ Secure session management
- ✅ Guest user limitations (shorter session, restricted permissions)

**Code Example:**
```java
if (!jwtUtil.validateToken(token)) {
    throw new SecurityException("Invalid token");
}
```

### 2. Input Validation

**Measures:**
- ✅ Username: 3-30 alphanumeric characters (regex validation)
- ✅ Password: Minimum 8 characters
- ✅ Email: RFC-compliant format
- ✅ Message: Maximum 2000 characters
- ✅ Room name: 3-100 characters

**Validation Rules:**
```java
private static final Pattern USERNAME_PATTERN =
    Pattern.compile("^[a-zA-Z0-9_-]{3,30}$");
```

**Location:** `backend/src/main/java/com/chatapp/service/InputValidator.java`

### 3. XSS (Cross-Site Scripting) Prevention

**Measures:**
- ✅ HTML tag stripping from messages
- ✅ Script tag removal
- ✅ Content sanitization before storage
- ✅ React's built-in XSS protection (auto-escaping)

**Sanitization:**
```java
public String sanitizeMessage(String message) {
    return message
        .replaceAll("<script[^>]*>.*?</script>", "")
        .replaceAll("<[^>]+>", "")
        .trim();
}
```

### 4. SQL Injection Prevention

**Measures:**
- ✅ JPA with parameterized queries (no raw SQL)
- ✅ Spring Data JPA repositories
- ✅ Type-safe query methods
- ✅ No dynamic SQL construction

**Example:**
```java
@Query("SELECT m FROM ChatMessageEntity m WHERE m.roomId = :roomId")
List<ChatMessageEntity> findByRoom(@Param("roomId") String roomId);
```

### 5. CSRF (Cross-Site Request Forgery)

**Measures:**
- ✅ Stateless JWT authentication (no cookies)
- ✅ CORS configuration in Envoy
- ✅ Origin validation
- ✅ Custom headers required

**CORS Configuration:**
```yaml
cors:
  allow_origin_string_match:
    - prefix: "*"  # Configure specific origins in production
  allow_methods: GET, PUT, DELETE, POST, OPTIONS
  allow_headers: authorization, content-type, x-grpc-web
```

### 6. Rate Limiting

**Configuration:**
```yaml
chat:
  rate-limit:
    login-attempts: 5
    login-window-minutes: 15
    messages-per-minute: 30
```

**Prevents:**
- Brute force login attacks
- Message spam
- DoS attacks

**Location:** `backend/src/main/resources/application.yml`

### 7. Username Enumeration Prevention

**Measures:**
- ✅ Generic error messages for login failures
- ✅ Same response time for valid/invalid usernames
- ✅ No distinction between "user not found" and "wrong password"

**Example:**
```java
if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
    return "Invalid username or password"; // Generic message
}
```

### 8. Session Security

**Measures:**
- ✅ Secure token storage (localStorage)
- ✅ Token expiration
- ✅ Logout invalidation (client-side)
- ✅ Guest users have shorter sessions

**Future Enhancement:**
- Token blacklisting with Redis
- Refresh token mechanism

### 9. Data Exposure

**Measures:**
- ✅ No password fields in API responses
- ✅ Sensitive data excluded from logs
- ✅ User-specific data filtering
- ✅ Room membership validation

### 10. Error Handling

**Measures:**
- ✅ Generic error messages to clients
- ✅ Detailed errors logged server-side only
- ✅ No stack traces exposed
- ✅ Graceful degradation

## Security Best Practices

### For Development

1. **Never commit secrets:**
   - JWT secret keys
   - Database credentials
   - API keys

2. **Use environment variables:**
   ```bash
   export JWT_SECRET=your-secret-key
   export DB_PASSWORD=your-db-password
   ```

3. **Keep dependencies updated:**
   ```bash
   mvn versions:display-dependency-updates
   npm audit
   ```

### For Production

1. **Change default secrets:**
   - Update `jwt.secret` in application.yml
   - Use strong, randomly generated keys (minimum 256 bits)

2. **Enable HTTPS:**
   - Configure SSL certificates in Envoy
   - Force HTTPS redirects
   - Use HSTS headers

3. **Database security:**
   - Use production database (PostgreSQL/MySQL)
   - Enable SSL for database connections
   - Use strong database passwords
   - Restrict database access by IP

4. **CORS configuration:**
   - Whitelist specific origins only
   - Remove wildcard (`*`) origins

5. **Monitoring:**
   - Enable security event logging
   - Set up intrusion detection
   - Monitor failed login attempts
   - Track suspicious activity

6. **Backup and disaster recovery:**
   - Regular database backups
   - Encrypted backup storage
   - Test recovery procedures

## Security Audit Checklist

- [ ] JWT secret changed from default
- [ ] HTTPS enabled with valid certificates
- [ ] CORS configured for production domains only
- [ ] Rate limiting enabled and tested
- [ ] Input validation on all endpoints
- [ ] Error messages don't leak sensitive info
- [ ] Database credentials secured
- [ ] Dependency vulnerabilities checked (`mvn dependency:check`, `npm audit`)
- [ ] Logging configured (no sensitive data in logs)
- [ ] Security headers configured (HSTS, CSP, etc.)
- [ ] Password policy enforced (length, complexity)
- [ ] Token expiration tested
- [ ] XSS prevention tested
- [ ] SQL injection prevention tested
- [ ] CSRF protection verified

## Reporting Security Issues

If you discover a security vulnerability, please:

1. **Do NOT** open a public issue
2. Email security concerns to: security@yourcompany.com
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Security Updates

This application should be regularly updated for security patches:

- Spring Boot: Check https://spring.io/security
- gRPC: Check https://github.com/grpc/grpc-java/security
- React: Check https://github.com/facebook/react/security
- Dependencies: Run `mvn dependency:check` and `npm audit`

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [gRPC Security Guide](https://grpc.io/docs/guides/auth/)

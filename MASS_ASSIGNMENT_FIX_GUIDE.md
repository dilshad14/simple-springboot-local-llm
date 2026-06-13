# Checkmarx Mass Assignment Vulnerability - Fix Guide

## Vulnerability Description

**Finding**: The DTOVariable at @File at @line may unintentionally allow setting the value of "save" in DTOVariable, in the object SvcImpl at @Line.

**Type**: Mass Assignment / Insecure Direct Object Reference (IDOR)

**Severity**: Medium to High

## What is Mass Assignment?

Mass Assignment occurs when an application automatically binds HTTP request parameters to object properties without proper validation or whitelisting. This allows attackers to set fields they shouldn't have access to.

## Vulnerable Pattern Example

```java
// ❌ VULNERABLE DTO
public class UserDTO {
    private String username;
    private String email;
    private boolean save;  // ⚠️ Sensitive field that shouldn't be settable
    private String role;   // ⚠️ Another sensitive field
    
    // Getters and setters
    public void setSave(boolean save) {
        this.save = save;
    }
    
    public boolean getSave() {
        return save;
    }
}

// ❌ VULNERABLE CONTROLLER
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        // ⚠️ All fields from request are bound, including "save"
        return userService.save(userDTO);
    }
}

// ❌ VULNERABLE SERVICE
@Service
public class UserServiceImpl implements UserService {
    
    public User save(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        
        // ⚠️ Uses the "save" field from DTO - attacker can manipulate this
        if (dto.getSave()) {
            // Perform save operation
        }
        
        return userRepository.save(user);
    }
}
```

## Attack Scenario

An attacker could send a request like:
```json
POST /api/users/create
{
  "username": "attacker",
  "email": "attacker@evil.com",
  "save": true  // ⚠️ Attacker sets this to manipulate behavior
}
```

## Fix Strategies

### Fix 1: Use @JsonIgnore (Recommended for Jackson)

Prevent the field from being deserialized from JSON:

```java
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDTO {
    private String username;
    private String email;
    
    @JsonIgnore  // ✅ Field cannot be set from JSON request
    private boolean save;
    
    // Only allow setting via constructor or internal method
    public UserDTO() {
        this.save = false; // Default value
    }
    
    // Internal method (not exposed via JSON)
    void setSaveInternal(boolean save) {
        this.save = save;
    }
    
    public boolean getSave() {
        return save;
    }
}
```

### Fix 2: Use @JsonProperty with Access Control

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

public class UserDTO {
    private String username;
    private String email;
    
    @JsonProperty(access = Access.READ_ONLY)  // ✅ Can only be read, not written
    private boolean save = false;
    
    public boolean getSave() {
        return save;
    }
}
```

### Fix 3: Use Separate DTOs (Best Practice)

Create separate DTOs for input and output:

```java
// ✅ Input DTO - Only contains fields user can set
public class UserCreateDTO {
    private String username;
    private String email;
    
    // No "save" field - user cannot set it
    
    // Getters and setters only for allowed fields
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// ✅ Internal DTO or Entity - Contains all fields
public class User {
    private String username;
    private String email;
    private boolean save;  // Only set internally
    
    // Internal setter
    void setSave(boolean save) {
        this.save = save;
    }
}

// ✅ Controller uses input DTO
@PostMapping("/create")
public ResponseEntity<User> createUser(@RequestBody UserCreateDTO userDTO) {
    return userService.save(userDTO);
}

// ✅ Service converts input DTO to entity
@Service
public class UserServiceImpl implements UserService {
    
    public User save(UserCreateDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setSave(false); // ✅ Set internally, not from user input
        
        return userRepository.save(user);
    }
}
```

### Fix 4: Use @InitBinder for Whitelisting (Spring MVC)

```java
@RestController
public class UserController {
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields("username", "email");  // ✅ Whitelist only allowed fields
        // "save" is not in the whitelist, so it will be ignored
    }
    
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody UserDTO userDTO) {
        return userService.save(userDTO);
    }
}
```

### Fix 5: Use Validation Groups

```java
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

public class UserDTO {
    
    @NotNull(groups = {UserInput.class})
    private String username;
    
    @NotNull(groups = {UserInput.class})
    private String email;
    
    // No validation group = cannot be set from input
    private boolean save;
    
    // Getters and setters
}

// In controller
@PostMapping("/create")
public ResponseEntity<User> createUser(
    @Validated(UserInput.class) @RequestBody UserDTO userDTO) {
    return userService.save(userDTO);
}
```

## Recommended Solution for Your Codebase

Based on Spring Boot best practices, I recommend **Fix 3 (Separate DTOs)** combined with **Fix 1 (@JsonIgnore)**:

1. **Create separate input DTOs** that only contain fields users should be able to set
2. **Use @JsonIgnore** on any sensitive fields in DTOs that must remain
3. **Validate and sanitize** all input in the service layer
4. **Set sensitive fields internally** in the service, never from user input

## Example Implementation

```java
// ✅ Secure Input DTO
public class DocumentMetadataDTO {
    private String chapterName;
    private String abstractText;
    private String searchTags;
    
    // No "save" or other sensitive fields
    
    // Constructors, getters, setters
}

// ✅ Controller
@PostMapping("/metadata")
public ResponseEntity<?> createMetadata(@RequestBody DocumentMetadataDTO dto) {
    return metadataService.process(dto);
}

// ✅ Service - Sets sensitive fields internally
@Service
public class MetadataServiceImpl {
    
    public Metadata process(DocumentMetadataDTO dto) {
        Metadata metadata = new Metadata();
        metadata.setChapterName(dto.getChapterName());
        metadata.setAbstractText(dto.getAbstractText());
        metadata.setSearchTags(dto.getSearchTags());
        
        // ✅ Set sensitive fields internally
        metadata.setSave(false);  // Set by business logic, not user input
        metadata.setCreatedDate(LocalDateTime.now());
        
        return metadataRepository.save(metadata);
    }
}
```

## Additional Security Measures

1. **Input Validation**: Use Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.)
2. **Sanitization**: Sanitize all string inputs to prevent injection attacks
3. **Authorization**: Verify user has permission to perform the operation
4. **Audit Logging**: Log all sensitive operations
5. **Rate Limiting**: Prevent abuse of endpoints

## Checklist for Fixing

- [ ] Identify the DTO class with the vulnerable field
- [ ] Identify the service implementation using the field
- [ ] Determine if the field should be settable by users
- [ ] Apply appropriate fix (JsonIgnore, separate DTOs, or whitelisting)
- [ ] Set sensitive fields internally in service layer
- [ ] Add input validation
- [ ] Test that the field cannot be set via HTTP request
- [ ] Update documentation

## References

- OWASP Mass Assignment: https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html
- Spring Security Best Practices
- Jackson Annotations Documentation



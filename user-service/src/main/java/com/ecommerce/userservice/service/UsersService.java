package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.exception.EmailAlreadyExistsException;
import com.ecommerce.userservice.exception.RoleNotFoundException;
import com.ecommerce.userservice.exception.UsersNotFoundException;
import com.ecommerce.userservice.exception.PasswordException;
import com.ecommerce.userservice.model.Role;
import com.ecommerce.userservice.model.Users;
import com.ecommerce.userservice.repository.UsersRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsersService implements UserDetailsService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    // =========================================================
    // Metodo para Spring Security
    // =========================================================

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usersRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email Not Found"));
    }

    // =========================================================
    // Operaciones CRUD
    // =========================================================

    //Crear Usuario
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequest) {
        if (usersRepository.existsByEmail(userRequest.email())){
            throw new EmailAlreadyExistsException(
                    "El correo ya se encuentra registrado"
            );
        }

        Users newUser = new Users();

        newUser.setName(userRequest.name());
        newUser.setPaternalName(userRequest.paternalName());
        newUser.setMaternalName(userRequest.maternalName());
        newUser.setEmail(userRequest.email());
        newUser.setPassword(passwordEncoder.encode(userRequest.password()));
        newUser.setRole(Role.USUARIO);

        Users savedUser = usersRepository.save(newUser);

        //despues de guardar el usuario se obtine su id para genera el id compuesto y guardarlo
        savedUser.setUserIdentifier(
                generateUserIdentifier(savedUser.getId(), savedUser.getRole())
        );

        savedUser = usersRepository.save(savedUser);


        UserResponseDTO response = new UserResponseDTO(
                savedUser.getId(),
                savedUser.getUserIdentifier(),
                savedUser.getName() + " " + savedUser.getPaternalName() + " " + savedUser.getMaternalName(),
                savedUser.getEmail(),
                savedUser.getRole().toString(),
                savedUser.getIsActive(),
                savedUser.getCreatedAt()
        );

        return response;
    }

    //Obtención de usuario por ID
    public UserResponseDTO findById(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UsersNotFoundException(
                        "Usuario no encontrado"
                        ));

        UserResponseDTO response = new UserResponseDTO(
                user.getId(),
                user.getUserIdentifier(),
                user.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                user.getEmail(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt()
        );

        return response;
    }

    //Obtener todos los usuarios
    public List<UserResponseDTO> findAll() {
        return usersRepository.findAll()
                .stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getUserIdentifier(),
                        user.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getIsActive(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    //Actualización de datos de usuario
    @Transactional
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO userRequest){
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UsersNotFoundException(
                        "Usuario no encontrado"
                ));

        if(userRequest.name() != null && !userRequest.name().isEmpty()) {
            user.setName(userRequest.name());
        }
        if(userRequest.paternalName() != null && !userRequest.paternalName().isEmpty()) {
            user.setPaternalName(userRequest.paternalName());
        }
        if(userRequest.maternalName() != null && !userRequest.maternalName().isEmpty()) {
            user.setMaternalName(userRequest.maternalName());
        }
        if(userRequest.email() != null && !userRequest.email().isEmpty()) {
            if(usersRepository.existsByEmailAndIdNot(userRequest.email(), id)) {
                throw new EmailAlreadyExistsException("El correo ya se encuentra registrado");
            }
            if (!userRequest.email().equals(user.getEmail())) {
                user.setEmail(userRequest.email());
                invalidateTokens(user);
            }
        }

        Users updatedUser = usersRepository.save(user);
        UserResponseDTO response = new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getUserIdentifier(),
                updatedUser.getName() + " " + updatedUser.getPaternalName() + " " + updatedUser.getMaternalName(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                updatedUser.getIsActive(),
                updatedUser.getCreatedAt()
        );

        return response;
    }

    // =========================================================
    // Operaciones con Email
    // =========================================================

    public UserResponseDTO findByEmail(String email) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsersNotFoundException(
                        "Usuario no encontrado"
                ));

        UserResponseDTO response = new UserResponseDTO(
                user.getId(),
                user.getUserIdentifier(),
                user.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                user.getEmail(),
                user.getRole().name(),
                user.getIsActive(),
                user.getCreatedAt()
        );

        return response;
    }

    // =========================================================
    // Operaciones con isActive
    // =========================================================

    //Activar o Desactivar usuario por ID
    public UserStatusDTO activateAndDeactivateUser(Long id, Boolean isActive){
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UsersNotFoundException("Usuario no encontrado"));

        user.setIsActive(isActive);
        if (Boolean.FALSE.equals(isActive)) invalidateTokens(user);
        Users updatedUser = usersRepository.save(user);

        UserStatusDTO response = new UserStatusDTO(
                updatedUser.getId(),
                updatedUser.getIsActive()
        );

        return response;
    }

    //Obtener isActive del usuario
    public UserStatusDTO findUserStatusById(Long id){
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new UsersNotFoundException("Usuario no encontrado"));

        UserStatusDTO response = new UserStatusDTO(
                user.getId(),
                user.getIsActive()
        );

        return response;
    }

    // Obtener TODOS los usuarios por isActive
    public List<UserResponseDTO> findAllUsersByIsActive(Boolean isActive) {

        List<Users> users = usersRepository.findAllByIsActive(isActive);

        return users.stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getUserIdentifier(),
                        user.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getIsActive(),
                        user.getCreatedAt()
                ))
                .toList();
    }



    // =========================================================
    // Operaciones por Rol
    // =========================================================

    //Obtener a todos los usuarios por rol
    public List<UserResponseDTO> findAllUsersByRole(String role){

        Role roleToEnum;

        try {
            roleToEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RoleNotFoundException("El rol no es válido: " + role);
        }

        List<Users> users = usersRepository.findAllByRole(roleToEnum);

        //Convertimos cada elemento Users de la lista en un UserResponseDTO
        return users.stream()
                .map(user -> new UserResponseDTO(
                        user.getId(),
                        user.getUserIdentifier(),
                        user.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getIsActive(),
                        user.getCreatedAt()
                ))
                .toList();
    }

    //Actualizar el rol de un usuario
    @Transactional
    public UserResponseDTO updateUserRole(Long id, Role newRole){
        Users user = usersRepository.findById(id)
                .orElseThrow(()-> new UsersNotFoundException(
                        "Usuario no encontrado"
                ));
        user.setRole(newRole);
        user.setUserIdentifier(generateUserIdentifier(user.getId(), user.getRole()));
        Users updatedUser = usersRepository.save(user);

        return new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getUserIdentifier(),
                updatedUser.getName() + " " + user.getPaternalName() + " " + user.getMaternalName(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                updatedUser.getIsActive(),
                updatedUser.getCreatedAt()
        );
    }

    // =========================================================
    // Generar id compuesto usando el ID
    // =========================================================

    private String generateUserIdentifier(Long id, Role role){
        String prefix;
        String suffix;

        switch (role){
            case USUARIO -> {
                prefix = "USR-";
                suffix = "-I";
            }
            case CLIENTE -> {
                prefix = "CLI-";
                suffix = "-C";
            }

            case VENDEDOR -> {
                prefix = "VEN-";
                suffix = "-V";
            }

            case REPARTIDOR -> {
                prefix = "REP-";
                suffix = "-R";
            }

            case ADMINISTRADOR -> {
                prefix = "ADM-";
                suffix = "-A";
            }
            default -> throw new IllegalArgumentException(
                    "No existe un identificador definido para el rol:" + role
            );
        }
        return prefix + String.format("%03d", id) + suffix;
    }

    // =========================================================
    // Cambiar Contraseña
    // =========================================================

    public void updatePassword(PasswordRequestDTO passwordRequestDTO,
                               String email){

        Users users = usersRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        //Valida que la contraseña coincida
        if(!passwordEncoder.matches(
                passwordRequestDTO.currentPassword(),
                users.getPassword())) {

            throw new PasswordException("currentPassword es Incorrecta");
        }

        //Valida que la nueva contraseña y la contraseña de confirmación coincidan
        if(!passwordRequestDTO.newPassword().equals(
                passwordRequestDTO.confirmPassword())) {

            throw new PasswordException("newPassword y confirmPassword no coinciden");
        }

        //Valida que la contraseña nueva sea diferente a la actual
        if(passwordEncoder.matches(
                passwordRequestDTO.newPassword(),
                users.getPassword())) {

            throw new PasswordException("La nueva contraseña debe ser diferente");
        }

        users.setPassword(passwordEncoder.encode(
                passwordRequestDTO.newPassword()));

        invalidateTokens(users);
        usersRepository.save(users);
    }

    // =========================================================
    // Invalidar el JWT y refreshToken
    // =========================================================

    public void invalidateTokens(Users user){
        user.setJwtId(null);
        user.setJwtExpiresAt(null);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
    }

    @Transactional
    public UserResponseDTO updateCurrentUser(String username,UpdateUserRequestDTO userRequest) {
        Users user = usersRepository.findByEmail(username)
                .orElseThrow(() -> new UsersNotFoundException(
                        "Usuario no encontrado"
                ));

        if(userRequest.name() != null && !userRequest.name().isEmpty()) {
            user.setName(userRequest.name());
        }
        if(userRequest.paternalName() != null && !userRequest.paternalName().isEmpty()) {
            user.setPaternalName(userRequest.paternalName());
        }
        if(userRequest.maternalName() != null && !userRequest.maternalName().isEmpty()) {
            user.setMaternalName(userRequest.maternalName());
        }
        if(userRequest.email() != null && !userRequest.email().isEmpty()) {
            if(usersRepository.existsByEmailAndIdNot(userRequest.email(), user.getId())) {
                throw new EmailAlreadyExistsException("El correo ya se encuentra registrado");
            }
            if (!userRequest.email().equals(user.getEmail())) {
                user.setEmail(userRequest.email());
                invalidateTokens(user);
            }
        }

        Users updatedUser = usersRepository.save(user);
        UserResponseDTO response = new UserResponseDTO(
                updatedUser.getId(),
                updatedUser.getUserIdentifier(),
                updatedUser.getName() + " " + updatedUser.getPaternalName() + " " + updatedUser.getMaternalName(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                updatedUser.getIsActive(),
                updatedUser.getCreatedAt()
        );

        return response;
    }
}

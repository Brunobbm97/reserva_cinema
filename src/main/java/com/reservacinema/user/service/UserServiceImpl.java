package com.reservacinema.user.service;

import com.reservacinema.common.exception.BusinessException;
import com.reservacinema.common.exception.NotFoundException;
import com.reservacinema.user.dto.UserResponse;
import com.reservacinema.user.entity.Role;
import com.reservacinema.user.entity.User;
import com.reservacinema.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse changeRole(Long userId, Role newRole, Long requesterId) {
        // Evita que o admin tire o próprio acesso de admin por engano e fique trancado do lado de fora.
        if (userId.equals(requesterId) && newRole != Role.ADMIN) {
            throw new BusinessException("Você não pode remover o seu próprio papel de ADMIN");
        }
        User user = findUser(userId);
        user.setRole(newRole);
        // Não precisamos chamar save(): dentro de uma transação, o JPA detecta a mudança
        // no objeto e faz o UPDATE sozinho ao final (isso se chama "dirty checking").
        return UserResponse.from(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário " + id + " não encontrado"));
    }
}

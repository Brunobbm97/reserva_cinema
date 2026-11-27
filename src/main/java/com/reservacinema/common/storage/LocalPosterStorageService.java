package com.reservacinema.common.storage;

import com.reservacinema.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalPosterStorageService implements PosterStorageService {

    // Só aceitamos imagens. Checar o content-type evita que alguém suba, por exemplo, um .html ou .jsp.
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path root;

    public LocalPosterStorageService(StorageProperties properties) {
        this.root = Path.of(properties.posterDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de pôsteres", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo de pôster vazio");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType)) {
            throw new BusinessException("Formato inválido. Envie uma imagem JPEG, PNG ou WebP");
        }

        // Geramos um nome novo (UUID). Nunca usamos o nome enviado pelo usuário direto no disco:
        // ele poderia conter algo como "../../config" e escrever fora da pasta (path traversal).
        String extension = extensionFor(contentType);
        String storedName = UUID.randomUUID() + extension;
        Path target = root.resolve(storedName).normalize();

        // Trava de segurança extra: o alvo tem que continuar dentro da pasta raiz.
        if (!target.startsWith(root)) {
            throw new BusinessException("Caminho de arquivo inválido");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o pôster", e);
        }
        return "posters/" + storedName;
    }

    @Override
    public void delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        String fileName = Path.of(relativePath).getFileName().toString();
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Não impedimos a operação principal por causa de um arquivo órfão; apenas ignoramos.
        }
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}

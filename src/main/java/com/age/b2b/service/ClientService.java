package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.dto.ClientSignupDto;
import com.age.b2b.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder; // ★ 수정됨
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder; // ★ BCryptPasswordEncoder -> PasswordEncoder로 변경

    // 파일 저장 경로 (C드라이브 b2b_uploads 폴더)
    private final String UPLOAD_DIR = "C:\\b2b_uploads\\";

    // 중복 체크
    public boolean checkDuplicate(String type, String value) {
        return switch (type) {
            case "id" -> clientRepository.existsByUsername(value);
            case "number" -> clientRepository.existsByBusinessNumber(value);
            case "email" -> clientRepository.existsByEmail(value);
            case "phone" -> clientRepository.existsByPhone(value);
            default -> false;
        };
    }

    // 회원가입 (파일 처리 포함)
    @Transactional
    public void signup(ClientSignupDto dto) throws IOException {
        Client client = new Client();

        client.setUsername(dto.getUsername());
        client.setPassword(passwordEncoder.encode(dto.getPassword()));
        client.setBusinessName(dto.getBusinessName());
        client.setBusinessNumber(dto.getBusinessNumber());
        client.setOwnerName(dto.getOwnerName());
        client.setPhone(dto.getPhone());
        client.setEmail(dto.getEmail());

        client.setZipCode(dto.getZipCode());
        client.setAddress(dto.getAddress());
        client.setDetailAddress(dto.getDetailAddress());

        client.setClientCategory(dto.getClientCategory());
        client.setApprovalStatus(ClientStatus.WAITING); // 대기 상태

        // 파일 저장 로직
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            String savedPath = saveFile(dto.getFile());
            client.setBusinessLicensePath(savedPath);
        }

        clientRepository.save(client);
    }

    private String saveFile(MultipartFile file) throws IOException {
        File folder = new File(UPLOAD_DIR);
        if (!folder.exists()) {
            folder.mkdirs(); // 폴더 생성
        }

        String originalName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String savedName = uuid + "_" + originalName;
        String fullPath = UPLOAD_DIR + savedName;

        file.transferTo(new File(fullPath));
        return fullPath;
    }

    // 1. 가입 대기 목록 조회
    public Page<Client> getWaitingList(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return clientRepository.findAllByApprovalStatusAndBusinessNameContaining(
                    ClientStatus.WAITING, keyword, pageable);
        }
        return clientRepository.findAllByApprovalStatus(ClientStatus.WAITING, pageable);
    }

    // 2. 가입 승인/거절 처리
    @Transactional
    public void updateApprovalStatus(Long clientId, String status) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if ("APPROVE".equals(status)) {
            client.setApprovalStatus(ClientStatus.APPROVED);
        } else if ("REJECT".equals(status)) {
            client.setApprovalStatus(ClientStatus.REJECTED);
        }
    }

    // 3. 이미지 파일 불러오기 (Resource 반환)
    public Resource getBusinessLicenseImage(String filename) throws MalformedURLException {
        String fullPath = UPLOAD_DIR + filename;
        return new UrlResource("file:" + fullPath);
    }
}
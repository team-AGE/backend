package com.age.b2b.service;

import com.age.b2b.domain.Client;
import com.age.b2b.domain.common.ClientStatus;
import com.age.b2b.dto.ClientMyPageDto;
import com.age.b2b.dto.ClientMyPageUpdateDto;
import com.age.b2b.dto.ClientSignupDto;
import com.age.b2b.dto.PasswordDto;
import com.age.b2b.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

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
    public Page<Client> getRequestList(String keyword, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "clientId")
        );

        if (keyword != null && !keyword.isEmpty()) {
            // 이름 검색 (상태 필터링 X)
            return clientRepository.findByBusinessNameContaining(keyword, sortedPageable);
        }
        // 전체 조회 (상태 필터링 X)
        return clientRepository.findAll(sortedPageable);
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

    // 1. 아이디 찾기
    public void findId(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));

        // 메일 발송
        mailService.sendIdMail(client.getEmail(), client.getUsername());
    }

    // 2. 비밀번호 찾기 (임시 비밀번호 발급)
    @Transactional
    public void findPassword(String username, String email) {
        Client client = clientRepository.findByUsernameAndEmail(username, email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));

        // 임시 비밀번호 생성 (8자리)
        String tempPw = UUID.randomUUID().toString().substring(0, 8);

        // DB 업데이트
        client.setPassword(passwordEncoder.encode(tempPw));

        // 메일 발송 (평문 전송)
        mailService.sendTempPwMail(client.getEmail(), tempPw);
    }

    // 마이페이지 조회
    public ClientMyPageDto getMyPageData(Client client) {
        System.out.println("Client 객체 확인: " + client); // DB에서 넘어온 값
        ClientMyPageDto dto = new ClientMyPageDto();

        dto.setType(client.getClientCategory());
        dto.setCompany(client.getBusinessName());
        dto.setNumber(client.getBusinessNumber());
        dto.setCeo(client.getOwnerName());
        dto.setPhone(client.getPhone());
        dto.setUsername(client.getUsername());
        dto.setEmail(client.getEmail());
        dto.setAddress(client.getAddress());
        dto.setDetailAddress(client.getDetailAddress());
        dto.setBizFileName(client.getBusinessLicensePath() != null ?
                new File(client.getBusinessLicensePath()).getName() : null);

        System.out.println("ClientMyPageDto 확인: " + dto); // 서비스에서 만들어진 DTO

        return dto;
    }

    // 마이페이지 비밀번호 변경
    @Transactional
    public void updatePassword(Client client, PasswordDto update) {

        Client managedClient = clientRepository.findById(client.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("고객사 정보 없음"));

        if (!passwordEncoder.matches(update.getCurrentPassword(), managedClient.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (!update.getNewPassword().equals(update.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        managedClient.setPassword(passwordEncoder.encode(update.getNewPassword()));

        // 명시적 save (안 해도 되지만 가독성 + 안정성 ↑)
        clientRepository.save(managedClient);
    }
    // 마이페이지 수정
    @Transactional
    public void updateProfile(Client client, ClientMyPageUpdateDto dto) {

        Client managedClient = clientRepository.findById(client.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("고객사 정보 없음"));

        managedClient.setEmail(dto.getEmail());
        managedClient.setPhone(dto.getPhone());

        clientRepository.save(managedClient);
    }


}
package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.EventCheckinSubmissionResponse;
import com.classhub.classhubapi.entity.CheckinSubmissionStatus;
import com.classhub.classhubapi.entity.ClassMember;
import com.classhub.classhubapi.entity.Event;
import com.classhub.classhubapi.entity.EventCheckinSubmission;
import com.classhub.classhubapi.entity.EventParticipant;
import com.classhub.classhubapi.entity.NotificationTargetType;
import com.classhub.classhubapi.entity.NotificationType;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.ClassMemberRepository;
import com.classhub.classhubapi.repository.EventCheckinSubmissionRepository;
import com.classhub.classhubapi.repository.EventParticipantRepository;
import com.classhub.classhubapi.repository.EventRepository;
import com.classhub.classhubapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCheckinSubmissionService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventCheckinSubmissionRepository submissionRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Transactional
    public EventCheckinSubmissionResponse submit(Long eventId, Long userId, MultipartFile file) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Su kien khong ton tai"));
        authorizationService.requireMember(userId, event.getClassroom().getId());

        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BadRequestException("Ban chua dang ky su kien nay"));

        if (participant.isCheckedIn()) {
            throw new BadRequestException("Ban da duoc check-in su kien nay");
        }

        if (submissionRepository.existsByEventIdAndUserIdAndStatus(
                eventId, userId, CheckinSubmissionStatus.PENDING)) {
            throw new BadRequestException("Ban da gui anh diem danh dang cho duyet");
        }

        String imagePath = null;
        try {
            imagePath = fileStorageService.storeEventCheckinImage(eventId, userId, file);

            EventCheckinSubmission submission = EventCheckinSubmission.builder()
                    .event(event)
                    .user(participant.getUser())
                    .participant(participant)
                    .imagePath(imagePath)
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status(CheckinSubmissionStatus.PENDING)
                    .build();

            EventCheckinSubmission savedSubmission = submissionRepository.save(submission);
            createCheckinSubmittedNotification(savedSubmission, userId);
            return toResponse(savedSubmission);
        } catch (RuntimeException ex) {
            // Nếu DB save lỗi sau khi file đã ghi, xóa file để tránh mồ côi
            if (imagePath != null) {
                fileStorageService.deleteByRelativePathQuietly(imagePath);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<EventCheckinSubmissionResponse> getByEvent(Long eventId, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Su kien khong ton tai"));
        authorizationService.requireAdmin(adminId, event.getClassroom().getId());

        return submissionRepository.findByEventIdOrderBySubmittedAtDesc(eventId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EventCheckinSubmissionResponse approve(Long submissionId, Long adminId) {
        EventCheckinSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BadRequestException("Anh diem danh khong ton tai"));
        authorizationService.requireAdmin(adminId, submission.getEvent().getClassroom().getId());

        if (submission.getStatus() != CheckinSubmissionStatus.PENDING) {
            throw new BadRequestException("Chi co the duyet anh dang cho duyet");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BadRequestException("Admin khong ton tai"));
        LocalDateTime now = LocalDateTime.now();

        submission.setStatus(CheckinSubmissionStatus.APPROVED);
        submission.setReviewedBy(admin);
        submission.setReviewedAt(now);

        EventParticipant participant = submission.getParticipant();
        participant.setCheckedIn(true);
        participant.setCheckedInAt(now);
        participant.setCheckedBy(admin);
        eventParticipantRepository.save(participant);

        createCheckinReviewedNotification(
                submission,
                adminId,
                NotificationType.CHECKIN_APPROVED,
                "Điểm danh đã được duyệt",
                "Ảnh điểm danh của bạn cho sự kiện " + submission.getEvent().getTitle() + " đã được duyệt.");

        return toResponse(submission);
    }

    @Transactional
    public EventCheckinSubmissionResponse reject(Long submissionId, Long adminId, String reason) {
        EventCheckinSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BadRequestException("Anh diem danh khong ton tai"));
        authorizationService.requireAdmin(adminId, submission.getEvent().getClassroom().getId());

        if (submission.getStatus() != CheckinSubmissionStatus.PENDING) {
            throw new BadRequestException("Chi co the tu choi anh dang cho duyet");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new BadRequestException("Admin khong ton tai"));

        submission.setStatus(CheckinSubmissionStatus.REJECTED);
        submission.setReviewedBy(admin);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setRejectedReason(reason);

        createCheckinReviewedNotification(
                submission,
                adminId,
                NotificationType.CHECKIN_REJECTED,
                "Điểm danh bị từ chối",
                "Ảnh điểm danh của bạn cho sự kiện " + submission.getEvent().getTitle() + " đã bị từ chối. Vui lòng kiểm tra lại.");

        return toResponse(submission);
    }

    private void createCheckinSubmittedNotification(EventCheckinSubmission submission, Long userId) {
        Event event = submission.getEvent();
        Long classroomId = event.getClassroom().getId();
        List<Long> recipientUserIds = classMemberRepository.findByClassroomId(classroomId).stream()
                .filter(member -> member.getRole() == ClassMember.Role.ADMIN)
                .map(member -> member.getUser().getId())
                .filter(adminUserId -> !adminUserId.equals(userId))
                .collect(Collectors.toList());

        try {
            notificationService.createNotification(
                    classroomId,
                    NotificationType.CHECKIN_SUBMITTED,
                    "Có ảnh điểm danh chờ duyệt",
                    submission.getUser().getFullName() + " đã gửi ảnh điểm danh cho sự kiện: " + event.getTitle(),
                    NotificationTargetType.CHECKIN_SUBMISSION,
                    submission.getId(),
                    userId,
                    recipientUserIds);
        } catch (RuntimeException ex) {
            log.warn("Cannot create check-in submitted notification for submission {}", submission.getId(), ex);
        }
    }

    private void createCheckinReviewedNotification(
            EventCheckinSubmission submission,
            Long adminId,
            NotificationType type,
            String title,
            String message) {
        Long recipientUserId = submission.getUser().getId();
        if (recipientUserId.equals(adminId)) {
            return;
        }

        try {
            notificationService.createNotification(
                    submission.getEvent().getClassroom().getId(),
                    type,
                    title,
                    message,
                    NotificationTargetType.CHECKIN_SUBMISSION,
                    submission.getId(),
                    adminId,
                    List.of(recipientUserId));
        } catch (RuntimeException ex) {
            log.warn("Cannot create check-in reviewed notification for submission {}", submission.getId(), ex);
        }
    }

    private EventCheckinSubmissionResponse toResponse(EventCheckinSubmission submission) {
        User reviewedBy = submission.getReviewedBy();
        return EventCheckinSubmissionResponse.builder()
                .id(submission.getId())
                .eventId(submission.getEvent().getId())
                .userId(submission.getUser().getId())
                .fullName(submission.getUser().getFullName())
                .imageUrl(submission.getImagePath())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .reviewedByName(reviewedBy != null ? reviewedBy.getFullName() : null)
                .reviewedAt(submission.getReviewedAt())
                .rejectedReason(submission.getRejectedReason())
                .build();
    }
}

package com.classhub.classhubapi.service;

import com.classhub.classhubapi.dto.CreateEventRequest;
import com.classhub.classhubapi.dto.EventParticipantResponse;
import com.classhub.classhubapi.dto.EventResponse;
import com.classhub.classhubapi.entity.Event;
import com.classhub.classhubapi.entity.EventCheckinSubmission;
import com.classhub.classhubapi.entity.EventParticipant;
import com.classhub.classhubapi.entity.NotificationTargetType;
import com.classhub.classhubapi.entity.NotificationType;
import com.classhub.classhubapi.entity.User;
import com.classhub.classhubapi.exception.BadRequestException;
import com.classhub.classhubapi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventCheckinSubmissionRepository eventCheckinSubmissionRepository;
    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final AuthorizationService authorizationService;
    private final NotificationService notificationService;

    // === TẠO SỰ KIỆN === (Admin)
    @Transactional
    public EventResponse createEvent(CreateEventRequest request, Long userId) {
        authorizationService.requireAdmin(userId, request.getClassroomId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));
        var classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new BadRequestException("Lớp học không tồn tại"));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .eventTime(request.getEventTime())
                .classroom(classroom)
                .createdBy(user)
                .build();
        eventRepository.save(event);

        List<Long> recipientUserIds = classMemberRepository.findByClassroomId(classroom.getId()).stream()
                .map(member -> member.getUser().getId())
                .filter(memberUserId -> !memberUserId.equals(userId))
                .collect(Collectors.toList());
        notificationService.createNotification(
                classroom.getId(),
                NotificationType.EVENT_CREATED,
                "Có sự kiện mới",
                "Lớp " + classroom.getClassName() + " vừa tạo sự kiện: " + event.getTitle(),
                NotificationTargetType.EVENT,
                event.getId(),
                userId,
                recipientUserIds);

        return toEventResponse(event, 0, 0);
    }

    // === DANH SÁCH SỰ KIỆN CỦA LỚP ===
    public List<EventResponse> getEventsByClassroom(Long classroomId, Long userId) {
        authorizationService.requireMember(userId, classroomId);

        List<Event> events = eventRepository.findByClassroomIdOrderByEventTimeDesc(classroomId);
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());
        Map<Long, EventParticipantRepository.EventParticipantCount> countsByEventId =
                eventParticipantRepository.countByEventIds(eventIds).stream()
                        .collect(Collectors.toMap(
                                EventParticipantRepository.EventParticipantCount::getEventId,
                                count -> count));

        return events.stream()
                .map(event -> {
                    EventParticipantRepository.EventParticipantCount counts =
                            countsByEventId.get(event.getId());
                    int volunteerCount = counts != null ? Math.toIntExact(counts.getVolunteerCount()) : 0;
                    int checkedInCount = counts != null ? Math.toIntExact(counts.getCheckedInCount()) : 0;
                    return toEventResponse(event, volunteerCount, checkedInCount);
                })
                .collect(Collectors.toList());
    }

    // === ĐĂNG KÝ THAM GIA (Volunteer) ===
    @Transactional
    public EventParticipantResponse volunteer(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Sự kiện không tồn tại"));
        // B2: user phải thuộc lớp của sự kiện
        authorizationService.requireMember(userId, event.getClassroom().getId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User không tồn tại"));

        if (eventParticipantRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BadRequestException("Bạn đã đăng ký tham gia sự kiện này rồi");
        }

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .checkedIn(false)
                .build();
        eventParticipantRepository.save(participant);
        return toParticipantResponse(participant);
    }

    // === HỦY ĐĂNG KÝ ===
    @Transactional
    public void cancelVolunteer(Long eventId, Long userId) {
        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn chưa đăng ký sự kiện này"));

        if (participant.isCheckedIn()) {
            throw new BadRequestException("Không thể hủy đăng ký sau khi đã check-in");
        }

        eventParticipantRepository.delete(participant);
    }

    // === ADMIN XEM DANH SÁCH NGƯỜI ĐĂNG KÝ ===
    @Transactional(readOnly = true)
    public List<EventParticipantResponse> getParticipants(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Sự kiện không tồn tại"));
        authorizationService.requireAdmin(userId, event.getClassroom().getId());

        return eventParticipantRepository.findByEventId(eventId).stream()
                .map(participant -> {
                    EventCheckinSubmission latestSubmission = eventCheckinSubmissionRepository
                            .findTopByEventIdAndUserIdOrderBySubmittedAtDesc(
                                    eventId, participant.getUser().getId())
                            .orElse(null);
                    return toParticipantResponse(participant, latestSubmission);
                })
                .collect(Collectors.toList());
    }

    // === CHECK-IN === (B4)
    @Transactional
    public EventParticipantResponse checkIn(Long eventId, Long targetUserId, Long adminUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BadRequestException("Sự kiện không tồn tại"));
        // B2: admin phải thuộc lớp của sự kiện
        authorizationService.requireAdmin(adminUserId, event.getClassroom().getId());

        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, targetUserId)
                .orElseThrow(() -> new BadRequestException("Sinh viên chưa đăng ký sự kiện này"));

        if (participant.isCheckedIn()) {
            throw new BadRequestException("Sinh viên này đã được check-in rồi");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new BadRequestException("Admin không tồn tại"));

        participant.setCheckedIn(true);
        participant.setCheckedInAt(LocalDateTime.now());
        participant.setCheckedBy(admin); // B4: lưu ai check-in
        eventParticipantRepository.save(participant);

        return toParticipantResponse(participant);
    }

    // === SỰ KIỆN ĐÃ ĐĂNG KÝ CỦA BẢN THÂN ===
    @Transactional(readOnly = true)
    public List<EventParticipantResponse> getMyEvents(Long userId, Long classroomId) {
        authorizationService.requireMember(userId, classroomId);

        return eventParticipantRepository
                .findByUserIdAndEvent_ClassroomId(userId, classroomId)
                .stream()
                .map(p -> {
                    EventCheckinSubmission latestSubmission = eventCheckinSubmissionRepository
                            .findTopByEventIdAndUserIdOrderBySubmittedAtDesc(
                                    p.getEvent().getId(), userId)
                            .orElse(null);
                    return toParticipantResponse(p, latestSubmission);
                })
                .collect(Collectors.toList());
    }

    // === Helpers ===
    private EventResponse toEventResponse(Event event, int volunteerCount, int checkedInCount) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .eventTime(event.getEventTime())
                .createdByName(event.getCreatedBy().getFullName())
                .volunteerCount(volunteerCount)
                .checkedInCount(checkedInCount)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private EventParticipantResponse toParticipantResponse(EventParticipant p) {
        return toParticipantResponse(p, null);
    }

    private EventParticipantResponse toParticipantResponse(
            EventParticipant p,
            EventCheckinSubmission latestSubmission) {
        return EventParticipantResponse.builder()
                .id(p.getId())
                .eventId(p.getEvent().getId())                                 // bổ sung cho FE
                .userId(p.getUser().getId())
                .fullName(p.getUser().getFullName())
                .eventTitle(p.getEvent().getTitle())
                .checkedIn(p.isCheckedIn())
                .checkedInAt(p.getCheckedInAt())
                .checkedByName(p.getCheckedBy() != null                         // B4
                        ? p.getCheckedBy().getFullName() : null)
                .registeredAt(p.getRegisteredAt())
                .checkinSubmissionId(latestSubmission != null ? latestSubmission.getId() : null)
                .checkinSubmissionStatus(latestSubmission != null ? latestSubmission.getStatus() : null)
                .checkinImageUrl(latestSubmission != null ? latestSubmission.getImagePath() : null)
                .checkinSubmittedAt(latestSubmission != null ? latestSubmission.getSubmittedAt() : null)
                .build();
    }
}

package com.shopethethao.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.shopethethao.dto.UserHistoryDTO;
import com.shopethethao.modules.account.Account;
import com.shopethethao.modules.account.AccountDAO;
import com.shopethethao.modules.userHistory.UserActionType;
import com.shopethethao.modules.userHistory.UserHistory;
import com.shopethethao.modules.userHistory.UserHistoryDAO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserHistoryService {

    @Autowired
    private UserHistoryDAO userHistoryDAO;

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private UserHistorySSEService sseService;

    private static final int MAX_HISTORY_ITEMS = 100;

    @Transactional(rollbackFor = Exception.class)
    public void logUserAction(String userId, UserActionType actionType, String note, String ipAddress,
            String deviceInfo) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("Invalid user action logging attempt - userId: null or empty");
            throw new IllegalArgumentException("userId cannot be null or empty");
        }

        try {
            Account account = accountDAO.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User action logging failed - userId: {} not found", userId);
                        return new IllegalArgumentException("User not found: " + userId);
                    });

            UserHistory history = createUserHistory(account, actionType, note, normalizeIpAddress(ipAddress), deviceInfo);
            saveUserHistory(history);
            notifyClientsOfActivityChange(actionType);

            log.info("User action logged - userId: {}, action: {}, ip: {}", 
                userId, actionType, history.getIpAddress());
        } catch (Exception e) {
            log.error("User action logging failed - userId: {}, action: {}, error: {}", 
                userId, actionType, e.getMessage());
            throw new RuntimeException("Failed to log user action", e);
        }
    }

    private UserHistory createUserHistory(Account account, UserActionType actionType, 
            String note, String ipAddress, String deviceInfo) {
        UserHistory history = new UserHistory();
        history.setAccount(account);
        history.setActionType(actionType);
        history.setNote(note);
        history.setIpAddress(ipAddress);
        history.setDeviceInfo(deviceInfo != null ? deviceInfo : "unknown");
        history.setHistoryDateTime(LocalDateTime.now());
        history.setStatus(1);
        history.setReadStatus(0);
        return history;
    }

    private String normalizeIpAddress(String ipAddress) {
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return "127.0.0.1";
        }
        return ipAddress != null ? ipAddress : "unknown";
    }

    private void saveUserHistory(UserHistory history) {
        try {
            userHistoryDAO.save(history);
            log.debug("User history saved - userId: {}, action: {}", 
                history.getUserId(), history.getActionType());
        } catch (Exception e) {
            log.error("User history save failed - userId: {}, action: {}, error: {}", 
                history.getUserId(), history.getActionType(), e.getMessage());
            throw new RuntimeException("Failed to save user history", e);
        }
    }

    @Transactional(readOnly = true)
    private void notifyClientsOfActivityChange(UserActionType actionType) {
        try {
            if (actionType.isAuthAction()) {
                Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content",
                    getLatestAuthActivities());
                sseService.notifyAuthActivity(data);
                log.debug("Auth activity notification sent for action: {}", actionType);
            } 
            
            if (actionType.isAdminAction() || true) {
                Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content",
                    getLatestAdminActivities());
                sseService.notifyAdminActivity(data);
                log.debug("Admin activity notification sent for action: {}", actionType);
            }
        } catch (Exception e) {
            log.warn("Activity notification failed for action: {}, error: {}", 
                actionType, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<UserHistoryDTO> getLatestAuthActivities() {
        try {
            Pageable pageable = PageRequest.of(0, MAX_HISTORY_ITEMS, Sort.by(Sort.Direction.DESC, "historyDateTime"));
            return userHistoryDAO.findByActionTypeIn(Arrays.asList(
                    UserActionType.LOGIN,
                    UserActionType.LOGOUT,
                    UserActionType.LOGIN_FAILED,
                    UserActionType.RELOGIN,
                    UserActionType.SIGNUP,
                    UserActionType.CREATEACCOUNTFAILED), 
                    pageable)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching latest auth activities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<UserHistoryDTO> getLatestAdminActivities() {
        Pageable pageable = PageRequest.of(0, MAX_HISTORY_ITEMS, Sort.by(Sort.Direction.DESC, "historyDateTime"));
        return userHistoryDAO.findByActionTypeNotIn(Arrays.asList(
                UserActionType.LOGIN,
                UserActionType.LOGOUT,
                UserActionType.LOGIN_FAILED,
                UserActionType.RELOGIN,
                UserActionType.SIGNUP,
                UserActionType.CREATEACCOUNTFAILED),
                pageable)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }



    public void sendInitialAuthActivitiesToEmitter(SseEmitter emitter) {
        try {
            Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content", getLatestAuthActivities());
            emitter.send(SseEmitter.event()
                .name("AUTH_ACTIVITY")
                .data(data)
                .id(String.valueOf(System.currentTimeMillis())));
        } catch (IOException e) {
            log.error("Failed to send initial auth activities to emitter: {}", e.getMessage());
        }
    }

    public void sendInitialAdminActivitiesToEmitter(SseEmitter emitter) {
        try {
            Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content", getLatestAdminActivities());
            emitter.send(SseEmitter.event()
                .name("ADMIN_ACTIVITY")
                .data(data)
                .id(String.valueOf(System.currentTimeMillis())));
        } catch (IOException e) {
            log.error("Failed to send initial admin activities to emitter: {}", e.getMessage());
        }
    }

    public List<UserHistoryDTO> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return userHistoryDAO.findByDateRange(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<UserHistoryDTO> getHistoryWithFilters(
            UserActionType actionType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String userId,
            Pageable pageable) {

        return userHistoryDAO.findWithFilters(actionType, startDate, endDate, userId, pageable)
                .map(this::convertToDTO);
    }

    public List<UserHistoryDTO> getUserHistoryByDateRange(String userId, LocalDateTime startDate,
            LocalDateTime endDate) {
        return userHistoryDAO.findByDateRange(startDate, endDate).stream()
                .filter(history -> history.getUserId().equals(userId)) // Changed from history.getUser().getId()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Map<UserActionType, Long> getActionStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<UserHistory> histories = userHistoryDAO.findByDateRange(startDate, endDate);
        return histories.stream()
                .collect(Collectors.groupingBy(
                        UserHistory::getActionType,
                        Collectors.counting()));
    }

    @Transactional
    public void markNotificationsAsRead(String userId) {
        List<UserHistory> histories = userHistoryDAO.findByUserIdAndReadStatus(userId, 0);
        for (UserHistory history : histories) {
            history.setReadStatus(1);
        }
        userHistoryDAO.saveAll(histories);
        log.debug("Marked notifications as read for user {}", userId);
    }

    @Transactional
    public boolean markAsRead(Long historyId) {
        Optional<UserHistory> optionalHistory = userHistoryDAO.findById(historyId);
        if (optionalHistory.isPresent()) {
            UserHistory history = optionalHistory.get();
            history.setReadStatus(1);
            userHistoryDAO.save(history);
            log.debug("Marked notification {} as read", historyId);
            return true;
        }
        return false;
    }
    
    @Transactional
    public void markAllAuthAsRead() {
        List<UserActionType> authActionTypes = Arrays.asList(
            UserActionType.LOGIN,
            UserActionType.LOGOUT,
            UserActionType.LOGIN_FAILED,
            UserActionType.RELOGIN,
            UserActionType.SIGNUP,
            UserActionType.CREATEACCOUNTFAILED
        );
        
        List<UserHistory> histories = userHistoryDAO.findByActionTypeInAndReadStatus(authActionTypes, 0);
        for (UserHistory history : histories) {
            history.setReadStatus(1);
        }
        
        if (!histories.isEmpty()) {
            userHistoryDAO.saveAll(histories);
            log.debug("Marked {} auth notifications as read", histories.size());
            
            // Notify clients of the change
            Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content", getLatestAuthActivities());
            sseService.notifyAuthActivity(data);
        }
    }
    
    @Transactional
    public void markAllAdminAsRead() {
        List<UserActionType> authActionTypes = Arrays.asList(
            UserActionType.LOGIN,
            UserActionType.LOGOUT,
            UserActionType.LOGIN_FAILED,
            UserActionType.RELOGIN,
            UserActionType.SIGNUP,
            UserActionType.CREATEACCOUNTFAILED
        );
        
        List<UserHistory> histories = userHistoryDAO.findByActionTypeNotInAndReadStatus(authActionTypes, 0);
        for (UserHistory history : histories) {
            history.setReadStatus(1);
        }
        
        if (!histories.isEmpty()) {
            userHistoryDAO.saveAll(histories);
            log.debug("Marked {} admin notifications as read", histories.size());
            
            // Notify clients of the change
            Map<String, List<UserHistoryDTO>> data = Collections.singletonMap("content", getLatestAdminActivities());
            sseService.notifyAdminActivity(data);
        }
    }
    
    public Map<String, Integer> getUnreadCounts() {
        Map<String, Integer> counts = new HashMap<>();
        
        // Auth action types
        List<UserActionType> authActionTypes = Arrays.asList(
            UserActionType.LOGIN,
            UserActionType.LOGOUT,
            UserActionType.LOGIN_FAILED,
            UserActionType.RELOGIN,
            UserActionType.SIGNUP,
            UserActionType.CREATEACCOUNTFAILED
        );
        
        int authCount = userHistoryDAO.countByActionTypeInAndReadStatus(authActionTypes, 0);
        int adminCount = userHistoryDAO.countByActionTypeNotInAndReadStatus(authActionTypes, 0);
        
        counts.put("authCount", authCount);
        counts.put("adminCount", adminCount);
        counts.put("totalCount", authCount + adminCount);
        
        return counts;
    }

    private UserHistoryDTO convertToDTO(UserHistory history) {
        if (history == null) {
            return null;
        }

        UserHistoryDTO dto = new UserHistoryDTO();
        dto.setIdHistory(history.getIdHistory());
        dto.setUserId(history.getUserId());
        dto.setUsername(history.getUsername()); 
        dto.setUserRole(history.getUserRole()); 
        dto.setActionType(history.getActionType());
        dto.setNote(history.getNote());
        dto.setIpAddress(history.getIpAddress());
        dto.setDeviceInfo(history.getDeviceInfo());
        dto.setHistoryDateTime(history.getHistoryDateTime());
        dto.setStatus(history.getStatus());
        dto.setReadStatus(history.getReadStatus());

        return dto;
    }
}

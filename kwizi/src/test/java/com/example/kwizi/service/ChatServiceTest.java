package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.ChatService.ChatOperationNotAllowedException;
import com.example.kwizi.exception.ChatService.DuplicateChatMemberException;
import com.example.kwizi.exception.ChatService.NotGroupChatException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.GroupChat;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatRepository chatRepository;
    @Mock private ChatMemberRepository chatMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SystemMessageService systemMessageService;
    @Mock private NotificationService notificationService;
    @Mock private FileStorageService fileStorageService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private ChatService chatService;

    private User creator;
    private User member1;
    private User member2;
    private Chat groupChat;
    private Chat privateChat;
    private ChatMember creatorMember;
    private ChatMember adminMember;
    private ChatMember regularMember;

    @BeforeEach
    void setUp() {
        creator = createUser(1L, "creator");
        member1 = createUser(2L, "member1");
        member2 = createUser(3L, "member2");

        groupChat = createGroupChat(100L);
        privateChat = createPrivateChat(200L);

        creatorMember = createChatMember(100L, 1L, creator, ChatRole.OWNER);
        adminMember = createChatMember(100L, 2L, member1, ChatRole.ADMIN);
        regularMember = createChatMember(100L, 3L, member2, ChatRole.MEMBER);
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Chat createGroupChat(Long id) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setChatType(ChatType.GROUP);
        chat.setCreatedAt(OffsetDateTime.now());
        chat.setLastActivityAt(OffsetDateTime.now());

        GroupChat groupChatDetails = new GroupChat();
        groupChatDetails.setGroupName("Test Group");
        chat.setGroupChat(groupChatDetails);

        return chat;
    }

    private Chat createPrivateChat(Long id) {
        Chat chat = new Chat();
        chat.setId(id);
        chat.setChatType(ChatType.PRIVATE);
        return chat;
    }

    private ChatMember createChatMember(Long chatId, Long userId, User user, ChatRole role) {
        ChatMember member = new ChatMember();
        member.setId(new ChatMember.ChatMemberId(chatId, userId));
        member.setChat(groupChat);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    @Test
    void createGroupChat_MemberNotFound_ThrowsException() {
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setGroupName("Test Group");
        request.setInitialMemberIds(Arrays.asList(2L, 999L));

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(chatRepository.save(any(Chat.class))).thenReturn(groupChat);
        when(userRepository.findAllById(Arrays.asList(2L, 999L))).thenReturn(Collections.singletonList(member1));

        assertThrows(UserNotFoundException.class, () ->
                chatService.createGroupChat(request, "creator"));
    }

    @Test
    void createGroupChat_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setGroupName("Test");

        assertThrows(UserNotFoundException.class, () ->
                chatService.createGroupChat(request, "unknown"));
    }

    @Test
    void createGroupChat_DuplicateMembers_ThrowsException() {
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setGroupName("Test");
        request.setInitialMemberIds(Arrays.asList(2L, 2L));

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        assertThrows(DuplicateChatMemberException.class, () ->
                chatService.createGroupChat(request, "creator"));
    }

    @Test
    void createPrivateChat_Success() {
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("member1");

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("member1")).thenReturn(Optional.of(member1));
        when(chatRepository.save(any(Chat.class))).thenReturn(privateChat);
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.empty());
        when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));

        chatService.createPrivateChat(request, "creator");

        verify(chatRepository, times(2)).save(any(Chat.class));
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
    }

    @Test
    void createPrivateChat_ChatWithSelf_ThrowsException() {
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("creator");

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        assertThrows(ChatOperationNotAllowedException.class, () ->
                chatService.createPrivateChat(request, "creator"));
    }

    @Test
    void createPrivateChat_AlreadyExists_ThrowsException() {
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("member1");

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("member1")).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.of(300L));

        assertThrows(DuplicateChatMemberException.class, () ->
                chatService.createPrivateChat(request, "creator"));
    }

    @Test
    void addChatMember_Success() {
        AddChatMemberRequestDto request = new AddChatMemberRequestDto();
        request.setChatId(100L);
        request.setUserId(3L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(chatMemberRepository.existsByChatIdAndUserId(100L, 3L)).thenReturn(false);

        chatService.addChatMember(request, "creator");

        verify(chatMemberRepository).save(any());
        verify(systemMessageService).createUserAddedMessage(any(), any(), any());
    }

    @Test
    void addChatMember_ChatNotFound_ThrowsException() {
        when(chatRepository.findById(999L)).thenReturn(Optional.empty());
        AddChatMemberRequestDto request = new AddChatMemberRequestDto();
        request.setChatId(999L);

        assertThrows(ChatNotFoundException.class, () ->
                chatService.addChatMember(request, "creator"));
    }

    @Test
    void addChatMember_AlreadyInChat_ThrowsException() {
        AddChatMemberRequestDto request = new AddChatMemberRequestDto();
        request.setChatId(100L);
        request.setUserId(2L);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.existsByChatIdAndUserId(100L, 2L)).thenReturn(true);

        assertThrows(DuplicateChatMemberException.class, () ->
                chatService.addChatMember(request, "creator"));
    }

    @Test
    void addChatMember_PrivateChat_ThrowsException() {
        AddChatMemberRequestDto request = new AddChatMemberRequestDto();
        request.setChatId(200L);
        request.setUserId(3L);

        when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));

        assertThrows(ChatOperationNotAllowedException.class, () ->
                chatService.addChatMember(request, "creator"));
    }

    @Test
    void setAdmin_Success() {
        Long chatId = 100L;
        ChatMember regularMember = createChatMember(chatId, 2L, member1, ChatRole.MEMBER);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 1L)).thenReturn(Optional.of(creatorMember));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 2L)).thenReturn(Optional.of(regularMember));

        chatService.setAdmin(chatId, 2L, 1L);

        assertEquals(ChatRole.ADMIN, regularMember.getRole());
        verify(systemMessageService).createUserPromotedMessage(any(), any(), any());
        verify(notificationService).notifyUserPromoted(any(), any(), any());
    }

    @Test
    void setAdmin_UserAlreadyAdmin_ThrowsException() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorMember));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(adminMember));

        assertThrows(BusinessLogicException.class, () ->
                chatService.setAdmin(100L, 2L, 1L));
    }

    @Test
    void setAdmin_NotOwner_ThrowsException() {
        ChatMember requester = createChatMember(100L, 2L, member1, ChatRole.MEMBER);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(requester));

        assertThrows(AccessDeniedException.class, () ->
                chatService.setAdmin(100L, 3L, 2L));
    }

    @Test
    void demoteAdminToMember_Success() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorMember));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(adminMember));

        chatService.demoteAdminToMember(100L, 2L, 1L);

        verify(chatMemberRepository).save(argThat(member -> member.getRole() == ChatRole.MEMBER));
    }

    @Test
    void demoteAdminToMember_PrivateChat_ThrowsException() {
        User requester = createUser(1L, "requester");
        User target = createUser(2L, "target");

        when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(ChatOperationNotAllowedException.class, () ->
                chatService.demoteAdminToMember(200L, 2L, 1L));
    }

    @Test
    void demoteAdminToMember_NotOwner_ThrowsException() {
        ChatMember requester = createChatMember(100L, 2L, member1, ChatRole.ADMIN);
        ChatMember target = createChatMember(100L, 3L, member2, ChatRole.ADMIN);

        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(requester));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class, () ->
                chatService.demoteAdminToMember(100L, 3L, 2L));
    }

    @Test
    void removeChatMember_OwnerRemovesMember_Success() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorMember));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(regularMember));

        chatService.removeChatMember(100L, 3L, 1L);

        verify(chatMemberRepository).delete(regularMember);
    }

    @Test
    void removeChatMember_OwnerRemovesSelf_ThrowsException() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorMember));

        assertThrows(BusinessLogicException.class, () ->
                chatService.removeChatMember(100L, 1L, 1L));
    }

    @Test
    void removeChatMember_AdminRemovesMember_Success() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 2L)).thenReturn(Optional.of(adminMember));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(regularMember));

        chatService.removeChatMember(100L, 3L, 2L);

        verify(chatMemberRepository).delete(regularMember);
    }

    @Test
    void removeChatMember_MemberTriesToRemove_ThrowsException() {
        Long chatId = 100L;
        ChatMember requester = createChatMember(chatId, 3L, member2, ChatRole.MEMBER);
        ChatMember target = createChatMember(chatId, 2L, member1, ChatRole.MEMBER);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(3L)).thenReturn(Optional.of(member2));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 3L)).thenReturn(Optional.of(requester));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 2L)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class, () ->
                chatService.removeChatMember(chatId, 2L, 3L));
    }

    @Test
    void leaveChat_Success() {
        Long chatId = 100L;
        ChatMember memberToLeave = createChatMember(chatId, 2L, member1, ChatRole.MEMBER);
        ChatMember owner = createChatMember(chatId, 1L, creator, ChatRole.OWNER);

        Set<ChatMember> chatMembers = new HashSet<>(Arrays.asList(owner, memberToLeave));
        groupChat.setChatMembers(chatMembers);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member1));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 2L)).thenReturn(Optional.of(memberToLeave));

        chatService.leaveChat(chatId, 2L);

        verify(chatMemberRepository).delete(memberToLeave);
        verify(systemMessageService).createUserLeftMessage(any(), any());
    }

    @Test
    void leaveChat_OwnerTriesToLeave_ThrowsException() {
        Long chatId = 100L;
        ChatMember owner = createChatMember(chatId, 1L, creator, ChatRole.OWNER);
        Set<ChatMember> chatMembers = new HashSet<>(Collections.singletonList(owner));
        groupChat.setChatMembers(chatMembers);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 1L)).thenReturn(Optional.of(owner));

        assertThrows(BusinessLogicException.class, () ->
                chatService.leaveChat(chatId, 1L));
    }

    @Test
    void leaveChat_PrivateChat_ThrowsException() {
        when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        assertThrows(NotGroupChatException.class, () ->
                chatService.leaveChat(200L, 1L));
    }

    @Test
    void updateGroupName_Success() {
        Long chatId = 100L;
        String newName = "New Group Name";

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 1L)).thenReturn(Optional.of(creatorMember));

        chatService.updateGroupName(chatId, newName, 1L);

        assertEquals(newName, groupChat.getGroupChat().getGroupName());
        verify(systemMessageService).createGroupNameChangedMessage(any(), any(), any(), any());
        verify(notificationService).notifyGroupNameChanged(any(), any(), any(), any());
        verify(chatRepository, times(2)).save(groupChat);
    }

    @Test
    void updateGroupName_PrivateChat_ThrowsException() {
        when(chatRepository.findById(200L)).thenReturn(Optional.of(privateChat));

        assertThrows(BusinessLogicException.class, () ->
                chatService.updateGroupName(200L, "New Name", 1L));
    }

    @Test
    void updateGroupName_RegularMember_ThrowsException() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 3L)).thenReturn(Optional.of(regularMember));

        assertThrows(AccessDeniedException.class, () ->
                chatService.updateGroupName(100L, "New Name", 3L));
    }

    @Test
    void updateChatAvatar_Success() {
        Long chatId = 100L;

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 1L)).thenReturn(Optional.of(creatorMember));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(fileStorageService.saveChatAvatar(multipartFile, chatId)).thenReturn("avatar-url");

        chatService.updateChatAvatar(chatId, multipartFile, 1L);

        assertEquals("avatar-url", groupChat.getGroupChat().getAvatarUrl());
        verify(systemMessageService).createGroupPhotoChangedMessage(any(), any());
        verify(notificationService).notifyGroupPhotoChanged(any(), any());
        verify(chatRepository, times(2)).save(groupChat);
    }

    @Test
    void updateChatAvatar_EmptyFile_ThrowsException() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));
        when(chatMemberRepository.findByChatIdAndUserId(100L, 1L)).thenReturn(Optional.of(creatorMember));
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThrows(BusinessLogicException.class, () ->
                chatService.updateChatAvatar(100L, multipartFile, 1L));
    }

    @Test
    void updateChatAvatar_PrivateChat_ThrowsException() {
        Long chatId = 200L;
        ChatMember member = createChatMember(chatId, 1L, creator, ChatRole.MEMBER);
        Set<ChatMember> members = new HashSet<>(Collections.singletonList(member));
        privateChat.setChatMembers(members);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(privateChat));
        when(chatMemberRepository.findByChatIdAndUserId(chatId, 1L)).thenReturn(Optional.of(member));

        assertThrows(BusinessLogicException.class, () ->
                chatService.updateChatAvatar(chatId, multipartFile, 1L));
    }

    @Test
    void updateChatActivity_Success() {
        when(chatRepository.findById(100L)).thenReturn(Optional.of(groupChat));

        chatService.updateChatActivity(100L);

        verify(chatRepository).save(groupChat);
        assertNotNull(groupChat.getLastActivityAt());
    }

    @Test
    void updateChatActivity_ChatNotFound_ThrowsException() {
        when(chatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ChatNotFoundException.class, () ->
                chatService.updateChatActivity(999L));
    }
}
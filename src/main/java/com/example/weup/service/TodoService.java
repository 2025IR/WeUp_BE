package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.TodoAssigneeResponseDTO;
import com.example.weup.dto.response.TodoListResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.TodoMemberRepository;
import com.example.weup.repository.TodoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final MemberService memberService;
    private final S3Service s3Service;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final TodoRepository todoRepository;
    private final TodoMemberRepository todoMemberRepository;

    @Transactional
    public Map<String, Object> createTodo(Long userId,
                                          Long projectId,
                                          List<Long> memberIds,
                                          String todoName,
                                          LocalDate startDate,
                                          LocalDate endDate) {

        Member requestMember = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (!memberService.hasAccess(userId, projectId) || memberService.isDeletedMember(requestMember.getMemberId())) {
            log.error("프로젝트 소속이 아니거나, 삭제된 멤버입니다.");
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다."));

        Todo todo = Todo.builder()
                .project(project)
                .todoName(todoName)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        todoRepository.save(todo);

        for (Long memberId : memberIds) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다: " + memberId));

            TodoMember todoMember = TodoMember.builder()
                    .todo(todo)
                    .member(member)
                    .build();

            todoMemberRepository.save(todoMember);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todoId", todo.getTodoId());
        result.put("assignedMemberCount", memberIds.size());

        return result;
    }

    public List<TodoListResponseDTO> getTodoList(Long userId, Long projectId) {

        Member requestMember = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (!memberService.hasAccess(userId, projectId) || memberService.isDeletedMember(requestMember.getMemberId())) {
            log.error("프로젝트 소속이 아니거나, 삭제된 멤버입니다.");
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        List<Todo> todos = todoRepository.findByProject_ProjectId(projectId);

        return todos.stream()
                .map(todo -> {
                    List<TodoAssigneeResponseDTO> assignees = todo.getTodoMembers().stream()
                            .map(tm -> {
                                Member member = tm.getMember();
                                User user = member.getUser();

                                return TodoAssigneeResponseDTO.builder()
                                        .memberId(member.getMemberId())
                                        .name(user.getName())
                                        .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                                        .build();
                            })
                            .collect(Collectors.toList());

                    boolean isMyTodo = assignees.stream()
                            .anyMatch(a -> a.getMemberId().equals(requestMember.getMemberId()));

                    Optional<TodoAssigneeResponseDTO> requesterDtoOpt = assignees.stream()
                            .filter(a -> a.getMemberId().equals(requestMember.getMemberId()))
                            .findFirst();

                    requesterDtoOpt.ifPresent(requesterDto -> {
                        assignees.remove(requesterDto);
                        assignees.add(0, requesterDto);
                    });

                    return TodoListResponseDTO.builder()
                            .todoId(todo.getTodoId())
                            .todoName(todo.getTodoName())
                            .startDate(todo.getStartDate())
                            .endDate(todo.getEndDate())
                            .status(todo.getTodoStatus())
                            .isMyTodo(isMyTodo)
                            .assignee(assignees)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> editTodo(Long userId,
                                        Long projectId,
                                        Long todoId,
                                        List<Long> memberIds,
                                        String todoName,
                                        LocalDate startDate,
                                        LocalDate endDate) {

        Member requestMember = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (!memberService.hasAccess(userId, projectId) || memberService.isDeletedMember(requestMember.getMemberId())) {
            log.error("프로젝트 소속이 아니거나, 삭제된 멤버입니다.");
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("투두를 찾을 수 없습니다."));

        if (!todo.getProject().getProjectId().equals(projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        todo.setTodoName(todoName);
        todo.setStartDate(startDate);
        todo.setEndDate(endDate);

        todoMemberRepository.deleteByTodo_TodoId(todoId);

        for (Long memberId : memberIds) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다: " + memberId));

            TodoMember todoMember = TodoMember.builder()
                    .todo(todo)
                    .member(member)
                    .build();

            todoMemberRepository.save(todoMember);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todoId", todo.getTodoId());

        return result;
    }

    @Transactional
    public Map<String, Object> editTodoStatus(Long userId,
                                                Long projectId,
                                                Long todoId,
                                                Byte status) {

        Member requestMember = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (!memberService.hasAccess(userId, projectId) || memberService.isDeletedMember(requestMember.getMemberId())) {
            log.error("프로젝트 소속이 아니거나, 삭제된 멤버입니다.");
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 투두를 찾을 수 없습니다."));

        if (!todo.getProject().getProjectId().equals(projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        todo.setTodoStatus(status);
        todoRepository.save(todo);

        Map<String, Object> result = new HashMap<>();
        result.put("todoId", todo.getTodoId());
        result.put("status", status);

        return result;
    }

    @Transactional
    public void deleteTodo(Long userId, Long projectId, Long todoId) {

        Member requestMember = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (!memberService.hasAccess(userId, projectId) || memberService.isDeletedMember(requestMember.getMemberId())) {
            log.error("프로젝트 소속이 아니거나, 삭제된 멤버입니다.");
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new IllegalArgumentException("해당 투두를 찾을 수 없습니다."));

        if (!todo.getProject().getProjectId().equals(projectId)) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        todoMemberRepository.deleteAllByTodo_TodoId(todoId);

        todoRepository.deleteById(todoId);
    }
}
